package pl.szymtrener.analytics;

import org.springframework.stereotype.Component;
import pl.szymtrener.consent.ConsentedViews;

import java.util.List;

/** Odslony za zgoda dla eksportu danych zgody. */
@Component
class ConsentedPageViews implements ConsentedViews {

    private final PageViewRepository views;

    ConsentedPageViews(PageViewRepository views) {
        this.views = views;
    }

    @Override
    public List<View> find(long consentId) {
        return views.findByConsentIdOrderByViewedAt(consentId).stream()
                .map(v -> new View(v.getPath(), v.getViewedAt()))
                .toList();
    }
}
