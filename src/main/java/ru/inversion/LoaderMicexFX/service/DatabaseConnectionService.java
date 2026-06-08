package ru.inversion.LoaderMicexFX.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.db.BoardFilterRepository;
import ru.inversion.LoaderMicexFX.db.MicexTargetRepository;
import ru.inversion.LoaderMicexFX.db.MutableDataSource;
import ru.inversion.LoaderMicexFX.db.SecurityFilterRepository;
import ru.inversion.LoaderMicexFX.model.DatabaseConnectionSettings;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Lazy;

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

    @Value("${app.db.auto-connect-on-start:true}")
    private boolean autoConnectOnStart;

    @Value("${spring.datasource.url}")
    private String defaultUrl;

    @Value("${spring.datasource.username}")
    private String defaultUsername;

    @Value("${spring.datasource.password:}")
    private String defaultPassword;

    private volatile DatabaseConnectionSettings current;
    private volatile boolean configured;

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

    @PostConstruct
    void tryAutoConnectOnStart() {
        if (!autoConnectOnStart) {
            log.info("БД: автоподключение выключено — откройте http://localhost:8080/ и нажмите «Подключиться»");
            return;
        }
        try {
            apply(getDefaults());
            log.info("БД: автоподключение из spring.datasource / config/application.properties OK");
        } catch (Exception e) {
            log.warn("БД: автоподключение не удалось — откройте http://localhost:8080/ : {}", e.getMessage());
        }
    }

    public MutableDataSource getMutableDataSource() {
        return mutableDataSource;
    }

    public DatabaseConnectionSettings getDefaults() {
        return DatabaseConnectionSettings.fromJdbcUrl(defaultUrl, defaultUsername, defaultPassword);
    }

    public DatabaseConnectionSettings getCurrent() {
        return current != null ? current : getDefaults();
    }

    public boolean isConfigured() {
        return configured && mutableDataSource.isConfigured();
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

        current = copy(settings);
        configured = true;

        loaderConstantsService.reload();
        bufferConfigService.reload();
        micexTargetRepository.reload();
        boardFilterRepository.reload();
        securityFilterRepository.reload();

        log.info("Подключение к БД обновлено: {}", settings.effectiveJdbcUrl());
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
            settings.rebuildJdbcUrl();
        }
        if (settings.getUsername() == null || settings.getUsername().isBlank()) {
            throw new SQLException("Укажите имя пользователя");
        }
    }

    private static DatabaseConnectionSettings copy(DatabaseConnectionSettings s) {
        DatabaseConnectionSettings c = new DatabaseConnectionSettings();
        c.setJdbcUrl(s.effectiveJdbcUrl());
        c.setHost(s.getHost());
        c.setPort(s.getPort());
        c.setDatabase(s.getDatabase());
        c.setUsername(s.getUsername());
        c.setPassword(s.getPassword());
        return c;
    }
}
