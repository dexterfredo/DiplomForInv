package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicexPgCompositeTest {

    @Test
    void formatLiteralQuotesTextFields() {
        assertEquals("(\"S\")", MicexPgComposite.formatLiteral(new Object[] {"S"}));
    }

    @Test
    void formatLiteralUnquotesNumericStrings() {
        assertEquals("(123)", MicexPgComposite.formatLiteral(new Object[] {"123"}));
    }
}
