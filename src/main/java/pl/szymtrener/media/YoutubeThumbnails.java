package pl.szymtrener.media;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Miniatury filmow YouTube z naszego serwera zamiast z i.ytimg.com: odslona wpisu nie wysyla adresu IP
 * odwiedzajacego do Google (polityka prywatnosci: dane nie wychodza poza EOG). Film laduje sie dopiero
 * po kliknieciu fasady. Miniatura trafia do biblioteki mediow jako {@code youtube-<id>.jpg}, raz na film.
 */
@Service
public class YoutubeThumbnails {

    public static final String SOURCE_PREFIX = "https://i.ytimg.com/vi/";
    private static final Pattern SOURCE = Pattern.compile("^https://i\\.ytimg\\.com/vi/([A-Za-z0-9_-]{11})/[A-Za-z0-9_]+\\.jpg(\\?[^\\s\"]*)?$");

    private final MediaService media;
    private final MediaRepository library;
    private final ThumbnailFetcher fetcher;

    public YoutubeThumbnails(MediaService media, MediaRepository library, ThumbnailFetcher fetcher) {
        this.media = media;
        this.library = library;
        this.fetcher = fetcher;
    }

    /**
     * Kazdy obraz z i.ytimg.com dostaje lokalny adres. Gdy miniatury nie da sie pobrac (albo adres nie jest
     * zwykla miniatura), obraz znika: fasada z przyciskiem odtwarzania dziala dalej, a adres Google nie zostaje.
     */
    public String localize(String html) {
        if (html == null || !html.contains(SOURCE_PREFIX)) return html;
        return rewrite(html, true).orElseThrow();
    }

    /**
     * Wariant dla przepisywania starych wpisow: gdy choc jednej miniatury nie da sie pobrac, tresc zostaje
     * bez zmian (pusto), zeby chwilowy brak sieci przy starcie nie skasowal obrazow na stale.
     */
    public Optional<String> localizeAllOrNothing(String html) {
        if (html == null || !html.contains(SOURCE_PREFIX)) return Optional.ofNullable(html);
        return rewrite(html, false);
    }

    /** Bez obrazow z i.ytimg.com (adres tylko w tekscie albo linku) tresc wraca ta sama, bez ponownej serializacji. */
    private Optional<String> rewrite(String html, boolean dropFailed) {
        Document doc = Jsoup.parseBodyFragment(html);
        doc.outputSettings().prettyPrint(false);
        var images = doc.select("img[src^=" + SOURCE_PREFIX + "]");
        if (images.isEmpty()) return Optional.of(html);
        for (Element img : images) {
            Matcher m = SOURCE.matcher(img.attr("src"));
            if (!m.matches()) {
                img.remove(); // nie miniatura filmu: nie pobieramy, a adres Google nie moze zostac
                continue;
            }
            Optional<String> local = localUrl(m.group(1));
            if (local.isPresent()) img.attr("src", local.get());
            else if (dropFailed) img.remove();
            else return Optional.empty();
        }
        return Optional.of(doc.body().html());
    }

    private Optional<String> localUrl(String videoId) {
        String name = "youtube-" + videoId + ".jpg";
        Optional<MediaFile> known = library.findFirstByOriginalName(name);
        if (known.isPresent()) return Optional.of(known.get().publicUrl());
        return fetcher.fetch(videoId).map(bytes -> media.store(bytes, name, "image/jpeg", "").publicUrl());
    }
}
