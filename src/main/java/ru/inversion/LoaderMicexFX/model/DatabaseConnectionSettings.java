package ru.inversion.LoaderMicexFX.model;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseConnectionSettings {

    private static final Pattern JDBC_PG =
            Pattern.compile("jdbc:postgresql://([^/:]+)(?::(\\d+))?/([^?]+)", Pattern.CASE_INSENSITIVE);

    private String jdbcUrl;
    private String host;
    private int port = 5432;
    private String database;
    private String username;
    private String password;

    public static DatabaseConnectionSettings fromJdbcUrl(
            String jdbcUrl, String username, String password) {
        DatabaseConnectionSettings s = new DatabaseConnectionSettings();
        s.setUsername(username);
        s.setPassword(password != null ? password : "");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            s.setHost("localhost");
            s.setPort(5432);
            s.setDatabase("exchange_db");
            s.rebuildJdbcUrl();
            return s;
        }
        s.setJdbcUrl(jdbcUrl.trim());
        Matcher m = JDBC_PG.matcher(s.getJdbcUrl());
        if (m.find()) {
            s.setHost(m.group(1));
            if (m.group(2) != null && !m.group(2).isBlank()) {
                s.setPort(Integer.parseInt(m.group(2)));
            }
            s.setDatabase(m.group(3));
        } else {
            s.setHost("localhost");
            s.setPort(5432);
            s.setDatabase("exchange_db");
        }
        return s;
    }

    public String effectiveJdbcUrl() {
        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            return jdbcUrl.trim();
        }
        return buildJdbcUrl(host, port, database);
    }

    public void rebuildJdbcUrl() {
        this.jdbcUrl = buildJdbcUrl(host, port, database);
    }

    public static String buildJdbcUrl(String host, int port, String database) {
        String h = host == null || host.isBlank() ? "localhost" : host.trim();
        int p = port > 0 ? port : 5432;
        String db = database == null || database.isBlank() ? "exchange_db" : database.trim();
        return "jdbc:postgresql://" + h + ":" + p + "/" + db;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
