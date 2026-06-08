package ru.inversion.LoaderMicexFX.gateway;

import com.micex.client.Client;
import com.micex.client.ClientException;
import com.micex.client.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.GatewayConnectionSettings;
import ru.inversion.LoaderMicexFX.model.MicexConnectionType;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;
import ru.inversion.LoaderMicexFX.service.BufferConfigService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SimpleGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(SimpleGatewayClient.class);

    
    private static final int APP_ERROR_CODE = -1;

    private Client client;
    private boolean connected;
    private final MicexMarketBinder marketBinder;
    private final BufferConfigService bufferConfigService;

    
    private final List<String> openedTables = new ArrayList<>();
    
    private final List<String> failedTables = new ArrayList<>();

    
    @Value("${app.gateway.mode:asts-bridge}")
    private String connectionMode;

    @Value("${app.gateway.packet-size:}")
    private String packetSize;
    @Value("${app.gateway.host:}")
    private String host;
    @Value("${app.gateway.interface:}")
    private String apiInterface;
    @Value("${app.gateway.server:}")
    private String server;
    @Value("${app.gateway.service:}")
    private String service;
    @Value("${app.gateway.broadcast:}")
    private String broadcast;
    @Value("${app.gateway.pref-broadcast:}")
    private String prefBroadcast;
    @Value("${app.gateway.user-id:}")
    private String userId;
    @Value("${app.gateway.password:}")
    private String password;
    @Value("${app.gateway.feedback:}")
    private String feedback;
    @Value("${app.gateway.board:TQBR}")
    private String board;

    /** До 20 попыток подключения к шлюзу. */
    @Value("${app.gateway.connect-max-attempts:20}")
    private int connectMaxAttempts;

    @Value("${app.gateway.connect-retry-delay-ms:300}")
    private long connectRetryDelayMs;

    public SimpleGatewayClient(MicexMarketBinder marketBinder, BufferConfigService bufferConfigService) {
        this.marketBinder = marketBinder;
        this.bufferConfigService = bufferConfigService;
    }

    
    private volatile GatewayConnectionSettings runtimeSettings;

    
    public void connect() throws ClientException {
        List<BufferConfig> buffers = bufferConfigService.getActiveBuffers();
        if (buffers.isEmpty()) {
            throw new ClientException(APP_ERROR_CODE,
                    "Список буферов пуст (type_src=" + bufferConfigService.getTypeSrc()
                            + ", type_section=" + bufferConfigService.getTypeSection()
                            + "). Подключитесь к БД до Connect или включите app.loader.buffers.embedded-only=true");
        }

        GatewayConnectionSettings settings = effectiveSettings();
        if (useAstsBridge(settings) && MicexConnectionStrings.resolveHostWithPort(settings).isBlank()) {
            throw new ClientException(APP_ERROR_CODE,
                    "Режим ASTS Bridge: укажите HOST=адрес:порт (напр. localhost:15005) в WEB или app.gateway.host");
        }

        int maxAttempts = Math.max(1, connectMaxAttempts);
        ClientException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (attempt > 1) {
                log.info("MICEX connect: повтор {}/{}", attempt, maxAttempts);
            }
            try {
                connectOnce(buffers, settings);
                if (attempt > 1) {
                    log.info("MICEX connect OK с попытки {}/{}", attempt, maxAttempts);
                }
                return;
            } catch (ClientException e) {
                lastFailure = e;
                disconnect();
                if (attempt < maxAttempts) {
                    log.warn("MICEX connect попытка {}/{} не удалась: {}", attempt, maxAttempts, e.getMessage());
                    sleepBeforeConnectRetry();
                }
            }
        }
        String msg = lastFailure != null ? lastFailure.getMessage() : "неизвестная ошибка";
        throw new ClientException(APP_ERROR_CODE,
                msg + " (попыток подключения: " + maxAttempts + ")");
    }

    private void connectOnce(List<BufferConfig> buffers, GatewayConnectionSettings settings) throws ClientException {
        openedTables.clear();
        failedTables.clear();

        client = new Client();
        if (useAstsBridge(settings)) {
            String mteString = MicexConnectionStrings.buildConnectString(settings);
            log.info("MICEX connect type={}, MTEConnect:\n{}",
                    settings.getConnectionType(), maskConnectString(mteString));
            startAstsBridge(client, settings, mteString);
        } else {
            Map<String, String> params = buildGatewayParams();
            log.info("MICEX connect mode=multicast params: {}", maskSecrets(params));
            client.start(params);
        }

        Set<String> boards = parseBoards(board);
        if (boards.isEmpty()) {
            log.info("Board filter OFF: all boards (etalon ALL)");
        } else {
            try {
                client.selectBoards(boards);
            } catch (Exception e) {
                if (useAstsBridge(settings)) {
                    log.warn("ASTS Bridge: selectBoards({}) rejected: {}", boards, errorText(e));
                } else if (e instanceof ClientException ce) {
                    throw ce;
                } else {
                    throw new ClientException(APP_ERROR_CODE, "selectBoards failed: " + errorText(e));
                }
            }
        }

        marketBinder.setStaticEmission(true);
        openTablesFromDb(buffers);
        openTesystimeTable();

        if (openedTables.isEmpty()) {
            throw new ClientException(APP_ERROR_CODE,
                    "Ни одна таблица MICEX не открылась. Ошибки: " + failedTables);
        }

        connected = true;
        log.info("MICEX connect OK. Open tables: {}", openedTables);
    }

    private void sleepBeforeConnectRetry() {
        if (connectRetryDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(connectRetryDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    
    private void startAstsBridge(Client client, GatewayConnectionSettings settings, String mteConnectString) throws ClientException {
        Map<String, String> startParams = MicexConnectionStrings.astsBridgeStartParams(settings);
        try {
            log.info("ASTS Bridge: Client.start params: {}", maskSecrets(startParams));
            client.start(startParams);
            log.info("ASTS Bridge: Client.start OK");
            return;
        } catch (ClientException e) {
            log.warn("ASTS Bridge: Client.start rejected, fallback MTEConnect: {}", errorText(e));
        }
        MteConnectOutcome result = mteConnectViaReflection(mteConnectString);
        if (result.handle <= 0) {
            String msg = result.report != null && !result.report.isBlank()
                    ? result.report
                    : "MTEConnect failed, handle=" + result.handle;
            throw new ClientException(APP_ERROR_CODE, msg);
        }
        attachClientSession(client, result.handle);
        analyzeConnectionReport(client, result.report);
        ensureMarketInitialized(client);
        String systemId = "unknown";
        try {
            var info = client.getServerInfo();
            if (info != null && info.systemID != '\0') {
                systemId = Character.toString(info.systemID);
            }
        } catch (Exception e) {
            log.debug("ASTS Bridge: getServerInfo unavailable: {}", errorText(e));
        }
        try {
            client.getMarketInfo();
        } catch (Exception e) {
            log.debug("ASTS Bridge: getMarketInfo unavailable: {}", errorText(e));
        }
        log.info("ASTS Bridge: handle={}, system={}", client.handle(), systemId);
    }

    
    private static void ensureMarketInitialized(Client client) throws ClientException {
        try {
            var marketField = Client.class.getDeclaredField("market");
            marketField.setAccessible(true);
            Object market = marketField.get(client);
            if (market != null) {
                return;
            }
            var getMarket = Client.class.getDeclaredMethod("getMarket");
            getMarket.setAccessible(true);
            Object loaded = getMarket.invoke(client);
            if (loaded == null) {
                throw new ClientException(APP_ERROR_CODE,
                        "ASTS Bridge: market metadata не инициализирована (getMarket() вернул null)");
            }
            marketField.set(client, loaded);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new ClientException(APP_ERROR_CODE,
                    "ASTS Bridge: не удалось загрузить market metadata: " + errorText(e));
        } catch (ReflectiveOperationException e) {
            throw new ClientException(APP_ERROR_CODE,
                    "ASTS Bridge: ошибка инициализации market metadata: " + e.getMessage());
        }
    }

    private record MteConnectOutcome(int handle, String report) {
    }

    private static MteConnectOutcome mteConnectViaReflection(String mteConnectString) throws ClientException {
        try {
            Class<?> apiClass = Class.forName("com.micex.client.API");
            var method = apiClass.getDeclaredMethod("MTEConnect", String.class);
            method.setAccessible(true);
            Object result = method.invoke(null, mteConnectString);
            if (result == null) {
                return new MteConnectOutcome(-1, "MTEConnect returned null");
            }
            int handle = readIntField(result, "handle", -1);
            String report = readStringField(result, "report", null);
            return new MteConnectOutcome(handle, report);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new ClientException(APP_ERROR_CODE, "API.MTEConnect упал: " + errorText(e));
        } catch (ReflectiveOperationException e) {
            throw new ClientException(APP_ERROR_CODE, "Не удалось вызвать API.MTEConnect через reflection: " + e);
        }
    }

    private static int readIntField(Object obj, String field, int def) {
        try {
            var f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.getInt(obj);
        } catch (ReflectiveOperationException ignored) {
            return def;
        }
    }

    private static String readStringField(Object obj, String field, String def) {
        try {
            var f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            Object v = f.get(obj);
            return v == null ? def : v.toString();
        } catch (ReflectiveOperationException ignored) {
            return def;
        }
    }

    private static void attachClientSession(Client client, int handle) throws ClientException {
        try {
            var handleField = Client.class.getDeclaredField("handle");
            handleField.setAccessible(true);
            handleField.setInt(client, handle);

            var activeField = Client.class.getDeclaredField("active");
            activeField.setAccessible(true);
            activeField.setBoolean(client, true);
        } catch (ReflectiveOperationException e) {
            throw new ClientException(APP_ERROR_CODE,
                    "Сессия MTEConnect OK (handle=" + handle + "), но Client не инициализирован: " + e.getMessage());
        }
    }

    private static void analyzeConnectionReport(Client client, String report) {
        if (report == null || report.isBlank()) {
            return;
        }
        try {
            var method = Client.class.getDeclaredMethod("analyzeConnectionReport", String.class);
            method.setAccessible(true);
            method.invoke(client, report);
        } catch (java.lang.reflect.InvocationTargetException e) {
            log.warn("analyzeConnectionReport: {}", errorText(e));
        } catch (ReflectiveOperationException e) {
            log.debug("analyzeConnectionReport: {}", e.toString());
        }
    }

    private Map<String, String> buildGatewayParams() {
        return MicexConnectionStrings.multicastParams(effectiveSettings());
    }

    private boolean useAstsBridge(GatewayConnectionSettings s) {
        String mode = connectionMode == null ? "" : connectionMode.trim();
        if ("multicast".equalsIgnoreCase(mode) || "colocation".equalsIgnoreCase(mode)) {
            return false;
        }
        MicexConnectionType ct = s.getConnectionType();
        if (ct == MicexConnectionType.RS232 || ct == MicexConnectionType.TCP
                || ct == MicexConnectionType.TCP2 || ct == MicexConnectionType.CUSTOM) {
            return true;
        }
        if ("asts-bridge".equalsIgnoreCase(mode) || "tcp".equalsIgnoreCase(mode) || "tcpip".equalsIgnoreCase(mode)) {
            return true;
        }
        return s.getHost() != null && !s.getHost().isBlank();
    }

    
    private void openTesystimeTable() {
        if (openedTables.stream().anyMatch(t -> "TESYSTIME".equalsIgnoreCase(t))) {
            return;
        }
        try {
            openAndParse("TESYSTIME", true);
            openedTables.add("TESYSTIME");
            log.info("open OK: TESYSTIME (system time)");
        } catch (Exception e) {
            failedTables.add("TESYSTIME");
            log.warn("open FAIL: TESYSTIME — {}", errorText(e));
        }
    }

    
    private static Set<String> parseBoards(String boardConfig) {
        String raw = boardConfig == null ? "" : boardConfig.trim();
        if (raw.isEmpty() || "ALL".equalsIgnoreCase(raw) || "*".equals(raw)) {
            return Collections.emptySet();
        }
        Set<String> boards = new HashSet<>();
        for (String token : raw.split(",")) {
            String b = token.trim();
            if (!b.isEmpty()) {
                boards.add(b);
            }
        }
        return boards;
    }

    private static Map<String, String> maskSecrets(Map<String, String> params) {
        Map<String, String> copy = new HashMap<>(params);
        for (String key : List.of("Password", "PASSWORD")) {
            if (copy.containsKey(key)) {
                copy.put(key, "****");
            }
        }
        return copy;
    }

    private static String maskConnectString(String mteConnectString) {
        if (mteConnectString == null) {
            return "";
        }
        return mteConnectString.replaceAll("(?im)^(PASSWORD=).*$", "$1****");
    }

    public GatewayConnectionSettings getCurrentSettings() {
        GatewayConnectionSettings s = effectiveSettings();
        s.setConnectionText(toConnectionText(s));
        return s;
    }

    public void applyRuntimeSettings(GatewayConnectionSettings settings) {
        if (settings == null) {
            return;
        }
        this.runtimeSettings = parseConnectionText(settings.getConnectionText(), effectiveSettings());
    }

    private GatewayConnectionSettings effectiveSettings() {
        GatewayConnectionSettings rt = runtimeSettings;
        if (rt != null) {
            return rt;
        }
        GatewayConnectionSettings s = new GatewayConnectionSettings();
        s.setPacketSize(packetSize);
        s.setHost(host);
        s.setApiInterface(apiInterface);
        s.setServer(server);
        s.setService(service);
        s.setBroadcast(broadcast);
        s.setPrefBroadcast(prefBroadcast);
        s.setUserId(userId);
        s.setPassword(password);
        s.setFeedback(feedback);
        s.setConnectionType(MicexConnectionType.TCP2);
        s.setComBaudrate("115200");
        return s;
    }

    private static GatewayConnectionSettings parseConnectionText(String text, GatewayConnectionSettings base) {
        GatewayConnectionSettings s = copyOf(base);
        if (text == null || text.isBlank()) {
            return s;
        }
        for (String line : text.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            int eq = t.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = t.substring(0, eq).trim().toUpperCase();
            String value = t.substring(eq + 1).trim();
            switch (key) {
                case "HOST" -> s.setHost(value);
                case "INTERFACE" -> s.setApiInterface(value);
                case "SERVER" -> s.setServer(value);
                case "SERVICE" -> s.setService(value);
                case "BROADCAST" -> s.setBroadcast(value);
                case "PREF_BROADCAST", "PREF-BROADCAST", "PREFBROADCAST" -> s.setPrefBroadcast(value);
                case "USER_ID", "USER-ID", "USERID" -> s.setUserId(value);
                case "PASSWORD" -> s.setPassword(value);
                case "FEEDBACK", "FEEDBAK" -> s.setFeedback(value);
                default -> {
                }
            }
        }
        return s;
    }

    private static GatewayConnectionSettings copyOf(GatewayConnectionSettings src) {
        GatewayConnectionSettings s = new GatewayConnectionSettings();
        s.setPacketSize(src.getPacketSize());
        s.setHost(src.getHost());
        s.setApiInterface(src.getApiInterface());
        s.setServer(src.getServer());
        s.setService(src.getService());
        s.setBroadcast(src.getBroadcast());
        s.setPrefBroadcast(src.getPrefBroadcast());
        s.setUserId(src.getUserId());
        s.setPassword(src.getPassword());
        s.setFeedback(src.getFeedback());
        s.setConnectionType(src.getConnectionType());
        s.setComPort(src.getComPort());
        s.setComBaudrate(src.getComBaudrate());
        s.setTcpHost(src.getTcpHost());
        s.setTcpService(src.getTcpService());
        s.setCustomConnectText(src.getCustomConnectText());
        return s;
    }

    private String toConnectionText(GatewayConnectionSettings s) {
        if (useAstsBridge(s)) {
            return MicexConnectionStrings.buildConnectString(s).replace("\r\n", "\n").trim();
        }
        return String.join("\n",
                "INTERFACE=" + nz(s.getApiInterface()),
                "SERVER=" + nz(s.getServer()),
                "SERVICE=" + nz(s.getService()),
                "BROADCAST=" + nz(s.getBroadcast()),
                "PREF_BROADCAST=" + nz(s.getPrefBroadcast()),
                "USER_ID=" + nz(s.getUserId()),
                "PASSWORD=" + nz(s.getPassword()));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    
    private void openTablesFromDb(List<BufferConfig> buffers) {
        for (BufferConfig buff : buffers) {
            String table = buff.getMicexTable();
            if (table == null || table.isBlank()) {
                continue;
            }
            try {
                openAndParse(table, buff.isLoadFull());
                openedTables.add(table.trim().toUpperCase());
                log.info("open OK: {} (buffer {}, load_full={})",
                        table, buff.getTypeBuff(), buff.isLoadFull());
            } catch (Exception e) {
                failedTables.add(table);
                log.warn("open FAIL: {} — {}", table, errorText(e));
            }
        }
    }

    
    private void openAndParse(String tableName, boolean loadFull) throws ClientException {
        Parser parser = client.open(tableName, null, loadFull);
        parser.execute(marketBinder);
    }

    private static String errorText(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        String msg = t.getMessage();
        return msg != null ? msg : t.getClass().getSimpleName();
    }

    public boolean isConnected() {
        return connected && client != null;
    }

    public List<MicexTableRow> drainMicexTableRows() {
        return marketBinder.drainMicexTableRows();
    }

    public List<String> getOpenedTables() {
        return Collections.unmodifiableList(openedTables);
    }

    public List<String> getFailedTables() {
        return Collections.unmodifiableList(failedTables);
    }

    
    public void refreshTables() throws ClientException {
        refreshTables(false);
    }

    
    public void refreshTables(boolean snapshot) throws ClientException {
        if (!isConnected()) {
            return;
        }
        marketBinder.setStaticEmission(snapshot);
        Parser parser = client.refresh();
        if (parser.empty()) {
            return;
        }
        parser.execute(marketBinder);
    }

    public void disconnect() {
        marketBinder.reset();
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
            client = null;
        }
        connected = false;
        openedTables.clear();
        failedTables.clear();
    }
}
