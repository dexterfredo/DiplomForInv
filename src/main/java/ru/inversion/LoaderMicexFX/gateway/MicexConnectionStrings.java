package ru.inversion.LoaderMicexFX.gateway;

import ru.inversion.LoaderMicexFX.model.GatewayConnectionSettings;
import ru.inversion.LoaderMicexFX.model.MicexConnectionType;

import java.util.LinkedHashMap;
import java.util.Map;

final class MicexConnectionStrings {

    private static final String EOL = "\r\n";

    private MicexConnectionStrings() {
    }

    static String buildConnectString(GatewayConnectionSettings s) {
        MicexConnectionType type = s.getConnectionType() != null ? s.getConnectionType() : MicexConnectionType.TCP2;
        return switch (type) {
            case RS232 -> rs232ConnectString(s);
            case TCP -> tcpConnectString(s);
            case CUSTOM -> nz(s.getCustomConnectText()).replace("\n", EOL);
            case TCP2 -> astsBridgeConnectString(s);
        };
    }

    static Map<String, String> astsBridgeParams(GatewayConnectionSettings s) {
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

    static String astsBridgeConnectString(GatewayConnectionSettings s) {
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

    private static String rs232ConnectString(GatewayConnectionSettings s) {
        Map<String, String> params = new LinkedHashMap<>();
        put(params, "COM", s.getComPort());
        put(params, "BAUDRATE", s.getComBaudrate());
        return mapToConnectString(params);
    }

    private static String tcpConnectString(GatewayConnectionSettings s) {
        Map<String, String> params = new LinkedHashMap<>();
        String host = nz(s.getTcpHost());
        if (host.isBlank()) {
            host = resolveHostWithPort(s);
        }
        put(params, "HOST", host);
        put(params, "SERVICE", nz(s.getTcpService()).isBlank() ? s.getService() : s.getTcpService());
        return mapToConnectString(params);
    }

    private static String mapToConnectString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append(EOL);
        }
        return sb.toString();
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

    private static void put(Map<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value.trim());
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
