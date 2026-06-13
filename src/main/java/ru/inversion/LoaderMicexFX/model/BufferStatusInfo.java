package ru.inversion.LoaderMicexFX.model;

import java.time.Instant;

public class BufferStatusInfo {

    private int typeBuff;
    private String micexTable;
    private int pollIntervalSec;
    private long rowsSaved;
    private Instant lastSaveAt;
    private String saveState;
    private boolean started;

    public int getTypeBuff() {
        return typeBuff;
    }

    public void setTypeBuff(int typeBuff) {
        this.typeBuff = typeBuff;
    }

    public String getMicexTable() {
        return micexTable;
    }

    public void setMicexTable(String micexTable) {
        this.micexTable = micexTable;
    }

    public int getPollIntervalSec() {
        return pollIntervalSec;
    }

    public void setPollIntervalSec(int pollIntervalSec) {
        this.pollIntervalSec = pollIntervalSec;
    }

    public long getRowsSaved() {
        return rowsSaved;
    }

    public void setRowsSaved(long rowsSaved) {
        this.rowsSaved = rowsSaved;
    }

    public Instant getLastSaveAt() {
        return lastSaveAt;
    }

    public void setLastSaveAt(Instant lastSaveAt) {
        this.lastSaveAt = lastSaveAt;
    }

    public String getSaveState() {
        return saveState;
    }

    public void setSaveState(String saveState) {
        this.saveState = saveState;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }
}
