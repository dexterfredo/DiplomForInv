package ru.inversion.LoaderMicexFX.model;

public class LoaderUiState {

    private boolean loadDecimals = true;
    private boolean loadBoards = true;
    private boolean micexConnected;
    private boolean dealStarted;
    private boolean quoteStarted;
    private boolean decimalStarted;
    private boolean boardStarted;
    private boolean lotsizeStarted;

    private boolean lockConnectParams;
    private boolean lockSettingsCheckboxes;
    private boolean lockSettingsStart;
    private boolean lockConnectActions;

    private String dealTime = "00:00:00";
    private String quoteTime = "00:00:10";
    private String settingsTime = "02:30:00";
    private boolean dealTimerEnabled = true;
    private String connectionType = "TCP2";

    public boolean isLoadDecimals() {
        return loadDecimals;
    }

    public void setLoadDecimals(boolean loadDecimals) {
        this.loadDecimals = loadDecimals;
    }

    public boolean isLoadBoards() {
        return loadBoards;
    }

    public void setLoadBoards(boolean loadBoards) {
        this.loadBoards = loadBoards;
    }

    public boolean isMicexConnected() {
        return micexConnected;
    }

    public void setMicexConnected(boolean micexConnected) {
        this.micexConnected = micexConnected;
    }

    public boolean isDealStarted() {
        return dealStarted;
    }

    public void setDealStarted(boolean dealStarted) {
        this.dealStarted = dealStarted;
    }

    public boolean isQuoteStarted() {
        return quoteStarted;
    }

    public void setQuoteStarted(boolean quoteStarted) {
        this.quoteStarted = quoteStarted;
    }

    public boolean isDecimalStarted() {
        return decimalStarted;
    }

    public void setDecimalStarted(boolean decimalStarted) {
        this.decimalStarted = decimalStarted;
    }

    public boolean isBoardStarted() {
        return boardStarted;
    }

    public void setBoardStarted(boolean boardStarted) {
        this.boardStarted = boardStarted;
    }

    public boolean isLotsizeStarted() {
        return lotsizeStarted;
    }

    public void setLotsizeStarted(boolean lotsizeStarted) {
        this.lotsizeStarted = lotsizeStarted;
    }

    public boolean isSettingsRunning() {
        return decimalStarted || boardStarted;
    }

    public String getSettingsButtonLabel() {
        return isSettingsRunning() ? "Стоп" : "Старт";
    }

    public String getDealButtonLabel() {
        return dealStarted ? "Стоп" : "Старт";
    }

    public String getQuoteButtonLabel() {
        return quoteStarted ? "Стоп" : "Старт";
    }

    public String getConnectButtonLabel() {
        return micexConnected ? "Disconnect" : "Connect";
    }

    public boolean isLockConnectParams() {
        return lockConnectParams;
    }

    public void setLockConnectParams(boolean lockConnectParams) {
        this.lockConnectParams = lockConnectParams;
    }

    public boolean isLockSettingsCheckboxes() {
        return lockSettingsCheckboxes;
    }

    public void setLockSettingsCheckboxes(boolean lockSettingsCheckboxes) {
        this.lockSettingsCheckboxes = lockSettingsCheckboxes;
    }

    public boolean isLockSettingsStart() {
        return lockSettingsStart;
    }

    public void setLockSettingsStart(boolean lockSettingsStart) {
        this.lockSettingsStart = lockSettingsStart;
    }

    public boolean isLockConnectActions() {
        return lockConnectActions;
    }

    public void setLockConnectActions(boolean lockConnectActions) {
        this.lockConnectActions = lockConnectActions;
    }

    public String getDealTime() {
        return dealTime;
    }

    public void setDealTime(String dealTime) {
        this.dealTime = dealTime;
    }

    public String getQuoteTime() {
        return quoteTime;
    }

    public void setQuoteTime(String quoteTime) {
        this.quoteTime = quoteTime;
    }

    public String getSettingsTime() {
        return settingsTime;
    }

    public void setSettingsTime(String settingsTime) {
        this.settingsTime = settingsTime;
    }

    public boolean isDealTimerEnabled() {
        return dealTimerEnabled;
    }

    public void setDealTimerEnabled(boolean dealTimerEnabled) {
        this.dealTimerEnabled = dealTimerEnabled;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }
}
