package ru.inversion.LoaderMicexFX.service;

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

    private int typeSrc;
    private int typeSection;
    private int sectState;
    private int sectShare;
    private volatile boolean loaded;

    public LoaderConstantsService(TrGetConstRepository trGetConst) {
        this.trGetConst = trGetConst;
    }

    public int getTypeSrc() {
        ensureLoaded();
        return typeSrc;
    }

    public int getTypeSection() {
        ensureLoaded();
        return typeSection;
    }

    public boolean isMmvSectionSecurityMarket() {
        ensureLoaded();
        return typeSection == sectState || typeSection == sectShare;
    }

    public int sectState() {
        ensureLoaded();
        return sectState;
    }

    public int sectShare() {
        ensureLoaded();
        return sectShare;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void reload() {
        loadFromDatabase();
        loaded = true;
        log.info("tr_get_const из БД: type_src={}, type_section={} ({})",
                typeSrc, typeSection, typeSectionFn);
    }

    private void loadFromDatabase() {
        typeSrc = trGetConst.gcSrcMicex();
        typeSection = resolveTypeSectionFromDb(typeSectionFn);
        sectState = trGetConst.gcDealPlaceSectionState();
        sectShare = trGetConst.gcDealPlaceSectionShare();
    }

    private void ensureLoaded() {
        if (!loaded) {
            throw new IllegalStateException(
                    "tr_get_const не загружен — подключитесь к БД (кнопка «Подключиться» на /loader)");
        }
    }

    private int resolveTypeSectionFromDb(String fn) {
        if (fn == null || fn.isBlank()) {
            return trGetConst.gcMmvbSectFx();
        }
        return switch (fn.trim().toLowerCase()) {
            case "gc_mmvb_sect_fx" -> trGetConst.gcMmvbSectFx();
            case "gc_deal_place_section_state" -> trGetConst.gcDealPlaceSectionState();
            case "gc_deal_place_section_share" -> trGetConst.gcDealPlaceSectionShare();
            default -> throw new IllegalArgumentException(
                    "app.loader.type-section-fn: неизвестная функция '" + fn
                            + "'. Допустимо: gc_mmvb_sect_fx, gc_deal_place_section_state, gc_deal_place_section_share");
        };
    }
}
