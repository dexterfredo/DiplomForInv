package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;
import ru.inversion.LoaderMicexFX.model.MicexTargetEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MicexBuffRowParserTest {

    @Test
    void mapsMicexFieldsToRawBuffFields() {
        List<MicexTargetEntry> mapping = List.of(
                entry("secboard", "raw_secboard"),
                entry("seccode", "raw_seccode"),
                entry("decimals", "raw_decimals"));
        Map<String, Object> api = new LinkedHashMap<>();
        api.put("SECBOARD", "CETS");
        api.put("SECCODE", "USD000UTSTOM");
        api.put("DECIMALS", "4");

        Map<String, String> view = MicexBuffRowParser.mapFromTarget(mapping, api);

        assertEquals("CETS", view.get("raw_secboard"));
        assertEquals("USD000UTSTOM", view.get("raw_seccode"));
        assertEquals("4", view.get("raw_decimals"));
    }

    @Test
    void emptyTrimmedValuesAreOmitted() {
        List<MicexTargetEntry> mapping = List.of(entry("price", "raw_price"));
        Map<String, Object> api = Map.of("PRICE", "   ");

        Map<String, String> view = MicexBuffRowParser.mapFromTarget(mapping, api);

        assertNull(view.get("raw_price"));
    }

    private static MicexTargetEntry entry(String field, String buffField) {
        MicexTargetEntry e = new MicexTargetEntry();
        e.setField(field);
        e.setBuffField(buffField);
        e.setTypeSection(6246);
        e.setTypeBuff(2323);
        return e;
    }
}
