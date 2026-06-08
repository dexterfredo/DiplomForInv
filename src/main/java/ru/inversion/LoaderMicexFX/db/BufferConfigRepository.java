package ru.inversion.LoaderMicexFX.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.service.LoaderConstantsService;

import java.util.ArrayList;
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

    private static final String SQL_BY_TYPE_SRC = """
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
             ORDER BY b.type_buff
            """;

    private static final String SQL_BY_BUFF_IDS_PREFIX = """
            SELECT b.type_src::int AS type_src,
                   b.type_buff::int AS type_buff,
                   b.src_table_name,
                   b.package_,
                   b.view_,
                   b.function_
              FROM tr__data_view.v_tr_buff_target b
             WHERE b.type_buff IN (
            """;

    private final JdbcTemplate jdbc;
    private final LoaderConstantsService loaderConstants;

    @Value("${app.loader.poll-interval-sec:5}")
    private int defaultPollIntervalSec;

    @Value("${app.loader.load-full:true}")
    private boolean defaultLoadFull;

    @Value("${app.loader.buffers.embedded-only:false}")
    private boolean embeddedOnly;

    public BufferConfigRepository(JdbcTemplate jdbcTemplate, LoaderConstantsService loaderConstants) {
        this.jdbc = jdbcTemplate;
        this.loaderConstants = loaderConstants;
    }

    public List<BufferConfig> loadForTypeSrc(int typeSrc) {
        if (embeddedOnly) {
            log.info("Буферы: встроенный список (app.loader.buffers.embedded-only=true)");
            return defaultBuffersForSection();
        }
        int typeSection = loaderConstants.getTypeSection();
        try {
            List<BufferConfig> strict = query(SQL_STRICT, typeSrc, typeSection);
            if (!strict.isEmpty()) {
                log.info("Буферы из БД: {} шт. (type_src={}, type_section={})",
                        strict.size(), typeSrc, typeSection);
                return strict;
            }
            log.warn("tr_buff_target: 0 строк при type_src={} и type_section={} (нет пары в tr_micex_target?)",
                    typeSrc, typeSection);

            List<BufferConfig> bySrc = query(SQL_BY_TYPE_SRC, typeSrc);
            if (!bySrc.isEmpty()) {
                log.warn("Буферы из БД: {} шт. только по tr_buff_target (без tr_micex_target)",
                        bySrc.size());
                return bySrc;
            }

            int[] buffIds = buffIdsForSection().stream().mapToInt(Integer::intValue).toArray();
            List<BufferConfig> byBuff = queryByBuffIds(buffIds);
            if (!byBuff.isEmpty()) {
                log.warn("Буферы из БД: {} шт. по type_buff (type_src в таблице мог отличаться)",
                        byBuff.size());
                return byBuff;
            }

            log.warn("tr_buff_target пуст или недоступен — встроенный список буферов FX");
            return defaultBuffersForSection();
        } catch (Exception e) {
            log.warn("v_tr_buff_target недоступен ({}). Встроенный список буферов.",
                    e.getMessage());
            return defaultBuffersForSection();
        }
    }

    private List<BufferConfig> query(String sql, Object... args) {
        return jdbc.query(sql, rowMapper(), args);
    }

    private List<BufferConfig> queryByBuffIds(int[] buffIds) {
        if (buffIds.length == 0) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(buffIds.length, "?"));
        String sql = SQL_BY_BUFF_IDS_PREFIX + placeholders + """
                )
               AND b.src_table_name IS NOT NULL
               AND b.function_ IS NOT NULL
             ORDER BY b.type_buff
            """;
        Object[] args = new Object[buffIds.length];
        for (int i = 0; i < buffIds.length; i++) {
            args[i] = buffIds[i];
        }
        return jdbc.query(sql, rowMapper(), args);
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
            item.setBufferKind("DATA");
            return item;
        };
    }

    private List<Integer> buffIdsForSection() {
        List<Integer> ids = new ArrayList<>();
        ids.add(loaderConstants.buffMicexDecimal());
        ids.add(loaderConstants.buffMicexBoard());
        if (loaderConstants.isMmvSectionSecurityMarket()) {
            ids.add(loaderConstants.buffMicexOrderSec());
            ids.add(loaderConstants.buffMicexDealSec());
            ids.add(loaderConstants.buffMicexQuoteSec());
            ids.add(loaderConstants.buffMicexLotsize());
        } else {
            ids.add(loaderConstants.buffMicexDeal());
            ids.add(loaderConstants.buffMicexQuoteFx());
        }
        return ids;
    }

    private List<BufferConfig> defaultBuffersForSection() {
        int typeSrc = loaderConstants.getTypeSrc();
        if (loaderConstants.isMmvSectionSecurityMarket()) {
            return List.of(
                    make(typeSrc, loaderConstants.buffMicexDecimal(), "SECURITIES", "TR_API_LOADER",
                            "V_TR_BUFF_MICEX_DECIMALS", "MICEX_DECIMALS_DATA_INS"),
                    make(typeSrc, loaderConstants.buffMicexBoard(), "BOARDS", "TR_API_LOADER",
                            "V_TR_BUFF_MICEX_BOARD", "MICEX_BOARD_DATA_INS"),
                    make(typeSrc, loaderConstants.buffMicexOrderSec(), "ORDERS", "TR_API_LOADER",
                            "V_TR_BUFF_MICEX_SEC_ODA", "MICEX_SEC_ODA_DATA_INS"),
                    make(typeSrc, loaderConstants.buffMicexDealSec(), "TRADES", "TR_API_LOADER",
                            "V_TR_BUFF_MICEX_DEAL_SEC", "MICEX_DEAL_SEC_DATA_INS"),
                    make(typeSrc, loaderConstants.buffMicexQuoteSec(), "SECURITIES", "TR_API_LOADER",
                            "V_TR_BUFF_MICEX_SEC_QUOTE", "MICEX_SEC_QUOTE_DATA_INS"),
                    make(typeSrc, loaderConstants.buffMicexLotsize(), "SECURITIES", "TR_API_LOADER",
                            "V_TR_BUFF_MICEX_LOTSIZE", "MICEX_LOTSIZE_DATA_INS")
            );
        }
        return List.of(
                make(typeSrc, loaderConstants.buffMicexDecimal(), "SECURITIES", "TR_API_LOADER",
                        "V_TR_BUFF_MICEX_DECIMALS", "MICEX_DECIMALS_DATA_INS"),
                make(typeSrc, loaderConstants.buffMicexBoard(), "BOARDS", "TR_API_LOADER",
                        "V_TR_BUFF_MICEX_BOARD", "MICEX_BOARD_DATA_INS"),
                make(typeSrc, loaderConstants.buffMicexDeal(), "TRADES", "TR_API_LOADER",
                        "V_TR_BUFF_MICEX_DEAL_FX", "MICEX_DEAL_FX_DATA_INS"),
                make(typeSrc, loaderConstants.buffMicexQuoteFx(), "SECURITIES", "TR_API_LOADER",
                        "V_TR_BUFF_MICEX_FX_QUOTE", "MICEX_FX_QUOTE_DATA_INS")
        );
    }

    private static BufferConfig make(int typeSrc, int typeBuff, String micexTable, String pkg, String view, String function) {
        BufferConfig c = new BufferConfig();
        c.setTypeSrc(typeSrc);
        c.setTypeBuff(typeBuff);
        c.setMicexTable(micexTable);
        c.setPackageName(pkg);
        c.setViewName(view);
        c.setFunctionName(function);
        c.setSaveProcedure(function);
        c.setPollIntervalSec(5);
        c.setLoadFull(true);
        c.setBufferKind("DATA");
        return c;
    }

    /** Конфиг буфера по type_buff (для маршрутизации quote_sec 2325 и т.п.). */
    public BufferConfig loadByTypeBuff(int typeBuff) {
        List<BufferConfig> list = queryByBuffIds(new int[] { typeBuff });
        return list.isEmpty() ? null : list.get(0);
    }
}
