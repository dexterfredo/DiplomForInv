package ru.inversion.LoaderMicexFX.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.inversion.LoaderMicexFX.model.MicexTargetEntry;
import ru.inversion.LoaderMicexFX.service.LoaderConstantsService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MicexTargetRepository {

    private static final Logger log = LoggerFactory.getLogger(MicexTargetRepository.class);

    private static final String SQL = """
            SELECT type_section::int AS type_section,
                   type_buff::int AS type_buff,
                   field,
                   buff_field,
                   db_source
              FROM tr__data_view.v_tr_micex_target
             WHERE type_buff = ?
               AND type_section = ?
             ORDER BY buff_field
            """;

    private final JdbcTemplate jdbc;
    private final LoaderConstantsService loaderConstants;
    private final Map<Integer, List<MicexTargetEntry>> cache = new ConcurrentHashMap<>();

    public MicexTargetRepository(JdbcTemplate jdbcTemplate, LoaderConstantsService loaderConstants) {
        this.jdbc = jdbcTemplate;
        this.loaderConstants = loaderConstants;
    }

    public List<MicexTargetEntry> loadForBuff(int typeBuff) {
        return loadForBuff(typeBuff, loaderConstants.getTypeSection());
    }

    public List<MicexTargetEntry> loadForBuff(int typeBuff, int typeSection) {
        int cacheKey = typeBuff * 1_000_000 + typeSection;
        return cache.computeIfAbsent(cacheKey, k -> loadFromDb(typeBuff, typeSection));
    }

    public void reload() {
        cache.clear();
    }

    private List<MicexTargetEntry> loadFromDb(int typeBuff, int typeSection) {
        try {
            List<MicexTargetEntry> list = jdbc.query(SQL, (rs, rowNum) -> {
                MicexTargetEntry e = new MicexTargetEntry();
                e.setTypeSection(rs.getInt("type_section"));
                e.setTypeBuff(rs.getInt("type_buff"));
                e.setField(rs.getString("field"));
                e.setBuffField(rs.getString("buff_field"));
                e.setDbSource(rs.getString("db_source"));
                return e;
            }, typeBuff, typeSection);
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception ex) {
            log.warn("v_tr_micex_target type_buff={} type_section={}: {}",
                    typeBuff, typeSection, ex.getMessage());
        }
        return List.of();
    }
}
