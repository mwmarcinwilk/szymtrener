package pl.szymtrener.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YoutubeThumbnailsTest {

    private static final String ID = "dQw4w9WgXcQ";
    private static final String FACADE = "<div class=\"art-video yt-facade\" data-id=\"" + ID + "\">"
            + "<img src=\"https://i.ytimg.com/vi/" + ID + "/hqdefault.jpg\" alt=\"\" loading=\"lazy\"><span class=\"play\"></span></div>";
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};

    private final MediaService media = mock(MediaService.class);
    private final MediaRepository library = mock(MediaRepository.class);
    private final ThumbnailFetcher fetcher = mock(ThumbnailFetcher.class);
    private YoutubeThumbnails thumbnails;

    @BeforeEach
    void setUp() {
        thumbnails = new YoutubeThumbnails(media, library, fetcher);
        when(library.findFirstByOriginalName(anyString())).thenReturn(Optional.empty());
    }

    private static MediaFile stored(String key) {
        MediaFile f = new MediaFile();
        f.setStorageKey(key);
        f.setKind(MediaKind.IMAGE);
        return f;
    }

    @Test
    @DisplayName("miniatura z i.ytimg.com trafia do biblioteki, a wpis dostaje lokalny adres")
    void fetchesOnceAndRewrites() {
        when(fetcher.fetch(ID)).thenReturn(Optional.of(JPEG));
        when(media.store(any(), eq("youtube-" + ID + ".jpg"), eq("image/jpeg"), eq(""))).thenReturn(stored("2026/11/abc.jpg"));

        String html = thumbnails.localize(FACADE);

        assertThat(html).contains("src=\"/media/2026/11/abc.jpg\"", "data-id=\"" + ID + "\"").doesNotContain("ytimg");
    }

    @Test
    @DisplayName("miniatura już w bibliotece: bez ponownego pobierania")
    void reusesLibrary() {
        when(library.findFirstByOriginalName("youtube-" + ID + ".jpg")).thenReturn(Optional.of(stored("2026/10/old.jpg")));

        assertThat(thumbnails.localize(FACADE)).contains("src=\"/media/2026/10/old.jpg\"");
        verify(fetcher, never()).fetch(anyString());
    }

    @Test
    @DisplayName("nieudane pobranie: obraz znika, fasada zostaje, adres Google nie zostaje")
    void dropsImageWhenFetchFails() {
        when(fetcher.fetch(ID)).thenReturn(Optional.empty());

        String html = thumbnails.localize(FACADE);

        assertThat(html).doesNotContain("ytimg", "<img").contains("yt-facade", "class=\"play\"");
    }

    @Test
    @DisplayName("adres spoza wzorca miniatury nie wywołuje pobierania i nie zostaje w treści")
    void rejectsUnexpectedSource() {
        String html = thumbnails.localize("<p><img src=\"https://i.ytimg.com/vi/../../evil/x.jpg\" alt=\"x\"></p>");

        assertThat(html).doesNotContain("ytimg", "<img");
        verify(fetcher, never()).fetch(anyString());
    }

    @Test
    @DisplayName("treść bez miniatur YouTube wraca bez zmian")
    void leavesOtherContentAlone() {
        String html = "<p>Tekst <img src=\"/media/2026/11/x.jpg\" alt=\"x\"></p>";

        assertThat(thumbnails.localize(html)).isSameAs(html);
    }

    @Test
    @DisplayName("pobieranie przyjmuje tylko mały plik z sygnaturą JPEG")
    void fetcherAcceptsOnlySmallJpeg() {
        assertThat(HttpThumbnailFetcher.jpeg(JPEG)).isPresent();
        assertThat(HttpThumbnailFetcher.jpeg("<html>".getBytes())).isEmpty();
        byte[] big = new byte[HttpThumbnailFetcher.MAX_BYTES + 1];
        big[0] = (byte) 0xFF;
        big[1] = (byte) 0xD8;
        big[2] = (byte) 0xFF;
        assertThat(HttpThumbnailFetcher.jpeg(big)).isEmpty();
    }

    @Test
    @DisplayName("wariant wszystko albo nic: przy nieudanym pobraniu treść zostaje nietknięta")
    void allOrNothingKeepsContentOnFailure() {
        when(fetcher.fetch(ID)).thenReturn(Optional.empty());

        assertThat(thumbnails.localizeAllOrNothing(FACADE)).isEmpty();
    }

    @Test
    @DisplayName("miniatura z parametrem w adresie też trafia na nasz serwer")
    void acceptsQueryString() {
        when(fetcher.fetch(ID)).thenReturn(Optional.of(JPEG));
        when(media.store(any(), eq("youtube-" + ID + ".jpg"), eq("image/jpeg"), eq(""))).thenReturn(stored("2026/11/q.jpg"));

        String html = thumbnails.localize("<img src=\"https://i.ytimg.com/vi/" + ID + "/hq720.jpg?sqp=abc&amp;rs=x\" alt=\"\">");

        assertThat(html).contains("/media/2026/11/q.jpg").doesNotContain("ytimg");
    }

    @Test
    @DisplayName("adres i.ytimg.com tylko w tekście: treść wraca ta sama, bez ponownej serializacji")
    void textOnlyMentionIsUnchanged() {
        String html = "<p>Zobacz https://i.ytimg.com/vi/ w tekście</p>";

        assertThat(thumbnails.localizeAllOrNothing(html)).containsSame(html);
    }
}
