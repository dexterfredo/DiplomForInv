package ru.inversion.LoaderMicexFX.gateway;

import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.service.TesystimeSyncService;

import java.util.ArrayList;
import java.util.Map;

public class MicexBuffDefaults {

    public static void apply(
            BufferConfig buff,
            Map<String, String> viewFields,
            TesystimeSyncService tesystimeSyncService) {
        if (buff == null || viewFields == null || viewFields.isEmpty()) {
            return;
        }
        normalizeMicexDates(viewFields);
        if (buff.isQuoteBuffer()) {
            applyTradeDateFromTesystime(viewFields, tesystimeSyncService);
        }
        if (buff.isOrderBuffer()) {
            applyOrderDateFromTesystime(viewFields, tesystimeSyncService);
        }
    }

    public static void normalizeMicexDates(Map<String, String> viewFields) {
        for (Map.Entry<String, String> e : new ArrayList<>(viewFields.entrySet())) {
            if (e.getValue() == null || e.getValue().isBlank()) {
                continue;
            }
            String key = e.getKey().toLowerCase();
            if (isTradeDateKey(key)) {
                String normalized = MicexDateFormats.normalizeTradeDate(e.getValue());
                if (normalized != null) {
                    viewFields.put(key, normalized);
                }
            } else if (isTimeKey(key)) {
                String normalized = MicexDateFormats.normalizeTime(e.getValue());
                if (normalized != null) {
                    viewFields.put(key, normalized);
                }
            }
        }
    }

    private static void applyTradeDateFromTesystime(
            Map<String, String> viewFields,
            TesystimeSyncService tesystimeSyncService) {
        if (tesystimeSyncService == null) {
            return;
        }
        String rawDate = tesystimeSyncService.getMicexSysTradeDate();
        if (rawDate == null || rawDate.isBlank() || rawDate.indexOf('[') > 0) {
            return;
        }
        String date = MicexDateFormats.normalizeTradeDate(rawDate);
        if (date == null) {
            return;
        }
        viewFields.put("raw_tradedate", date);
    }

    private static void applyOrderDateFromTesystime(
            Map<String, String> viewFields,
            TesystimeSyncService tesystimeSyncService) {
        if (tesystimeSyncService == null) {
            return;
        }
        String date = MicexDateFormats.normalizeTradeDate(tesystimeSyncService.getMicexSysTradeDate());
        if (date == null) {
            return;
        }
        viewFields.put("raw_orderdate", date);
    }

    private static boolean isTradeDateKey(String key) {
        return "tradedate".equals(key)
                || "raw_tradedate".equals(key)
                || "settledate".equals(key)
                || "raw_settledate".equals(key)
                || "orderdate".equals(key)
                || "raw_orderdate".equals(key);
    }

    private static boolean isTimeKey(String key) {
        return "raw_time".equals(key)
                || "tradetime".equals(key)
                || "raw_tradetime".equals(key)
                || "ordertime".equals(key)
                || "raw_ordertime".equals(key)
                || "withdrawtime".equals(key)
                || "raw_withdrawtime".equals(key)
                || "activationtime".equals(key)
                || "raw_activationtime".equals(key);
    }
}
