package ru.inversion.LoaderMicexFX.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicexDealFxCallSqlTest {

    private static final String PG_TYPE = "tr__data_view.v_tr_buff_micex_deal_fx";
    private static final String QUALIFIED = "tr_api_loader.micex_deal_fx_data_ins";

    @Test
    void callSqlUsesThreePgObjectBindParameters() {
        PgRoutineCatalog.RoutineMeta proc = new PgRoutineCatalog.RoutineMeta(QUALIFIED, 'p');
        assertEquals("CALL " + QUALIFIED + "(?, ?, ?)", MicexRowRepository.buildDealFxCallSql(proc));
    }

    @Test
    void callSqlUsesSelectForFunction() {
        PgRoutineCatalog.RoutineMeta fn = new PgRoutineCatalog.RoutineMeta(QUALIFIED, 'f');
        assertEquals("SELECT * FROM " + QUALIFIED + "(?, ?, ?)", MicexRowRepository.buildDealFxCallSql(fn));
    }

    @Test
    void compositeTextLiteralQuotesMicexCodes() {
        Object[] main = new Object[24];
        main[8] = "S";
        main[20] = "S";
        String cast = MicexRowRepository.sqlCastComposite(main, PG_TYPE);
        assertTrue(cast.contains("\"S\""), cast);
        assertTrue(cast.startsWith("'("), cast);
    }
}
