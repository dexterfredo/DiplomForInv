package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicexFieldConverterTest {

    @Test
    void bigDecimalStripsTrailingZeros() {
        assertEquals("1.5", MicexFieldConverter.asMicexString(new BigDecimal("1.5000")));
    }

    @Test
    void localDateFormattedYyyymmdd() {
        assertEquals("20260602", MicexFieldConverter.asMicexString(LocalDate.of(2026, 6, 2)));
    }
}
