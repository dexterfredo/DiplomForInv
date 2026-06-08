package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;

import java.util.LinkedHashMap;
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
                "SECCODE", "USD000UTSTOM"
        ));
        String text = builder.buildFromApiRow(row);
        assertEquals("CETSUSD000UTSTOM", text);
    }

    @Test
    void keepsUntrimmedValuesInText() {
        MicexTableRow row = row("SECURITIES", Map.of(
                "SECBOARD", "CETS  ",
                "SECCODE", "USD000UTSTOM"
        ));
        String text = builder.buildFromApiRow(row);
        assertEquals("CETS  USD000UTSTOM", text);
    }

    @Test
    void padsMissingFieldWithSpaces() {
        MicexTableRow row = row("SECURITIES", Map.of("SECBOARD", "CETS"));
        String text = builder.buildFromApiRow(row);
        assertEquals("CETS" + " ".repeat(20), text);
    }

    @Test
    void returnsNullWhenSchemaMissing() {
        MicexTableRow row = row("UNKNOWN", Map.of("SECBOARD", "CETS"));
        assertNull(builder.buildFromApiRow(row));
    }

    @Test
    void emptyStringFieldPaddedWithSpaces() {
        MicexTableRow row = row("SECURITIES", Map.of(
                "SECBOARD", "CETS",
                "SECCODE", ""
        ));
        String text = builder.buildFromApiRow(row);
        assertEquals("CETS" + " ".repeat(20), text);
    }

    @Test
    void nullFieldValuePaddedWithSpaces() {
        MicexTableRow row = new MicexTableRow();
        row.setTableName("SECURITIES");
        row.putField("SECBOARD", "CETS");
        row.putField("SECCODE", null);
        String text = builder.buildFromApiRow(row);
        assertEquals("CETS" + " ".repeat(20), text);
    }

    @Test
    void literalNullStringTreatedAsContent() {
        MicexTableRow row = row("SECURITIES", Map.of(
                "SECBOARD", "CETS",
                "SECCODE", "null"
        ));
        String text = builder.buildFromApiRow(row);
        assertEquals("CETSnull", text);
    }

    private void seedSecuritiesSchema() {
        List<MicexTableSchemaRegistry.FieldDef> defs = List.of(
                new MicexTableSchemaRegistry.FieldDef("SECBOARD", 10),
                new MicexTableSchemaRegistry.FieldDef("SECCODE", 20)
        );
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
