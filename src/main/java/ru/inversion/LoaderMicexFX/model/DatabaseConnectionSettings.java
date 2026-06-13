package ru.inversion.LoaderMicexFX.model;

public class DatabaseConnectionSettings {

    private String jdbcUrl;
    private String username;
    private String password;

    public String effectiveJdbcUrl() {
        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            return jdbcUrl.trim();
        }
        return "jdbc:postgresql://localhost:5432/exchange_db";
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
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
