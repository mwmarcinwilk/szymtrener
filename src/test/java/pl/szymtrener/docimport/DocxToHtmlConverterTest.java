package pl.szymtrener.docimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.szymtrener.media.MediaFile;
import pl.szymtrener.media.MediaService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Najbardziej krucha czesc systemu: Word potrafi nazwac ten sam styl na kilka
 * sposobow, a POI zmienia sygnatury miedzy wersjami. Te testy pilnuja, ze
 * konwerter produkuje wylacznie tagi, ktore blog.css potrafi wyrenderowac.
 */
class DocxToHtmlConverterTest {

    private DocxToHtmlConverter converter;

    @BeforeEach
    void setUp() {
        MediaService media = mock(MediaService.class);
        MediaFile stored = mock(MediaFile.class);
        when(stored.publicUrl()).thenReturn("/media/2026/08/obrazek.png");
        when(media.store(any(), anyString(), anyString(), any())).thenReturn(stored);
        converter = new DocxToHtmlConverter(media);
    }

    @Test
    @DisplayName("nagłówki z angielskiego Worda schodzą o poziom niżej — H1 zostaje tytułem wpisu")
    void mapsEnglishHeadingsOneLevelDown() throws Exception {
        String html = convert(DocxFixtures.englishWord());

        assertThat(html).contains("<h2>Tytuł rozdziału</h2>");
        assertThat(html).contains("<h3>Podrozdział</h3>");
        assertThat(html).doesNotContain("<h1>");
    }

    @Test
    @DisplayName("nagłówki z polskiego Worda („Nagwek1\") są rozpoznawane tak samo")
    void mapsPolishHeadings() throws Exception {
        String html = convert(DocxFixtures.polishWord());

        assertThat(html).contains("<h2>Polski nagłówek pierwszego poziomu</h2>");
        assertThat(html).contains("<h3>Polski nagłówek drugiego poziomu</h3>");
    }

    @Test
    @DisplayName("formatowanie znakowe przechodzi na semantyczne tagi")
    void mapsCharacterFormatting() throws Exception {
        String html = convert(DocxFixtures.englishWord());

        assertThat(html).contains("<strong>pogrubione</strong>");
        assertThat(html).contains("<em>kursywa</em>");
        assertThat(html).contains("<u>podkreślone</u>");
        assertThat(html).contains("<s>przekreślone</s>");
    }

    @Test
    @DisplayName("listy, tabela i cytat trafiają na właściwe tagi, a listy zamykają się poprawnie")
    void mapsListsTableAndQuote() throws Exception {
        String html = convert(DocxFixtures.listsTableAndQuote());

        assertThat(html).contains("<li>Pierwszy punkt</li>");
        assertThat(html).contains("<li>Krok pierwszy</li>");
        assertThat(html).contains("<blockquote>Zdanie zacytowane.</blockquote>");
        assertThat(html).contains("<th scope=\"col\">Masa ciała</th>");
        assertThat(html).contains("<td>120 g</td>");

        // każdy otwarty znacznik listy musi się domknąć
        assertThat(count(html, "<ul>")).isEqualTo(count(html, "</ul>"));
        assertThat(count(html, "<ol>")).isEqualTo(count(html, "</ol>"));
        assertThat(count(html, "<ul>") + count(html, "<ol>")).isEqualTo(2);

        // Rozróżnienie punktowana/numerowana opiera się na getNumFmt() — metodzie,
        // której sygnatura zmieniała się między wersjami POI. Test pilnuje, że działa.
        assertThat(html).contains("<ul>");
        assertThat(html).contains("<ol>");
    }

    @Test
    @DisplayName("styl Worda nie przecieka do HTML-a — o wyglądzie decyduje blog.css")
    void dropsWordStyling() throws Exception {
        String html = convert(DocxFixtures.englishWord()) + convert(DocxFixtures.listsTableAndQuote());

        assertThat(html).doesNotContain("style=");
        assertThat(html).doesNotContain("<font");
        assertThat(html).doesNotContain("class=");
    }

    @Test
    @DisplayName("znaki groźne dla HTML-a są escapowane")
    void escapesHtmlUnsafeText() throws Exception {
        String html = convert(DocxFixtures.htmlUnsafeText());

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("A &amp; B &lt; C &gt; D");
    }

    @Test
    @DisplayName("pusty dokument daje pusty wynik zamiast wyjątku")
    void handlesEmptyDocument() throws Exception {
        ImportResult result = converter.convert(DocxFixtures.bytes(DocxFixtures.empty()), "pusty.docx");

        assertThat(result.html()).isBlank();
        assertThat(result.imageCount()).isZero();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    @DisplayName("lista numerowana bez jawnego w:ilvl też trafia na <ol>")
    void readsNumberFormatFromNumberingXml() throws Exception {
        String html = convert(DocxFixtures.orderedListWithoutIlvl());

        assertThat(html).contains("<ol>");
        assertThat(html).contains("<li>Krok bez jawnego poziomu</li>");
        assertThat(html).doesNotContain("<ul>");
    }

    @Test
    @DisplayName("lista bez definicji numeracji nie wywala importu — wpada na listę punktowaną")
    void survivesMissingNumberingDefinition() {
        assertThatCode(() -> {
            String html = convert(DocxFixtures.listWithoutNumberingDefinition());
            assertThat(html).contains("<li>Punkt bez definicji numeracji</li>");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nie generuje pustych akapitów z pustych linii Worda")
    void skipsEmptyParagraphs() throws Exception {
        String html = convert(DocxFixtures.englishWord());

        assertThat(html).doesNotContain("<p></p>");
        assertThat(html).doesNotContain("<li></li>");
    }

    private String convert(org.apache.poi.xwpf.usermodel.XWPFDocument doc) throws Exception {
        return converter.convert(DocxFixtures.bytes(doc), "test.docx").html();
    }

    private static int count(String haystack, String needle) {
        return (haystack.length() - haystack.replace(needle, "").length()) / needle.length();
    }
}
