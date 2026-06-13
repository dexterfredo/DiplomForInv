package ru.inversion.LoaderMicexFX.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.inversion.LoaderMicexFX.db.DbSaveDiagnosticsService;
import ru.inversion.LoaderMicexFX.gateway.SimpleGatewayClient;
import ru.inversion.LoaderMicexFX.model.ClientStatus;
import ru.inversion.LoaderMicexFX.model.GatewayConnectionSettings;
import ru.inversion.LoaderMicexFX.model.LoaderUiState;
import ru.inversion.LoaderMicexFX.service.BufferConfigService;
import ru.inversion.LoaderMicexFX.service.ExchangeWorkerService;
import ru.inversion.LoaderMicexFX.service.LoaderBufferControlService;
import ru.inversion.LoaderMicexFX.service.LoaderTimerPreferences;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StatusController.class)
@AutoConfigureMockMvc(addFilters = false)
class StatusControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeWorkerService workerService;
    @MockitoBean
    private BufferConfigService bufferConfigService;
    @MockitoBean
    private SimpleGatewayClient gatewayClient;
    @MockitoBean
    private LoaderBufferControlService bufferControl;
    @MockitoBean
    private LoaderTimerPreferences timerPreferences;
    @MockitoBean
    private DbSaveDiagnosticsService dbDiagnostics;

    private final ClientStatus status = new ClientStatus();
    private final LoaderUiState ui = new LoaderUiState();

    @BeforeEach
    void setUp() {
        when(workerService.getStatus()).thenReturn(status);
        when(gatewayClient.getCurrentSettings()).thenReturn(new GatewayConnectionSettings());
        when(bufferControl.buildUiState(anyBoolean(), any())).thenReturn(ui);
        when(dbDiagnostics.getRecentErrors()).thenReturn(List.of());
    }

    @Test
    void loaderPageRendersStatus() throws Exception {
        mockMvc.perform(get("/loader").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("loader-status"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Состояние загрузчика")));
    }

    @Test
    void dataPageRendersStatistics() throws Exception {
        mockMvc.perform(get("/loader/data").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("loader-data"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Статистика")));
    }
}
