package ru.inversion.LoaderMicexFX.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.db.DbSaveOutcome;
import ru.inversion.LoaderMicexFX.model.ClientStatus;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LoaderMetricsService {

    private final MeterRegistry registry;
    private final AtomicInteger gatewayConnected = new AtomicInteger(0);
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicInteger reconnectCount = new AtomicInteger(0);

    public LoaderMetricsService(MeterRegistry registry) {
        this.registry = registry;
        Gauge.builder("loader.gateway.connected", gatewayConnected, AtomicInteger::get)
                .description("1 если подключён к MICEX-шлюзу")
                .register(registry);
        Gauge.builder("loader.messages.total", totalMessages, AtomicLong::get)
                .description("Сообщений с шлюза (накопительно)")
                .register(registry);
        Gauge.builder("loader.reconnect.count", reconnectCount, AtomicInteger::get)
                .description("Число reconnect к шлюзу")
                .register(registry);
    }

    public void recordSaveOutcome(int typeBuff, DbSaveOutcome outcome) {
        if (outcome == null) {
            return;
        }
        registry.counter(
                "loader.db.save",
                "type_buff", String.valueOf(typeBuff),
                "status", outcome.status().name()).increment();
    }

    public void refreshStatus(ClientStatus status) {
        if (status == null) {
            return;
        }
        gatewayConnected.set(status.isConnected() ? 1 : 0);
        totalMessages.set(status.getTotalMessages());
        reconnectCount.set(status.getReconnectCount());
    }
}
