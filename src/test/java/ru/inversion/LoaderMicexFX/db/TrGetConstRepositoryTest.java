package ru.inversion.LoaderMicexFX.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrGetConstRepositoryTest {

    @Test
    void normalizeFunctionNameAcceptsConfiguredNames() {
        assertEquals("gc_mmvb_sect_fx", TrGetConstRepository.normalizeFunctionName("gc_mmvb_sect_fx"));
        assertEquals("gc_deal_place_section_share", TrGetConstRepository.normalizeFunctionName(" GC_DEAL_PLACE_SECTION_SHARE "));
    }

    @Test
    void normalizeFunctionNameRejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> TrGetConstRepository.normalizeFunctionName(""));
        assertThrows(IllegalArgumentException.class, () -> TrGetConstRepository.normalizeFunctionName("drop_table"));
        assertThrows(IllegalArgumentException.class, () -> TrGetConstRepository.normalizeFunctionName("gc_mmvb_sect_fx();--"));
    }
}
