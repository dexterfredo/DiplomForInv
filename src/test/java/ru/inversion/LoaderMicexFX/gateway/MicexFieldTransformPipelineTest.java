package ru.inversion.LoaderMicexFX.gateway;

import org.junit.jupiter.api.Test;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;
import ru.inversion.LoaderMicexFX.model.MicexTargetEntry;
import ru.inversion.LoaderMicexFX.service.TesystimeSyncService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
        MicexBuffDefaults.apply(quoteBuff(), view, null);

        assertEquals("CETS", view.get("raw_secboard"));
        assertEquals("15.4725", view.get("raw_bid"));
        assertFalse(view.containsKey("raw_offer"));
        assertEquals("095931", view.get("raw_time"));
    }

    @Test
    void quoteFxGetsTradeDateFromTesystimeWhenMissingInRow() {
        Map<String, String> view = mapQuoteFields(Map.of(
                "SECBOARD", "CETS",
                "SECCODE", "USD000UTSTOM",
                "LAST", "74.05"));

        MicexBuffDefaults.apply(quoteBuff(), view, tesystimeWithDate("20260602"));

        assertEquals("20260602", view.get("raw_tradedate"));
    }

    private static Map<String, String> mapQuoteFields(Map<String, Object> api) {
        return MicexBuffRowParser.mapFromTarget(List.of(
                entry("secboard", "raw_secboard"),
                entry("seccode", "raw_seccode"),
                entry("last", "raw_last_"),
                entry("bid", "raw_bid"),
                entry("offer", "raw_offer"),
                entry("time", "raw_time"),
                entry("offerdepth", "raw_offerdepth")), api);
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
        c.setFunctionName("MICEX_FX_QUOTE_DATA_INS");
        return c;
    }
}
