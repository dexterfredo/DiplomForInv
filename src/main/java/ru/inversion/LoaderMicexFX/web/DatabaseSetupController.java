package ru.inversion.LoaderMicexFX.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ru.inversion.LoaderMicexFX.model.DatabaseConnectionSettings;
import ru.inversion.LoaderMicexFX.service.DatabaseConnectionService;
import ru.inversion.LoaderMicexFX.service.ExchangeWorkerService;

@Controller
public class DatabaseSetupController {

    private final DatabaseConnectionService databaseConnectionService;
    private final ExchangeWorkerService exchangeWorkerService;

    public DatabaseSetupController(
            DatabaseConnectionService databaseConnectionService,
            ExchangeWorkerService exchangeWorkerService) {
        this.databaseConnectionService = databaseConnectionService;
        this.exchangeWorkerService = exchangeWorkerService;
    }

    @GetMapping("/")
    public String setupPage(Model model) {
        DatabaseConnectionSettings form = databaseConnectionService.isConfigured()
                ? databaseConnectionService.getCurrent()
                : databaseConnectionService.getDefaults();
        model.addAttribute("db", form);
        model.addAttribute("connected", databaseConnectionService.isConfigured());
        return "db-setup";
    }

    @PostMapping("/db/connect")
    public String connect(@ModelAttribute("db") DatabaseConnectionSettings form, Model model) {
        if (exchangeWorkerService.getStatus().isConnected()) {
            exchangeWorkerService.stopApi();
        }
        try {
            if (form.getJdbcUrl() == null || form.getJdbcUrl().isBlank()) {
                form.rebuildJdbcUrl();
            }
            databaseConnectionService.apply(form);
            return "redirect:/loader";
        } catch (Exception e) {
            model.addAttribute("db", form);
            model.addAttribute("connected", false);
            model.addAttribute("error", e.getMessage());
            return "db-setup";
        }
    }
}
