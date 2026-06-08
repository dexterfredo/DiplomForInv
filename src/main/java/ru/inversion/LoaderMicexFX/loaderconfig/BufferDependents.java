package ru.inversion.LoaderMicexFX.loaderconfig;

import java.util.ArrayList;
import java.util.List;

public final class BufferDependents {

    private BufferDependents() {
    }

    /**
     * FX (6246): deal → board + decimal (без 6181, lotsize в 5886).
     * Фондовый рынок: deal → board + decimal + lotsize (как MicexBufferParams).
     */
    public static List<Integer> forMaster(
            int masterTypeBuff,
            int deal,
            int quote,
            int board,
            int decimal,
            int lotsize,
            boolean includeLotsize) {
        if (masterTypeBuff == deal) {
            List<Integer> deps = new ArrayList<>(List.of(board, decimal));
            if (includeLotsize) {
                deps.add(lotsize);
            }
            return deps;
        }
        if (masterTypeBuff == quote) {
            return List.of(board, decimal);
        }
        return List.of();
    }
}
