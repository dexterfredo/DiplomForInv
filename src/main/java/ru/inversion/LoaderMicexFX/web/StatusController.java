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
import ru.inversion.LoaderMicexFX.model.LoaderUiState;
import ru.inversion.LoaderMicexFX.service.BufferConfigService;
import ru.inversion.LoaderMicexFX.service.ExchangeWorkerService;
import ru.inversion.LoaderMicexFX.service.LoaderBufferControlService;
import ru.inversion.LoaderMicexFX.service.LoaderTimerPreferences;

import java.util.List;
import java.util.Map;

@Controller
public class StatusController {

    private final ExchangeWorkerService workerService;
    private final BufferConfigService bufferConfigService;
    private final SimpleGatewayClient gatewayClient;
    private final LoaderBufferControlService bufferControl;
    private final LoaderTimerPreferences timerPreferences;
    private final DbSaveDiagnosticsService dbDiagnostics;

    public StatusController(
            ExchangeWorkerService workerService,
            BufferConfigService bufferConfigService,
            SimpleGatewayClient gatewayClient,
            LoaderBufferControlService bufferControl,
            LoaderTimerPreferences timerPreferences,
            DbSaveDiagnosticsService dbDiagnostics) {
        this.workerService = workerService;
        this.bufferConfigService = bufferConfigService;
        this.gatewayClient = gatewayClient;
        this.bufferControl = bufferControl;
        this.timerPreferences = timerPreferences;
        this.dbDiagnostics = dbDiagnostics;
    }

    @GetMapping("/loader")
    public String loader(Model model) {
        fillPage(model);
        return "loader-status";
    }

    @GetMapping("/loader/settings")
    public String settings(Model model) {
        fillPage(model);
        return "loader-settings";
    }

    @GetMapping("/loader/data")
    public String data(Model model) {
        dbDiagnostics.pollDatabase();
        fillPage(model);
        model.addAttribute("dbErrors", dbDiagnostics.getRecentErrors());
        return "loader-data";
    }

    @GetMapping("/api/buffers")
    @ResponseBody
    public List<BufferConfig> buffers() {
        return bufferConfigService.getActiveBuffers();
    }

    @GetMapping("/api/status")
    @ResponseBody
    public ClientStatus status() {
        return workerService.getStatus();
    }

    @GetMapping("/api/db/diagnostics")
    @ResponseBody
    public Map<String, Object> dbDiagnostics() {
        dbDiagnostics.pollDatabase();
        return dbDiagnostics.snapshot();
    }

    @PostMapping("/connect")
    public String connect(
            @ModelAttribute("gateway") GatewayConnectionSettings gateway,
            @RequestParam(value = "loadDecimals", required = false) Boolean loadDecimals,
            @RequestParam(value = "loadBoards", required = false) Boolean loadBoards,
            @RequestParam(value = "dealTime", required = false) String dealTime,
            @RequestParam(value = "quoteTime", required = false) String quoteTime,
            @RequestParam(value = "settingsTime", required = false) String settingsTime,
            @RequestParam(value = "dealTimerEnabled", required = false) Boolean dealTimerEnabled,
            @RequestParam(value = "returnTo", required = false) String returnTo) {
        saveForm(loadDecimals, loadBoards, dealTime, quoteTime, settingsTime, dealTimerEnabled);
        GatewayConnectionSettings settings = pickGateway(gateway);
        gatewayClient.applyRuntimeSettings(settings);
        workerService.startApi();
        return redirect(returnTo, "/loader");
    }

    @PostMapping("/disconnect")
    public String disconnect(@RequestParam(value = "returnTo", required = false) String returnTo) {
        workerService.stopApi();
        return redirect(returnTo, "/loader");
    }

    @PostMapping("/loader/settings/toggle")
    public String toggleSettings(
            @RequestParam(value = "loadDecimals", required = false) Boolean loadDecimals,
            @RequestParam(value = "loadBoards", required = false) Boolean loadBoards,
            @RequestParam(value = "dealTime", required = false) String dealTime,
            @RequestParam(value = "quoteTime", required = false) String quoteTime,
            @RequestParam(value = "settingsTime", required = false) String settingsTime,
            @RequestParam(value = "dealTimerEnabled", required = false) Boolean dealTimerEnabled) {
        return doToggle("/loader/settings", loadDecimals, loadBoards, dealTime, quoteTime, settingsTime, dealTimerEnabled,
                bufferControl.toggleSettings(workerService.getStatus().isConnected()));
    }

    @PostMapping("/loader/deal/toggle")
    public String toggleDeal(
            @RequestParam(value = "loadDecimals", required = false) Boolean loadDecimals,
            @RequestParam(value = "loadBoards", required = false) Boolean loadBoards,
            @RequestParam(value = "dealTime", required = false) String dealTime,
            @RequestParam(value = "quoteTime", required = false) String quoteTime,
            @RequestParam(value = "settingsTime", required = false) String settingsTime,
            @RequestParam(value = "dealTimerEnabled", required = false) Boolean dealTimerEnabled) {
        return doToggle("/loader/settings", loadDecimals, loadBoards, dealTime, quoteTime, settingsTime, dealTimerEnabled,
                bufferControl.toggleDeal(workerService.getStatus().isConnected()));
    }

    @PostMapping("/loader/quote/toggle")
    public String toggleQuote(
            @RequestParam(value = "loadDecimals", required = false) Boolean loadDecimals,
            @RequestParam(value = "loadBoards", required = false) Boolean loadBoards,
            @RequestParam(value = "dealTime", required = false) String dealTime,
            @RequestParam(value = "quoteTime", required = false) String quoteTime,
            @RequestParam(value = "settingsTime", required = false) String settingsTime,
            @RequestParam(value = "dealTimerEnabled", required = false) Boolean dealTimerEnabled) {
        return doToggle("/loader/settings", loadDecimals, loadBoards, dealTime, quoteTime, settingsTime, dealTimerEnabled,
                bufferControl.toggleQuote(workerService.getStatus().isConnected()));
    }

    private String doToggle(
            String page,
            Boolean loadDecimals,
            Boolean loadBoards,
            String dealTime,
            String quoteTime,
            String settingsTime,
            Boolean dealTimerEnabled,
            String error) {
        saveForm(loadDecimals, loadBoards, dealTime, quoteTime, settingsTime, dealTimerEnabled);
        if (error != null) {
            workerService.getStatus().setLastError(error);
        } else if (workerService.getStatus().isConnected()) {
            workerService.runSaveCycle();
        }
        return "redirect:" + page;
    }

    private void saveForm(
            Boolean loadDecimals,
            Boolean loadBoards,
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

    private void fillPage(Model model) {
        ClientStatus status = workerService.getStatus();
        model.addAttribute("status", status);
        model.addAttribute("gateway", gatewayClient.getCurrentSettings());
        LoaderUiState ui = bufferControl.buildUiState(status.isConnected(), timerPreferences);
        ui.setConnectionType("TCP2");
        model.addAttribute("ui", ui);
    }

    private GatewayConnectionSettings pickGateway(GatewayConnectionSettings submitted) {
        if (submitted != null && submitted.getConnectionText() != null && !submitted.getConnectionText().isBlank()) {
            return submitted;
        }
        return gatewayClient.getCurrentSettings();
    }

    private static String redirect(String returnTo, String fallback) {
        if (returnTo != null && returnTo.startsWith("/loader")) {
            return "redirect:" + returnTo;
        }
        return "redirect:" + fallback;
    }
}
