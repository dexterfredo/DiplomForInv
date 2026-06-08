package ru.inversion.LoaderMicexFX.gateway;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class MicexFieldConverter {

    private static final DateTimeFormatter TIME_HHMMSS =
            DateTimeFormatter.ofPattern("HHmmss");

    private MicexFieldConverter() {
    }

    
    public static String asMicexString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String s) {
            String n = MicexValueNormalizer.normalize(s);
            return n == null ? "" : n;
        }
        if (value instanceof LocalDate d) {
            return d.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        if (value instanceof LocalTime t) {
            return t.format(TIME_HHMMSS);
        }
        if (value instanceof LocalDateTime dt) {
            return dt.format(DateTimeFormatter.ofPattern("yyyyMMdd HHmmss"));
        }
        if (value instanceof Date d) {
            return new java.text.SimpleDateFormat("yyyyMMdd HHmmss").format(d);
        }
        if (value instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Number n) {
            return n.toString();
        }
        if (value instanceof Boolean b) {
            return b ? "1" : "0";
        }
        String n = MicexValueNormalizer.normalize(String.valueOf(value).trim());
        return n == null ? "" : n;
    }
}
