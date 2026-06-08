package ru.inversion.LoaderMicexFX.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OperDateRepository {

    private final JdbcTemplate jdbc;

    public OperDateRepository(MutableDataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public void setOperDateTime(String micexTime) {
        if (micexTime == null || micexTime.isBlank()) {
            return;
        }
        jdbc.queryForObject(
                "SELECT tr_get_const.set_oper_date_time(?)",
                Object.class,
                micexTime.trim());
    }
}
