package ru.inversion.LoaderMicexFX.model;

public class MicexTargetEntry {

    private int typeSection;
    private int typeBuff;
    
    private String field;
    
    private String buffField;
    private String dbSource;

    public int getTypeSection() {
        return typeSection;
    }

    public void setTypeSection(int typeSection) {
        this.typeSection = typeSection;
    }

    public int getTypeBuff() {
        return typeBuff;
    }

    public void setTypeBuff(int typeBuff) {
        this.typeBuff = typeBuff;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getBuffField() {
        return buffField;
    }

    public void setBuffField(String buffField) {
        this.buffField = buffField;
    }

    public String getDbSource() {
        return dbSource;
    }

    public void setDbSource(String dbSource) {
        this.dbSource = dbSource;
    }
}
