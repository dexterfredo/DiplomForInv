package ru.inversion.LoaderMicexFX.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.inversion.LoaderMicexFX.gateway.SimpleGatewayClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeWorkerServiceTest {

    @Mock
    private SimpleGatewayClient gatewayClient;
    @Mock
    private BufferConfigService bufferConfigService;
    @Mock
    private MicexLoaderService micexLoaderService;
    @Mock
    private LoaderBufferControlService bufferControl;
    @Mock
    private TesystimeSyncService tesystimeSyncService;

    private ExchangeWorkerService workerService;

    @BeforeEach
    void setUp() {
        workerService = new ExchangeWorkerService(
                gatewayClient,
                bufferConfigService,
                micexLoaderService,
                bufferControl,
                tesystimeSyncService);
    }

    @Test
    void startApi_whenAlreadyConnected_setsError() {
        when(gatewayClient.isConnected()).thenReturn(true);
        workerService.startApi();
        assertEquals("Уже подключено", workerService.getStatus().getLastError());
    }

    @Test
    void startApi_onConnectFailure_marksDisconnected() throws Exception {
        when(gatewayClient.isConnected()).thenReturn(false);
        doThrow(new RuntimeException("MICEX down")).when(gatewayClient).connect();
        workerService.startApi();
        assertFalse(workerService.getStatus().isConnected());
        verify(gatewayClient).disconnect();
    }
}
