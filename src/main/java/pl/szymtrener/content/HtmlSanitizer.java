package pl.szymtrener.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Kazdy HTML z edytora i z importu DOCX przechodzi tedy, zanim trafi do bazy.
 * Biala lista celowo pokrywa sie z tym, co potrafi wyrenderowac blog.css —
 * nic wiecej nie ma prawa wejsc do tresci.
 */
@Component
public class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.none()
            .addTags("h2", "h3", "h4", "p", "br", "hr", "strong", "em", "u", "s",
                     "ul", "ol", "li", "blockquote", "a", "img", "figure", "figcaption",
                     "table", "caption", "thead", "tbody", "tr", "th", "td",
                     "code", "pre", "sup", "sub", "cite", "div", "span", "time")
            // svg tylko dla ikon z blokow edytora (strzalka odtwarzania, ikona PDF).
            // Biala lista jest wylacznie pozytywna, wiec on*, <script> i inne
            // atrybuty i tak nie przechodza — test EditorBlotSanitizationTest to pilnuje.
            .addTags("svg", "path")
            .addAttributes("a", "href", "title", "rel", "target", "download", "class")
            .addAttributes("svg", "viewBox", "width", "height", "fill", "stroke",
                           "stroke-width", "stroke-linecap", "stroke-linejoin", "aria-hidden", "class")
            .addAttributes("path", "d", "fill", "stroke",
                           "stroke-width", "stroke-linecap", "stroke-linejoin")
            .addAttributes("span", "aria-hidden")
            .addAttributes("img", "src", "alt", "width", "height", "loading", "decoding")
            .addAttributes("th", "scope", "colspan", "rowspan")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("time", "datetime")
            .addAttributes("blockquote", "cite")
            .addAttributes("div", "class", "data-id", "data-media-id", "data-caption",
                           "data-name", "data-meta", "role", "tabindex", "aria-label")
            .addAttributes("span", "class")
            .addAttributes("figure", "class")
            .addProtocols("a", "href", "http", "https", "mailto", "tel", "#")
            .addProtocols("img", "src", "http", "https")
            .preserveRelativeLinks(true);

    /**
     * Ozdoby samego edytora: znacznik altu i przyciski edycji bloku. Edytor wycina
     * je przy zapisie, ale bialalista ich nie zatrzyma (span z klasa przechodzi),
     * wiec zapomniana sciezka zapisu wstawilaby autorowi „✎✕" w srodek artykulu.
     * Usuwamy je razem z zawartoscia, zanim cokolwiek innego sie wydarzy.
     */
    private static final String EDITOR_CHROME = "[data-chrome], .q-acts, .q-alt";

    public String clean(String html) {
        if (html == null || html.isBlank()) return "";
        Document.OutputSettings out = new Document.OutputSettings()
                .prettyPrint(false)
                .charset("UTF-8");

        // outputSettings TU, nie tylko nizej: domyslny prettyPrint wstawilby
        // do zapisywanej tresci wciecia i lamania linii wewnatrz elementow.
        Document incoming = Jsoup.parseBodyFragment(html);
        incoming.outputSettings(out);
        incoming.select(EDITOR_CHROME).remove();

        String cleaned = Jsoup.clean(incoming.body().html(), "https://szymtrener.pl/", SAFELIST, out);
        // linki zewnetrzne: bezpieczny rel, ale bez nofollow —
        // cytowanie zrodel jest punktowane, nie chcemy go oslabiac
        Document doc = Jsoup.parseBodyFragment(cleaned);
        doc.outputSettings(out);
        doc.select("a[href^=http]").forEach(a -> {
            if (!a.attr("href").contains("szymtrener.pl")) {
                a.attr("rel", "noopener");
                a.attr("target", "_blank");
            }
        });
        doc.select("img").forEach(img -> {
            if (!img.hasAttr("loading")) img.attr("loading", "lazy");
            if (!img.hasAttr("decoding")) img.attr("decoding", "async");
        });
        return doc.body().html();
    }
}
