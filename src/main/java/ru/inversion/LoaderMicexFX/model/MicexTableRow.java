package ru.inversion.LoaderMicexFX.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class MicexTableRow {

    private String tableName;
    private final Map<String, Object> fields = new LinkedHashMap<>();
    private final Map<String, String> viewFields = new LinkedHashMap<>();
    private boolean snapshot;
    private Instant receivedAt = Instant.now();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public Map<String, String> getViewFields() {
        return viewFields;
    }

    public void putField(String name, Object value) {
        if (name != null) {
            fields.put(name, value);
        }
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    public void setSnapshot(boolean snapshot) {
        this.snapshot = snapshot;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        if (receivedAt != null) {
            this.receivedAt = receivedAt;
        }
    }

    public MicexTableRow copyForRemap() {
        MicexTableRow copy = new MicexTableRow();
        copy.setTableName(tableName);
        copy.setSnapshot(snapshot);
        copy.receivedAt = receivedAt;
        copy.fields.putAll(fields);
        return copy;
    }
}
