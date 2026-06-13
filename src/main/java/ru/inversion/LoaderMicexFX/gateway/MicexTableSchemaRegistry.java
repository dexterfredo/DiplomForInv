package ru.inversion.LoaderMicexFX.gateway;

import com.micex.client.Meta;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MicexTableSchemaRegistry {

    public record FieldDef(String name, int size) {
    }

    private final Map<String, List<FieldDef>> byTable = new ConcurrentHashMap<>();

    public void captureFromMessage(Meta.Message message) {
        if (message == null || message.name == null || message.name.isBlank()) {
            return;
        }
        String table = message.name.toUpperCase();
        List<FieldDef> parsed = readFields(message);
        if (!parsed.isEmpty()) {
            byTable.put(table, parsed);
        }
    }

    public List<FieldDef> fieldsFor(String tableName) {
        if (tableName == null) {
            return List.of();
        }
        List<FieldDef> fields = byTable.get(tableName.toUpperCase());
        return fields == null ? List.of() : fields;
    }

    private static List<FieldDef> readFields(Meta.Message message) {
        Object[] candidates = {
                readMember(message, "fields"),
                readMember(message, "outFields"),
                readMember(message, "listOutFields")
        };
        for (Object candidate : candidates) {
            List<FieldDef> parsed = parseFieldArray(candidate);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }
        return List.of();
    }

    private static Object readMember(Object target, String name) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        try {
            var f = type.getField(name);
            return f.get(target);
        } catch (ReflectiveOperationException ignored) {
            try {
                var f = type.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (ReflectiveOperationException ignored2) {
                return null;
            }
        }
    }

    private static List<FieldDef> parseFieldArray(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof Iterable<?> iterable) {
            List<FieldDef> out = new ArrayList<>();
            for (Object item : iterable) {
                FieldDef def = toFieldDef(item);
                if (def != null) {
                    out.add(def);
                }
            }
            return out.isEmpty() ? List.of() : Collections.unmodifiableList(out);
        }
        if (raw.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(raw);
            List<FieldDef> out = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                FieldDef def = toFieldDef(java.lang.reflect.Array.get(raw, i));
                if (def != null) {
                    out.add(def);
                }
            }
            return out.isEmpty() ? List.of() : Collections.unmodifiableList(out);
        }
        return List.of();
    }

    private static FieldDef toFieldDef(Object fieldObj) {
        if (fieldObj == null) {
            return null;
        }
        String name = readStringField(fieldObj, "name");
        if (name == null || name.isBlank() || "TEXT".equalsIgnoreCase(name)) {
            return null;
        }
        int size = readIntField(fieldObj, "size", 0);
        if (size <= 0) {
            size = readIntField(fieldObj, "Size", 0);
        }
        return new FieldDef(name, Math.max(size, 0));
    }

    private static String readStringField(Object obj, String name) {
        Object v = readMember(obj, name);
        return v == null ? null : v.toString();
    }

    private static int readIntField(Object obj, String name, int def) {
        Object v = readMember(obj, name);
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(v.toString().trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }
}
