package ru.inversion.LoaderMicexFX.loader;

import org.junit.jupiter.api.Test;
import ru.inversion.LoaderMicexFX.loaderconfig.BufferDependents;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicexBufferTest {

    @Test
    void firstReadAllowsRefreshAndSave() {
        MicexBuffer buff = new MicexBuffer(null);
        assertTrue(buff.shouldRefreshCycle(30, false));
        assertTrue(buff.shouldSaveToDb(30));
    }

    @Test
    void dealWaitsUntilMainInterval() throws InterruptedException {
        MicexBuffer buff = new MicexBuffer(null);
        buff.completeRefreshCycle(10, false);
        assertFalse(buff.shouldRefreshCycle(10, false));
        assertFalse(buff.shouldSaveToDb(10));
        Thread.sleep(1100);
        assertFalse(buff.shouldRefreshCycle(10, false));
    }

    @Test
    void dealIntervalZeroAlwaysReady() {
        MicexBuffer buff = new MicexBuffer(null);
        buff.completeRefreshCycle(0, false);
        assertTrue(buff.shouldRefreshCycle(0, false));
        assertTrue(buff.shouldSaveToDb(0));
    }

    @Test
    void quoteRefreshEveryFiveSecondsWithoutSave() throws InterruptedException {
        MicexBuffer buff = new MicexBuffer(null);
        buff.completeRefreshCycle(10, true);
        assertFalse(buff.shouldSaveToDb(10));
        Thread.sleep(5100);
        assertTrue(buff.shouldRefreshCycle(10, true));
        assertFalse(buff.shouldSaveToDb(10));
        assertEquals("REFRESH", buff.saveStateLabel(10, true));
    }

    @Test
    void quoteSavesWhenMainIntervalElapsed() throws InterruptedException {
        MicexBuffer buff = new MicexBuffer(null);
        buff.completeRefreshCycle(2, true);
        Thread.sleep(2100);
        assertTrue(buff.shouldRefreshCycle(2, true));
        assertTrue(buff.shouldSaveToDb(2));
    }

    @Test
    void pendingRowsAccumulateUntilSaveCycle() {
        MicexBuffer buff = new MicexBuffer(null);
        buff.accumulateRows(List.of(new ru.inversion.LoaderMicexFX.model.MicexTableRow()));
        assertTrue(buff.hasPending());
        buff.drainPending();
        assertFalse(buff.hasPending());
    }

    @Test
    void fxDealDependentsExcludeLotsize() {
        List<Integer> deps = BufferDependents.forMaster(2323, 2323, 5922, 5887, 5886, 6181, false);
        assertEquals(List.of(5887, 5886), deps);
    }

    @Test
    void secMarketDealDependentsIncludeLotsize() {
        List<Integer> deps = BufferDependents.forMaster(2324, 2324, 2325, 5887, 5886, 6181, true);
        assertEquals(List.of(5887, 5886, 6181), deps);
    }
}
