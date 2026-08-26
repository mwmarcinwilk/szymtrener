package pl.szymtrener.content;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bloki edytora (film, PDF, tabela) przechodza przez sanitizer w drodze do bazy.
 * Musza z niego wyjsc w stanie, w ktorym:
 *   1. Quill rozpozna je przy ponownym otwarciu wpisu — po klasie blotu,
 *   2. blog.css potrafi je ostylowac na stronie publicznej.
 * Utrata ktoregokolwiek konczy sie „blok zniknal po zapisie".
 */
class EditorBlotSanitizationTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    /** Dokladnie to, co produkuje YouTubeBlot.create() w editor.js. */
    private static final String VIDEO = """
            <div class="art-video yt-facade" data-id="dQw4w9WgXcQ" role="button" tabindex="0"\
             aria-label="Odtwórz film" contenteditable="false">\
            <img src="https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg" alt="" loading="lazy">\
            <span class="play" aria-hidden="true"><svg viewBox="0 0 24 24" fill="currentColor" width="44" height="44">\
            <path d="M8 5v14l11-7z"/></svg></span></div>""";

    /** Dokladnie to, co produkuje PdfBlot.create(). */
    private static final String PDF = """
            <div class="art-pdf-block" data-media-id="12" contenteditable="false">\
            <a class="art-pdf" href="/pliki/12/plan.pdf" download>\
            <span class="ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="22" height="22">\
            <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/></svg></span>\
            <span><span class="nm">plan.pdf</span><span class="mt">PDF · 240 KB</span></span>\
            <span class="go"><span class="btn btn-outline">Pobierz</span></span></a></div>""";

    /** Dokladnie to, co produkuje TableBlot.create(). */
    private static final String TABLE = """
            <div class="q-table" contenteditable="false"><table><caption>Zapotrzebowanie</caption>\
            <thead><tr><th scope="col">Masa</th><th scope="col">Białko</th></tr></thead>\
            <tbody><tr><td>75 kg</td><td>120 g</td></tr></tbody></table></div>""";

    @Nested
    @DisplayName("Quill musi rozpoznać blok po zapisie")
    class BlotRecognition {

        @Test
        @DisplayName("film: klasa yt-facade i data-id przetrwają")
        void video() {
            String clean = sanitizer.clean(VIDEO);

            assertThat(clean).contains("yt-facade");
            assertThat(clean).contains("data-id=\"dQw4w9WgXcQ\"");
        }

        @Test
        @DisplayName("PDF: klasa art-pdf-block i data-media-id przetrwają")
        void pdf() {
            String clean = sanitizer.clean(PDF);

            assertThat(clean).contains("art-pdf-block");
            assertThat(clean).contains("data-media-id=\"12\"");
            // PdfBlot.value() odczytuje nazwę i rozmiar z tych klas
            assertThat(clean).contains("class=\"nm\"");
            assertThat(clean).contains("class=\"mt\"");
            assertThat(clean).contains("href=\"/pliki/12/plan.pdf\"");
        }

        @Test
        @DisplayName("tabela: klasa q-table i cała zawartość tabeli przetrwają")
        void table() {
            String clean = sanitizer.clean(TABLE);

            assertThat(clean).contains("q-table");
            assertThat(clean).contains("<caption>Zapotrzebowanie</caption>");
            assertThat(clean).contains("<th scope=\"col\">Masa</th>");
            assertThat(clean).contains("<td>120 g</td>");
        }
    }

    @Nested
    @DisplayName("blog.css musi mieć się czego złapać na stronie publicznej")
    class PublicStyling {

        @Test
        @DisplayName("film zachowuje klasę art-video i ikonę odtwarzania")
        void videoKeepsItsLook() {
            String clean = sanitizer.clean(VIDEO);

            assertThat(clean).contains("art-video");
            assertThat(clean).contains("class=\"play\"");
            assertThat(clean).contains("<svg");          // .art-video .play svg
            assertThat(clean).contains("<path");
        }

        @Test
        @DisplayName("blok PDF zachowuje klasę art-pdf na linku i ikonę")
        void pdfKeepsItsLook() {
            String clean = sanitizer.clean(PDF);

            assertThat(clean).contains("class=\"art-pdf\"");   // cały układ kafla wisi na tej klasie
            assertThat(clean).contains("class=\"ic\"");
            assertThat(clean).contains("<svg");
        }
    }

    @Nested
    @DisplayName("ozdoby edytora nie mają prawa wyjść na blog")
    class EditorChrome {

        /** Dokladnie to, co edytor trzyma w DOM-ie wokol wstawionego zdjecia. */
        private static final String FIGURE_WITH_CHROME = """
                <figure contenteditable="false">\
                <img src="/media/1/foto.jpg" alt="Trening" loading="lazy" decoding="async">\
                <figcaption>Podpis</figcaption>\
                <span class="q-alt" contenteditable="false" data-chrome>alt: Trening</span>\
                <span class="q-acts" contenteditable="false" data-chrome>\
                <button type="button" class="q-act" data-act="edit">✎</button>\
                <button type="button" class="q-act" data-act="del">✕</button></span></figure>""";

        @Test
        @DisplayName("znacznik altu i przyciski bloku znikają razem z treścią")
        void chromeIsRemovedWithItsText() {
            String clean = sanitizer.clean(FIGURE_WITH_CHROME);

            assertThat(clean).doesNotContain("q-alt");
            assertThat(clean).doesNotContain("q-acts");
            assertThat(clean).doesNotContain("q-act");
            // sam <button> i tak nie przeszedlby bialej listy, ale jego TEKST owszem
            assertThat(clean).doesNotContain("✎");
            assertThat(clean).doesNotContain("✕");
            assertThat(clean).doesNotContain("alt: Trening");
        }

        @Test
        @DisplayName("samo zdjęcie z podpisem zostaje nietknięte")
        void figureItselfSurvives() {
            String clean = sanitizer.clean(FIGURE_WITH_CHROME);

            assertThat(clean).contains("<figure");
            assertThat(clean).contains("src=\"/media/1/foto.jpg\"");
            assertThat(clean).contains("alt=\"Trening\"");
            assertThat(clean).contains("<figcaption>Podpis</figcaption>");
        }
    }

    @Nested
    @DisplayName("rozluźnienie białej listy nie może wpuścić skryptów")
    class StillSafe {

        @Test
        @DisplayName("svg nie przemyca skryptu ani obsługi zdarzeń")
        void svgCannotCarryScripts() {
            String attack = """
                    <div class="art-video yt-facade" data-id="x">\
                    <svg onload="alert(1)" viewBox="0 0 24 24"><script>alert(2)</script>\
                    <path d="M0 0" onclick="alert(3)"/></svg></div>""";

            String clean = sanitizer.clean(attack);

            assertThat(clean).doesNotContain("onload");
            assertThat(clean).doesNotContain("onclick");
            assertThat(clean).doesNotContain("alert");
            assertThat(clean).doesNotContain("<script");
        }

        @Test
        @DisplayName("link z klasą nadal nie przepuszcza javascript:")
        void linkClassDoesNotOpenJavascriptProtocol() {
            String attack = "<a class=\"art-pdf\" href=\"javascript:alert(1)\">Pobierz</a>";

            String clean = sanitizer.clean(attack);

            assertThat(clean).doesNotContain("javascript:");
        }

        @Test
        @DisplayName("obcy element osadzany dalej nie przechodzi")
        void iframeIsStillBlocked() {
            String clean = sanitizer.clean("<iframe src=\"https://evil.example\"></iframe><p>ok</p>");

            assertThat(clean).doesNotContain("iframe");
            assertThat(clean).contains("ok");
        }
    }
}
