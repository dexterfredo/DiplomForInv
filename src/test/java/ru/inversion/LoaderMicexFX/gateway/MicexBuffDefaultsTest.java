package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;
import ru.inversion.LoaderMicexFX.service.TesystimeSyncService;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MicexBuffDefaultsTest {

    @Test
    void quoteBuffGetsTradeDateFromTesystime() {
        Map<String, String> view = new LinkedHashMap<>();
        view.put("raw_secboard", "CETS");
        TesystimeSyncService tesy = tesystimeWithDate("20250602");

        MicexBuffDefaults.apply(quoteBuff(), view, tesy);

        assertEquals("20250602", view.get("raw_tradedate"));
    }

    @Test
    void quoteBuffRejectsTradeDateWithBracketLikeEtalon() {
        Map<String, String> view = new LinkedHashMap<>();
        TesystimeSyncService tesy = new TesystimeSyncService(null);
        setMicexSysTradeDate(tesy, "20250602[bad]");

        MicexBuffDefaults.apply(quoteBuff(), view, tesy);

        assertFalse(view.containsKey("raw_tradedate"));
    }

    private static TesystimeSyncService tesystimeWithDate(String date) {
        TesystimeSyncService tesy = new TesystimeSyncService(null);
        MicexTableRow row = new MicexTableRow();
        row.setTableName("TESYSTIME");
        row.setSnapshot(true);
        row.putField("DATE", date);
        row.putField("TIME", "120000");
        tesy.processRows(List.of(row));
        return tesy;
    }

    private static void setMicexSysTradeDate(TesystimeSyncService tesy, String date) {
        try {
            Field f = TesystimeSyncService.class.getDeclaredField("micexSysTradeDate");
            f.setAccessible(true);
            f.set(tesy, date);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static BufferConfig quoteBuff() {
        BufferConfig c = new BufferConfig();
        c.setTypeBuff(5922);
        c.setTypeSrc(2432);
        c.setFunctionName("MICEX_FX_QUOTE_DATA_INS");
        return c;
    }
}
