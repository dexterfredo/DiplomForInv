package ru.inversion.LoaderMicexFX.loader;

import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class MicexBuffer {

    public static final int QUOTE_LOAD_INTERVAL_SEC = 5;

    private final BufferConfig config;
    private boolean firstRead = true;
    private Instant lastSaveAt = Instant.EPOCH;
    private Instant lastRefreshAt = Instant.EPOCH;
    private long rowsSaved;
    private final List<MicexTableRow> pendingRows = new ArrayList<>();

    public MicexBuffer(BufferConfig config) {
        this.config = config;
    }

    public BufferConfig getConfig() {
        return config;
    }

    public int getTypeBuff() {
        return config.getTypeBuff();
    }

    public String getMicexTable() {
        return config.getMicexTable();
    }

    public long getRowsSaved() {
        return rowsSaved;
    }

    public Instant getLastSaveAt() {
        return lastSaveAt;
    }

    public boolean isFirstRead() {
        return firstRead;
    }

    public void accumulateRows(List<MicexTableRow> rows) {
        if (rows != null && !rows.isEmpty()) {
            pendingRows.addAll(rows);
        }
    }

    public boolean hasPending() {
        return !pendingRows.isEmpty();
    }

    public boolean pendingHasSnapshot() {
        for (MicexTableRow row : pendingRows) {
            if (row.isSnapshot()) {
                return true;
            }
        }
        return false;
    }

    public List<MicexTableRow> drainPending() {
        List<MicexTableRow> copy = new ArrayList<>(pendingRows);
        pendingRows.clear();
        return copy;
    }

    public boolean shouldRefreshCycle(int intervalSec, boolean quoteBuff) {
        if (firstRead) {
            return true;
        }
        long pos = currPosition();
        if (intervalSec <= 0 || pos >= intervalSec) {
            return true;
        }
        return quoteBuff && currPositionLoad() >= QUOTE_LOAD_INTERVAL_SEC;
    }

    public boolean shouldSaveToDb(int intervalSec) {
        if (firstRead) {
            return true;
        }
        return intervalSec <= 0 || currPosition() >= intervalSec;
    }

    public void completeRefreshCycle(int intervalSec, boolean quoteBuff) {
        long pos = currPosition();
        long posLoad = currPositionLoad();

        if (firstRead) {
            lastSaveAt = Instant.now();
            lastRefreshAt = Instant.now();
        }
        if (intervalSec <= 0 || pos >= intervalSec) {
            lastSaveAt = Instant.now();
        }
        if (quoteBuff && posLoad >= QUOTE_LOAD_INTERVAL_SEC) {
            lastRefreshAt = Instant.now();
        }
        firstRead = false;
    }

    public void addRowsSaved(int rowCount) {
        if (rowCount > 0) {
            rowsSaved += rowCount;
        }
    }

    public void resetAfterReconnect() {
        firstRead = true;
        lastSaveAt = Instant.EPOCH;
        lastRefreshAt = Instant.EPOCH;
        pendingRows.clear();
    }

    public String saveStateLabel(int intervalSec, boolean quoteBuff) {
        if (!shouldRefreshCycle(intervalSec, quoteBuff)) {
            return "WAIT";
        }
        return shouldSaveToDb(intervalSec) ? "READY" : "REFRESH";
    }

    long currPosition() {
        return secondsSince(lastSaveAt);
    }

    long currPositionLoad() {
        return secondsSince(lastRefreshAt);
    }

    private static long secondsSince(Instant from) {
        if (from.equals(Instant.EPOCH)) {
            return Long.MAX_VALUE / 2;
        }
        return ChronoUnit.SECONDS.between(from, Instant.now());
    }
}
