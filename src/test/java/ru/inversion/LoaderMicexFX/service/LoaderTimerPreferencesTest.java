package ru.inversion.LoaderMicexFX.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.BufferTimerGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LoaderTimerPreferencesTest {

    private LoaderTimerPreferences preferences;

    @BeforeEach
    void setUp() {
        preferences = new LoaderTimerPreferences();
        ReflectionTestUtils.setField(preferences, "defaultDealSeconds", 0);
        ReflectionTestUtils.setField(preferences, "defaultQuoteSeconds", 10);
        ReflectionTestUtils.setField(preferences, "defaultSettingsSeconds", 9000);
        ReflectionTestUtils.setField(preferences, "defaultDealTimerEnabled", true);
        preferences.init();
    }

    @Test
    void applyFromForm_updatesIntervals() {
        preferences.applyFromForm("00:00:05", "00:00:20", "01:00:00", false);
        assertEquals("00:00:05", preferences.formatDealTime());
        assertFalse(preferences.isDealTimerEnabled());
    }

    @Test
    void getIntervalSecFor_returnsGroupSpecificValues() {
        preferences.applyFromForm("00:00:07", "00:00:11", "00:10:00", null);
        BufferConfig deal = bufferWithGroup(BufferTimerGroup.DEAL);
        assertEquals(7, preferences.getIntervalSecFor(deal));
    }

    private static BufferConfig bufferWithGroup(BufferTimerGroup group) {
        BufferConfig config = new BufferConfig();
        config.setFunctionName(switch (group) {
            case DEAL -> "MICEX_DEAL_FX_DATA_INS";
            case QUOTE -> "MICEX_FX_QUOTE_DATA_INS";
            case SETTINGS -> "MICEX_DECIMALS_DATA_INS";
        });
        config.setBufferKind(BufferConfig.inferBufferKind(config.getFunctionName()));
        return config;
    }
}
