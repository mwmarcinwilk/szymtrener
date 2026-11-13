package pl.szymtrener.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import pl.szymtrener.media.YoutubeThumbnails;

import java.util.Optional;

/**
 * Jednorazowe przepisanie wpisow opublikowanych przed 11.2026, ktore maja jeszcze miniatury z i.ytimg.com.
 * Rusza w tle po starcie (siec nie opoznia ani nie zatrzymuje uruchomienia), kazdy wpis w osobnej transakcji
 * {@link PostService#replaceContentKeepingDates}: bez podbijania daty zmiany, z powiazaniami w bibliotece mediow.
 * Idempotentne: wpis, w ktorym choc jednej miniatury nie da sie pobrac, zostaje bez zmian i wraca przy nastepnym starcie.
 */
@Component
class YoutubeThumbnailBackfill {

    private static final Logger log = LoggerFactory.getLogger(YoutubeThumbnailBackfill.class);

    private final PostRepository posts;
    private final YoutubeThumbnails thumbnails;
    private final PostService postService;

    YoutubeThumbnailBackfill(PostRepository posts, YoutubeThumbnails thumbnails, PostService postService) {
        this.posts = posts;
        this.thumbnails = thumbnails;
        this.postService = postService;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        localizeLegacyPosts();
    }

    /** Liczba przepisanych wpisow. */
    int localizeLegacyPosts() {
        int done = 0;
        for (Post post : posts.findByContentHtmlContaining(YoutubeThumbnails.SOURCE_PREFIX)) {
            try {
                String html = post.getContentHtml();
                Optional<String> local = thumbnails.localizeAllOrNothing(html);
                if (local.isEmpty()) {
                    log.warn("Wpis {}: miniatury YouTube jeszcze nie pobrane, ponowna proba przy nastepnym starcie", post.getId());
                } else if (!local.get().equals(html)) {
                    postService.replaceContentKeepingDates(post.getId(), local.get());
                    done++;
                }
            } catch (RuntimeException e) {
                log.warn("Wpis {}: przeniesienie miniatur YouTube nieudane ({})", post.getId(), e.getClass().getSimpleName());
            }
        }
        if (done > 0) log.info("Miniatury YouTube przeniesione na nasz serwer we wpisach: {}", done);
        return done;
    }
}
