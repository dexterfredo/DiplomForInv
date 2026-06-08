package ru.inversion.LoaderMicexFX.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.loaderconfig.BufferDependents;
import ru.inversion.LoaderMicexFX.model.LoaderUiState;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LoaderBufferControlService {

    private static final Logger log = LoggerFactory.getLogger(LoaderBufferControlService.class);

    private final LoaderConstantsService constants;
    private final MicexLoaderService micexLoaderService;

    @Value("${app.loader.ui.load-decimals:true}")
    private boolean defaultLoadDecimals;

    @Value("${app.loader.ui.load-boards:true}")
    private boolean defaultLoadBoards;

    private volatile boolean loadDecimals;
    private volatile boolean loadBoards;
    private final Set<Integer> startedBuffers = Collections.synchronizedSet(new HashSet<>());

    public LoaderBufferControlService(LoaderConstantsService constants, @Lazy MicexLoaderService micexLoaderService) {
        this.constants = constants;
        this.micexLoaderService = micexLoaderService;
    }

    @jakarta.annotation.PostConstruct
    void initDefaults() {
        loadDecimals = defaultLoadDecimals;
        loadBoards = defaultLoadBoards;
    }

    public void setPreferences(boolean loadDecimals, boolean loadBoards) {
        this.loadDecimals = loadDecimals;
        this.loadBoards = loadBoards;
    }

    public boolean isLoadDecimals() {
        return loadDecimals;
    }

    public boolean isLoadBoards() {
        return loadBoards;
    }

    public boolean isBufferStarted(int typeBuff) {
        return startedBuffers.contains(typeBuff);
    }

    public boolean hasAnyBufferStarted() {
        return !startedBuffers.isEmpty();
    }

    public void clearAll() {
        startedBuffers.clear();
        log.info("Все буферы остановлены (отключение MICEX API)");
    }

    
    public synchronized String toggleSettings(boolean sessionReady) {
        if (!sessionReady) {
            return "Сначала подключитесь к MICEX (Connect).";
        }
        if (!loadDecimals && !loadBoards) {
            return "Включите галочку Decimals и/или Boards.";
        }
        int decimal = constants.buffMicexDecimal();
        int board = constants.buffMicexBoard();
        if (isBufferStarted(decimal) || isBufferStarted(board)) {
            stopBuffer(board);
            stopBuffer(decimal);
            log.info("Остановлены буферы настроек: board, decimal");
            return null;
        }
        if (loadBoards) {
            startBuffer(board);
        }
        if (loadDecimals) {
            startBuffer(decimal);
        }
        log.info("Запущены буферы настроек (decimals={}, boards={})", loadDecimals, loadBoards);
        return null;
    }

    
    public synchronized String toggleDeal(boolean sessionReady) {
        if (!sessionReady) {
            return "Сначала подключитесь к MICEX (Connect).";
        }
        int deal = constants.buffMicexDeal();
        if (isBufferStarted(deal)) {
            stopBuffer(deal);
            stopDependents(deal);
            log.info("Остановлен буфер сделок {}", deal);
            return null;
        }
        if (!startDependents(deal)) {
            return "Не удалось запустить зависимые буферы (board/decimal).";
        }
        startBuffer(deal);
        log.info("Запущен буфер сделок {}", deal);
        return null;
    }

    
    public synchronized String toggleQuote(boolean sessionReady) {
        if (!sessionReady) {
            return "Сначала подключитесь к MICEX (Connect).";
        }
        int quote = constants.buffMicexQuoteFx();
        if (isBufferStarted(quote)) {
            stopBuffer(quote);
            stopDependents(quote);
            log.info("Остановлен буфер котировок {}", quote);
            return null;
        }
        if (!startDependents(quote)) {
            return "Не удалось запустить зависимые буферы (board/decimal).";
        }
        startBuffer(quote);
        log.info("Запущен буфер котировок {}", quote);
        return null;
    }

    public LoaderUiState buildUiState(boolean micexConnected, LoaderTimerPreferences timers) {
        LoaderUiState ui = new LoaderUiState();
        if (timers != null) {
            ui.setDealTime(timers.formatDealTime());
            ui.setQuoteTime(timers.formatQuoteTime());
            ui.setSettingsTime(timers.formatSettingsTime());
            ui.setDealTimerEnabled(timers.isDealTimerEnabled());
        }
        ui.setLoadDecimals(loadDecimals);
        ui.setLoadBoards(loadBoards);
        ui.setMicexConnected(micexConnected);
        int deal = constants.buffMicexDeal();
        int quote = constants.buffMicexQuoteFx();
        int decimal = constants.buffMicexDecimal();
        int board = constants.buffMicexBoard();
        ui.setDealStarted(isBufferStarted(deal));
        ui.setQuoteStarted(isBufferStarted(quote));
        ui.setDecimalStarted(isBufferStarted(decimal));
        ui.setBoardStarted(isBufferStarted(board));

        boolean settingsRunning = ui.isSettingsRunning();
        boolean dealOrQuote = ui.isDealStarted() || ui.isQuoteStarted();
        ui.setLockConnectParams(micexConnected);
        ui.setLockSettingsCheckboxes(settingsRunning);
        ui.setLockSettingsStart(dealOrQuote);
        ui.setLockConnectActions(dealOrQuote || settingsRunning);
        return ui;
    }

    private void startBuffer(int typeBuff) {
        startedBuffers.add(typeBuff);
        micexLoaderService.prepareBuffersStarted(typeBuff);
    }

    private void stopBuffer(int typeBuff) {
        startedBuffers.remove(typeBuff);
    }

    
    private boolean startDependents(int masterTypeBuff) {
        List<Integer> deps = dependentTypes(masterTypeBuff);
        for (int dep : deps) {
            if (!canEnableBuffer(dep)) {
                continue;
            }
            if (!isBufferStarted(dep)) {
                startBuffer(dep);
            }
        }
        return true;
    }

    private void stopDependents(int masterTypeBuff) {
        for (int dep : dependentTypes(masterTypeBuff)) {
            if (!isBufferStarted(dep)) {
                continue;
            }
            if (isUsedByOtherRunningMaster(dep, masterTypeBuff)) {
                continue;
            }
            stopBuffer(dep);
        }
    }

    private boolean isUsedByOtherRunningMaster(int depTypeBuff, int stoppingMaster) {
        int deal = constants.buffMicexDeal();
        int quote = constants.buffMicexQuoteFx();
        for (int master : List.of(deal, quote)) {
            if (master == stoppingMaster || !isBufferStarted(master)) {
                continue;
            }
            if (dependentTypes(master).contains(depTypeBuff)) {
                return true;
            }
        }
        return false;
    }

    private List<Integer> dependentTypes(int masterTypeBuff) {
        return BufferDependents.forMaster(
                masterTypeBuff,
                constants.buffMicexDeal(),
                constants.buffMicexQuoteFx(),
                constants.buffMicexBoard(),
                constants.buffMicexDecimal(),
                constants.buffMicexLotsize(),
                constants.isMmvSectionSecurityMarket());
    }

    
    private boolean canEnableBuffer(int typeBuff) {
        if (typeBuff == constants.buffMicexDecimal()) {
            return loadDecimals;
        }
        if (typeBuff == constants.buffMicexBoard()) {
            return loadBoards;
        }
        return true;
    }
}
