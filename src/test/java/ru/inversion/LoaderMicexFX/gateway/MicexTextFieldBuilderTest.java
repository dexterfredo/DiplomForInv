package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MicexTextFieldBuilderTest {

    private MicexTableSchemaRegistry schemaRegistry;
    private MicexTextFieldBuilder builder;

    @BeforeEach
    void setUp() {
        schemaRegistry = new MicexTableSchemaRegistry();
        builder = new MicexTextFieldBuilder(schemaRegistry);
        seedSecuritiesSchema();
    }

    @Test
    void padsEmptyFieldsToFixedWidth() {
        MicexTableRow row = row("SECURITIES", Map.of(
                "SECBOARD", "CETS",
                "SECCODE", "USD000UTSTOM"));
        assertEquals("CETSUSD000UTSTOM", builder.buildFromApiRow(row));
    }

    @Test
    void returnsNullWhenSchemaMissing() {
        MicexTableRow row = row("UNKNOWN", Map.of("SECBOARD", "CETS"));
        assertNull(builder.buildFromApiRow(row));
    }

    private void seedSecuritiesSchema() {
        List<MicexTableSchemaRegistry.FieldDef> defs = List.of(
                new MicexTableSchemaRegistry.FieldDef("SECBOARD", 10),
                new MicexTableSchemaRegistry.FieldDef("SECCODE", 20));
        injectSchema("SECURITIES", defs);
    }

    private static MicexTableRow row(String table, Map<String, String> fields) {
        MicexTableRow row = new MicexTableRow();
        row.setTableName(table);
        fields.forEach(row::putField);
        return row;
    }

    @SuppressWarnings("unchecked")
    private void injectSchema(String table, List<MicexTableSchemaRegistry.FieldDef> defs) {
        try {
            var field = MicexTableSchemaRegistry.class.getDeclaredField("byTable");
            field.setAccessible(true);
            Map<String, List<MicexTableSchemaRegistry.FieldDef>> map =
                    (Map<String, List<MicexTableSchemaRegistry.FieldDef>>) field.get(schemaRegistry);
            map.put(table, defs);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
