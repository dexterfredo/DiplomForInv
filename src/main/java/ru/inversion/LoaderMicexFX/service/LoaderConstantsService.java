package ru.inversion.LoaderMicexFX.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.inversion.LoaderMicexFX.db.TrGetConstRepository;

@Service
public class LoaderConstantsService {

    private static final Logger log = LoggerFactory.getLogger(LoaderConstantsService.class);

    private final TrGetConstRepository trGetConst;

    @Value("${app.loader.type-section-fn:gc_mmvb_sect_fx}")
    private String typeSectionFn;

    
    @Value("${app.loader.fallback.type-src:2432}")
    private int fallbackTypeSrc;

    @Value("${app.loader.fallback.type-section:-1}")
    private int fallbackTypeSection;

    @Value("${app.loader.fallback.buff-deal:2323}")
    private int fallbackBuffDeal;

    @Value("${app.loader.fallback.buff-quote-fx:5922}")
    private int fallbackBuffQuoteFx;

    @Value("${app.loader.fallback.buff-deal-sec:2324}")
    private int fallbackBuffDealSec;

    @Value("${app.loader.fallback.buff-quote-sec:2325}")
    private int fallbackBuffQuoteSec;

    @Value("${app.loader.fallback.buff-order-sec:5885}")
    private int fallbackBuffOrderSec;

    @Value("${app.loader.fallback.buff-decimal:5886}")
    private int fallbackBuffDecimal;

    @Value("${app.loader.fallback.buff-board:5887}")
    private int fallbackBuffBoard;

    @Value("${app.loader.fallback.buff-lotsize:6181}")
    private int fallbackBuffLotsize;

    @Value("${app.loader.fallback.sect-state:1774}")
    private int fallbackSectState;

    @Value("${app.loader.fallback.sect-share:1775}")
    private int fallbackSectShare;

    private int typeSrc;
    private int typeSection;
    private int buffDeal;
    private int buffQuoteFx;
    private int buffDealSec;
    private int buffQuoteSec;
    private int buffOrderSec;
    private int buffDecimal;
    private int buffBoard;
    private int buffLotsize;
    private int sectState;
    private int sectShare;
    private volatile boolean fromDatabase;

    public LoaderConstantsService(TrGetConstRepository trGetConst) {
        this.trGetConst = trGetConst;
    }

    
    @PostConstruct
    void init() {
        applyFallback();
        log.info("Константы (fallback, БД ещё не подключена): type_src={}, type_section={}, deal={}",
                typeSrc, typeSection, buffDeal);
    }

    public int getTypeSrc() {
        return typeSrc;
    }

    public int getTypeSection() {
        return typeSection;
    }

    public int buffMicexDeal() {
        return buffDeal;
    }

    public int buffMicexQuoteFx() {
        return buffQuoteFx;
    }

    public int buffMicexDealSec() {
        return buffDealSec;
    }

    public int buffMicexQuoteSec() {
        return buffQuoteSec;
    }

    public int buffMicexOrderSec() {
        return buffOrderSec;
    }

    public int buffMicexDecimal() {
        return buffDecimal;
    }

    public int buffMicexBoard() {
        return buffBoard;
    }

    public int buffMicexLotsize() {
        return buffLotsize;
    }

    public boolean isMmvSectionSecurityMarket() {
        return typeSection == sectState || typeSection == sectShare;
    }

    public boolean isBuffFxQuote(int typeBuff) {
        return typeBuff == buffQuoteFx;
    }

    /** Буферы котировок (FX и SEC). */
    public boolean isBuffQuote(int typeBuff) {
        return typeBuff == buffQuoteFx || typeBuff == buffQuoteSec;
    }

    public int sectState() {
        return sectState;
    }

    public int sectShare() {
        return sectShare;
    }

    public boolean isFromDatabase() {
        return fromDatabase;
    }

    
    public void reload() {
        try {
            loadFromDatabase();
            fromDatabase = true;
            log.info("tr_get_const из БД: type_src={}, type_section={} ({}), deal={}, quote_fx={}",
                    typeSrc, typeSection, typeSectionFn, buffDeal, buffQuoteFx);
        } catch (Exception e) {
            fromDatabase = false;
            applyFallback();
            log.warn("tr_get_const недоступен ({}), используются fallback. Проверьте 23_tr_get_const.sql и подключение к БД.",
                    e.getMessage());
        }
    }

    private void loadFromDatabase() {
        typeSrc = trGetConst.gcSrcMicex();
        typeSection = resolveTypeSectionFromDb(typeSectionFn);
        buffDeal = trGetConst.gcBuffMicexDeal();
        buffQuoteFx = trGetConst.gcBuffMicexQuoteFx();
        buffDealSec = trGetConst.gcBuffMicexDealSec();
        buffQuoteSec = trGetConst.gcBuffMicexQuoteSec();
        buffOrderSec = trGetConst.gcBuffMicexOrderSec();
        buffDecimal = trGetConst.gcBuffMicexDecimal();
        buffBoard = trGetConst.gcBuffMicexBoard();
        buffLotsize = trGetConst.gcBuffMicexLotsize();
        sectState = trGetConst.gcDealPlaceSectionState();
        sectShare = trGetConst.gcDealPlaceSectionShare();
    }

    private void applyFallback() {
        typeSrc = fallbackTypeSrc;
        typeSection = resolveTypeSectionFallback(typeSectionFn);
        buffDeal = fallbackBuffDeal;
        buffQuoteFx = fallbackBuffQuoteFx;
        buffDealSec = fallbackBuffDealSec;
        buffQuoteSec = fallbackBuffQuoteSec;
        buffOrderSec = fallbackBuffOrderSec;
        buffDecimal = fallbackBuffDecimal;
        buffBoard = fallbackBuffBoard;
        buffLotsize = fallbackBuffLotsize;
        sectState = fallbackSectState;
        sectShare = fallbackSectShare;
    }

    private int resolveTypeSectionFromDb(String fn) {
        return trGetConst.callFunction(resolveTypeSectionFunction(fn));
    }

    private int resolveTypeSectionFallback(String fn) {
        if (fallbackTypeSection >= 0) {
            return fallbackTypeSection;
        }
        return -1;
    }

    private String resolveTypeSectionFunction(String fn) {
        if (fn != null && !fn.isBlank()) {
            return fn.trim();
        }
        if (typeSectionFn != null && !typeSectionFn.isBlank()) {
            return typeSectionFn.trim();
        }
        throw new IllegalStateException(
                "Задайте app.loader.type-section-fn (например gc_mmvb_sect_fx)");
    }
}
