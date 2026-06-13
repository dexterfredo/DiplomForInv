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
        return call("tr_get_const.gc_mmvb_sect_fx()");
    }

    public int gcDealPlaceSectionState() {
        return call("tr_get_const.gc_deal_place_section_state()");
    }

    public int gcDealPlaceSectionShare() {
        return call("tr_get_const.gc_deal_place_section_share()");
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
            throw new IllegalStateException("Ошибка " + sql + ": " + ex.getMessage(), ex);
        }
    }
}
