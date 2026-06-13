package ru.inversion.LoaderMicexFX.gateway;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class MicexDateFormats {

    private static final DateTimeFormatter DATE_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_HHMMSS = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATETIME_YYYYMMDD_HHMMSS =
            DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");

    public static String normalizeTradeDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.contains("[")) {
            return null;
        }
        if (s.matches("\\d{8}")) {
            return s;
        }
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                return LocalDate.parse(s).format(DATE_YYYYMMDD);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        String digits = s.replaceAll("\\D", "");
        if (digits.length() >= 8) {
            return digits.substring(0, 8);
        }
        return null;
    }

    public static String normalizeTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.matches("\\d{6}")) {
            return s;
        }
        if (s.matches("\\d{2}:\\d{2}:\\d{2}")) {
            return s.replace(":", "");
        }
        String digits = s.replaceAll("\\D", "");
        if (digits.length() >= 6) {
            return digits.substring(0, 6);
        }
        return null;
    }

    public static Timestamp parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if ("now".equalsIgnoreCase(s)) {
            return Timestamp.from(java.time.Instant.now());
        }
        try {
            if (s.matches("\\d{8} \\d{6}")) {
                LocalDateTime dt = LocalDateTime.parse(s, DATETIME_YYYYMMDD_HHMMSS);
                return Timestamp.valueOf(dt);
            }
            if (s.matches("\\d{8}")) {
                LocalDate d = LocalDate.parse(s, DATE_YYYYMMDD);
                return Timestamp.valueOf(d.atStartOfDay());
            }
            if (s.matches("\\d{6}")) {
                LocalTime t = LocalTime.parse(s, TIME_HHMMSS);
                LocalDate today = LocalDate.now(ZoneId.systemDefault());
                return Timestamp.valueOf(LocalDateTime.of(today, t));
            }
            if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDate d = LocalDate.parse(s);
                return Timestamp.valueOf(d.atStartOfDay());
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }
        return null;
    }
}
