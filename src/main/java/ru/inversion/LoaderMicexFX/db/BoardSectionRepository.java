package ru.inversion.LoaderMicexFX.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class BoardSectionRepository {

    private static final Logger log = LoggerFactory.getLogger(BoardSectionRepository.class);

    private static final String SQL = """
            SELECT type_section::int
              FROM tr__data_view.v_tf_dict_board
             WHERE upper(board) = upper(?)
               AND if_load_exch_data = ?
             LIMIT 1
            """;

    private final JdbcTemplate jdbc;

    @Value("${app.loader.board.if-load-exch-data:81}")
    private int ifLoadExchDataTrue;

    private final Map<String, Integer> cache = new ConcurrentHashMap<>();
    private volatile boolean cacheLoaded;

    public BoardSectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    public Integer resolveTypeSection(String boardCode) {
        if (boardCode == null || boardCode.isBlank()) {
            return null;
        }
        String key = boardCode.trim().toUpperCase();
        if (!cacheLoaded) {
            loadAll();
        }
        return cache.get(key);
    }

    public void reload() {
        cache.clear();
        cacheLoaded = false;
    }

    private void loadAll() {
        synchronized (cache) {
            if (cacheLoaded) {
                return;
            }
            try {
                jdbc.query(
                        """
                        SELECT upper(board) AS board, type_section::int AS type_section
                          FROM tr__data_view.v_tf_dict_board
                         WHERE if_load_exch_data = ?
                           AND board IS NOT NULL
                        """,
                        rs -> {
                            while (rs.next()) {
                                String board = rs.getString("board");
                                Object sectionObj = rs.getObject("type_section");
                                if (board != null && !board.isBlank() && sectionObj instanceof Number num) {
                                    cache.put(board.trim().toUpperCase(), num.intValue());
                                }
                            }
                            return null;
                        },
                        ifLoadExchDataTrue);
                log.info("Справочник board→type_section: {} площадок", cache.size());
            } catch (Exception e) {
                log.warn("v_tf_dict_board недоступен для type_section: {}", e.getMessage());
            }
            cacheLoaded = true;
        }
    }
}
