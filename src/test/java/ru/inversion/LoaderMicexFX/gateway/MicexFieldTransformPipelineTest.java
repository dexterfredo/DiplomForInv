package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;
import ru.inversion.LoaderMicexFX.db.ViewColumnMeta;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;
import ru.inversion.LoaderMicexFX.model.MicexTargetEntry;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Преобразования до вызова БД: парсер → defaults → struct builder.
 * Сценарии с пустыми, частичными и «битым» данными как с шлюза MICEX.
 */
class MicexFieldTransformPipelineTest {

    @Test
    void quoteFxPartialUpdateOmitsEmptyBidOffer() {
        Map<String, Object> api = Map.of(
                "SECBOARD", "CETS",
                "SECCODE", "KZTRUB_TOM",
                "LAST", "15.4675",
                "BID", "15.4725",
                "OFFER", "",
                "TIME", "09:59:31",
                "OFFERDEPTH", "0");

        Map<String, String> view = mapQuoteFields(api);
        MicexBuffDefaults.apply(quoteBuff(), view, null, constants());

        assertEquals("CETS", view.get("raw_secboard"));
        assertEquals("KZTRUB_TOM", view.get("raw_seccode"));
        assertEquals("15.4675", view.get("raw_last_"));
        assertEquals("15.4725", view.get("raw_bid"));
        assertFalse(view.containsKey("raw_offer"));
        assertEquals("095931", view.get("raw_time"));
        assertEquals("0", view.get("raw_offerdepth"));
    }

    @Test
    void quoteFxGetsTradeDateFromTesystimeWhenMissingInRow() {
        Map<String, String> view = mapQuoteFields(Map.of(
                "SECBOARD", "CETS",
                "SECCODE", "USD000UTSTOM",
                "LAST", "74.05"));

        TesystimeSyncService tesy = tesystimeWithDate("20260602");
        MicexBuffDefaults.apply(quoteBuff(), view, tesy, constants());

        assertEquals("20260602", view.get("raw_tradedate"));
    }

    @Test
    void dealFxNormalizesTradeDateAndTime() {
        Map<String, String> view = mapDealFields(Map.of(
                "SECBOARD", "CETS",
                "SECCODE", "USD000UTSTOM",
                "TRADENO", "900000001",
                "TRADEDATE", "2026-06-02",
                "TRADETIME", "14:30:45",
                "PRICE", "73.9850",
                "VALUE", "73985.00",
                "COMMISSION", "0.00"));

        MicexBuffDefaults.apply(dealBuff(), view, null, constants());

        assertEquals("20260602", view.get("raw_tradedate"));
        assertEquals("143045", view.get("raw_tradetime"));
        assertEquals("73985.00", view.get("raw_value_"));
        assertEquals("0.00", view.get("raw_commission"));
    }

    @Test
    void dealFxNullPlaceholderFieldsOmitted() {
        Map<String, Object> api = new LinkedHashMap<>();
        api.put("SECBOARD", "CETS");
        api.put("SECCODE", "USD000UTSTOM");
        api.put("TRADENO", "900000002");
        api.put("TRADEDATE", "20260602");
        api.put("TRADETIME", "102230");
        api.put("BROKERREF", "[null]");
        api.put("EXTREF", "null");
        api.put("CPFIRMID", "   ");

        Map<String, String> view = mapDealFields(api);
        MicexBuffDefaults.apply(dealBuff(), view, null, constants());

        assertFalse(view.containsKey("raw_brokerref"));
        assertFalse(view.containsKey("raw_extref"));
        assertFalse(view.containsKey("raw_cpfirmid"));
        assertEquals("900000002", view.get("raw_tradeno"));
    }

    @Test
    void structBuilderNullsForEmptyMappedFields() {
        Map<String, String> view = new LinkedHashMap<>();
        view.put("raw_secboard", "CETS");
        view.put("raw_seccode", "USD000UTSTOM");
        view.put("raw_bid", "   ");
        view.put("raw_offer", "");
        view.put("raw_last_", "74.05");

        List<ViewColumnMeta> cols = List.of(
                new ViewColumnMeta("raw_secboard", "varchar"),
                new ViewColumnMeta("raw_seccode", "varchar"),
                new ViewColumnMeta("raw_bid", "varchar"),
                new ViewColumnMeta("raw_offer", "varchar"),
                new ViewColumnMeta("raw_last_", "varchar"));

        Object[] attrs = MicexBuffStructBuilder.buildAttributes(cols, view, null);

        assertEquals("CETS", attrs[0]);
        assertEquals("USD000UTSTOM", attrs[1]);
        assertNull(attrs[2]);
        assertNull(attrs[3]);
        assertEquals("74.05", attrs[4]);
    }

    @Test
    void structBuilderNormalizesTradeTimeColumns() {
        Map<String, String> view = Map.of(
                "raw_tradedate", "2026-06-02",
                "raw_tradetime", "14:30:45",
                "raw_settledate", "20260605");

        List<ViewColumnMeta> cols = List.of(
                new ViewColumnMeta("raw_tradedate", "varchar"),
                new ViewColumnMeta("raw_tradetime", "varchar"),
                new ViewColumnMeta("raw_settledate", "varchar"));

        Object[] attrs = MicexBuffStructBuilder.buildAttributes(cols, view, null);

        assertEquals("20260602", attrs[0]);
        assertEquals("143045", attrs[1]);
        assertEquals("20260605", attrs[2]);
    }

    @Test
    void decimalBuffKeepsZeroLotSize() {
        Map<String, String> view = mapDecimalFields(Map.of(
                "SECBOARD", "CETS",
                "SECCODE", "CNYRUB_TMS",
                "DECIMALS", "6",
                "LOTSIZE", "1"));

        MicexBuffDefaults.apply(decimalBuff(), view, null, constants());

        assertEquals("6", view.get("raw_decimals"));
        assertEquals("1", view.get("raw_lotsize"));
    }

    @Test
    void incrementalQuoteRowWithMissingOptionalFields() {
        Map<String, Object> api = Map.of(
                "SECBOARD", "CETS",
                "SECCODE", "USD000UTSTOM",
                "LAST", "74.0600",
                "TIME", "14:30:50",
                "DECIMALS", "4");

        Map<String, String> view = mapQuoteFields(api);
        MicexBuffDefaults.apply(quoteBuff(), view, tesystimeWithDate("20260602"), constants());

        assertEquals("74.0600", view.get("raw_last_"));
        assertEquals("143050", view.get("raw_time"));
        assertEquals("4", view.get("raw_decimals"));
        assertFalse(view.containsKey("raw_bid"));
        assertFalse(view.containsKey("raw_open_"));
    }

    @Test
    void boardBuffMapsMinimalFields() {
        Map<String, String> view = MicexBuffRowParser.mapFromTarget(
                List.of(entry("boardid", "raw_boardid"), entry("boardname", "raw_boardname"),
                        entry("marketid", "raw_marketid"), entry("status", "raw_status")),
                Map.of("BOARDID", "CETS", "BOARDNAME", "", "MARKETID", "CURR", "STATUS", "A"));

        assertEquals("CETS", view.get("raw_boardid"));
        assertFalse(view.containsKey("raw_boardname"));
        assertEquals("CURR", view.get("raw_marketid"));
        assertEquals("A", view.get("raw_status"));
    }

    private static Map<String, String> mapQuoteFields(Map<String, Object> api) {
        return MicexBuffRowParser.mapFromTarget(List.of(
                entry("secboard", "raw_secboard"),
                entry("seccode", "raw_seccode"),
                entry("last", "raw_last_"),
                entry("bid", "raw_bid"),
                entry("offer", "raw_offer"),
                entry("open", "raw_open_"),
                entry("time", "raw_time"),
                entry("offerdepth", "raw_offerdepth"),
                entry("decimals", "raw_decimals")), api);
    }

    private static Map<String, String> mapDealFields(Map<String, Object> api) {
        return MicexBuffRowParser.mapFromTarget(List.of(
                entry("secboard", "raw_secboard"),
                entry("seccode", "raw_seccode"),
                entry("tradeno", "raw_tradeno"),
                entry("tradedate", "raw_tradedate"),
                entry("tradetime", "raw_tradetime"),
                entry("price", "raw_price"),
                entry("value", "raw_value_"),
                entry("commission", "raw_commission"),
                entry("brokerref", "raw_brokerref"),
                entry("extref", "raw_extref"),
                entry("cpfirmid", "raw_cpfirmid")), api);
    }

    private static Map<String, String> mapDecimalFields(Map<String, Object> api) {
        return MicexBuffRowParser.mapFromTarget(List.of(
                entry("secboard", "raw_secboard"),
                entry("seccode", "raw_seccode"),
                entry("decimals", "raw_decimals"),
                entry("lotsize", "raw_lotsize")), api);
    }

    private static MicexTargetEntry entry(String field, String buffField) {
        MicexTargetEntry e = new MicexTargetEntry();
        e.setField(field);
        e.setBuffField(buffField);
        e.setTypeSection(6246);
        return e;
    }

    private static TesystimeSyncService tesystimeWithDate(String date) {
        TesystimeSyncService tesy = new TesystimeSyncService(null);
        MicexTableRow row = new MicexTableRow();
        row.setTableName("TESYSTIME");
        row.putField("DATE", date);
        row.putField("TIME", "120000");
        tesy.processRows(List.of(row));
        return tesy;
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

    private static BufferConfig decimalBuff() {
        BufferConfig c = new BufferConfig();
        c.setTypeBuff(5886);
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
            setIntField(c, "fallbackBuffDecimal", 5886);
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
