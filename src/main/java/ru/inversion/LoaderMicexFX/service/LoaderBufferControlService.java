package ru.inversion.LoaderMicexFX.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.loaderconfig.BufferDependents;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.LoaderUiState;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class LoaderBufferControlService {

    private static final Logger log = LoggerFactory.getLogger(LoaderBufferControlService.class);

    private final LoaderConstantsService constants;
    private final BufferConfigService bufferConfigService;
    private final MicexLoaderService micexLoaderService;

    @Value("${app.loader.ui.load-decimals:true}")
    private boolean defaultLoadDecimals;

    @Value("${app.loader.ui.load-boards:true}")
    private boolean defaultLoadBoards;

    private volatile boolean loadDecimals;
    private volatile boolean loadBoards;
    private final Set<Integer> startedBuffers = Collections.synchronizedSet(new HashSet<>());

    public LoaderBufferControlService(
            LoaderConstantsService constants,
            BufferConfigService bufferConfigService,
            @Lazy MicexLoaderService micexLoaderService) {
        this.constants = constants;
        this.bufferConfigService = bufferConfigService;
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

    public synchronized String toggleSettings(boolean micexConnected) {
        if (!micexConnected) {
            return "Сначала подключитесь к MICEX (Connect).";
        }
        if (!loadDecimals && !loadBoards) {
            return "Включите галочку Decimals и/или Boards.";
        }
        Optional<BufferConfig> decimal = bufferConfigService.findDecimalBuffer();
        Optional<BufferConfig> board = bufferConfigService.findBoardBuffer();
        if (decimal.isEmpty() && board.isEmpty()) {
            return "В БД нет буферов decimal/board (tr_buff_target).";
        }
        boolean decimalStarted = decimal.map(b -> isBufferStarted(b.getTypeBuff())).orElse(false);
        boolean boardStarted = board.map(b -> isBufferStarted(b.getTypeBuff())).orElse(false);
        if (decimalStarted || boardStarted) {
            board.ifPresent(b -> stopBuffer(b.getTypeBuff()));
            decimal.ifPresent(b -> stopBuffer(b.getTypeBuff()));
            log.info("Остановлены буферы настроек: board, decimal");
            return null;
        }
        if (loadBoards) {
            board.ifPresent(b -> startBuffer(b.getTypeBuff()));
        }
        if (loadDecimals) {
            decimal.ifPresent(b -> startBuffer(b.getTypeBuff()));
        }
        log.info("Запущены буферы настроек (decimals={}, boards={})", loadDecimals, loadBoards);
        return null;
    }

    public synchronized String toggleDeal(boolean micexConnected) {
        if (!micexConnected) {
            return "Сначала подключитесь к MICEX (Connect).";
        }
        Optional<BufferConfig> deal = bufferConfigService.findDealBuffer();
        if (deal.isEmpty()) {
            return "В БД нет буфера сделок DEAL_FX (tr_buff_target).";
        }
        int dealId = deal.get().getTypeBuff();
        if (isBufferStarted(dealId)) {
            stopBuffer(dealId);
            stopDependents(dealId);
            log.info("Остановлен буфер сделок {}", dealId);
            return null;
        }
        if (!startDependents(dealId)) {
            return "Не удалось запустить зависимые буферы (board/decimal).";
        }
        startBuffer(dealId);
        log.info("Запущен буфер сделок {}", dealId);
        return null;
    }

    public synchronized String toggleQuote(boolean micexConnected) {
        if (!micexConnected) {
            return "Сначала подключитесь к MICEX (Connect).";
        }
        Optional<BufferConfig> quote = bufferConfigService.findPrimaryQuoteBuffer();
        if (quote.isEmpty()) {
            return "В БД нет буфера котировок (tr_buff_target).";
        }
        int quoteId = quote.get().getTypeBuff();
        if (isBufferStarted(quoteId)) {
            stopBuffer(quoteId);
            stopDependents(quoteId);
            log.info("Остановлен буфер котировок {}", quoteId);
            return null;
        }
        if (!startDependents(quoteId)) {
            return "Не удалось запустить зависимые буферы (board/decimal).";
        }
        startBuffer(quoteId);
        log.info("Запущен буфер котировок {}", quoteId);
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

        Optional<BufferConfig> deal = bufferConfigService.findDealBuffer();
        Optional<BufferConfig> quote = bufferConfigService.findPrimaryQuoteBuffer();
        Optional<BufferConfig> decimal = bufferConfigService.findDecimalBuffer();
        Optional<BufferConfig> board = bufferConfigService.findBoardBuffer();

        ui.setDealStarted(deal.map(b -> isBufferStarted(b.getTypeBuff())).orElse(false));
        ui.setQuoteStarted(quote.map(b -> isBufferStarted(b.getTypeBuff())).orElse(false));
        ui.setDecimalStarted(decimal.map(b -> isBufferStarted(b.getTypeBuff())).orElse(false));
        ui.setBoardStarted(board.map(b -> isBufferStarted(b.getTypeBuff())).orElse(false));

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
        for (BufferConfig master : bufferConfigService.findMasterBuffers()) {
            int masterId = master.getTypeBuff();
            if (masterId == stoppingMaster || !isBufferStarted(masterId)) {
                continue;
            }
            if (dependentTypes(masterId).contains(depTypeBuff)) {
                return true;
            }
        }
        return false;
    }

    private List<Integer> dependentTypes(int masterTypeBuff) {
        BufferConfig master = bufferConfigService.findByTypeBuff(masterTypeBuff);
        return BufferDependents.forMaster(
                master,
                bufferConfigService.getActiveBuffers(),
                constants.isMmvSectionSecurityMarket());
    }

    private boolean canEnableBuffer(int typeBuff) {
        BufferConfig cfg = bufferConfigService.findByTypeBuff(typeBuff);
        if (cfg == null) {
            return true;
        }
        if (cfg.isDecimalBuffer()) {
            return loadDecimals;
        }
        if (cfg.isBoardBuffer()) {
            return loadBoards;
        }
        return true;
    }
}
