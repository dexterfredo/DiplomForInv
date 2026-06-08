package ru.inversion.LoaderMicexFX.gateway;

public final class MicexValueNormalizer {

    private MicexValueNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return "";
        }
        if (isNullPlaceholder(s)) {
            return null;
        }
        if (s.length() >= 2 && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']') {
            String inner = s.substring(1, s.length() - 1).trim();
            if (inner.isEmpty() || isNullPlaceholder(inner)) {
                return null;
            }
            return inner;
        }
        if (s.indexOf('[') > 0) {
            return "";
        }
        return s;
    }

    private static boolean isNullPlaceholder(String s) {
        return "null".equalsIgnoreCase(s)
                || "[null]".equalsIgnoreCase(s)
                || "NULL".equals(s);
    }
}
