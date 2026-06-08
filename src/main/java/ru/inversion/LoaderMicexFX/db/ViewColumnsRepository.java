package ru.inversion.LoaderMicexFX.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.inversion.LoaderMicexFX.model.BufferConfig;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ViewColumnsRepository {

    private static final Logger log = LoggerFactory.getLogger(ViewColumnsRepository.class);

    private static final String SQL = """
            SELECT column_name, data_type
              FROM information_schema.columns
             WHERE table_schema = ?
               AND table_name = ?
             ORDER BY ordinal_position
            """;

    private static final String METADATA_PATH = "/metadata/view_columns.json";

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Map<String, List<ViewColumnMeta>> cache = new ConcurrentHashMap<>();

    public ViewColumnsRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbc = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ViewColumnMeta> columnsFor(BufferConfig buff) {
        String view = buff.getPgViewName();
        if (view.isBlank()) {
            return List.of();
        }
        return cache.computeIfAbsent(view, this::load);
    }

    private List<ViewColumnMeta> load(String viewName) {
        try {
            List<ViewColumnMeta> fromDb = jdbc.query(SQL, (rs, n) -> new ViewColumnMeta(
                    rs.getString("column_name").toLowerCase(),
                    rs.getString("data_type")), BufferConfig.SCHEMA_VIEW, viewName);
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        } catch (Exception e) {
            log.warn("columns {}.{}: {}", BufferConfig.SCHEMA_VIEW, viewName, e.getMessage());
        }
        List<ViewColumnMeta> fromJson = loadFromClasspath(viewName);
        if (!fromJson.isEmpty()) {
            log.info("columns {}.{}: fallback from metadata/view_columns.json", BufferConfig.SCHEMA_VIEW, viewName);
        }
        return fromJson;
    }

    private List<ViewColumnMeta> loadFromClasspath(String viewName) {
        try (InputStream in = ViewColumnsRepository.class.getResourceAsStream(METADATA_PATH)) {
            if (in == null) {
                return List.of();
            }
            Map<String, List<String>> all = objectMapper.readValue(in, new TypeReference<>() {
            });
            List<String> names = all.get(viewName);
            if (names == null || names.isEmpty()) {
                return List.of();
            }
            List<ViewColumnMeta> cols = new ArrayList<>(names.size());
            for (String name : names) {
                cols.add(new ViewColumnMeta(name.toLowerCase(), "varchar"));
            }
            return cols;
        } catch (Exception e) {
            log.warn("metadata {} for {}: {}", METADATA_PATH, viewName, e.getMessage());
            return List.of();
        }
    }
}
