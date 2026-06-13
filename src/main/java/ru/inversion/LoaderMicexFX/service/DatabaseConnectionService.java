package ru.inversion.LoaderMicexFX.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.db.BoardFilterRepository;
import ru.inversion.LoaderMicexFX.db.MicexTargetRepository;
import ru.inversion.LoaderMicexFX.db.MutableDataSource;
import ru.inversion.LoaderMicexFX.db.SecurityFilterRepository;
import ru.inversion.LoaderMicexFX.model.DatabaseConnectionSettings;

import java.sql.Connection;
import java.sql.SQLException;

@Service
public class DatabaseConnectionService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionService.class);

    private final MutableDataSource mutableDataSource;
    private final BufferConfigService bufferConfigService;
    private final BoardFilterRepository boardFilterRepository;
    private final SecurityFilterRepository securityFilterRepository;
    private final MicexTargetRepository micexTargetRepository;
    private final LoaderConstantsService loaderConstantsService;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    public DatabaseConnectionService(
            MutableDataSource mutableDataSource,
            @Lazy BufferConfigService bufferConfigService,
            @Lazy BoardFilterRepository boardFilterRepository,
            @Lazy SecurityFilterRepository securityFilterRepository,
            @Lazy MicexTargetRepository micexTargetRepository,
            @Lazy LoaderConstantsService loaderConstantsService) {
        this.mutableDataSource = mutableDataSource;
        this.bufferConfigService = bufferConfigService;
        this.boardFilterRepository = boardFilterRepository;
        this.securityFilterRepository = securityFilterRepository;
        this.micexTargetRepository = micexTargetRepository;
        this.loaderConstantsService = loaderConstantsService;
    }

    public void applyFromCredentials(String username, String password) throws SQLException {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new SQLException("spring.datasource.url не задан в application.properties");
        }
        DatabaseConnectionSettings settings = new DatabaseConnectionSettings();
        settings.setJdbcUrl(jdbcUrl.trim());
        settings.setUsername(username);
        settings.setPassword(password != null ? password : "");
        apply(settings);
    }

    public void apply(DatabaseConnectionSettings settings) throws SQLException {
        if (settings == null) {
            throw new SQLException("Параметры подключения не заданы");
        }
        normalize(settings);
        testConnection(settings);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.effectiveJdbcUrl());
        config.setUsername(settings.getUsername());
        config.setPassword(settings.getPassword() != null ? settings.getPassword() : "");
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        config.setPoolName("LoaderMicexFX-dynamic");
        config.setConnectionTimeout(10_000);

        HikariDataSource pool = new HikariDataSource(config);
        mutableDataSource.setTarget(pool);

        loaderConstantsService.reload();
        bufferConfigService.reload();
        micexTargetRepository.reload();
        boardFilterRepository.reload();
        securityFilterRepository.reload();

        log.info("Подключение к БД: {}", settings.effectiveJdbcUrl());
    }

    public void disconnect() {
        mutableDataSource.setTarget(null);
        log.info("Подключение к БД закрыто (выход из системы)");
    }

    public void testConnection(DatabaseConnectionSettings settings) throws SQLException {
        normalize(settings);
        try (Connection c = openRawConnection(settings)) {
            c.isValid(3);
        }
    }

    private static Connection openRawConnection(DatabaseConnectionSettings settings) throws SQLException {
        return java.sql.DriverManager.getConnection(
                settings.effectiveJdbcUrl(),
                settings.getUsername(),
                settings.getPassword() != null ? settings.getPassword() : "");
    }

    private static void normalize(DatabaseConnectionSettings settings) throws SQLException {
        if (settings.getJdbcUrl() == null || settings.getJdbcUrl().isBlank()) {
            throw new SQLException("JDBC URL не задан (spring.datasource.url)");
        }
        if (settings.getUsername() == null || settings.getUsername().isBlank()) {
            throw new SQLException("Укажите имя пользователя");
        }
    }
}
