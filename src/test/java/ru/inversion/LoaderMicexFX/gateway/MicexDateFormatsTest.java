package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MicexDateFormatsTest {

    @Test
    void normalizeTradeDateYyyymmdd() {
        assertEquals("20260602", MicexDateFormats.normalizeTradeDate("20260602"));
    }

    @Test
    void normalizeTradeDateRejectsInvalid() {
        assertNull(MicexDateFormats.normalizeTradeDate("20250602[bad]"));
    }
}
