package ru.inversion.LoaderMicexFX.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.model.DbErrorEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DbSaveDiagnosticsService {

    private static final Logger log = LoggerFactory.getLogger(DbSaveDiagnosticsService.class);
    private static final Logger errorsFile = LoggerFactory.getLogger("ru.inversion.LoaderMicexFX.errorsFile");

    private final JdbcTemplate jdbcTemplate;
    @Value("${app.loader.db.poll-enabled:true}")
    private boolean pollEnabled;

    @Value("${app.loader.db.poll-count-query:}")
    private String pollCountQuery;

    @Value("${app.loader.db.error-query:}")
    private String errorQuery;

    @Value("${app.loader.db.error-history-size:100}")
    private int errorHistorySize;

    private volatile String lastDbError;
    private volatile Instant lastDbErrorAt;
    private volatile String lastDbPollMessage;
    private volatile Instant lastDbPollAt;

    private final ConcurrentHashMap<String, AtomicLong> skipCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> recentBuffCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastWarnAt = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<DbErrorEntry> recentErrors = new ConcurrentLinkedDeque<>();

    public DbSaveDiagnosticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordOutcome(int typeBuff, String procedure, DbSaveOutcome outcome) {
        if (outcome == null) {
            return;
        }
        switch (outcome.status()) {
            case SAVED -> clearDbErrorIfMatches(typeBuff);
            case SKIPPED -> {
                bumpSkip(typeBuff, outcome.reason());
                warnRateLimited(typeBuff, procedure, outcome.reason());
            }
            case SQL_ERROR, PROC_ERROR -> recordDbError(
                    typeBuff,
                    procedure,
                    outcome.status().name(),
                    outcome.reason(),
                    outcome.stackTrace());
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

    public List<DbErrorEntry> getRecentErrors() {
        return Collections.unmodifiableList(new ArrayList<>(recentErrors));
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
        m.put("recentErrors", getRecentErrors().stream().map(this::errorToMap).toList());
        return m;
    }

    private Map<String, Object> errorToMap(DbErrorEntry e) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("at", e.getFirstAt() != null ? e.getFirstAt().toString() : null);
        row.put("lastAt", e.getLastAt() != null ? e.getLastAt().toString() : null);
        row.put("repeatCount", e.getRepeatCount());
        row.put("typeBuff", e.getTypeBuff());
        row.put("procedure", e.getProcedure());
        row.put("status", e.getStatus());
        row.put("message", e.getMessage());
        row.put("stackTrace", e.getStackTrace());
        return row;
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
            lastDbPollMessage = "poll-count-query не задан";
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
                recordDbError(0, "poll", "POLL_ERROR", msg, null);
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

    private synchronized void recordDbError(
            int typeBuff,
            String procedure,
            String status,
            String message,
            String stackTrace) {
        String proc = procedure == null || procedure.isBlank() ? "?" : procedure;
        String summary = formatProc(typeBuff, proc, shortDbMessage(message));
        Instant now = Instant.now();
        lastDbError = summary;
        lastDbErrorAt = now;

        DbErrorEntry head = recentErrors.peekFirst();
        if (head != null && head.matches(typeBuff, proc, status, message)) {
            head.bump(now);
            return;
        }

        log.warn("DB: {}", summary);
        if (stackTrace != null && !stackTrace.isBlank()) {
            errorsFile.warn("DB stack (buff {}):\n{}", typeBuff, stackTrace);
        } else if (message != null && !message.isBlank() && !message.equals(shortDbMessage(message))) {
            errorsFile.warn("DB detail (buff {}): {}", typeBuff, message);
        }
        recentErrors.addFirst(new DbErrorEntry(now, typeBuff, proc, status, shortDbMessage(message), stackTrace));
        trimErrorHistory();
    }

    private void trimErrorHistory() {
        int max = Math.max(10, errorHistorySize);
        while (recentErrors.size() > max) {
            recentErrors.pollLast();
        }
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
        if (e.getCause() != null) {
            e = e.getCause();
        }
        String msg = e.getMessage();
        return msg != null ? msg : e.getClass().getSimpleName();
    }

    static String shortDbMessage(String message) {
        if (message == null || message.isBlank()) {
            return "?";
        }
        int errorIdx = message.indexOf("ERROR:");
        if (errorIdx >= 0) {
            String part = message.substring(errorIdx);
            int nl = part.indexOf('\n');
            if (nl > 0) {
                part = part.substring(0, nl);
            }
            int where = part.indexOf("  Где:");
            if (where > 0) {
                part = part.substring(0, where);
            }
            return part.trim();
        }
        int nl = message.indexOf('\n');
        if (nl > 0) {
            return message.substring(0, nl).trim();
        }
        return message.trim();
    }
}
