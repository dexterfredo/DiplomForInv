package ru.inversion.LoaderMicexFX.loaderconfig;

import ru.inversion.LoaderMicexFX.model.BufferConfig;

import java.util.ArrayList;
import java.util.List;

public class BufferDependents {

    public static List<Integer> forMaster(BufferConfig master, List<BufferConfig> all, boolean includeLotsize) {
        if (master == null || all == null || all.isEmpty()) {
            return List.of();
        }
        if (!master.isDealBuffer() && !master.isQuoteBuffer()) {
            return List.of();
        }
        List<Integer> deps = new ArrayList<>();
        for (BufferConfig b : all) {
            if (b.isBoardBuffer() || b.isDecimalBuffer()) {
                deps.add(b.getTypeBuff());
            }
        }
        if (master.isDealBuffer() && includeLotsize) {
            for (BufferConfig b : all) {
                if (b.isLotsizeBuffer()) {
                    deps.add(b.getTypeBuff());
                }
            }
        }
        return deps;
    }
}
