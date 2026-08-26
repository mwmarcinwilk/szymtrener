package pl.szymtrener.content;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Kontrakt ze zlecenia (CLAUDE.md, sekcja 8): w edytorze bloki mediow nosza klasy
 * `q-*`, a w tresci publicznej NIE MA prawa zostac zadna z nich ani przyciski
 * `.q-acts`. Wpis napisany w panelu ma wygladac jak artykul wzorcowy, wiec
 * zamiana musi produkowac dokladnie klasy z `blog.css`.
 */
class EditorHtmlTest {

    private final EditorHtml editor = new EditorHtml();
    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    /** Dokladnie to, co trzyma Quill po wstawieniu bloku przez `admin-editor.js`. */
    private static final String FIGURE = """
            <figure class="q-fig" data-src="/media/2026/08/foto.jpg" data-alt="Trening siłowy"\
             data-caption="Podpis pod zdjęciem" contenteditable="false">\
            <img src="/media/2026/08/foto.jpg" alt="Trening siłowy">\
            <figcaption>Podpis pod zdjęciem</figcaption>\
            <span class="q-alt">alt: Trening siłowy</span>\
            <span class="q-acts" contenteditable="false">\
            <button type="button" class="q-act" data-act="edit">✎</button>\
            <button type="button" class="q-act" data-act="del">✕</button></span></figure>""";

    private static final String VIDEO = """
            <div class="q-vid" data-id="LcXpFy7s-GQ" data-caption="Jak budować mięśnie" contenteditable="false">\
            <span class="q-vid-thumb"><img src="https://i.ytimg.com/vi/LcXpFy7s-GQ/hqdefault.jpg" alt="">\
            <span class="q-play">▶</span></span>\
            <span class="q-vid-tx"><b>Film: Jak budować mięśnie</b><i>Osadzenie bez cookies</i></span>\
            <span class="q-acts"><button class="q-act" data-act="del">✕</button></span></div>""";

    private static final String PDF = """
            <div class="q-pdf" data-name="plan.pdf" data-label="Plan startowy" data-meta="410 KB"\
             data-url="/pliki/12/plan.pdf" data-media-id="12" contenteditable="false">\
            <span class="q-pdf-ic">PDF</span>\
            <span class="q-pdf-tx"><b>Plan startowy</b><i>plan.pdf · 410 KB</i></span>\
            <span class="q-acts"><button class="q-act" data-act="del">✕</button></span></div>""";

    /** Pelna sciezka zapisu: zamiana, potem biala lista. */
    private String publish(String editorHtml) {
        return sanitizer.clean(editor.toPublication(editorHtml));
    }

    @Nested
    @DisplayName("klasy edytora nie mogą wyjść na blog")
    class NoEditorClasses {

        @Test
        @DisplayName("po zamianie nie ma żadnej klasy q-* ani przycisków bloku")
        void editorClassesAreGone() {
            String clean = publish(FIGURE + VIDEO + PDF);

            assertThat(clean).doesNotContain("q-fig");
            assertThat(clean).doesNotContain("q-vid");
            assertThat(clean).doesNotContain("q-pdf");
            assertThat(clean).doesNotContain("q-acts");
            assertThat(clean).doesNotContain("q-alt");
            assertThat(clean).doesNotContain("contenteditable");
            // tekst ozdób też nie może zostać
            assertThat(clean).doesNotContain("✎");
            assertThat(clean).doesNotContain("alt: Trening siłowy");
        }
    }

    @Nested
    @DisplayName("blok zamienia się w HTML, który rozumie blog.css")
    class PublicationHtml {

        @Test
        @DisplayName("zdjęcie → czysty figure z altem, podpisem i leniwym ładowaniem")
        void figure() {
            String clean = publish(FIGURE);

            assertThat(clean).contains("<figure>");
            assertThat(clean).contains("src=\"/media/2026/08/foto.jpg\"");
            assertThat(clean).contains("alt=\"Trening siłowy\"");
            assertThat(clean).contains("<figcaption>Podpis pod zdjęciem</figcaption>");
            assertThat(clean).contains("loading=\"lazy\"");
            assertThat(clean).contains("decoding=\"async\"");
        }

        @Test
        @DisplayName("film → miniatura art-video yt-facade z data-id")
        void video() {
            String clean = publish(VIDEO);

            assertThat(clean).contains("art-video");
            assertThat(clean).contains("yt-facade");
            assertThat(clean).contains("data-id=\"LcXpFy7s-GQ\"");
            assertThat(clean).contains("class=\"play\"");
            assertThat(clean).contains("i.ytimg.com/vi/LcXpFy7s-GQ/hqdefault.jpg");
        }

        @Test
        @DisplayName("PDF → karta art-pdf z linkiem do pliku")
        void pdf() {
            String clean = publish(PDF);

            assertThat(clean).contains("class=\"art-pdf\"");
            assertThat(clean).contains("href=\"/pliki/12/plan.pdf\"");
            assertThat(clean).contains("Plan startowy");
            assertThat(clean).contains("410 KB");
        }

        @Test
        @DisplayName("zwykła treść przechodzi nietknięta")
        void plainContentSurvives() {
            String clean = publish("<h2>Nagłówek</h2><p>Akapit z <strong>wyróżnieniem</strong>.</p><ul><li>punkt</li></ul>");

            assertThat(clean).contains("<h2>Nagłówek</h2>");
            assertThat(clean).contains("<strong>wyróżnieniem</strong>");
            assertThat(clean).contains("<li>punkt</li>");
        }
    }

    @Nested
    @DisplayName("obieg edytor → baza → edytor niczego nie gubi")
    class RoundTrip {

        /** Dokladnie to, co dzieje sie przy wejsciu w edycje zapisanego wpisu. */
        private String reopen(String editorHtml) {
            return editor.toEditor(publish(editorHtml));
        }

        @Test
        @DisplayName("zdjęcie wraca ze źródłem, altem i podpisem")
        void figureSurvivesRoundTrip() {
            String back = reopen(FIGURE);

            assertThat(back).contains("class=\"q-fig\"");
            assertThat(back).contains("data-src=\"/media/2026/08/foto.jpg\"");
            assertThat(back).contains("data-alt=\"Trening siłowy\"");
            assertThat(back).contains("data-caption=\"Podpis pod zdjęciem\"");
        }

        @Test
        @DisplayName("film wraca z identyfikatorem i podpisem, nie znika")
        void videoSurvivesRoundTrip() {
            String back = reopen(VIDEO);

            assertThat(back).contains("class=\"q-vid\"");
            assertThat(back).contains("data-id=\"LcXpFy7s-GQ\"");
            assertThat(back).contains("data-caption=\"Jak budować mięśnie\"");
        }

        @Test
        @DisplayName("PDF wraca z adresem, nazwą i rozmiarem, nie znika")
        void pdfSurvivesRoundTrip() {
            String back = reopen(PDF);

            assertThat(back).contains("class=\"q-pdf\"");
            assertThat(back).contains("data-url=\"/pliki/12/plan.pdf\"");
            assertThat(back).contains("data-media-id=\"12\"");
            assertThat(back).contains("data-name=\"plan.pdf\"");
            assertThat(back).contains("410 KB");
        }

        @Test
        @DisplayName("drugi zapis po edycji daje ten sam HTML publikacji — obieg jest stabilny")
        void secondSaveIsIdempotent() {
            String firstSave = publish(FIGURE + VIDEO + PDF);
            String secondSave = publish(editor.toEditor(firstSave));

            assertThat(secondSave).isEqualTo(firstSave);
        }
    }

    @Nested
    @DisplayName("zdjęcie bez altu blokuje zapis")
    class AltRequired {

        @Test
        @DisplayName("brak altu kończy się komunikatem dla autora")
        void missingAltIsRejected() {
            String withoutAlt = FIGURE.replace("data-alt=\"Trening siłowy\"", "data-alt=\"\"")
                                      .replace("alt=\"Trening siłowy\"", "alt=\"\"");

            assertThatThrownBy(() -> editor.requireImageAlts(publish(withoutAlt)))
                    .isInstanceOf(EditorHtml.InvalidContentException.class)
                    .hasMessage("Uzupełnij tekst alternatywny zdjęcia");
        }

        @Test
        @DisplayName("miniatura filmu ma pusty alt celowo i nie blokuje zapisu")
        void videoThumbnailIsExempt() {
            assertThatCode(() -> editor.requireImageAlts(publish(VIDEO))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("komplet altów przechodzi")
        void completeAltsPass() {
            assertThatCode(() -> editor.requireImageAlts(publish(FIGURE))).doesNotThrowAnyException();
        }
    }
}
