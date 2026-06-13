package ru.inversion.LoaderMicexFX.gateway;

import ru.inversion.LoaderMicexFX.db.ViewColumnMeta;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class MicexBuffStructBuilder {

    public static Object[] buildAttributes(
            List<ViewColumnMeta> columns,
            Map<String, String> viewFields,
            String textPayload) {
        Object[] attrs = new Object[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            ViewColumnMeta col = columns.get(i);
            String name = col.name();
            String raw = viewFields.get(name);
            if ("text".equals(name) && textPayload != null) {
                raw = textPayload;
            }
            attrs[i] = toSqlValue(col.dataType(), name, raw);
        }
        return attrs;
    }

    private static Object toSqlValue(String dataType, String columnName, String raw) {
        raw = MicexValueNormalizer.normalize(raw);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (isTimestampColumn(columnName) && isTimestampType(dataType)) {
            Timestamp ts = MicexDateFormats.parseTimestamp(raw);
            return ts != null ? ts : raw;
        }
        if (isTradeDateColumn(columnName)) {
            String d = MicexDateFormats.normalizeTradeDate(raw);
            return d != null ? d : raw;
        }
        if (isTimeColumn(columnName)) {
            String t = MicexDateFormats.normalizeTime(raw);
            return t != null ? t : raw;
        }
        return raw;
    }

    private static boolean isTimestampType(String dataType) {
        if (dataType == null) {
            return false;
        }
        String t = dataType.toLowerCase();
        return t.contains("timestamp") || "date".equals(t);
    }

    private static boolean isTimestampColumn(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase();
        return "insert_date".equals(n)
                || "entry_date".equals(n)
                || "expiry_date".equals(n)
                || "insert_datetime".equals(n)
                || "tradedatetime".equals(n);
    }

    private static boolean isTradeDateColumn(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase();
        return "tradedate".equals(n)
                || "raw_tradedate".equals(n)
                || "settledate".equals(n)
                || "raw_settledate".equals(n)
                || "orderdate".equals(n)
                || "raw_orderdate".equals(n);
    }

    private static boolean isTimeColumn(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase();
        return "raw_time".equals(n)
                || "tradetime".equals(n)
                || "raw_tradetime".equals(n)
                || "ordertime".equals(n)
                || "raw_ordertime".equals(n)
                || "withdrawtime".equals(n)
                || "raw_withdrawtime".equals(n)
                || "activationtime".equals(n)
                || "raw_activationtime".equals(n);
    }

    public static void ensureDefaultDates(Map<String, String> viewFields, List<ViewColumnMeta> columns) {
        for (String col : List.of("insert_date", "entry_date", "expiry_date")) {
            if (columns.stream().anyMatch(c -> col.equals(c.name())) && !viewFields.containsKey(col)) {
                viewFields.put(col, "now");
            }
        }
    }
}
