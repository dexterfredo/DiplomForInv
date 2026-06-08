package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;
import ru.inversion.LoaderMicexFX.db.ViewColumnMeta;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;
import ru.inversion.LoaderMicexFX.service.LoaderConstantsService;
import ru.inversion.LoaderMicexFX.service.TesystimeSyncService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class MicexBuffDefaultsTest {

    @Test
    void quoteBuffGetsTradeDateFromTesystime() {
        Map<String, String> view = new LinkedHashMap<>();
        view.put("raw_secboard", "CETS");
        TesystimeSyncService tesy = tesystimeWithDate("20250602");

        MicexBuffDefaults.apply(quoteBuff(), view, tesy, constants());

        assertEquals("20250602", view.get("raw_tradedate"));
        assertFalse(view.containsKey("tradedate"));
    }

    @Test
    void quoteBuffRejectsTradeDateWithBracketLikeEtalon() {
        Map<String, String> view = new LinkedHashMap<>();
        TesystimeSyncService tesy = new TesystimeSyncService(null);
        setMicexSysTradeDate(tesy, "20250602[bad]");

        MicexBuffDefaults.apply(quoteBuff(), view, tesy, constants());

        assertFalse(view.containsKey("raw_tradedate"));
    }

    @Test
    void dealFxDoesNotBuildParsedTradeDateTime() {
        Map<String, String> view = new LinkedHashMap<>();
        view.put("raw_tradedate", "20250602");
        view.put("raw_tradetime", "153045");

        MicexBuffDefaults.apply(dealBuff(), view, null, constants());

        assertEquals("20250602", view.get("raw_tradedate"));
        assertEquals("153045", view.get("raw_tradetime"));
        assertFalse(view.containsKey("tradedatetime"));
        assertFalse(view.containsKey("tradedate"));
    }

    @Test
    void emptyValuesBecomeNullInStructBuilder() {
        Object[] attrs = MicexBuffStructBuilder.buildAttributes(
                List.of(new ViewColumnMeta("raw_price", "numeric")),
                Map.of("raw_price", "   "),
                null);
        assertNull(attrs[0]);
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
        return c;
    }

    private static BufferConfig dealBuff() {
        BufferConfig c = new BufferConfig();
        c.setTypeBuff(2323);
        c.setTypeSrc(2432);
        return c;
    }

    private static LoaderConstantsService constants() {
        LoaderConstantsService c = new LoaderConstantsService(null);
        try {
            Method m = LoaderConstantsService.class.getDeclaredMethod("applyFallback");
            m.setAccessible(true);
            m.invoke(c);
            setIntField(c, "fallbackBuffQuoteFx", 5922);
            setIntField(c, "fallbackBuffDeal", 2323);
            m.invoke(c);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return c;
    }

    private static void setIntField(Object target, String name, int value) throws ReflectiveOperationException {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(target, value);
    }
}
