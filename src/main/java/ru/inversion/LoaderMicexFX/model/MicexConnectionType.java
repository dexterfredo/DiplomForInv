package ru.inversion.LoaderMicexFX.model;

public enum MicexConnectionType {
    RS232,
    TCP,
    TCP2,
    CUSTOM;

    public static MicexConnectionType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return TCP2;
        }
        return switch (raw.trim().toUpperCase()) {
            case "RS232", "RS-232" -> RS232;
            case "TCP", "TCPIP" -> TCP;
            case "TCP2", "TCPIP2" -> TCP2;
            case "CUSTOM" -> CUSTOM;
            default -> TCP2;
        };
    }
}
