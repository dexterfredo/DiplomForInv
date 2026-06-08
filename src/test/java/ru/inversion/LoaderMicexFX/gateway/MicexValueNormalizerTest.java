package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MicexValueNormalizerTest {

    @Test
    void nullReturnsNull() {
        assertNull(MicexValueNormalizer.normalize(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", ""})
    void blankBecomesEmpty(String raw) {
        assertEquals("", MicexValueNormalizer.normalize(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "NULL", "[null]", " Null "})
    void nullPlaceholdersBecomeNull(String raw) {
        assertNull(MicexValueNormalizer.normalize(raw));
    }

    @Test
    void bracketWrappedValueUnwrapped() {
        assertEquals("74.0525", MicexValueNormalizer.normalize("[74.0525]"));
        assertEquals("CETS", MicexValueNormalizer.normalize("[CETS]"));
    }

    @Test
    void emptyBracketsBecomeNull() {
        assertNull(MicexValueNormalizer.normalize("[]"));
        assertNull(MicexValueNormalizer.normalize("[   ]"));
    }

    @Test
    void bracketInsideStringBecomesEmpty() {
        assertEquals("", MicexValueNormalizer.normalize("20250602[bad]"));
        assertEquals("", MicexValueNormalizer.normalize("10.5[partial"));
    }

    @Test
    void normalValueTrimmed() {
        assertEquals("CETS", MicexValueNormalizer.normalize("  CETS  "));
    }
}
