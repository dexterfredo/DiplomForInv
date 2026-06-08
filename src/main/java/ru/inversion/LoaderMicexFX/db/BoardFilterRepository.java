package ru.inversion.LoaderMicexFX.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class BoardFilterRepository {

    private static final Logger log = LoggerFactory.getLogger(BoardFilterRepository.class);

    private final JdbcTemplate jdbc;

    
    @Value("${app.gateway.board:ALL}")
    private String boardConfig;

    
    @Value("${app.loader.board.if-load-exch-data:81}")
    private int ifLoadExchDataTrue;

    private volatile Set<String> allowedBoards;

    public BoardFilterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    public boolean isAllowed(String secboard) {
        if (secboard == null || secboard.isBlank()) {
            return true;
        }
        Set<String> allowed = getAllowed();
        if (allowed.isEmpty()) {
            return true;
        }
        return allowed.contains(secboard.trim().toUpperCase());
    }

    public Set<String> getAllowed() {
        if (allowedBoards == null) {
            synchronized (this) {
                if (allowedBoards == null) {
                    allowedBoards = load();
                }
            }
        }
        return allowedBoards;
    }

    public void reload() {
        allowedBoards = null;
    }

    private Set<String> load() {
        Set<String> fromView = loadFromView();
        Set<String> explicit = parseExplicitBoards(boardConfig);
        if (!explicit.isEmpty()) {
            fromView.retainAll(explicit);
            log.info("Фильтр board: v_tf_dict_board и config {} — {} площадок",
                    explicit, fromView.size());
        } else {
            log.info("Фильтр board: v_tf_dict_board (if_load_exch_data={}) — {} площадок",
                    ifLoadExchDataTrue, fromView.size());
        }
        if (fromView.isEmpty()) {
            log.warn("Нет площадок после фильтра. Проверьте seed 21_seed_tf_dict_and_micex_board.sql и v_tf_dict_board");
            if (!explicit.isEmpty()) {
                return explicit;
            }
        }
        return fromView;
    }

    
    private Set<String> loadFromView() {
        try {
            List<String> list = jdbc.queryForList(
                    """
                    SELECT upper(board)
                      FROM tr__data_view.v_tf_dict_board
                     WHERE if_load_exch_data = ?
                    """,
                    String.class,
                    ifLoadExchDataTrue);
            return list.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toCollection(HashSet::new));
        } catch (Exception e) {
            log.warn("v_tf_dict_board недоступно: {}. Fallback по app.gateway.board", e.getMessage());
            return parseExplicitBoards(boardConfig);
        }
    }

    
    static Set<String> parseExplicitBoards(String config) {
        String raw = config == null ? "" : config.trim();
        if (raw.isEmpty() || "ALL".equalsIgnoreCase(raw) || "*".equals(raw)) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
