package pl.szymtrener.content;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.szymtrener.media.YoutubeThumbnails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YoutubeThumbnailBackfillTest {

    private static final String LEGACY = "<img src=\"https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg\">";

    private final PostRepository posts = mock(PostRepository.class);
    private final YoutubeThumbnails thumbnails = mock(YoutubeThumbnails.class);
    private final PostService postService = mock(PostService.class);
    private final YoutubeThumbnailBackfill backfill = new YoutubeThumbnailBackfill(posts, thumbnails, postService);

    private Post legacyPost(String html) {
        Post post = new Post();
        post.setContentHtml(html);
        when(posts.findByContentHtmlContaining(YoutubeThumbnails.SOURCE_PREFIX)).thenReturn(List.of(post));
        return post;
    }

    @Test
    @DisplayName("stary wpis dostaje lokalne miniatury przez podmianę bez daty edycji i z powiązaniami mediów")
    void rewritesLegacyPosts() {
        Post post = legacyPost(LEGACY);
        when(thumbnails.localizeAllOrNothing(LEGACY)).thenReturn(Optional.of("<img src=\"/media/2026/11/a.jpg\">"));

        assertThat(backfill.localizeLegacyPosts()).isEqualTo(1);
        verify(postService).replaceContentKeepingDates(post.getId(), "<img src=\"/media/2026/11/a.jpg\">");
    }

    @Test
    @DisplayName("nieudane pobranie przy starcie: wpis bez zmian, ponowna próba przy kolejnym starcie")
    void keepsPostWhenThumbnailUnavailable() {
        legacyPost(LEGACY);
        when(thumbnails.localizeAllOrNothing(LEGACY)).thenReturn(Optional.empty());

        assertThat(backfill.localizeLegacyPosts()).isZero();
        verify(postService, never()).replaceContentKeepingDates(any(), anyString());
    }

    @Test
    @DisplayName("adres i.ytimg.com tylko w tekście: wpis nie jest zapisywany przy każdym starcie")
    void skipsPostsWithoutThumbnails() {
        String html = "<p>Zobacz https://i.ytimg.com/vi/ w tekście</p>";
        legacyPost(html);
        when(thumbnails.localizeAllOrNothing(html)).thenReturn(Optional.of(html));

        assertThat(backfill.localizeLegacyPosts()).isZero();
        verify(postService, never()).replaceContentKeepingDates(any(), anyString());
    }

    @Test
    @DisplayName("błąd zapisu jednego wpisu nie przerywa pozostałych ani startu aplikacji")
    void survivesFailures() {
        legacyPost(LEGACY);
        when(thumbnails.localizeAllOrNothing(LEGACY)).thenThrow(new IllegalStateException("db"));

        assertThat(backfill.localizeLegacyPosts()).isZero();
    }
}
