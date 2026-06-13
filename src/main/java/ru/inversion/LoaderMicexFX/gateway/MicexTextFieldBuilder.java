package ru.inversion.LoaderMicexFX.gateway;

import org.springframework.stereotype.Component;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;

import java.util.List;
import java.util.Map;

@Component
public class MicexTextFieldBuilder {

    private final MicexTableSchemaRegistry schemaRegistry;

    public MicexTextFieldBuilder(MicexTableSchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public String buildFromApiRow(MicexTableRow row) {
        if (row == null || row.getTableName() == null) {
            return null;
        }
        List<MicexTableSchemaRegistry.FieldDef> defs = schemaRegistry.fieldsFor(row.getTableName());
        if (defs.isEmpty()) {
            return null;
        }
        Map<String, Object> values = row.getFields();
        StringBuilder dataRow = new StringBuilder();
        for (MicexTableSchemaRegistry.FieldDef def : defs) {
            Object raw = values.get(def.name());
            if (raw == null) {
                for (Map.Entry<String, Object> e : values.entrySet()) {
                    if (e.getKey() != null && def.name().equalsIgnoreCase(e.getKey())) {
                        raw = e.getValue();
                        break;
                    }
                }
            }
            String str = raw == null ? "" : String.valueOf(raw);
            if (str.trim().isEmpty()) {
                if (def.size() > 0) {
                    dataRow.append(" ".repeat(def.size()));
                }
            } else {
                dataRow.append(str);
            }
        }
        String s = dataRow.toString();
        return s.length() > 4000 ? s.substring(0, 4000) : s;
    }
}
