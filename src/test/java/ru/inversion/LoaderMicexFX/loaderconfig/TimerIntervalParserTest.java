package ru.inversion.LoaderMicexFX.loaderconfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimerIntervalParserTest {

    @Test
    void toIntervalSeconds_parsesHhMmSs() {
        assertEquals(10, TimerIntervalParser.toIntervalSeconds("00:00:10"));
    }

    @Test
    void formatSeconds_roundTrip() {
        assertEquals("00:00:10", TimerIntervalParser.formatSeconds(10));
    }
}
