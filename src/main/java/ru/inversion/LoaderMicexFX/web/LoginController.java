package ru.inversion.LoaderMicexFX.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Value("${spring.datasource.username:}")
    private String defaultUsername;

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        if (error != null) {
            model.addAttribute("error", "Неверный логин или пароль, либо база данных недоступна");
        }
        if (logout != null) {
            model.addAttribute("message", "Сеанс завершён");
        }
        if (defaultUsername != null && !defaultUsername.isBlank()) {
            model.addAttribute("defaultUsername", defaultUsername);
        }
        return "login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/loader";
    }
}
