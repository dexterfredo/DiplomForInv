package ru.inversion.LoaderMicexFX.service;

import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.db.BufferConfigRepository;
import ru.inversion.LoaderMicexFX.db.MicexTargetRepository;
import ru.inversion.LoaderMicexFX.model.BufferConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class BufferConfigService {

    private final BufferConfigRepository repository;
    private final MicexTargetRepository targetRepository;
    private final LoaderConstantsService loaderConstants;

    private List<BufferConfig> list;

    public BufferConfigService(
            BufferConfigRepository repository,
            MicexTargetRepository targetRepository,
            LoaderConstantsService loaderConstants) {
        this.repository = repository;
        this.targetRepository = targetRepository;
        this.loaderConstants = loaderConstants;
    }

    public List<BufferConfig> getActiveBuffers() {
        if (list == null) {
            list = repository.loadForTypeSrc(loaderConstants.getTypeSrc());
        }
        return list;
    }

    public void reload() {
        list = null;
        loaderConstants.reload();
        targetRepository.reload();
    }

    public Set<String> getMicexTableNames() {
        Set<String> set = new HashSet<>();
        for (BufferConfig c : getActiveBuffers()) {
            if (c.getMicexTable() != null) {
                set.add(c.getMicexTable().trim().toUpperCase());
            }
        }
        return set;
    }

    public BufferConfig findByMicexTable(String micexTable) {
        if (micexTable == null) {
            return null;
        }
        String key = micexTable.trim().toUpperCase();
        for (BufferConfig c : getActiveBuffers()) {
            if (c.getMicexTable() != null && c.getMicexTable().trim().toUpperCase().equals(key)) {
                return c;
            }
        }
        return null;
    }

    public BufferConfig findByTypeBuff(int typeBuff) {
        for (BufferConfig c : getActiveBuffers()) {
            if (c.getTypeBuff() == typeBuff) {
                return c;
            }
        }
        return repository.loadByTypeBuff(typeBuff);
    }

    public Optional<BufferConfig> findDealBuffer() {
        return getActiveBuffers().stream()
                .filter(BufferConfig::isMultiLegDealSave)
                .findFirst();
    }

    public Optional<BufferConfig> findPrimaryQuoteBuffer() {
        if (loaderConstants.isMmvSectionSecurityMarket()) {
            return getActiveBuffers().stream()
                    .filter(BufferConfig::isSecQuoteBuffer)
                    .findFirst();
        }
        return getActiveBuffers().stream()
                .filter(BufferConfig::isFxQuoteBuffer)
                .findFirst();
    }

    public BufferConfig findFxQuoteBuffer() {
        for (BufferConfig c : getActiveBuffers()) {
            if (c.isFxQuoteBuffer()) {
                return c;
            }
        }
        return repository.loadByFunctionContains("FX_QUOTE");
    }

    public BufferConfig findSecQuoteBuffer() {
        for (BufferConfig c : getActiveBuffers()) {
            if (c.isSecQuoteBuffer()) {
                return c;
            }
        }
        return repository.loadByFunctionContains("SEC_QUOTE");
    }

    public Optional<BufferConfig> findDecimalBuffer() {
        return getActiveBuffers().stream().filter(BufferConfig::isDecimalBuffer).findFirst();
    }

    public Optional<BufferConfig> findBoardBuffer() {
        return getActiveBuffers().stream().filter(BufferConfig::isBoardBuffer).findFirst();
    }

    public List<BufferConfig> findMasterBuffers() {
        return getActiveBuffers().stream()
                .filter(b -> b.isDealBuffer() || b.isQuoteBuffer())
                .toList();
    }

    public int getTypeSrc() {
        return loaderConstants.getTypeSrc();
    }

    public int getTypeSection() {
        return loaderConstants.getTypeSection();
    }
}
