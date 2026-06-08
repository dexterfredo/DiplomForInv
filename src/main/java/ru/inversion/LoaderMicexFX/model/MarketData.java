package ru.inversion.LoaderMicexFX.model;

import java.math.BigDecimal;
import java.time.Instant;

public class MarketData {
    private String symbol;
    private BigDecimal price;
    private long volume;
    private Instant eventTime;
    private boolean staticData;
    
    private String sourceTable;

    public String getSymbol() {
        return symbol;
    }
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    public BigDecimal getPrice() {
        return price;
    }
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    public long getVolume() {
        return volume;
    }
    public void setVolume(long volume) {
        this.volume = volume;
    }
    public Instant getEventTime() {
        return eventTime;
    }
    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }
    public boolean isStaticData() {
        return staticData;
    }
    public void setStaticData(boolean staticData) {
        this.staticData = staticData;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }
}
