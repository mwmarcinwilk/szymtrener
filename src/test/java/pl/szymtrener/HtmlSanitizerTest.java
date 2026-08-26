package pl.szymtrener;

import org.junit.jupiter.api.Test;
import pl.szymtrener.content.HtmlSanitizer;

import static org.junit.jupiter.api.Assertions.*;

class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void usuwaSkryptyIStyleZWklejonejTresci() {
        String brudny = "<p>Tekst</p><script>alert(1)</script><p style=\"color:red\">Drugi</p>";
        String czysty = sanitizer.clean(brudny);
        assertFalse(czysty.contains("script"));
        assertFalse(czysty.contains("style"));
        assertTrue(czysty.contains("Drugi"));
    }

    @Test
    void zachowujeTabeleZNaglowkami() {
        String html = "<table><thead><tr><th scope=\"col\">Wiek</th></tr></thead>"
                    + "<tbody><tr><td>30–50</td></tr></tbody></table>";
        String czysty = sanitizer.clean(html);
        assertTrue(czysty.contains("<th scope=\"col\">"));
        assertTrue(czysty.contains("30–50"));
    }

    @Test
    void zachowujeBlokFilmuZEdytora() {
        String html = "<div class=\"art-video yt-facade\" data-id=\"LcXpFy7s-GQ\"><img src=\"https://i.ytimg.com/vi/x/hqdefault.jpg\"></div>";
        String czysty = sanitizer.clean(html);
        assertTrue(czysty.contains("yt-facade"));
        assertTrue(czysty.contains("data-id"));
    }

    @Test
    void dodajeLazyLoadingObrazkom() {
        String czysty = sanitizer.clean("<p><img src=\"https://szymtrener.pl/media/a.jpg\" alt=\"opis\"></p>");
        assertTrue(czysty.contains("loading=\"lazy\""));
    }
}
