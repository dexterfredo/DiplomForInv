package ru.inversion.LoaderMicexFX.model;

import java.time.Instant;
import java.util.List;

public class ClientStatus {

    private boolean connected;
    private Instant startedAt;
    private Instant lastUpdateAt;
    private long totalMessages;
    private String lastError;
    private String lastDbError;
    private Instant lastDbErrorAt;
    private String dbSkipSummary;
    private String dbPollMessage;
    private List<String> openedTables = List.of();
    private List<String> failedTables = List.of();
    private List<BufferStatusInfo> bufferStatuses = List.of();
    private int reconnectCount;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getLastUpdateAt() {
        return lastUpdateAt;
    }

    public void setLastUpdateAt(Instant lastUpdateAt) {
        this.lastUpdateAt = lastUpdateAt;
    }

    public long getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(long totalMessages) {
        this.totalMessages = totalMessages;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getLastDbError() {
        return lastDbError;
    }

    public void setLastDbError(String lastDbError) {
        this.lastDbError = lastDbError;
    }

    public Instant getLastDbErrorAt() {
        return lastDbErrorAt;
    }

    public void setLastDbErrorAt(Instant lastDbErrorAt) {
        this.lastDbErrorAt = lastDbErrorAt;
    }

    public String getDbSkipSummary() {
        return dbSkipSummary;
    }

    public void setDbSkipSummary(String dbSkipSummary) {
        this.dbSkipSummary = dbSkipSummary;
    }

    public String getDbPollMessage() {
        return dbPollMessage;
    }

    public void setDbPollMessage(String dbPollMessage) {
        this.dbPollMessage = dbPollMessage;
    }

    public List<String> getOpenedTables() {
        return openedTables;
    }

    public void setOpenedTables(List<String> openedTables) {
        this.openedTables = openedTables != null ? openedTables : List.of();
    }

    public List<String> getFailedTables() {
        return failedTables;
    }

    public void setFailedTables(List<String> failedTables) {
        this.failedTables = failedTables != null ? failedTables : List.of();
    }

    public List<BufferStatusInfo> getBufferStatuses() {
        return bufferStatuses;
    }

    public void setBufferStatuses(List<BufferStatusInfo> bufferStatuses) {
        this.bufferStatuses = bufferStatuses != null ? bufferStatuses : List.of();
    }

    public int getReconnectCount() {
        return reconnectCount;
    }

    public void setReconnectCount(int reconnectCount) {
        this.reconnectCount = reconnectCount;
    }
}
