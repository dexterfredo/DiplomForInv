package ru.inversion.LoaderMicexFX.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.service.LoaderConstantsService;

import java.util.List;

@Repository
public class BufferConfigRepository {

    private static final Logger log = LoggerFactory.getLogger(BufferConfigRepository.class);

    private static final String SQL_STRICT = """
            SELECT b.type_src::int AS type_src,
                   b.type_buff::int AS type_buff,
                   b.src_table_name,
                   b.package_,
                   b.view_,
                   b.function_
              FROM tr__data_view.v_tr_buff_target b
             WHERE b.type_src = ?
               AND b.src_table_name IS NOT NULL
               AND b.function_ IS NOT NULL
               AND EXISTS (
                   SELECT 1
                     FROM tr__data_view.v_tr_micex_target m
                    WHERE m.type_buff = b.type_buff
                      AND m.type_section = ?
               )
             ORDER BY b.type_buff
            """;

    private static final String SQL_BY_TYPE_BUFF = """
            SELECT b.type_src::int AS type_src,
                   b.type_buff::int AS type_buff,
                   b.src_table_name,
                   b.package_,
                   b.view_,
                   b.function_
              FROM tr__data_view.v_tr_buff_target b
             WHERE b.type_buff = ?
               AND b.src_table_name IS NOT NULL
               AND b.function_ IS NOT NULL
             LIMIT 1
            """;

    private static final String SQL_BY_FUNCTION = """
            SELECT b.type_src::int AS type_src,
                   b.type_buff::int AS type_buff,
                   b.src_table_name,
                   b.package_,
                   b.view_,
                   b.function_
              FROM tr__data_view.v_tr_buff_target b
             WHERE upper(b.function_) LIKE ?
               AND b.src_table_name IS NOT NULL
               AND b.function_ IS NOT NULL
             ORDER BY b.type_buff
             LIMIT 1
            """;

    private final JdbcTemplate jdbc;
    private final LoaderConstantsService loaderConstants;

    @Value("${app.loader.poll-interval-sec:5}")
    private int defaultPollIntervalSec;

    @Value("${app.loader.load-full:true}")
    private boolean defaultLoadFull;

    public BufferConfigRepository(JdbcTemplate jdbcTemplate, LoaderConstantsService loaderConstants) {
        this.jdbc = jdbcTemplate;
        this.loaderConstants = loaderConstants;
    }

    public List<BufferConfig> loadForTypeSrc(int typeSrc) {
        int typeSection = loaderConstants.getTypeSection();
        List<BufferConfig> list = jdbc.query(SQL_STRICT, rowMapper(), typeSrc, typeSection);
        if (list.isEmpty()) {
            throw new IllegalStateException(
                    "v_tr_buff_target: нет буферов для type_src=" + typeSrc
                            + " и type_section=" + typeSection
                            + " (проверьте tr_buff_target и tr_micex_target)");
        }
        log.info("Буферы из БД: {} шт. (type_src={}, type_section={})", list.size(), typeSrc, typeSection);
        return list;
    }

    public BufferConfig loadByTypeBuff(int typeBuff) {
        List<BufferConfig> list = jdbc.query(SQL_BY_TYPE_BUFF, rowMapper(), typeBuff);
        return list.isEmpty() ? null : list.get(0);
    }

    public BufferConfig loadByFunctionContains(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return null;
        }
        List<BufferConfig> list = jdbc.query(
                SQL_BY_FUNCTION,
                rowMapper(),
                "%" + fragment.trim().toUpperCase() + "%");
        return list.isEmpty() ? null : list.get(0);
    }

    private RowMapper<BufferConfig> rowMapper() {
        return (rs, rowNum) -> {
            BufferConfig item = new BufferConfig();
            item.setTypeSrc(rs.getInt("type_src"));
            item.setTypeBuff(rs.getInt("type_buff"));
            item.setMicexTable(rs.getString("src_table_name"));
            item.setPackageName(rs.getString("package_"));
            item.setViewName(rs.getString("view_"));
            String fn = rs.getString("function_");
            item.setFunctionName(fn);
            item.setSaveProcedure(fn);
            item.setPollIntervalSec(defaultPollIntervalSec);
            item.setLoadFull(defaultLoadFull);
            item.setBufferKind(BufferConfig.inferBufferKind(fn));
            return item;
        };
    }
}
