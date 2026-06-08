package ru.inversion.LoaderMicexFX.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class PgRoutineCatalog {

    private static final Logger log = LoggerFactory.getLogger(PgRoutineCatalog.class);

    private static final String SQL = """
            SELECT n.nspname, p.prokind::text AS prokind
              FROM pg_proc p
              JOIN pg_namespace n ON p.pronamespace = n.oid
             WHERE p.proname = ?
               AND strpos(pg_get_function_identity_arguments(p.oid), ?) > 0
             ORDER BY CASE WHEN n.nspname = ? THEN 0 ELSE 1 END
             LIMIT 1
            """;

    private final JdbcTemplate jdbc;
    private final ConcurrentMap<String, RoutineMeta> cache = new ConcurrentHashMap<>();

    public PgRoutineCatalog(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    public RoutineMeta lookup(String routineName, String preferredSchema, String compositeTypeSuffix) {
        String key = routineName + "|" + preferredSchema + "|" + compositeTypeSuffix;
        return cache.computeIfAbsent(key, k -> load(routineName, preferredSchema, compositeTypeSuffix));
    }

    private RoutineMeta load(String routineName, String preferredSchema, String compositeTypeSuffix) {
        try {
            List<RoutineMeta> rows = jdbc.query(
                    SQL,
                    (rs, rowNum) -> new RoutineMeta(
                            rs.getString("nspname") + "." + routineName,
                            rs.getString("prokind").charAt(0)),
                    routineName,
                    compositeTypeSuffix,
                    preferredSchema);
            if (!rows.isEmpty()) {
                RoutineMeta meta = rows.get(0);
                log.info("pg_proc {} -> {} prokind={}", routineName, meta.qualifiedName(), meta.prokind());
                return meta;
            }
        } catch (Exception e) {
            log.warn("pg_proc lookup {}: {}", routineName, e.getMessage());
        }
        String fallback = preferredSchema + "." + routineName;
        log.warn("pg_proc: {} не найден, fallback {}", routineName, fallback);
        return new RoutineMeta(fallback, 'p');
    }

    public record RoutineMeta(String qualifiedName, char prokind) {
        public boolean isFunction() {
            return prokind == 'f';
        }
    }
}
