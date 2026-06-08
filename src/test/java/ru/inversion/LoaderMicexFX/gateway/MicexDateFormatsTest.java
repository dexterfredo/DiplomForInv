package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MicexDateFormatsTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "20250602[bad]"})
    void normalizeTradeDateRejectsInvalid(String raw) {
        assertNull(MicexDateFormats.normalizeTradeDate(raw));
    }

    @Test
    void normalizeTradeDateYyyymmdd() {
        assertEquals("20260602", MicexDateFormats.normalizeTradeDate("20260602"));
    }

    @Test
    void normalizeTradeDateIso() {
        assertEquals("20260605", MicexDateFormats.normalizeTradeDate("2026-06-05"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "ab:cd"})
    void normalizeTimeRejectsInvalid(String raw) {
        assertNull(MicexDateFormats.normalizeTime(raw));
    }

    @Test
    void normalizeTimeHhmmssCompact() {
        assertEquals("143045", MicexDateFormats.normalizeTime("143045"));
    }

    @Test
    void normalizeTimeWithColons() {
        assertEquals("143045", MicexDateFormats.normalizeTime("14:30:45"));
        assertEquals("095931", MicexDateFormats.normalizeTime("09:59:31"));
    }

    @Test
    void parseTimestampNow() {
        Timestamp ts = MicexDateFormats.parseTimestamp("now");
        assertNotNull(ts);
    }

    @Test
    void parseTimestampDateTimeCompact() {
        Timestamp ts = MicexDateFormats.parseTimestamp("20260602 143045");
        assertNotNull(ts);
        assertEquals("2026-06-02 14:30:45.0", ts.toString());
    }

    @Test
    void parseTimestampBlankReturnsNull() {
        assertNull(MicexDateFormats.parseTimestamp(""));
        assertNull(MicexDateFormats.parseTimestamp("   "));
    }
}
