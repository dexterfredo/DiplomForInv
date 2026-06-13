package ru.inversion.LoaderMicexFX.model;

public class BufferConfig {

    public static final String SCHEMA_TABLE = "tr__data_temp";
    public static final String SCHEMA_VIEW = "tr__data_view";
    public static final String SCHEMA_API = "tr_api_loader";

    private int typeSrc;
    private int typeBuff;
    private String micexTable;
    private String saveProcedure;
    private String packageName;
    private String viewName;
    private String functionName;
    private int pollIntervalSec;
    private boolean loadFull;
    private String bufferKind;

    public int getTypeSrc() {
        return typeSrc;
    }

    public void setTypeSrc(int typeSrc) {
        this.typeSrc = typeSrc;
    }

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

    public String getSaveProcedure() {
        return saveProcedure;
    }

    public void setSaveProcedure(String saveProcedure) {
        this.saveProcedure = saveProcedure;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getPgSchema() {
        if (packageName == null || packageName.isBlank()) {
            return SCHEMA_API;
        }
        return packageName.trim().toLowerCase();
    }

    public String getPgViewName() {
        if (viewName == null || viewName.isBlank()) {
            return "";
        }
        return viewName.trim().toLowerCase();
    }

    public String getQualifiedProcedure() {
        return getQualifiedFunction();
    }

    public String getQualifiedFunction() {
        String fn = functionName;
        if (fn == null || fn.isBlank()) {
            fn = saveProcedure;
        }
        if (fn == null || fn.isBlank()) {
            return "";
        }
        return getPgSchema() + "." + fn.trim().toLowerCase();
    }

    public String getStorageTableName() {
        if (viewName == null || viewName.isBlank()) {
            return "";
        }
        String v = viewName.trim().toUpperCase();
        if (v.startsWith("V_TR_BUFF_MICEX_")) {
            return "tr_buff_" + v.substring("V_TR_BUFF_".length()).toLowerCase();
        }
        if (v.startsWith("V_")) {
            return v.substring(2).toLowerCase();
        }
        return v.toLowerCase();
    }

    public String getQualifiedTable() {
        String t = getStorageTableName();
        return t.isEmpty() ? "" : SCHEMA_TABLE + "." + t;
    }

    public String getQualifiedView() {
        String t = getStorageTableName();
        if (t.isEmpty()) {
            return "";
        }
        return SCHEMA_VIEW + ".v_" + t;
    }

    public int getPollIntervalSec() {
        return pollIntervalSec;
    }

    public void setPollIntervalSec(int pollIntervalSec) {
        this.pollIntervalSec = pollIntervalSec;
    }

    public boolean isLoadFull() {
        return loadFull;
    }

    public void setLoadFull(boolean loadFull) {
        this.loadFull = loadFull;
    }

    public String getBufferKind() {
        return bufferKind;
    }

    public void setBufferKind(String bufferKind) {
        this.bufferKind = bufferKind;
    }

    public String normalizedFunction() {
        String fn = functionName;
        if (fn == null || fn.isBlank()) {
            fn = saveProcedure;
        }
        return fn == null ? "" : fn.trim().toUpperCase();
    }

    public static String inferBufferKind(String function) {
        String fn = function == null ? "" : function.trim().toUpperCase();
        if (fn.contains("DEAL_FX")) {
            return "DEAL_FX";
        }
        if (fn.contains("DEAL_SEC")) {
            return "DEAL_SEC";
        }
        if (fn.contains("FX_QUOTE")) {
            return "QUOTE_FX";
        }
        if (fn.contains("SEC_QUOTE")) {
            return "QUOTE_SEC";
        }
        if (fn.contains("DECIMAL")) {
            return "DECIMAL";
        }
        if (fn.contains("BOARD")) {
            return "BOARD";
        }
        if (fn.contains("LOTSIZE")) {
            return "LOTSIZE";
        }
        if (fn.contains("ODA") || (fn.contains("ORDER") && fn.contains("SEC"))) {
            return "ORDER_SEC";
        }
        return fn.isEmpty() ? "DATA" : fn;
    }

    public boolean isMultiLegDealSave() {
        return normalizedFunction().contains("DEAL_FX");
    }

    public boolean isQuoteBuffer() {
        return normalizedFunction().contains("QUOTE");
    }

    public boolean isFxQuoteBuffer() {
        return normalizedFunction().contains("FX_QUOTE");
    }

    public boolean isSecQuoteBuffer() {
        return normalizedFunction().contains("SEC_QUOTE");
    }

    public boolean isDealBuffer() {
        return normalizedFunction().contains("DEAL");
    }

    public boolean isDecimalBuffer() {
        return normalizedFunction().contains("DECIMAL");
    }

    public boolean isBoardBuffer() {
        String fn = normalizedFunction();
        return fn.contains("BOARD") && !fn.contains("DECIMAL");
    }

    public boolean isLotsizeBuffer() {
        return normalizedFunction().contains("LOTSIZE");
    }

    public boolean isOrderBuffer() {
        String fn = normalizedFunction();
        return fn.contains("ODA") || (fn.contains("ORDER") && fn.contains("SEC"));
    }

    public BufferTimerGroup getTimerGroup() {
        String fn = normalizedFunction();
        if (fn.contains("DEAL")) {
            return BufferTimerGroup.DEAL;
        }
        if (fn.contains("QUOTE")) {
            return BufferTimerGroup.QUOTE;
        }
        return BufferTimerGroup.SETTINGS;
    }
}
