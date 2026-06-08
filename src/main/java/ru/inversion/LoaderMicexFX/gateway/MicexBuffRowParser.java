package ru.inversion.LoaderMicexFX.gateway;

import org.springframework.stereotype.Component;
import ru.inversion.LoaderMicexFX.db.MicexTargetRepository;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;
import ru.inversion.LoaderMicexFX.model.MicexTargetEntry;
import ru.inversion.LoaderMicexFX.service.LoaderConstantsService;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class MicexBuffRowParser {

    private final MicexTargetRepository targetRepository;
    private final LoaderConstantsService loaderConstants;

    public MicexBuffRowParser(MicexTargetRepository targetRepository, LoaderConstantsService loaderConstants) {
        this.targetRepository = targetRepository;
        this.loaderConstants = loaderConstants;
    }

    public void mapApiToViewFields(MicexTableRow row, BufferConfig buff) {
        mapApiToViewFields(row, buff, loaderConstants.getTypeSection());
    }

    public void mapApiToViewFields(MicexTableRow row, BufferConfig buff, int typeSection) {
        row.getViewFields().clear();
        if (row == null || row.getFields().isEmpty() || buff == null) {
            return;
        }
        int typeBuff = buff.getTypeBuff();
        List<MicexTargetEntry> mapping = targetRepository.loadForBuff(typeBuff, typeSection);
        if (mapping.isEmpty()) {
            return;
        }
        Map<String, Object> apiFields = upperKeys(row.getFields());
        Map<String, String> view = mapFromTarget(mapping, apiFields);
        enrichCatalogFields(view, buff, typeSection);
        row.getViewFields().putAll(view);
    }

    private void enrichCatalogFields(Map<String, String> view, BufferConfig buff, int typeSection) {
        view.putIfAbsent("type_src", String.valueOf(buff.getTypeSrc()));
        view.putIfAbsent("type_buff", String.valueOf(buff.getTypeBuff()));
        view.putIfAbsent("type_section", String.valueOf(typeSection));
    }

    static Map<String, String> mapFromTarget(List<MicexTargetEntry> mapping, Map<String, Object> apiFields) {
        Map<String, String> view = new LinkedHashMap<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (MicexTargetEntry e : mapping) {
            if (e.getBuffField() == null || e.getBuffField().isBlank()) {
                continue;
            }
            String bf = e.getBuffField().toLowerCase();
            if (!seen.add(bf)) {
                continue;
            }
            String v = resolveMicexField(e.getField(), apiFields);
            if (v != null) {
                view.put(bf, v);
            }
        }
        return view;
    }

    private static String resolveMicexField(String field, Map<String, Object> fields) {
        if (field == null || field.isBlank()) {
            return null;
        }
        Object v = fields.get(field.trim().toUpperCase());
        if (v == null) {
            return null;
        }
        String s = MicexFieldConverter.asMicexString(v);
        return s.isEmpty() ? null : s;
    }

    private static Map<String, Object> upperKeys(Map<String, Object> src) {
        Map<String, Object> u = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : src.entrySet()) {
            if (e.getKey() != null) {
                u.put(e.getKey().toUpperCase(), e.getValue());
            }
        }
        return u;
    }
}
