package pl.szymtrener.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/** Liczby liczone raz przy zapisie wpisu, a nie przy kazdym wyswietleniu. */
@Component
public class ContentMetrics {

    private static final int WORDS_PER_MINUTE = 200;

    public record Result(int wordCount, int readingMinutes, boolean hasVideo, boolean hasPdf) {}

    public Result analyse(String contentHtml, String lead) {
        Document doc = Jsoup.parseBodyFragment(contentHtml == null ? "" : contentHtml);
        String text = ((lead == null ? "" : lead) + " " + doc.text()).trim();
        int words = text.isBlank() ? 0 : text.split("\\s+").length;
        int minutes = Math.max(1, (int) Math.ceil(words / (double) WORDS_PER_MINUTE));
        boolean video = !doc.select("[data-video-id], .yt-facade").isEmpty();
        boolean pdf = !doc.select("a.art-pdf, a[href*=/pliki/]").isEmpty();
        return new Result(words, minutes, video, pdf);
    }
}
