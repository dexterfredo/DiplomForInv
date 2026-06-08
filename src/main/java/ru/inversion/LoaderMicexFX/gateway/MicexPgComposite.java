package ru.inversion.LoaderMicexFX.gateway;

import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.sql.Timestamp;

/** Значение composite-типа PostgreSQL для вызова процедур. */
public final class MicexPgComposite {

    private MicexPgComposite() {
    }

    public static PGobject toPgObject(String pgCompositeType, Object[] attrs) throws SQLException {
        PGobject obj = new PGobject();
        obj.setType(pgCompositeType);
        obj.setValue(formatLiteral(attrs));
        return obj;
    }

    public static String formatLiteral(Object[] attrs) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < attrs.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(formatField(attrs[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String formatField(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Number n) {
            return n.toString();
        }
        if (value instanceof Timestamp ts) {
            return quote(formatTimestamp(ts));
        }
        String s = String.valueOf(value);
        if (s.matches("-?\\d+(\\.\\d+)?")) {
            return s;
        }
        return quote(s);
    }

    private static String formatTimestamp(Timestamp ts) {
        String s = ts.toString();
        if (s.endsWith(".0")) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
