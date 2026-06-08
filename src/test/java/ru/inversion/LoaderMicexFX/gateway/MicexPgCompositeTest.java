package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicexPgCompositeTest {

    @Test
    void formatLiteralUsesCompositeTextInputRules() {
        assertEquals("(,\"abc\",6246)", MicexPgComposite.formatLiteral(new Object[] { null, "abc", "6246" }));
    }

    @Test
    void formatLiteralUnquotesNumericStrings() {
        assertEquals("(2323,10.5)", MicexPgComposite.formatLiteral(new Object[] { "2323", "10.5" }));
    }

    @Test
    void formatLiteralQuotesMicexSingleLetterCodes() {
        Object[] attrs = new Object[24];
        attrs[8] = "S";
        attrs[20] = "S";
        attrs[21] = "T1";
        attrs[22] = "T";
        String literal = MicexPgComposite.formatLiteral(attrs);
        assertTrue(literal.contains("\"S\""), literal);
        assertTrue(literal.contains("\"T1\""), literal);
        assertTrue(literal.contains("\"T\""), literal);
        assertFalse(literal.matches(".*(?<!\")\\bS\\b.*"), literal);
    }
}
