package ru.inversion.LoaderMicexFX.service;

import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.db.BufferConfigRepository;
import ru.inversion.LoaderMicexFX.db.MicexTargetRepository;
import ru.inversion.LoaderMicexFX.model.BufferConfig;

import java.util.List;

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

    public java.util.Set<String> getMicexTableNames() {
        java.util.Set<String> set = new java.util.HashSet<>();
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

    public int getTypeSrc() {
        return loaderConstants.getTypeSrc();
    }

    public int getTypeSection() {
        return loaderConstants.getTypeSection();
    }
}
