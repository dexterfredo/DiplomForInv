package ru.inversion.LoaderMicexFX.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.inversion.LoaderMicexFX.loaderconfig.TimerIntervalParser;
import ru.inversion.LoaderMicexFX.model.BufferConfig;
import ru.inversion.LoaderMicexFX.model.BufferTimerGroup;

@Component
public class LoaderTimerPreferences {

    @Value("${app.loader.timer.deal-seconds:0}")
    private int defaultDealSeconds;

    @Value("${app.loader.timer.quote-seconds:10}")
    private int defaultQuoteSeconds;

    @Value("${app.loader.timer.settings-seconds:9000}")
    private int defaultSettingsSeconds;

    @Value("${app.loader.timer.deal-enabled:true}")
    private boolean defaultDealTimerEnabled;

    private volatile int dealSeconds;
    private volatile int quoteSeconds;
    private volatile int settingsSeconds;
    private volatile boolean dealTimerEnabled = true;

    @jakarta.annotation.PostConstruct
    void init() {
        dealSeconds = defaultDealSeconds;
        quoteSeconds = defaultQuoteSeconds;
        settingsSeconds = defaultSettingsSeconds;
        dealTimerEnabled = defaultDealTimerEnabled;
    }

    public void applyFromForm(String dealTime, String quoteTime, String settingsTime, Boolean dealTimerEnabledFlag) {
        if (dealTime != null && !dealTime.isBlank()) {
            dealSeconds = TimerIntervalParser.toIntervalSeconds(dealTime);
        }
        if (quoteTime != null && !quoteTime.isBlank()) {
            quoteSeconds = TimerIntervalParser.toIntervalSeconds(quoteTime);
        }
        if (settingsTime != null && !settingsTime.isBlank()) {
            settingsSeconds = TimerIntervalParser.toIntervalSeconds(settingsTime);
        }
        if (dealTimerEnabledFlag != null) {
            dealTimerEnabled = dealTimerEnabledFlag;
        }
    }

    public int getIntervalSecFor(BufferConfig buff) {
        if (buff == null) {
            return 5;
        }
        BufferTimerGroup group = buff.getTimerGroup();
        return switch (group) {
            case DEAL -> dealSeconds;
            case QUOTE -> quoteSeconds;
            case SETTINGS -> settingsSeconds;
        };
    }

    public String formatDealTime() {
        return TimerIntervalParser.formatSeconds(dealSeconds);
    }

    public String formatQuoteTime() {
        return TimerIntervalParser.formatSeconds(quoteSeconds);
    }

    public String formatSettingsTime() {
        return TimerIntervalParser.formatSeconds(settingsSeconds);
    }

    public boolean isDealTimerEnabled() {
        return dealTimerEnabled;
    }
}
