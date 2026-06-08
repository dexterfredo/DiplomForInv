package ru.inversion.LoaderMicexFX.db;

public record DbSaveOutcome(Status status, String reason, Long buffId) {

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
        return new DbSaveOutcome(Status.SAVED, null, buffId);
    }

    public static DbSaveOutcome skipped(String reason) {
        return new DbSaveOutcome(Status.SKIPPED, reason, null);
    }

    public static DbSaveOutcome sqlError(String message) {
        return new DbSaveOutcome(Status.SQL_ERROR, message, null);
    }

    public static DbSaveOutcome procError(String message) {
        return new DbSaveOutcome(Status.PROC_ERROR, message, null);
    }
}
