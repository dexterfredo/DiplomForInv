package ru.inversion.LoaderMicexFX.loaderconfig;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class TimerIntervalParser {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm[:ss]");

    private TimerIntervalParser() {
    }

    public static int toIntervalSeconds(String timeOfDay) {
        if (timeOfDay == null || timeOfDay.isBlank()) {
            return 0;
        }
        String t = timeOfDay.trim();
        try {
            LocalTime lt = LocalTime.parse(t, TIME);
            return lt.toSecondOfDay();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    public static String formatSeconds(int seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
