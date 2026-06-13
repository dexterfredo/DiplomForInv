package ru.inversion.LoaderMicexFX.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.db.BoardFilterRepository;
import ru.inversion.LoaderMicexFX.db.BoardSectionRepository;
import ru.inversion.LoaderMicexFX.db.DbSaveDiagnosticsService;
import ru.inversion.LoaderMicexFX.db.DbSaveOutcome;
import ru.inversion.LoaderMicexFX.db.MicexRowRepository;
import ru.inversion.LoaderMicexFX.db.SecurityFilterRepository;
import ru.inversion.LoaderMicexFX.gateway.SimpleGatewayClient;
import ru.inversion.LoaderMicexFX.loader.MicexBuffer;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.BufferStatusInfo;
import ru.inversion.LoaderMicexFX.model.ClientStatus;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MicexLoaderService {

    private static final Logger log = LoggerFactory.getLogger(MicexLoaderService.class);

    private final SimpleGatewayClient gatewayClient;
    private final BufferConfigService bufferConfigService;
    private final MicexRowRepository micexRowRepository;
    private final BoardFilterRepository boardFilterRepository;
    private final BoardSectionRepository boardSectionRepository;
    private final SecurityFilterRepository securityFilterRepository;
    private final LoaderConstantsService loaderConstants;
    private final LoaderBufferControlService bufferControl;
    private final LoaderTimerPreferences timerPreferences;
    private final DbSaveDiagnosticsService dbDiagnostics;

    @Value("${app.loader.reconnect-on-error:true}")
    private boolean reconnectOnError;

    @Value("${app.loader.reconnect-max-attempts:2}")
    private int reconnectMaxAttempts;

    private final List<MicexBuffer> runtimeBuffers = new ArrayList<>();
    private final Set<String> openedTableSet = new HashSet<>();

    public MicexLoaderService(
            SimpleGatewayClient gatewayClient,
            BufferConfigService bufferConfigService,
            MicexRowRepository micexRowRepository,
            BoardFilterRepository boardFilterRepository,
            BoardSectionRepository boardSectionRepository,
            SecurityFilterRepository securityFilterRepository,
            LoaderConstantsService loaderConstants,
            LoaderBufferControlService bufferControl,
            LoaderTimerPreferences timerPreferences,
            DbSaveDiagnosticsService dbDiagnostics) {
        this.gatewayClient = gatewayClient;
        this.bufferConfigService = bufferConfigService;
        this.micexRowRepository = micexRowRepository;
        this.boardFilterRepository = boardFilterRepository;
        this.boardSectionRepository = boardSectionRepository;
        this.securityFilterRepository = securityFilterRepository;
        this.loaderConstants = loaderConstants;
        this.bufferControl = bufferControl;
        this.timerPreferences = timerPreferences;
        this.dbDiagnostics = dbDiagnostics;
    }

    public void initAfterConnect(ClientStatus status) {
        runtimeBuffers.clear();
        openedTableSet.clear();
        for (String t : gatewayClient.getOpenedTables()) {
            if (t != null) {
                openedTableSet.add(t.toUpperCase());
            }
        }

        for (BufferConfig cfg : bufferConfigService.getActiveBuffers()) {
            String micexTable = cfg.getMicexTable();
            if (micexTable != null && openedTableSet.contains(micexTable.toUpperCase())) {
                runtimeBuffers.add(new MicexBuffer(cfg));
            }
        }
        if (runtimeBuffers.isEmpty() && !bufferConfigService.getActiveBuffers().isEmpty()) {
            log.warn("No overlap open-tables {} vs DB buffers — write all active buffers",
                    openedTableSet);
            for (BufferConfig cfg : bufferConfigService.getActiveBuffers()) {
                if (cfg.getMicexTable() != null && !cfg.getMicexTable().isBlank()) {
                    runtimeBuffers.add(new MicexBuffer(cfg));
                }
            }
        }
        log.info("DB write buffers registered: {}", runtimeBuffers.size());
        boardSectionRepository.reload();
        syncBufferStatus(status);
    }

    public void clear() {
        runtimeBuffers.clear();
        openedTableSet.clear();
    }

    public void prepareBuffersStarted(int... typeBuffs) {
        if (typeBuffs == null || typeBuffs.length == 0) {
            return;
        }
        Set<Integer> set = new HashSet<>();
        for (int t : typeBuffs) {
            set.add(t);
        }
        for (MicexBuffer b : runtimeBuffers) {
            if (set.contains(b.getTypeBuff())) {
                b.resetAfterReconnect();
            }
        }
    }

    public void syncBufferStatus(ClientStatus status) {
        syncBufferStatusInternal(status);
    }

    public int saveRowsWithIntervals(List<MicexTableRow> allRows, ClientStatus status) {
        if (allRows == null || allRows.isEmpty()) {
            syncBufferStatusInternal(status);
            syncDbDiagnostics(status);
            return 0;
        }

        Map<String, List<MicexTableRow>> byTable = new HashMap<>();
        for (MicexTableRow row : allRows) {
            if (row.getTableName() == null) {
                continue;
            }
            String key = row.getTableName().toUpperCase();
            byTable.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        int saved = 0;
        if (runtimeBuffers.isEmpty()) {
            return saveRowsDirect(byTable, status);
        }
        for (MicexBuffer buff : runtimeBuffers) {
            String table = buff.getMicexTable().toUpperCase();
            List<MicexTableRow> incoming = byTable.get(table);
            if (incoming != null && !incoming.isEmpty()) {
                buff.accumulateRows(incoming);
            }

            if (!bufferControl.isBufferStarted(buff.getTypeBuff())) {
                if (incoming != null && !incoming.isEmpty()) {
                    dbDiagnostics.recordBufferNotStarted(buff.getTypeBuff());
                }
                continue;
            }

            if (!buff.hasPending()) {
                continue;
            }

            BufferConfig buffCfg = buff.getConfig();
            int intervalSec = timerPreferences.getIntervalSecFor(buffCfg);
            boolean quoteBuff = buffCfg.isQuoteBuffer();
            boolean snapshotBatch = buff.pendingHasSnapshot();

            if (!snapshotBatch && !buff.shouldRefreshCycle(intervalSec, quoteBuff)) {
                dbDiagnostics.recordWaitingTimer(buff.getTypeBuff(), intervalSec);
                continue;
            }

            if (!snapshotBatch && !buff.shouldSaveToDb(intervalSec)) {
                buff.completeRefreshCycle(intervalSec, quoteBuff);
                continue;
            }

            List<MicexTableRow> chunk = buff.drainPending();

            int n;
            int skipped = 0;
            if (buffCfg.isMultiLegDealSave()) {
                n = micexRowRepository.saveDealFxChunk(
                        buffCfg,
                        chunk,
                        row -> passBoardFilter(row) && passSecurityFilter(row, buff.getConfig()));
            } else {
                n = 0;
                int notPersisted = 0;
                for (MicexTableRow row : chunk) {
                    if (!passExchangeFilters(row, buff.getConfig())) {
                        skipped++;
                        continue;
                    }
                    for (SaveTarget target : resolveSaveTargets(buff.getConfig(), row)) {
                        MicexTableRow saveRow = target.remap() ? row.copyForRemap() : row;
                        DbSaveOutcome outcome = micexRowRepository.save(
                                target.config(), saveRow, target.typeSection());
                        dbDiagnostics.recordOutcome(
                                target.config().getTypeBuff(),
                                target.config().getQualifiedProcedure(),
                                outcome);
                        if (outcome.saved()) {
                            n++;
                        } else {
                            notPersisted++;
                        }
                    }
                }
                if (n == 0 && notPersisted > 0 && skipped == 0) {
                    log.warn("DB saved: {} — 0 rows из {} (см. logs/errors.log, skip: {})",
                            formatBufferRoute(buff.getConfig(), table),
                            chunk.size(),
                            dbDiagnostics.getSkipSummary());
                }
            }
            buff.completeRefreshCycle(intervalSec, quoteBuff);
            if (n > 0) {
                buff.addRowsSaved(n);
                saved += n;
                log.info("DB saved: {} — {} rows (filtered out: {})",
                        formatBufferRoute(buff.getConfig(), table), n, skipped);
            } else if (!chunk.isEmpty() && buffCfg.isMultiLegDealSave()) {
                log.info("DB saved: {} — 0 rows from chunk {}",
                        formatBufferRoute(buff.getConfig(), table), chunk.size());
            }
        }

        syncBufferStatusInternal(status);
        syncDbDiagnostics(status);
        return saved;
    }

    private int saveRowsDirect(Map<String, List<MicexTableRow>> byTable, ClientStatus status) {
        int saved = 0;
        for (Map.Entry<String, List<MicexTableRow>> e : byTable.entrySet()) {
            BufferConfig cfg = bufferConfigService.findByMicexTable(e.getKey());
            if (cfg == null) {
                log.warn("Нет BufferConfig для таблицы {}", e.getKey());
                continue;
            }
            int n;
            if (cfg.isMultiLegDealSave()) {
                n = micexRowRepository.saveDealFxChunk(
                        cfg,
                        e.getValue(),
                        row -> passBoardFilter(row) && passSecurityFilter(row, cfg));
            } else {
                n = 0;
                for (MicexTableRow row : e.getValue()) {
                    if (!passExchangeFilters(row, cfg)) {
                        continue;
                    }
                    DbSaveOutcome outcome = micexRowRepository.save(cfg, row);
                    dbDiagnostics.recordOutcome(cfg.getTypeBuff(), cfg.getQualifiedProcedure(), outcome);
                    if (outcome.saved()) {
                        n++;
                    }
                }
            }
            if (n > 0) {
                saved += n;
                log.info("DB saved (direct): {} — {} rows", formatBufferRoute(cfg, e.getKey()), n);
            }
        }
        syncBufferStatusInternal(status);
        syncDbDiagnostics(status);
        return saved;
    }

    private void syncDbDiagnostics(ClientStatus status) {
        status.setLastDbError(dbDiagnostics.getLastDbError());
        status.setLastDbErrorAt(dbDiagnostics.getLastDbErrorAt());
        status.setDbSkipSummary(dbDiagnostics.getSkipSummary());
        status.setDbPollMessage(dbDiagnostics.getLastDbPollMessage());
    }

    private boolean passExchangeFilters(MicexTableRow row, BufferConfig cfg) {
        return passBoardFilter(row) && passSecurityFilter(row, cfg);
    }

    private boolean isSectionSecurityBuffer(BufferConfig cfg) {
        if (cfg == null) {
            return false;
        }
        if (cfg.isFxQuoteBuffer()) {
            return false;
        }
        return loaderConstants.isMmvSectionSecurityMarket();
    }

    private boolean passBoardFilter(MicexTableRow row) {
        String table = row.getTableName();
        if (table == null) {
            return true;
        }
        String t = table.toUpperCase();
        if (!"SECURITIES".equals(t) && !"TRADES".equals(t) && !"ORDERS".equals(t)) {
            return true;
        }
        String secboard = fieldValue(row, "SECBOARD");
        if (secboard == null) {
            return true;
        }
        return boardFilterRepository.isAllowed(secboard);
    }

    private boolean passSecurityFilter(MicexTableRow row, BufferConfig cfg) {
        if (!securityFilterRepository.isEnabled()) {
            return true;
        }
        if (!isSectionSecurityBuffer(cfg)) {
            return true;
        }
        String table = row.getTableName();
        if (table == null || !"SECURITIES".equalsIgnoreCase(table)) {
            return true;
        }
        String seccode = fieldValue(row, "SECCODE");
        if (seccode == null) {
            return false;
        }
        return securityFilterRepository.isAllowed(seccode);
    }

    private static String fieldValue(MicexTableRow row, String name) {
        Map<String, Object> f = row.getFields();
        if (f == null) {
            return null;
        }
        for (Map.Entry<String, Object> e : f.entrySet()) {
            if (e.getKey() != null && name.equalsIgnoreCase(e.getKey())) {
                Object v = e.getValue();
                if (v == null) {
                    return null;
                }
                String s = String.valueOf(v).trim();
                return s.isEmpty() ? null : s;
            }
        }
        return null;
    }

    public boolean tryReconnect(ClientStatus status) {
        if (!reconnectOnError) {
            return false;
        }
        for (int attempt = 1; attempt <= reconnectMaxAttempts; attempt++) {
            try {
                log.info("Переподключение к MICEX, попытка {}", attempt);
                gatewayClient.disconnect();
                bufferConfigService.reload();
                boardFilterRepository.reload();
                boardSectionRepository.reload();
                securityFilterRepository.reload();
                gatewayClient.connect();
                initAfterConnect(status);
                status.setOpenedTables(new ArrayList<>(gatewayClient.getOpenedTables()));
                status.setFailedTables(new ArrayList<>(gatewayClient.getFailedTables()));
                status.setReconnectCount(status.getReconnectCount() + 1);
                status.setLastError("Переподключение OK (попытка " + attempt + ")");
                return true;
            } catch (Exception e) {
                log.warn("Переподключение не удалось: {}", e.getMessage());
                String msg = "Переподключение " + attempt + " FAIL: " + e.getMessage();
                status.setLastError(msg);
            }
        }
        status.setConnected(false);
        return false;
    }

    private void syncBufferStatusInternal(ClientStatus status) {
        List<BufferStatusInfo> list = new ArrayList<>();
        for (MicexBuffer b : runtimeBuffers) {
            BufferStatusInfo info = new BufferStatusInfo();
            info.setTypeBuff(b.getTypeBuff());
            info.setMicexTable(b.getMicexTable());
            BufferConfig cfg = b.getConfig();
            int interval = timerPreferences.getIntervalSecFor(cfg);
            boolean quoteBuff = cfg.isQuoteBuffer();
            info.setPollIntervalSec(interval);
            info.setRowsSaved(b.getRowsSaved());
            Instant last = b.getLastSaveAt();
            info.setLastSaveAt(last.equals(Instant.EPOCH) ? null : last);
            info.setSaveState(b.saveStateLabel(interval, quoteBuff));
            info.setStarted(bufferControl.isBufferStarted(b.getTypeBuff()));
            list.add(info);
        }
        status.setBufferStatuses(list);
    }

    private String formatBufferRoute(BufferConfig cfg, String micexTable) {
        if (cfg == null) {
            return "MICEX " + (micexTable == null ? "?" : micexTable.toUpperCase());
        }
        String table = micexTable == null || micexTable.isBlank()
                ? cfg.getMicexTable()
                : micexTable;
        return String.format("MICEX %s / %s",
                table == null ? "?" : table.toUpperCase(),
                bufferKindLabel(cfg));
    }

    private String bufferKindLabel(BufferConfig cfg) {
        String kind = cfg.getBufferKind();
        return kind == null || kind.isBlank() ? "BUFF_" + cfg.getTypeBuff() : kind;
    }

    private record SaveTarget(BufferConfig config, int typeSection, boolean remap) {
    }

    private List<SaveTarget> resolveSaveTargets(BufferConfig buff, MicexTableRow row) {
        int loaderSection = loaderConstants.getTypeSection();
        Integer boardSection = resolveBoardSection(row);

        if (buff.isQuoteBuffer()) {
            if (buff.isSecQuoteBuffer()) {
                return List.of(new SaveTarget(buff, loaderSection, false));
            }
            SaveTarget quote = routeQuoteTarget(boardSection, loaderSection);
            return quote != null ? List.of(quote) : List.of();
        }
        if (buff.isDecimalBuffer() || buff.isBoardBuffer()) {
            int section = boardSection != null ? boardSection : loaderSection;
            return List.of(new SaveTarget(buff, section, false));
        }
        return List.of(new SaveTarget(buff, loaderSection, false));
    }

    private SaveTarget routeQuoteTarget(Integer boardSection, int loaderSection) {
        if (boardSection == null || boardSection == loaderSection) {
            BufferConfig fx = bufferConfigService.findFxQuoteBuffer();
            return fx != null ? new SaveTarget(fx, loaderSection, false) : null;
        }
        if (boardSection == loaderConstants.sectState() || boardSection == loaderConstants.sectShare()) {
            BufferConfig sec = bufferConfigService.findSecQuoteBuffer();
            if (sec == null) {
                return null;
            }
            return new SaveTarget(sec, boardSection, true);
        }
        return null;
    }

    private Integer resolveBoardSection(MicexTableRow row) {
        String table = row.getTableName();
        if (table == null) {
            return null;
        }
        String t = table.toUpperCase();
        if ("BOARDS".equals(t)) {
            String boardId = fieldValue(row, "BOARDID");
            return boardSectionRepository.resolveTypeSection(boardId);
        }
        if ("SECURITIES".equals(t) || "TRADES".equals(t) || "ORDERS".equals(t)) {
            return boardSectionRepository.resolveTypeSection(fieldValue(row, "SECBOARD"));
        }
        return null;
    }

}
