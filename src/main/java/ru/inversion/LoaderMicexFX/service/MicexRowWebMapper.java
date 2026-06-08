package ru.inversion.LoaderMicexFX.service;

import ru.inversion.LoaderMicexFX.model.MarketData;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public final class MicexRowWebMapper {

    private MicexRowWebMapper() {
    }

    public static MarketData toMarketData(MicexTableRow row) {
        Map<String, Object> f = row.getFields();
        String table = row.getTableName() != null ? row.getTableName() : "MICEX";
        String board = null;
        String code = null;
        for (Map.Entry<String, Object> e : f.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String k = e.getKey().toUpperCase();
            if ("SECBOARD".equals(k)) {
                board = String.valueOf(e.getValue());
            }
            if ("SECCODE".equals(k)) {
                code = String.valueOf(e.getValue());
            }
        }
        String symbol;
        if (board != null && code != null) {
            symbol = board + ":" + code;
        } else {
            Object tradeno = f.get("TRADENO");
            if (tradeno == null) {
                tradeno = f.get("ORDERNO");
            }
            symbol = tradeno != null ? String.valueOf(tradeno) : table + ":?";
        }

        MarketData md = new MarketData();
        md.setSourceTable(table);
        md.setSymbol(symbol);
        md.setPrice(BigDecimal.ZERO);
        md.setVolume(0);
        md.setEventTime(Instant.now());
        md.setStaticData(row.isSnapshot());

        for (String priceKey : new String[]{"LAST", "PRICE", "WAPRICE", "BID", "OFFER"}) {
            Object v = f.get(priceKey);
            if (v == null) {
                for (Map.Entry<String, Object> e : f.entrySet()) {
                    if (e.getKey() != null && e.getKey().equalsIgnoreCase(priceKey)) {
                        v = e.getValue();
                        break;
                    }
                }
            }
            if (v instanceof Number n) {
                md.setPrice(BigDecimal.valueOf(n.doubleValue()));
                break;
            }
        }
        return md;
    }
}
