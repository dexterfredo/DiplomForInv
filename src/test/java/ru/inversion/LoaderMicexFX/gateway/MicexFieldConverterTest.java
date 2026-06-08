package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicexFieldConverterTest {

    @Test
    void nullBecomesEmptyString() {
        assertEquals("", MicexFieldConverter.asMicexString(null));
    }

    @Test
    void nullPlaceholderBecomesEmpty() {
        assertEquals("", MicexFieldConverter.asMicexString("[null]"));
        assertEquals("", MicexFieldConverter.asMicexString("null"));
    }

    @Test
    void zeroCommissionPreserved() {
        assertEquals("0.00", MicexFieldConverter.asMicexString("0.00"));
        assertEquals("0", MicexFieldConverter.asMicexString("0"));
    }

    @Test
    void emptyAndWhitespaceBecomeEmpty() {
        assertEquals("", MicexFieldConverter.asMicexString(""));
        assertEquals("", MicexFieldConverter.asMicexString("   "));
    }

    @Test
    void bigDecimalStripsTrailingZeros() {
        assertEquals("73.985", MicexFieldConverter.asMicexString(new BigDecimal("73.9850")));
        assertEquals("10.8524", MicexFieldConverter.asMicexString(new BigDecimal("10.852400")));
    }

    @Test
    void localDateFormattedYyyymmdd() {
        assertEquals("20260602", MicexFieldConverter.asMicexString(LocalDate.of(2026, 6, 2)));
    }

    @Test
    void localTimeFormattedHhmmss() {
        assertEquals("143045", MicexFieldConverter.asMicexString(LocalTime.of(14, 30, 45)));
    }

    @Test
    void booleanAsOneZero() {
        assertEquals("1", MicexFieldConverter.asMicexString(Boolean.TRUE));
        assertEquals("0", MicexFieldConverter.asMicexString(Boolean.FALSE));
    }

    @Test
    void bracketWrappedUnwrapped() {
        assertEquals("74.05", MicexFieldConverter.asMicexString("[74.05]"));
    }
}
