package pl.szymtrener.seo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ocena widocznosci wpisu — serwerowy odpowiednik funkcji {@code score()}
 * z {@code admin-editor.js}.
 *
 * Warunki i ich kolejnosc sa przepisane z makiety jeden do jednego. To celowe:
 * autor widzi te sama liste w edytorze na zywo i w ostrzezeniu przy zapisie,
 * a dwie rozne listy podwazylyby zaufanie do obu.
 *
 * Liczymy na HTML-u W POSTACI EDYTORA (klasy `q-*`), bo taki dostajemy z panelu
 * przy ocenie na zywo. Metoda {@link #ofPublication} przelicza to samo dla tresci
 * juz zapisanej.
 */
@Service
public class SeoScoreService {

    private static final int TITLE_MIN = 30, TITLE_MAX = 65;
    private static final int DESC_MIN = 120, DESC_MAX = 160;
    private static final int WORDS_MIN = 600;
    private static final int LINKS_MIN = 2;

    /** Pojedynczy warunek listy kontrolnej. */
    public record Check(boolean ok, String text) {}

    /**
     * @param score   procent spelnionych warunkow — ten sam, ktory rysuje pierscien
     * @param label   etykieta slowna z makiety
     * @param hint    podsumowanie pod etykieta
     * @param checks  lista kontrolna w kolejnosci z makiety
     */
    public record Result(int score, String label, String hint, List<Check> checks) {

        /** Warunki niespelnione — to one trafiaja do ostrzezenia przed publikacja. */
        public List<Check> pending() {
            return checks.stream().filter(c -> !c.ok()).toList();
        }
    }

    /**
     * @param contentHtml tresc w postaci edytora (z klasami `q-*`)
     * @param seoTitle    tytul SEO; pusty liczy sie jako niespelniony warunek
     * @param seoDesc     opis meta
     * @param coverAlt    alt zdjecia glownego
     */
    public Result evaluate(String contentHtml, String seoTitle, String seoDesc, String coverAlt) {
        String html = contentHtml == null ? "" : contentHtml;
        Document doc = Jsoup.parseBodyFragment(html);
        String text = doc.text();
        int words = text.isBlank() ? 0 : text.trim().split("\\s+").length;

        String title = trim(seoTitle);
        String description = trim(seoDesc);

        List<Check> checks = List.of(
                new Check(title.length() >= TITLE_MIN && title.length() <= TITLE_MAX,
                        "Tytuł SEO ma 30–65 znaków"),
                new Check(description.length() >= DESC_MIN && description.length() <= DESC_MAX,
                        "Meta opis ma 120–160 znaków"),
                new Check(!doc.select("h2").isEmpty(),
                        "Treść zawiera nagłówki H2"),
                new Check(hasImage(doc),
                        "Wstawione zdjęcie z tekstem alternatywnym"),
                new Check(allImagesHaveAlt(doc),
                        "Wszystkie zdjęcia mają alt"),
                new Check(trim(coverAlt).length() > 5,
                        "Zdjęcie główne ma opis alt"),
                new Check(doc.select("a[href]").size() >= LINKS_MIN,
                        "Co najmniej 2 linki wewnętrzne"),
                new Check(words >= WORDS_MIN,
                        "Objętość powyżej 600 słów (" + words + ")"),
                new Check(text.matches("(?s).*\\d.*"),
                        "Dane liczbowe w treści")
        );

        long done = checks.stream().filter(Check::ok).count();
        int score = (int) Math.round(done * 100.0 / checks.size());
        return new Result(score, label(score), hint(done, checks.size()), checks);
    }

    /**
     * To samo dla tresci juz zamienionej na postac publikacji — tam zdjecia siedza
     * w zwyklym `figure`, a nie w `figure.q-fig`.
     */
    public Result ofPublication(String publicationHtml, String seoTitle, String seoDesc, String coverAlt) {
        return evaluate(publicationHtml, seoTitle, seoDesc, coverAlt);
    }

    /** Zdjecie w tresci: `figure.q-fig` w edytorze albo zwykly `figure` po zapisie. */
    private static boolean hasImage(Document doc) {
        return !doc.select("figure.q-fig, figure img").isEmpty();
    }

    /**
     * Miniatura filmu ma alt pusty celowo (opis niesie aria-label bloku), wiec
     * nie liczy sie do tego warunku — inaczej kazdy wpis z filmem bylby wadliwy.
     */
    private static boolean allImagesHaveAlt(Document doc) {
        return doc.select("img").stream()
                .filter(img -> img.closest(".yt-facade") == null && img.closest(".q-vid") == null)
                .allMatch(img -> !img.attr("alt").isBlank());
    }

    private static String label(int score) {
        if (score >= 85) return "Dobra widoczność";
        if (score >= 60) return "Wymaga dopracowania";
        return "Słaba widoczność";
    }

    private static String hint(long done, int total) {
        return done == total ? "Wszystkie warunki spełnione."
                             : "Do uzupełnienia: " + (total - done) + " z " + total + ".";
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
