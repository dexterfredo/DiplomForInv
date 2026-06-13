package ru.inversion.LoaderMicexFX.model;

import java.time.Instant;
import java.util.Objects;

public class DbErrorEntry {

    private final int typeBuff;
    private final String procedure;
    private final String status;
    private final String message;
    private final String stackTrace;
    private Instant firstAt;
    private Instant lastAt;
    private int repeatCount;

    public DbErrorEntry(
            Instant at,
            int typeBuff,
            String procedure,
            String status,
            String message,
            String stackTrace) {
        this.firstAt = at;
        this.lastAt = at;
        this.typeBuff = typeBuff;
        this.procedure = procedure;
        this.status = status;
        this.message = message;
        this.stackTrace = stackTrace;
        this.repeatCount = 1;
    }

    public Instant getFirstAt() {
        return firstAt;
    }

    public Instant getLastAt() {
        return lastAt;
    }

    public int getTypeBuff() {
        return typeBuff;
    }

    public String getProcedure() {
        return procedure;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public boolean hasStackTrace() {
        return stackTrace != null && !stackTrace.isBlank();
    }

    public boolean matches(int typeBuff, String procedure, String status, String message) {
        return this.typeBuff == typeBuff
                && Objects.equals(this.procedure, procedure)
                && Objects.equals(this.status, status)
                && Objects.equals(this.message, message);
    }

    public void bump(Instant at) {
        repeatCount++;
        lastAt = at;
    }
}
