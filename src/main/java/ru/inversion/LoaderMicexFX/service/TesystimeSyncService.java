package ru.inversion.LoaderMicexFX.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.db.OperDateRepository;
import ru.inversion.LoaderMicexFX.gateway.MicexDateFormats;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class TesystimeSyncService {

    private static final Logger log = LoggerFactory.getLogger(TesystimeSyncService.class);

    private final OperDateRepository operDateRepository;

    @Value("${app.loader.tesystime.sync-interval-sec:30}")
    private int syncIntervalSec;

    private Instant lastSyncAt = Instant.EPOCH;

    private volatile String micexSysTradeDate = "";

    public TesystimeSyncService(OperDateRepository operDateRepository) {
        this.operDateRepository = operDateRepository;
    }

    public void reset() {
        lastSyncAt = Instant.EPOCH;
        micexSysTradeDate = "";
    }

    public String getMicexSysTradeDate() {
        return micexSysTradeDate;
    }

    /** Вызывается раз в секунду из {@link ExchangeWorkerService#poll()}. */
    public void onMasterTick() {
        // счётчик секунд через lastSyncAt в processRows
    }

    public void processRows(List<MicexTableRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        boolean dueByInterval = syncIntervalSec > 0
                && ChronoUnit.SECONDS.between(lastSyncAt, Instant.now()) >= syncIntervalSec;
        for (MicexTableRow row : rows) {
            if (!"TESYSTIME".equalsIgnoreCase(row.getTableName())) {
                continue;
            }
            String date = MicexDateFormats.normalizeTradeDate(extractField(row, "DATE"));
            if (date != null) {
                micexSysTradeDate = date;
            }
            if (!dueByInterval && !row.isSnapshot()) {
                continue;
            }
            String time = MicexDateFormats.normalizeTime(extractField(row, "TIME"));
            String operDateTime = buildOperDateTime(date, time);
            if (operDateTime == null) {
                continue;
            }
            try {
                operDateRepository.setOperDateTime(operDateTime);
                log.debug("TESYSTIME set_oper_date_time({})", operDateTime);
                lastSyncAt = Instant.now();
            } catch (Exception e) {
                log.warn("set_oper_date_time: {}", e.getMessage());
            }
        }
    }

    private static String buildOperDateTime(String date, String time) {
        if (date == null || date.isBlank()) {
            return null;
        }
        if (time != null && !time.isBlank()) {
            return date + " " + time;
        }
        return date;
    }

    private static String extractField(MicexTableRow row, String key) {
        Map<String, Object> f = row.getFields();
        if (f == null) {
            return null;
        }
        Object v = f.get(key);
        if (v == null) {
            for (Map.Entry<String, Object> e : f.entrySet()) {
                if (e.getKey() != null && key.equalsIgnoreCase(e.getKey())) {
                    v = e.getValue();
                    break;
                }
            }
        }
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }
}
