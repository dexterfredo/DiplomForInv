package ru.inversion.LoaderMicexFX.db;

import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

public class MutableDataSource extends AbstractDataSource {

    private final AtomicReference<DataSource> target = new AtomicReference<>();

    public void setTarget(DataSource dataSource) {
        DataSource previous = target.getAndSet(dataSource);
        closeQuietly(previous);
    }

    public boolean isConfigured() {
        return target.get() != null;
    }

    @Override
    public Connection getConnection() throws SQLException {
        DataSource ds = target.get();
        if (ds == null) {
            throw new SQLException("База данных не настроена. Откройте начальную страницу и нажмите «Подключиться».");
        }
        return ds.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        DataSource ds = target.get();
        if (ds == null) {
            throw new SQLException("База данных не настроена.");
        }
        return ds.getConnection(username, password);
    }

    private static void closeQuietly(DataSource ds) {
        if (ds instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
