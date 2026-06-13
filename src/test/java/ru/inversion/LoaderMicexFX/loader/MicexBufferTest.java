package ru.inversion.LoaderMicexFX.loader;

import org.junit.jupiter.api.Test;
import ru.inversion.LoaderMicexFX.loaderconfig.BufferDependents;
import ru.inversion.LoaderMicexFX.model.BufferConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicexBufferTest {

    @Test
    void dealIntervalZeroAlwaysReady() {
        MicexBuffer buff = new MicexBuffer(null);
        buff.completeRefreshCycle(0, false);
        assertTrue(buff.shouldRefreshCycle(0, false));
        assertTrue(buff.shouldSaveToDb(0));
    }

    @Test
    void fxDealDependentsExcludeLotsize() {
        List<BufferConfig> all = List.of(
                board(5887), decimal(5886), dealFx(2323), lotsize(6181));
        List<Integer> deps = BufferDependents.forMaster(dealFx(2323), all, false);
        assertEquals(List.of(5887, 5886), deps);
    }

    private static BufferConfig dealFx(int typeBuff) {
        BufferConfig c = new BufferConfig();
        c.setTypeBuff(typeBuff);
        c.setFunctionName("MICEX_DEAL_FX_DATA_INS");
        c.setBufferKind(BufferConfig.inferBufferKind(c.getFunctionName()));
        return c;
    }

    private static BufferConfig board(int typeBuff) {
        BufferConfig c = new BufferConfig();
        c.setTypeBuff(typeBuff);
        c.setFunctionName("MICEX_BOARD_DATA_INS");
        c.setBufferKind(BufferConfig.inferBufferKind(c.getFunctionName()));
        return c;
    }

    private static BufferConfig decimal(int typeBuff) {
        BufferConfig c = new BufferConfig();
        c.setTypeBuff(typeBuff);
        c.setFunctionName("MICEX_DECIMALS_DATA_INS");
        c.setBufferKind(BufferConfig.inferBufferKind(c.getFunctionName()));
        return c;
    }

    private static BufferConfig lotsize(int typeBuff) {
        BufferConfig c = new BufferConfig();
        c.setTypeBuff(typeBuff);
        c.setFunctionName("MICEX_LOTSIZE_DATA_INS");
        c.setBufferKind(BufferConfig.inferBufferKind(c.getFunctionName()));
        return c;
    }
}
