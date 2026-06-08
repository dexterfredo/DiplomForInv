package ru.inversion.LoaderMicexFX.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TrGetConstRepository {

    private static final Logger log = LoggerFactory.getLogger(TrGetConstRepository.class);

    private final JdbcTemplate jdbc;

    public TrGetConstRepository(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    public int gcSrcMicex() {
        return call("tr_get_const.gc_src_micex()");
    }

    public int gcMmvbSectFx() {
        return callFunction("gc_mmvb_sect_fx");
    }

    public int gcDealPlaceSectionState() {
        return callFunction("gc_deal_place_section_state");
    }

    public int gcDealPlaceSectionShare() {
        return callFunction("gc_deal_place_section_share");
    }

    public int callFunction(String functionName) {
        return call("tr_get_const." + normalizeFunctionName(functionName) + "()");
    }

    static String normalizeFunctionName(String functionName) {
        if (functionName == null || functionName.isBlank()) {
            throw new IllegalArgumentException("Имя функции tr_get_const не задано");
        }
        String fn = functionName.trim().toLowerCase();
        if (!fn.matches("gc_[a-z0-9_]+")) {
            throw new IllegalArgumentException("Недопустимое имя функции tr_get_const: " + functionName);
        }
        return fn;
    }

    public int gcBuffMicexDeal() {
        return call("tr_get_const.gc_buff_micex_deal()");
    }

    public int gcBuffMicexDealSec() {
        return call("tr_get_const.gc_buff_micex_deal_sec()");
    }

    public int gcBuffMicexQuoteSec() {
        return call("tr_get_const.gc_buff_micex_quote_sec()");
    }

    public int gcBuffMicexOrderSec() {
        return call("tr_get_const.gc_buff_micex_order_sec()");
    }

    public int gcBuffMicexDecimal() {
        return call("tr_get_const.gc_buff_micex_decimal()");
    }

    public int gcBuffMicexBoard() {
        return call("tr_get_const.gc_buff_micex_board()");
    }

    public int gcBuffMicexQuoteFx() {
        return call("tr_get_const.gc_buff_micex_quote_fx()");
    }

    public int gcBuffMicexLotsize() {
        return call("tr_get_const.gc_buff_micex_lotsize()");
    }

    private int call(String sql) {
        try {
            Number n = jdbc.queryForObject("SELECT " + sql, Number.class);
            if (n == null) {
                throw new IllegalStateException(sql + " вернул NULL");
            }
            return n.intValue();
        } catch (Exception ex) {
            log.error("{}: {}", sql, ex.getMessage());
            throw new IllegalStateException(
                    "Выполните sql/generated/23_tr_get_const.sql: " + sql, ex);
        }
    }
}
