package ru.inversion.LoaderMicexFX.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbErrorEntryTest {

    @Test
    void matches_comparesProcedureStatusAndMessage() {
        Instant at = Instant.parse("2026-06-01T10:00:00Z");
        DbErrorEntry entry = new DbErrorEntry(at, 2323, "micex_deal_fx_data_ins", "SQL_ERROR", "proc missing", null);
        assertTrue(entry.matches(2323, "micex_deal_fx_data_ins", "SQL_ERROR", "proc missing"));
    }

    @Test
    void bump_incrementsRepeatCount() {
        Instant first = Instant.parse("2026-06-01T10:00:00Z");
        Instant second = Instant.parse("2026-06-01T10:05:00Z");
        DbErrorEntry entry = new DbErrorEntry(first, 5887, "proc", "ERR", "msg", null);
        entry.bump(second);
        assertEquals(2, entry.getRepeatCount());
    }
}
