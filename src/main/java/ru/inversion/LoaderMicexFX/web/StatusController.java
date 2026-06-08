package ru.inversion.LoaderMicexFX.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.inversion.LoaderMicexFX.db.DbSaveDiagnosticsService;
import ru.inversion.LoaderMicexFX.gateway.SimpleGatewayClient;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.ClientStatus;
import ru.inversion.LoaderMicexFX.model.GatewayConnectionSettings;
import ru.inversion.LoaderMicexFX.model.MicexConnectionType;
import ru.inversion.LoaderMicexFX.service.BufferConfigService;
import ru.inversion.LoaderMicexFX.service.DatabaseConnectionService;
import ru.inversion.LoaderMicexFX.service.ExchangeWorkerService;
import ru.inversion.LoaderMicexFX.service.LoaderBufferControlService;
import ru.inversion.LoaderMicexFX.model.LoaderUiState;
import ru.inversion.LoaderMicexFX.service.LoaderTimerPreferences;

import java.util.List;

@Controller
public class StatusController {

    private final ExchangeWorkerService workerService;
    private final BufferConfigService bufferConfigService;
    private final SimpleGatewayClient gatewayClient;
    private final DatabaseConnectionService databaseConnectionService;
    private final LoaderBufferControlService bufferControl;
    private final LoaderTimerPreferences timerPreferences;
    private final DbSaveDiagnosticsService dbDiagnostics;

    public StatusController(
            ExchangeWorkerService workerService,
            BufferConfigService bufferConfigService,
            SimpleGatewayClient gatewayClient,
            DatabaseConnectionService databaseConnectionService,
            LoaderBufferControlService bufferControl,
            LoaderTimerPreferences timerPreferences,
            DbSaveDiagnosticsService dbDiagnostics) {
        this.workerService = workerService;
        this.bufferConfigService = bufferConfigService;
        this.gatewayClient = gatewayClient;
        this.databaseConnectionService = databaseConnectionService;
        this.bufferControl = bufferControl;
        this.timerPreferences = timerPreferences;
        this.dbDiagnostics = dbDiagnostics;
    }

    @GetMapping("/loader")
    public String loader(Model model) {
        if (!databaseConnectionService.isConfigured()) {
            return "redirect:/";
        }
        ClientStatus status = workerService.getStatus();
        GatewayConnectionSettings gw = gatewayClient.getCurrentSettings();
        model.addAttribute("status", status);
        model.addAttribute("gateway", gw);
        model.addAttribute("dbUrl", databaseConnectionService.getCurrent().effectiveJdbcUrl());
        LoaderUiState ui = bufferControl.buildUiState(status.isSessionReady(), timerPreferences);
        if (gw.getConnectionType() != null) {
            ui.setConnectionType(gw.getConnectionType().name());
        }
        model.addAttribute("ui", ui);
        return "status";
    }

    @GetMapping("/api/buffers")
    @ResponseBody
    public List<BufferConfig> buffers() {
        if (!databaseConnectionService.isConfigured()) {
            return List.of();
        }
        return bufferConfigService.getActiveBuffers();
    }

    @GetMapping("/api/status")
    @ResponseBody
    public ClientStatus status() {
        return workerService.getStatus();
    }

    @GetMapping("/api/db/diagnostics")
    @ResponseBody
    public java.util.Map<String, Object> dbDiagnostics() {
        if (!databaseConnectionService.isConfigured()) {
            return java.util.Map.of("error", "DB not configured");
        }
        dbDiagnostics.pollDatabase();
        return dbDiagnostics.snapshot();
    }

    @PostMapping("/connect")
    public String connect(
            @ModelAttribute("gateway") GatewayConnectionSettings gateway,
            @RequestParam(value = "loadDecimals", required = false) Boolean loadDecimals,
            @RequestParam(value = "loadBoards", required = false) Boolean loadBoards,
            @RequestParam(value = "connectionType", required = false) String connectionType,
            @RequestParam(value = "dealTime", required = false) String dealTime,
            @RequestParam(value = "quoteTime", required = false) String quoteTime,
            @RequestParam(value = "settingsTime", required = false) String settingsTime,
            @RequestParam(value = "dealTimerEnabled", required = false) Boolean dealTimerEnabled) {
        if (!databaseConnectionService.isConfigured()) {
            return "redirect:/";
        }
        applyFormState(loadDecimals, loadBoards, connectionType, dealTime, quoteTime, settingsTime, dealTimerEnabled);
        gatewayClient.applyRuntimeSettings(enrichGateway(gateway, connectionType));
        workerService.startApi();
        return "redirect:/loader";
    }

    @PostMapping("/disconnect")
    public String disconnect() {
        if (!databaseConnectionService.isConfigured()) {
            return "redirect:/";
        }
        workerService.stopApi();
        return "redirect:/loader";
    }

    @PostMapping("/loader/settings/toggle")
    public String toggleSettings(
            @RequestParam(value = "loadDecimals", required = false) Boolean loadDecimals,
            @RequestParam(value = "loadBoards", required = false) Boolean loadBoards,
            @RequestParam(value = "connectionType", required = false) String connectionType,
            @RequestParam(value = "dealTime", required = false) String dealTime,
            @RequestParam(value = "quoteTime", required = false) String quoteTime,
            @RequestParam(value = "settingsTime", required = false) String settingsTime,
            @RequestParam(value = "dealTimerEnabled", required = false) Boolean dealTimerEnabled) {
        return toggleBufferRoute(loadDecimals, loadBoards, connectionType, dealTime, quoteTime, settingsTime, dealTimerEnabled,
                () -> bufferControl.toggleSettings(workerService.getStatus().isSessionReady()));
    }

    @PostMapping("/loader/deal/toggle")
    public String toggleDeal(
            @RequestParam(value = "loadDecimals", required = false) Boolean loadDecimals,
            @RequestParam(value = "loadBoards", required = false) Boolean loadBoards,
            @RequestParam(value = "connectionType", required = false) String connectionType,
            @RequestParam(value = "dealTime", required = false) String dealTime,
            @RequestParam(value = "quoteTime", required = false) String quoteTime,
            @RequestParam(value = "settingsTime", required = false) String settingsTime,
            @RequestParam(value = "dealTimerEnabled", required = false) Boolean dealTimerEnabled) {
        return toggleBufferRoute(loadDecimals, loadBoards, connectionType, dealTime, quoteTime, settingsTime, dealTimerEnabled,
                () -> bufferControl.toggleDeal(workerService.getStatus().isSessionReady()));
    }

    @PostMapping("/loader/quote/toggle")
    public String toggleQuote(
            @RequestParam(value = "loadDecimals", required = false) Boolean loadDecimals,
            @RequestParam(value = "loadBoards", required = false) Boolean loadBoards,
            @RequestParam(value = "connectionType", required = false) String connectionType,
            @RequestParam(value = "dealTime", required = false) String dealTime,
            @RequestParam(value = "quoteTime", required = false) String quoteTime,
            @RequestParam(value = "settingsTime", required = false) String settingsTime,
            @RequestParam(value = "dealTimerEnabled", required = false) Boolean dealTimerEnabled) {
        return toggleBufferRoute(loadDecimals, loadBoards, connectionType, dealTime, quoteTime, settingsTime, dealTimerEnabled,
                () -> bufferControl.toggleQuote(workerService.getStatus().isSessionReady()));
    }

    private String toggleBufferRoute(
            Boolean loadDecimals,
            Boolean loadBoards,
            String connectionType,
            String dealTime,
            String quoteTime,
            String settingsTime,
            Boolean dealTimerEnabled,
            java.util.function.Supplier<String> action) {
        if (!databaseConnectionService.isConfigured()) {
            return "redirect:/";
        }
        applyFormState(loadDecimals, loadBoards, connectionType, dealTime, quoteTime, settingsTime, dealTimerEnabled);
        String err = action.get();
        if (err != null) {
            workerService.getStatus().setLastError(err);
        } else if (workerService.getStatus().isSessionReady()) {
            workerService.runSaveCycle();
        }
        return "redirect:/loader";
    }

    private void applyFormState(
            Boolean loadDecimals,
            Boolean loadBoards,
            String connectionType,
            String dealTime,
            String quoteTime,
            String settingsTime,
            Boolean dealTimerEnabled) {
        if (loadDecimals != null || loadBoards != null) {
            bufferControl.setPreferences(
                    loadDecimals != null ? loadDecimals : bufferControl.isLoadDecimals(),
                    loadBoards != null ? loadBoards : bufferControl.isLoadBoards());
        }
        timerPreferences.applyFromForm(dealTime, quoteTime, settingsTime, dealTimerEnabled);
    }

    private GatewayConnectionSettings enrichGateway(GatewayConnectionSettings gateway, String connectionType) {
        if (gateway == null) {
            gateway = new GatewayConnectionSettings();
        }
        gateway.setConnectionType(MicexConnectionType.fromString(connectionType));
        return gateway;
    }
}
