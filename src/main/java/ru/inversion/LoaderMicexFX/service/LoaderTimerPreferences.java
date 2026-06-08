package ru.inversion.LoaderMicexFX.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.inversion.LoaderMicexFX.loaderconfig.TimerIntervalParser;

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

    /** Интервал записи в БД (сек) для каждого type_buff. */
    public int getIntervalSecFor(int typeBuff, LoaderConstantsService constants) {
        if (typeBuff == constants.buffMicexDeal()) {
            return dealSeconds;
        }
        if (typeBuff == constants.buffMicexQuoteFx()) {
            return quoteSeconds;
        }
        if (typeBuff == constants.buffMicexDecimal()
                || typeBuff == constants.buffMicexBoard()
                || typeBuff == constants.buffMicexLotsize()) {
            return settingsSeconds;
        }
        return 5;
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

    /** Галочка «Таймер сделок» на форме — только вкл/выкл поля интервала. */
    public boolean isDealTimerEnabled() {
        return dealTimerEnabled;
    }
}
