package pl.szymtrener.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.szymtrener.analytics.PageViewRepository;

import java.time.Duration;
import java.time.Instant;

/**
 * Retencja danych (RODO). Tabela page_view rosnie z kazda odslona i bez
 * kasowania staje sie najwiekszym zbiorem w bazie — a dane starsze niz rok
 * nie sluza juz niczemu poza kopia zapasowa.
 */
@Component
public class CleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);
    private static final Duration RETENTION = Duration.ofDays(365);

    private final PageViewRepository views;

    public CleanupScheduler(PageViewRepository views) {
        this.views = views;
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgeOldPageViews() {
        Instant cutoff = Instant.now().minus(RETENTION);
        int removed = views.deleteOlderThan(cutoff);
        if (removed > 0) {
            log.info("Usunieto odslon starszych niz {} dni: {}", RETENTION.toDays(), removed);
        }
    }
}
