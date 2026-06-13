package ru.inversion.LoaderMicexFX.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class SecurityFilterRepository {

    private static final Logger log = LoggerFactory.getLogger(SecurityFilterRepository.class);

    private final JdbcTemplate jdbc;

    @Value("${app.loader.security.filter-enabled:true}")
    private boolean filterEnabled;

    private volatile Set<String> allowedSecCodes;
    private volatile boolean dictionaryLoaded;

    public SecurityFilterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    public boolean isEnabled() {
        return filterEnabled;
    }

    public boolean isAllowed(String seccode) {
        if (!filterEnabled) {
            return true;
        }
        ensureLoaded();
        if (!dictionaryLoaded) {
            return true;
        }
        if (seccode == null || seccode.isBlank()) {
            return false;
        }
        return allowedSecCodes.contains(seccode.trim().toUpperCase());
    }

    public Set<String> getAllowed() {
        ensureLoaded();
        return dictionaryLoaded ? allowedSecCodes : Set.of();
    }

    public void reload() {
        allowedSecCodes = null;
        dictionaryLoaded = false;
    }

    private void ensureLoaded() {
        if (!filterEnabled) {
            return;
        }
        if (allowedSecCodes == null) {
            synchronized (this) {
                if (allowedSecCodes == null) {
                    load();
                }
            }
        }
    }

    private void load() {
        try {
            List<String> list = jdbc.queryForList(
                    """
                    SELECT upper(tf_code_micex)
                      FROM tr__data_view.v_tr_dict_sec
                     WHERE tf_code_micex IS NOT NULL
                       AND btrim(tf_code_micex) <> ''
                    """,
                    String.class);
            allowedSecCodes = list.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toCollection(HashSet::new));
            dictionaryLoaded = true;
            log.info("Фильтр seccode: v_tr_dict_sec — {} инструментов", allowedSecCodes.size());
            if (allowedSecCodes.isEmpty()) {
                log.warn("Справочник инструментов пуст (v_tr_dict_sec)");
            }
        } catch (Exception e) {
            allowedSecCodes = Set.of();
            dictionaryLoaded = false;
            log.warn("v_tr_dict_sec недоступно: {} — фильтр seccode пропускается", e.getMessage());
        }
    }
}
