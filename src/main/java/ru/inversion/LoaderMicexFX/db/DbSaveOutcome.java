package ru.inversion.LoaderMicexFX.db;

import java.io.PrintWriter;
import java.io.StringWriter;

public record DbSaveOutcome(Status status, String reason, Long buffId, String stackTrace) {

    public enum Status {
        SAVED,
        SKIPPED,
        SQL_ERROR,
        PROC_ERROR
    }

    public boolean saved() {
        return status == Status.SAVED;
    }

    public static DbSaveOutcome saved(Long buffId) {
        return new DbSaveOutcome(Status.SAVED, null, buffId, null);
    }

    public static DbSaveOutcome skipped(String reason) {
        return new DbSaveOutcome(Status.SKIPPED, reason, null, null);
    }

    public static DbSaveOutcome sqlError(String message) {
        return sqlError(message, null);
    }

    public static DbSaveOutcome sqlError(String message, Throwable cause) {
        return new DbSaveOutcome(Status.SQL_ERROR, message, null, formatStack(cause));
    }

    public static DbSaveOutcome procError(String message) {
        return procError(message, null);
    }

    public static DbSaveOutcome procError(String message, Throwable cause) {
        return new DbSaveOutcome(Status.PROC_ERROR, message, null, formatStack(cause));
    }

    private static String formatStack(Throwable cause) {
        if (cause == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        cause.printStackTrace(new PrintWriter(sw));
        return sw.toString().trim();
    }
}
