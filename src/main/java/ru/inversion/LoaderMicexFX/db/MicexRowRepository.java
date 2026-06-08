package ru.inversion.LoaderMicexFX.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Value;
import ru.inversion.LoaderMicexFX.gateway.MicexBuffDefaults;
import ru.inversion.LoaderMicexFX.gateway.MicexBuffRowParser;
import ru.inversion.LoaderMicexFX.gateway.MicexBuffStructBuilder;
import ru.inversion.LoaderMicexFX.gateway.MicexPgComposite;
import ru.inversion.LoaderMicexFX.gateway.MicexTextFieldBuilder;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;
import ru.inversion.LoaderMicexFX.model.MicexTargetEntry;
import ru.inversion.LoaderMicexFX.service.LoaderConstantsService;
import ru.inversion.LoaderMicexFX.service.TesystimeSyncService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Repository
public class MicexRowRepository {

    private static final Logger log = LoggerFactory.getLogger(MicexRowRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final MicexTargetRepository targetRepository;
    private final ViewColumnsRepository viewColumnsRepository;
    private final MicexBuffRowParser buffRowParser;
    private final LoaderConstantsService loaderConstants;
    private final MicexTextFieldBuilder textFieldBuilder;
    private final TesystimeSyncService tesystimeSyncService;
    private final DbSaveDiagnosticsService dbDiagnostics;
    private final PgRoutineCatalog pgRoutineCatalog;

    @Value("${app.loader.bool-true:81}")
    private int boolTrue;

    @Value("${app.loader.db.verify-buff-id:true}")
    private boolean verifyBuffId;

    @Value("${app.loader.db.verify-query:SELECT COALESCE(MAX(buff_id), 0) FROM tr__data_temp.tr_buff WHERE type_buff = ?}")
    private String verifyQuery;

    public MicexRowRepository(
            JdbcTemplate jdbcTemplate,
            MicexTargetRepository targetRepository,
            ViewColumnsRepository viewColumnsRepository,
            MicexBuffRowParser buffRowParser,
            LoaderConstantsService loaderConstants,
            MicexTextFieldBuilder textFieldBuilder,
            TesystimeSyncService tesystimeSyncService,
            DbSaveDiagnosticsService dbDiagnostics,
            PgRoutineCatalog pgRoutineCatalog) {
        this.jdbcTemplate = jdbcTemplate;
        this.targetRepository = targetRepository;
        this.viewColumnsRepository = viewColumnsRepository;
        this.buffRowParser = buffRowParser;
        this.loaderConstants = loaderConstants;
        this.textFieldBuilder = textFieldBuilder;
        this.tesystimeSyncService = tesystimeSyncService;
        this.dbDiagnostics = dbDiagnostics;
        this.pgRoutineCatalog = pgRoutineCatalog;
    }

    public DbSaveOutcome save(BufferConfig buff, MicexTableRow row) {
        return save(buff, row, loaderConstants.getTypeSection());
    }

    public DbSaveOutcome save(BufferConfig buff, MicexTableRow row, int typeSection) {
        if (buff == null || row == null || row.getFields().isEmpty()) {
            return DbSaveOutcome.skipped("EMPTY_ROW");
        }
        String qualified = buff.getQualifiedProcedure();
        if (qualified.isBlank()) {
            return DbSaveOutcome.skipped("NO_PROCEDURE");
        }
        if (buff.getTypeBuff() == loaderConstants.buffMicexDeal()) {
            return DbSaveOutcome.skipped("USE_SAVE_DEAL_FX_CHUNK");
        }

        buffRowParser.mapApiToViewFields(row, buff, typeSection);

        List<MicexTargetEntry> mapping = targetRepository.loadForBuff(buff.getTypeBuff(), typeSection);
        if (mapping.isEmpty()) {
            return DbSaveOutcome.skipped("NO_MICEX_TARGET");
        }

        Map<String, String> viewFields = row.getViewFields();
        if (viewFields.isEmpty()) {
            return DbSaveOutcome.skipped("EMPTY_VIEW_FIELDS");
        }

        List<ViewColumnMeta> columns = viewColumnsRepository.columnsFor(buff);
        if (columns.isEmpty()) {
            return DbSaveOutcome.skipped("NO_VIEW_COLUMNS:" + buff.getPgViewName());
        }

        MicexBuffStructBuilder.ensureDefaultDates(viewFields, columns);
        MicexBuffDefaults.apply(buff, viewFields, tesystimeSyncService, loaderConstants);
        String textPayload = resolveTextField(row, distinctRawMapping(mapping), viewFields);
        if (textPayload == null || textPayload.isBlank()) {
            return DbSaveOutcome.skipped("EMPTY_TEXT");
        }

        String pgViewType = BufferConfig.SCHEMA_VIEW + "." + buff.getPgViewName();
        Object[] attrs = MicexBuffStructBuilder.buildAttributes(columns, viewFields, textPayload);

        Long maxBefore = null;
        if (verifyBuffId) {
            maxBefore = maxBuffId(buff.getTypeBuff());
            if (maxBefore == null) {
                dbDiagnostics.recordVerifyUnavailable(
                        "verify-query недоступен на этой БД — проверка buff_id отключена");
            }
        }

        try {
            jdbcTemplate.execute((ConnectionCallback<Void>) conn -> {
                try {
                    callInOut(conn, qualified, pgViewType, attrs);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                return null;
            });
            if (verifyBuffId && maxBefore != null) {
                Long maxAfter = maxBuffId(buff.getTypeBuff());
                if (maxAfter == null) {
                    return DbSaveOutcome.skipped("VERIFY_QUERY_FAILED");
                }
                if (maxAfter <= maxBefore) {
                    return DbSaveOutcome.skipped("CALL_OK_NO_BUFF_ID");
                }
                return DbSaveOutcome.saved(maxAfter);
            }
            return DbSaveOutcome.saved(null);
        } catch (Exception e) {
            log.warn("Ошибка вызова {}: {}", qualified, e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return DbSaveOutcome.sqlError(msg);
        }
    }

    
    public int saveDealFxChunk(BufferConfig buff, List<MicexTableRow> chunk, Predicate<MicexTableRow> mainRowFilter) {
        if (buff == null || chunk == null || chunk.isEmpty()) {
            return 0;
        }
        String qualified = buff.getQualifiedProcedure();
        if (qualified.isBlank()) {
            return 0;
        }

        List<MicexTargetEntry> mapping = targetRepository.loadForBuff(buff.getTypeBuff());
        if (mapping.isEmpty()) {
            return 0;
        }
        List<MicexTargetEntry> rawMapping = distinctRawMapping(mapping);
        List<ViewColumnMeta> columns = viewColumnsRepository.columnsFor(buff);
        if (columns.isEmpty()) {
            return 0;
        }

        String pgViewType = BufferConfig.SCHEMA_VIEW + "." + buff.getPgViewName();
        Map<String, String> emptyLeg = emptyLegViewFields(buff, mapping);
        int saved = 0;
        int i = 0;
        while (i < chunk.size()) {
            MicexTableRow main = chunk.get(i);
            if (mainRowFilter != null && !mainRowFilter.test(main)) {
                i++;
                continue;
            }

            Object[] attrsMain = buildDealFxLegAttributes(buff, main, columns, rawMapping, mapping, true);
            if (attrsMain == null) {
                i++;
                continue;
            }
            // Вторая и третья нога сделки пока не заполняются
            Object[] attrsNear = buildDealFxLegAttributesFromMap(buff, columns, emptyLeg, mapping, "");
            Object[] attrsFar = buildDealFxLegAttributesFromMap(buff, columns, emptyLeg, mapping, "");

            PgRoutineCatalog.RoutineMeta routine = pgRoutineCatalog.lookup(
                    routineName(qualified),
                    routineSchema(qualified),
                    buff.getPgViewName());

            try {
                DealFxCallResult result = jdbcTemplate.execute((ConnectionCallback<DealFxCallResult>) conn -> {
                    try {
                        return callDealFx(conn, routine, pgViewType, attrsMain, attrsNear, attrsFar);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                if (result.errMessage() != null && !result.errMessage().isBlank()) {
                    log.warn("DEAL_FX {}: {}", qualified, result.errMessage());
                    dbDiagnostics.recordOutcome(buff.getTypeBuff(), qualified, DbSaveOutcome.procError(result.errMessage()));
                } else if (result.inserted()) {
                    saved++;
                }
                i++;
            } catch (Exception e) {
                log.warn("DEAL_FX {}: {}", qualified, e.getMessage());
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                dbDiagnostics.recordOutcome(buff.getTypeBuff(), qualified, DbSaveOutcome.sqlError(msg));
                i++;
            }
        }
        return saved;
    }

    
    private Object[] buildDealFxLegAttributes(
            BufferConfig buff,
            MicexTableRow row,
            List<ViewColumnMeta> columns,
            List<MicexTargetEntry> rawMapping,
            List<MicexTargetEntry> mapping,
            boolean requireNonEmptyText) {
        buffRowParser.mapApiToViewFields(row, buff);
        Map<String, String> viewFields = new LinkedHashMap<>(row.getViewFields());
        if (viewFields.isEmpty()) {
            return requireNonEmptyText
                    ? null
                    : buildDealFxLegAttributesFromMap(buff, columns, emptyLegViewFields(buff, mapping), mapping, "");
        }
        MicexBuffStructBuilder.ensureDefaultDates(viewFields, columns);
        MicexBuffDefaults.apply(buff, viewFields, tesystimeSyncService, loaderConstants);
        String textPayload = resolveTextField(row, rawMapping, viewFields);
        if (requireNonEmptyText && (textPayload == null || textPayload.isBlank())) {
            return null;
        }
        String text = textPayload != null ? textPayload : "";
        return MicexBuffStructBuilder.buildAttributes(columns, viewFields, text);
    }

    private Object[] buildDealFxLegAttributesFromMap(
            BufferConfig buff,
            List<ViewColumnMeta> columns,
            Map<String, String> viewFields,
            List<MicexTargetEntry> mapping,
            String textPayload) {
        Map<String, String> copy = new LinkedHashMap<>(viewFields);
        MicexBuffStructBuilder.ensureDefaultDates(copy, columns);
        MicexBuffDefaults.apply(buff, copy, tesystimeSyncService, loaderConstants);
        return MicexBuffStructBuilder.buildAttributes(columns, copy, textPayload);
    }

    private static Map<String, String> emptyLegViewFields(BufferConfig buff, List<MicexTargetEntry> mapping) {
        Map<String, String> view = new LinkedHashMap<>();
        view.put("type_src", String.valueOf(buff.getTypeSrc()));
        view.put("type_buff", String.valueOf(buff.getTypeBuff()));
        int section = mapping.stream().mapToInt(MicexTargetEntry::getTypeSection).min().orElse(1);
        view.put("type_section", String.valueOf(section));
        return view;
    }

    private String resolveTextField(
            MicexTableRow row,
            List<MicexTargetEntry> rawMapping,
            Map<String, String> viewFields) {
        String padded = textFieldBuilder.buildFromApiRow(row);
        if (padded != null) {
            return padded;
        }
        return buildTextField(rawMapping, viewFields);
    }

    private record DealFxCallResult(boolean inserted, boolean isSwap, String errMessage) {
    }

    private DealFxCallResult callDealFx(
            Connection conn,
            PgRoutineCatalog.RoutineMeta routine,
            String pgViewType,
            Object[] attrsMain,
            Object[] attrsNear,
            Object[] attrsFar) throws SQLException {
        String sql = buildDealFxCallSql(routine);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, MicexPgComposite.toPgObject(pgViewType, attrsMain));
            ps.setObject(2, MicexPgComposite.toPgObject(pgViewType, attrsNear));
            ps.setObject(3, MicexPgComposite.toPgObject(pgViewType, attrsFar));
            ps.execute();
            return readDealFxOutParams(ps);
        }
    }

    static String buildDealFxCallSql(PgRoutineCatalog.RoutineMeta routine) {
        if (routine.isFunction()) {
            return "SELECT * FROM " + routine.qualifiedName() + "(?, ?, ?)";
        }
        return "CALL " + routine.qualifiedName() + "(?, ?, ?)";
    }

    /** Формат строки для тестов composite-типа. */
    static String sqlCastComposite(Object[] attrs, String pgViewType) {
        String body = MicexPgComposite.formatLiteral(attrs).replace("'", "''");
        return "'" + body + "'::" + pgViewType;
    }

    private static String routineName(String qualified) {
        int dot = qualified.lastIndexOf('.');
        return dot >= 0 ? qualified.substring(dot + 1) : qualified;
    }

    private static String routineSchema(String qualified) {
        int dot = qualified.lastIndexOf('.');
        return dot >= 0 ? qualified.substring(0, dot) : BufferConfig.SCHEMA_API;
    }

    private DealFxCallResult readDealFxOutParams(Statement stmt) throws SQLException {
        ResultSet rs = stmt.getResultSet();
        while (rs == null && (stmt.getUpdateCount() != -1 || stmt.getMoreResults())) {
            rs = stmt.getResultSet();
        }
        if (rs == null) {
            return new DealFxCallResult(true, false, null);
        }
        if (rs.next()) {
            Object swap = rs.getObject(1);
            String err = rs.getString(2);
            boolean inserted = err == null || err.isBlank();
            return new DealFxCallResult(inserted, isBoolTrue(swap), err);
        }
        return new DealFxCallResult(true, false, null);
    }

    private boolean isBoolTrue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Number n) {
            int v = n.intValue();
            return v == boolTrue || v == 1;
        }
        try {
            int v = Integer.parseInt(value.toString().trim());
            return v == boolTrue || v == 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void callInOut(Connection conn, String qualifiedProc, String pgViewType, Object[] attrs)
            throws SQLException {
        String sql = buildCallSql(qualifiedProc, pgViewType, attrs.length);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < attrs.length; i++) {
                ps.setObject(i + 1, attrs[i]);
            }
            ps.execute();
        }
    }

    private static String buildCallSql(String qualifiedProc, String pgViewType, int attrsCount) {
        StringJoiner q = new StringJoiner(", ");
        for (int i = 0; i < attrsCount; i++) {
            q.add("?");
        }
        return "CALL " + qualifiedProc + "((ROW(" + q + "))::" + pgViewType + ")";
    }

    private Long maxBuffId(int typeBuff) {
        if (verifyQuery == null || verifyQuery.isBlank()) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(verifyQuery, Long.class, typeBuff);
        } catch (Exception e) {
            log.debug("maxBuffId type_buff={}: {}", typeBuff, e.getMessage());
            return null;
        }
    }

    private static List<MicexTargetEntry> distinctRawMapping(List<MicexTargetEntry> mapping) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        return mapping.stream()
                .filter(e -> e.getBuffField() != null && e.getBuffField().toLowerCase().startsWith("raw_"))
                .sorted(java.util.Comparator.comparing(e -> e.getBuffField().toLowerCase()))
                .filter(e -> seen.add(e.getBuffField().toLowerCase()))
                .collect(Collectors.toList());
    }

    static String resolveViewParam(Map<String, String> viewFields, String buffField) {
        if (buffField == null || buffField.isBlank()) {
            return null;
        }
        return viewFields.get(buffField.toLowerCase());
    }

    private static String buildTextField(List<MicexTargetEntry> mapping, Map<String, String> viewFields) {
        StringBuilder sb = new StringBuilder();
        for (MicexTargetEntry e : mapping) {
            if (e.getBuffField() == null) {
                continue;
            }
            String v = resolveViewParam(viewFields, e.getBuffField());
            if (v != null) {
                sb.append(v);
            }
        }
        String s = sb.toString();
        return s.length() > 4000 ? s.substring(0, 4000) : s;
    }
}
