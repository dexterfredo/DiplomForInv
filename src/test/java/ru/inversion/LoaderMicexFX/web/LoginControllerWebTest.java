package ru.inversion.LoaderMicexFX.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.datasource.username=postgres")
class LoginControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageRendersForm() throws Exception {
        mockMvc.perform(get("/login").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Вход")));
    }

    @Test
    void loginPageShowsLogoutMessage() throws Exception {
        mockMvc.perform(get("/login").param("logout", "1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Сеанс завершён")));
    }
}
