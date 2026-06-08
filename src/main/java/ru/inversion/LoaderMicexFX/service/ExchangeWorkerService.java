package ru.inversion.LoaderMicexFX.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.gateway.SimpleGatewayClient;
import ru.inversion.LoaderMicexFX.model.ClientStatus;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;
import ru.inversion.LoaderMicexFX.monitoring.LoaderMetricsService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExchangeWorkerService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeWorkerService.class);

    private final SimpleGatewayClient gatewayClient;
    private final BufferConfigService bufferConfigService;
    private final MicexLoaderService micexLoaderService;
    private final LoaderBufferControlService bufferControl;
    private final TesystimeSyncService tesystimeSyncService;
    private final LoaderMetricsService loaderMetrics;
    private final ClientStatus status = new ClientStatus();

    @Value("${app.monitor.rows-per-table:40}")
    private int monitorRowsPerTable;

    public ExchangeWorkerService(
            SimpleGatewayClient gatewayClient,
            BufferConfigService bufferConfigService,
            MicexLoaderService micexLoaderService,
            LoaderBufferControlService bufferControl,
            TesystimeSyncService tesystimeSyncService,
            LoaderMetricsService loaderMetrics) {
        this.gatewayClient = gatewayClient;
        this.bufferConfigService = bufferConfigService;
        this.micexLoaderService = micexLoaderService;
        this.bufferControl = bufferControl;
        this.tesystimeSyncService = tesystimeSyncService;
        this.loaderMetrics = loaderMetrics;
    }

    public synchronized void startApi() {
        if (gatewayClient.isConnected()) {
            status.setLastError("Уже подключено");
            return;
        }
        status.setLastError(null);
        status.clearMonitorBuffers();
        status.setReconnectCount(0);

        try {
            bufferConfigService.reload();
            gatewayClient.connect();

            status.setConnected(true);
            status.setOpenedTables(new ArrayList<>(gatewayClient.getOpenedTables()));
            status.setFailedTables(new ArrayList<>(gatewayClient.getFailedTables()));
            status.setStartedAt(Instant.now());

            micexLoaderService.initAfterConnect(status);
            if (bufferControl.hasAnyBufferStarted()) {
                processMicexRows(gatewayClient.drainMicexTableRows());
            } else {
                gatewayClient.drainMicexTableRows();
            }
        } catch (Exception e) {
            gatewayClient.disconnect();
            micexLoaderService.clear();
            status.setConnected(false);
            status.setLastError(formatError(e));
        } finally {
            publishMetrics();
        }
    }

    public synchronized void stopApi() {
        gatewayClient.disconnect();
        bufferControl.clearAll();
        tesystimeSyncService.reset();
        micexLoaderService.clear();
        clearSessionStatus();
        publishMetrics();
    }

    private void clearSessionStatus() {
        status.setConnected(false);
        status.setLastError(null);
        status.clearMonitorBuffers();
        status.setOpenedTables(List.of());
        status.setFailedTables(List.of());
        status.setBufferStatuses(List.of());
    }

    @Scheduled(fixedDelayString = "${app.poll-ms:3000}")
    public synchronized void poll() {
        try {
            if (!gatewayClient.isConnected()) {
                return;
            }
            tesystimeSyncService.onMasterTick();
            gatewayClient.refreshTables();
            processMicexRows(gatewayClient.drainMicexTableRows());
        } catch (Exception e) {
            status.setLastError(formatError(e));
            if (micexLoaderService.tryReconnect(status)) {
                try {
                    gatewayClient.refreshTables();
                    processMicexRows(gatewayClient.drainMicexTableRows());
                } catch (Exception e2) {
                    status.setLastError(formatError(e2));
                    gatewayClient.disconnect();
                    micexLoaderService.clear();
                    status.setConnected(false);
                }
            } else {
                gatewayClient.disconnect();
                micexLoaderService.clear();
                status.setConnected(false);
            }
        } finally {
            publishMetrics();
        }
    }

    public synchronized void runSaveCycle() {
        if (!gatewayClient.isConnected()) {
            return;
        }
        try {
            gatewayClient.refreshTables(true);
            processMicexRows(gatewayClient.drainMicexTableRows());
        } catch (Exception e) {
            status.setLastError(formatError(e));
        } finally {
            publishMetrics();
        }
    }

    private void processMicexRows(List<MicexTableRow> rows) {
        if (rows == null || rows.isEmpty()) {
            if (bufferControl.hasAnyBufferStarted()) {
                micexLoaderService.syncBufferStatus(status);
            }
            return;
        }
        tesystimeSyncService.processRows(rows);
        int saved = micexLoaderService.saveRowsWithIntervals(rows, status);
        if (saved > 0) {
            status.setLastUpdateAt(Instant.now());
            status.setTotalMessages(status.getTotalMessages() + saved);
        }
        for (MicexTableRow row : rows) {
            status.pushEventByTable(MicexRowWebMapper.toMarketData(row), monitorRowsPerTable);
        }
    }

    private static String formatError(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable t = e;
        while (t != null) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            String m = t.getMessage();
            if (m != null && !m.isBlank()) {
                sb.append(m);
            } else {
                sb.append(t.getClass().getSimpleName());
            }
            t = t.getCause();
            if (sb.length() > 500) {
                sb.append("...");
                break;
            }
        }
        return sb.toString();
    }

    private void publishMetrics() {
        loaderMetrics.refreshStatus(status);
    }

    public synchronized ClientStatus getStatus() {
        return status;
    }

    @PreDestroy
    public void onShutdown() {
        gatewayClient.disconnect();
    }
}
