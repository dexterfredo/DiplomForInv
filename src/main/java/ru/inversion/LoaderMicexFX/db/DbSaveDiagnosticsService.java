package ru.inversion.LoaderMicexFX.db;

import ru.inversion.LoaderMicexFX.monitoring.LoaderMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DbSaveDiagnosticsService {

    private static final Logger log = LoggerFactory.getLogger(DbSaveDiagnosticsService.class);

    private final JdbcTemplate jdbcTemplate;
    private final LoaderMetricsService loaderMetrics;

    @Value("${app.loader.db.poll-enabled:true}")
    private boolean pollEnabled;

    @Value("${app.loader.db.poll-count-query:}")
    private String pollCountQuery;

    @Value("${app.loader.db.error-query:}")
    private String errorQuery;

    private volatile String lastDbError;
    private volatile Instant lastDbErrorAt;
    private volatile String lastDbPollMessage;
    private volatile Instant lastDbPollAt;

    private final ConcurrentHashMap<String, AtomicLong> skipCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> recentBuffCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastWarnAt = new ConcurrentHashMap<>();

    public DbSaveDiagnosticsService(JdbcTemplate jdbcTemplate, LoaderMetricsService loaderMetrics) {
        this.jdbcTemplate = jdbcTemplate;
        this.loaderMetrics = loaderMetrics;
    }

    public void recordOutcome(int typeBuff, String procedure, DbSaveOutcome outcome) {
        if (outcome == null) {
            return;
        }
        loaderMetrics.recordSaveOutcome(typeBuff, outcome);
        switch (outcome.status()) {
            case SAVED -> clearDbErrorIfMatches(typeBuff);
            case SKIPPED -> {
                bumpSkip(typeBuff, outcome.reason());
                warnRateLimited(typeBuff, procedure, outcome.reason());
            }
            case SQL_ERROR, PROC_ERROR -> setDbError(formatProc(typeBuff, procedure, outcome.reason()));
            default -> {
            }
        }
    }

    public void recordBufferNotStarted(int typeBuff) {
        bumpSkip(typeBuff, "BUFFER_NOT_STARTED");
        warnRateLimited(typeBuff, null, "буфер не запущен (кнопка Старт на странице)");
    }

    public void recordWaitingTimer(int typeBuff, int intervalSec) {
        bumpSkip(typeBuff, "WAIT_TIMER");
        warnRateLimited(typeBuff, null, "ожидание интервала " + intervalSec + " сек");
    }

    public void recordVerifyUnavailable(String detail) {
        bumpSkip(0, "VERIFY_UNAVAILABLE");
        warnRateLimited(0, null, detail);
    }

    public String getLastDbError() {
        return lastDbError;
    }

    public Instant getLastDbErrorAt() {
        return lastDbErrorAt;
    }

    public String getSkipSummary() {
        if (skipCounters.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        skipCounters.forEach((key, count) -> {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(key).append('=').append(count.get());
        });
        return sb.toString();
    }

    public Map<String, Long> getRecentBuffCounts() {
        return Map.copyOf(recentBuffCounts);
    }

    public String getLastDbPollMessage() {
        return lastDbPollMessage;
    }

    public Instant getLastDbPollAt() {
        return lastDbPollAt;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lastDbError", lastDbError);
        m.put("lastDbErrorAt", lastDbErrorAt != null ? lastDbErrorAt.toString() : null);
        m.put("skipSummary", getSkipSummary());
        m.put("recentBuffCounts", getRecentBuffCounts());
        m.put("lastDbPollMessage", lastDbPollMessage);
        m.put("lastDbPollAt", lastDbPollAt != null ? lastDbPollAt.toString() : null);
        m.put("pollCountQueryConfigured", pollCountQuery != null && !pollCountQuery.isBlank());
        m.put("errorQueryConfigured", errorQuery != null && !errorQuery.isBlank());
        return m;
    }

    @Scheduled(fixedDelayString = "${app.loader.db.poll-ms:30000}")
    public void pollDatabase() {
        if (!pollEnabled) {
            return;
        }
        pollBuffCounts();
        pollConfiguredErrorQuery();
    }

    private void pollBuffCounts() {
        lastDbPollAt = Instant.now();
        if (pollCountQuery == null || pollCountQuery.isBlank()) {
            lastDbPollMessage = "poll-count-query не задан (на чужой БД укажите свой SQL в config/application.properties)";
            return;
        }
        try {
            recentBuffCounts.clear();
            jdbcTemplate.query(pollCountQuery, rs -> {
                int cols = rs.getMetaData().getColumnCount();
                if (cols >= 2) {
                    Object key = rs.getObject(1);
                    long cnt = rs.getLong(2);
                    recentBuffCounts.put(key == null ? "?" : String.valueOf(key), cnt);
                } else if (cols == 1) {
                    recentBuffCounts.put("total", rs.getLong(1));
                }
            });
            lastDbPollMessage = recentBuffCounts.isEmpty()
                    ? "OK: запрос выполнен, строк за период нет"
                    : "OK: " + recentBuffCounts;
        } catch (Exception e) {
            lastDbPollMessage = "poll: " + rootMessage(e);
            log.debug("DB poll-count-query: {}", rootMessage(e));
        }
    }

    private void pollConfiguredErrorQuery() {
        if (errorQuery == null || errorQuery.isBlank()) {
            return;
        }
        try {
            String msg = jdbcTemplate.query(errorQuery, rs -> {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString(1);
            });
            if (msg != null && !msg.isBlank()) {
                setDbError("poll: " + msg);
            }
        } catch (Exception e) {
            log.debug("DB error-query: {}", rootMessage(e));
        }
    }

    private void bumpSkip(int typeBuff, String reason) {
        String key = typeBuff + ":" + (reason == null ? "?" : reason);
        skipCounters.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    private void warnRateLimited(int typeBuff, String procedure, String reason) {
        String key = typeBuff + ":" + reason;
        Instant now = Instant.now();
        Instant prev = lastWarnAt.get(key);
        if (prev != null && now.isBefore(prev.plusSeconds(30))) {
            return;
        }
        lastWarnAt.put(key, now);
        log.warn("DB skip: buff {} {} — {}", typeBuff, procedure == null ? "" : procedure, reason);
    }

    private void setDbError(String message) {
        lastDbError = message;
        lastDbErrorAt = Instant.now();
        log.warn("DB: {}", message);
    }

    private void clearDbErrorIfMatches(int typeBuff) {
        if (lastDbError != null && lastDbError.contains("buff " + typeBuff)) {
            lastDbError = null;
            lastDbErrorAt = null;
        }
    }

    private static String formatProc(int typeBuff, String procedure, String reason) {
        String proc = procedure == null || procedure.isBlank() ? "?" : procedure;
        return "buff " + typeBuff + " / " + proc + ": " + reason;
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        if (t instanceof SQLException sql) {
            String sqlState = sql.getSQLState();
            if (sqlState != null && !sqlState.isBlank()) {
                String msg = sql.getMessage();
                return msg != null && !msg.isBlank() ? msg + " [SQLState=" + sqlState + "]" : sqlState;
            }
        }
        String msg = t.getMessage();
        return msg != null && !msg.isBlank() ? msg : t.getClass().getSimpleName();
    }
}
