package ru.inversion.LoaderMicexFX.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DbSaveDiagnosticsServiceTest {

    @Test
    void shortDbMessage_extractsPostgresErrorLine() {
        String raw = "org.postgresql.util.PSQLException: ERROR: Валюта инструмента не идентифицирована по коду [Code MICEX] = \"KZT\"\n"
                + "  Где: PL/pgSQL function err(text) line 5 at RAISE";
        assertEquals(
                "ERROR: Валюта инструмента не идентифицирована по коду [Code MICEX] = \"KZT\"",
                DbSaveDiagnosticsService.shortDbMessage(raw));
    }
}
