package ru.inversion.LoaderMicexFX.gateway;

import ru.inversion.LoaderMicexFX.model.GatewayConnectionSettings;

import java.util.LinkedHashMap;
import java.util.Map;

class MicexConnectionStrings {

    private static final String EOL = "\r\n";

    static String buildConnectString(GatewayConnectionSettings s) {
        return mapToConnectString(astsBridgeParams(s));
    }

    static Map<String, String> astsBridgeStartParams(GatewayConnectionSettings s) {
        Map<String, String> params = new LinkedHashMap<>();
        put(params, "Host", resolveHostWithPort(s));
        put(params, "Server", s.getServer());
        put(params, "UserID", s.getUserId());
        put(params, "Password", s.getPassword());
        put(params, "Interface", s.getApiInterface());
        put(params, "Feedback", s.getFeedback());
        put(params, "PacketSize", s.getPacketSize());
        return params;
    }

    static Map<String, String> multicastParams(GatewayConnectionSettings s) {
        Map<String, String> params = new LinkedHashMap<>();
        put(params, "PacketSize", s.getPacketSize());
        put(params, "Interface", s.getApiInterface());
        put(params, "Server", s.getServer());
        put(params, "Service", s.getService());
        put(params, "Broadcast", s.getBroadcast());
        put(params, "PrefBroadcast", s.getPrefBroadcast());
        put(params, "UserID", s.getUserId());
        put(params, "Password", s.getPassword());
        return params;
    }

    static String resolveHostWithPort(GatewayConnectionSettings s) {
        String h = nz(s.getHost()).trim();
        if (h.isEmpty()) {
            return "";
        }
        if (h.contains(":")) {
            return h;
        }
        String service = nz(s.getService()).trim();
        if (service.matches("\\d+")) {
            return h + ":" + service;
        }
        return h;
    }

    private static Map<String, String> astsBridgeParams(GatewayConnectionSettings s) {
        Map<String, String> params = new LinkedHashMap<>();
        put(params, "HOST", resolveHostWithPort(s));
        put(params, "SERVER", s.getServer());
        put(params, "USERID", s.getUserId());
        put(params, "PASSWORD", s.getPassword());
        put(params, "INTERFACE", s.getApiInterface());
        put(params, "FEEDBACK", s.getFeedback());
        put(params, "LOGGING", "0");
        return params;
    }

    private static String mapToConnectString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append(EOL);
        }
        return sb.toString();
    }

    private static void put(Map<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value.trim());
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
