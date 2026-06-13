package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MicexValueNormalizerTest {

    @Test
    void nullReturnsNull() {
        assertNull(MicexValueNormalizer.normalize(null));
    }

    @Test
    void nullPlaceholderBecomesNull() {
        assertNull(MicexValueNormalizer.normalize("[null]"));
    }
}
