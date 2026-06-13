package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;
import ru.inversion.LoaderMicexFX.db.ViewColumnMeta;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MicexBuffStructBuilderTest {

    @Test
    void emptyMappedFieldsBecomeNull() {
        List<ViewColumnMeta> cols = List.of(
                new ViewColumnMeta("raw_bid", "varchar"),
                new ViewColumnMeta("raw_last_", "varchar"));
        Object[] attrs = MicexBuffStructBuilder.buildAttributes(
                cols,
                Map.of("raw_bid", "   ", "raw_last_", "74.05"),
                null);
        assertNull(attrs[0]);
        assertEquals("74.05", attrs[1]);
    }

    @Test
    void tradeDateAndTimeNormalized() {
        List<ViewColumnMeta> cols = List.of(
                new ViewColumnMeta("raw_tradedate", "varchar"),
                new ViewColumnMeta("raw_tradetime", "varchar"));
        Object[] attrs = MicexBuffStructBuilder.buildAttributes(
                cols,
                Map.of("raw_tradedate", "2026-06-02", "raw_tradetime", "14:30:45"),
                null);
        assertEquals("20260602", attrs[0]);
        assertEquals("143045", attrs[1]);
    }
}
