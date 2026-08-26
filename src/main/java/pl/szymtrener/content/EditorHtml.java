package pl.szymtrener.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Zamiana reprezentacji edytora na HTML publikacji.
 *
 * W edytorze bloki mediow nosza klasy `q-fig` / `q-vid` / `q-pdf` wraz z ozdobami
 * panelu (znacznik altu, przyciski bloku) — tak wygladaja w makiecie i tak dziala
 * `admin-editor.js`. Do bazy i na blog idzie inna postac: klasy z `blog.css`
 * (`art-video`, `yt-facade`, `art-pdf`, zwykly `figure`), zeby wpis napisany
 * w panelu wygladal identycznie jak artykul wzorcowy.
 *
 * Ta klasa jest jedynym miejscem, w ktorym ta zamiana zachodzi. W tresci publicznej
 * nie ma prawa zostac zadna klasa `q-*` ani przycisk `.q-acts` — pilnuje tego
 * {@code EditorBlotSanitizationTest}.
 */
@Component
public class EditorHtml {

    /** Rzucany, gdy tresc nie nadaje sie do publikacji — komunikat idzie wprost do autora. */
    public static class InvalidContentException extends RuntimeException {
        public InvalidContentException(String message) { super(message); }
    }

    /**
     * @param html surowy HTML z Quilla
     * @return HTML w postaci publikacyjnej, jeszcze przed bialalista sanitizera
     */
    public String toPublication(String html) {
        if (html == null || html.isBlank()) return "";

        Document doc = Jsoup.parseBodyFragment(html);
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false).charset("UTF-8"));

        // najpierw ozdoby panelu — inaczej ich tekst („alt: …", „✎✕") wsiakalby w tresc
        doc.select(".q-acts, .q-alt, [data-chrome]").remove();

        doc.select("figure.q-fig").forEach(EditorHtml::figureToPublication);
        doc.select("div.q-vid, .q-vid").forEach(EditorHtml::videoToPublication);
        doc.select("div.q-pdf, .q-pdf").forEach(EditorHtml::pdfToPublication);

        return doc.body().html();
    }

    /** `figure.q-fig` z atrybutami data-* → czysty `<figure>` z obrazkiem i podpisem. */
    private static void figureToPublication(Element figure) {
        String src = firstNonBlank(figure.attr("data-src"), figure.select("img").attr("src"));
        String alt = firstNonBlank(figure.attr("data-alt"), figure.select("img").attr("alt"));
        String caption = firstNonBlank(figure.attr("data-caption"), figure.select("figcaption").text());

        figure.clearAttributes();
        figure.empty();
        Element image = figure.appendElement("img")
                .attr("src", src)
                .attr("alt", alt)
                .attr("loading", "lazy")
                .attr("decoding", "async");
        if (!caption.isBlank()) figure.appendElement("figcaption").text(caption);
        // podpis nie zastepuje altu: czytnik ekranu czyta alt, nie figcaption
        if (alt.isBlank()) image.attr("alt", "");
    }

    /**
     * `div.q-vid` → miniatura z przyciskiem odtwarzania, dokladnie taka, jaka
     * rozumie `main.js` na stronie publicznej (podmienia ja na iframe po kliknieciu).
     */
    private static void videoToPublication(Element block) {
        String id = firstNonBlank(block.attr("data-id"), "");
        String caption = block.attr("data-caption");
        block.clearAttributes();
        block.empty();
        block.attr("class", "art-video yt-facade")
             .attr("data-id", id)
             // podpis zostaje na wrapperze: bez niego powrot do edycji gubilby go bezpowrotnie
             .attr("data-caption", caption)
             .attr("role", "button")
             .attr("tabindex", "0")
             .attr("aria-label", "Odtwórz film");
        block.appendElement("img")
             .attr("src", "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg")
             .attr("alt", "")
             .attr("loading", "lazy");
        Element play = block.appendElement("span").attr("class", "play").attr("aria-hidden", "true");
        play.append("<svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"44\" height=\"44\">"
                  + "<path d=\"M8 5v14l11-7z\"/></svg>");
    }

    /** `div.q-pdf` → karta pobrania z `blog.css`. */
    private static void pdfToPublication(Element block) {
        String url = firstNonBlank(block.attr("data-url"), "#");
        String name = firstNonBlank(block.attr("data-name"), "Plik PDF");
        String label = firstNonBlank(block.attr("data-label"), name);
        String meta = firstNonBlank(block.attr("data-meta"), "");
        String mediaId = block.attr("data-media-id");

        Element replacement = new Element("div").attr("class", "art-pdf-block");
        if (!mediaId.isBlank()) replacement.attr("data-media-id", mediaId);
        // nazwa pliku i rozmiar sa potrzebne przy powrocie do edycji — w samej karcie
        // widac juz tylko etykiete, wiec trzymamy je na wrapperze
        replacement.attr("data-name", name).attr("data-meta", meta);

        Element link = replacement.appendElement("a")
                .attr("class", "art-pdf")
                .attr("href", url)
                .attr("download", "");
        Element icon = link.appendElement("span").attr("class", "ic");
        icon.append("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\""
                  + " stroke-linecap=\"round\" stroke-linejoin=\"round\" width=\"22\" height=\"22\">"
                  + "<path d=\"M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z\"/>"
                  + "<path d=\"M14 2v6h6M16 13H8M16 17H8\"/></svg>");
        Element text = link.appendElement("span");
        text.appendElement("span").attr("class", "nm").text(label);
        text.appendElement("span").attr("class", "mt").text(meta.isBlank() ? "PDF" : "PDF · " + meta);
        link.appendElement("span").attr("class", "go")
            .appendElement("span").attr("class", "btn btn-outline").text("Pobierz");

        block.replaceWith(replacement);
    }

    /**
     * Zamiana powrotna: HTML publikacji → postac edytora.
     *
     * Bez tej metody edycja zapisanego wpisu kasowala media: Quill rozpoznaje bloki
     * wylacznie po klasach `q-*`, wiec `figure` bez klasy tracil zrodlo i alt,
     * a `art-video` i `art-pdf` znikaly z tresci przy wczytaniu.
     */
    public String toEditor(String publicationHtml) {
        if (publicationHtml == null || publicationHtml.isBlank()) return "";

        Document doc = Jsoup.parseBodyFragment(publicationHtml);
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false).charset("UTF-8"));

        doc.select("figure").forEach(EditorHtml::figureToEditor);
        doc.select("div.yt-facade, div.art-video").forEach(EditorHtml::videoToEditor);
        doc.select("div.art-pdf-block").forEach(EditorHtml::pdfToEditor);

        return doc.body().html();
    }

    private static void figureToEditor(Element figure) {
        Element img = figure.selectFirst("img");
        String src = img == null ? "" : img.attr("src");
        String alt = img == null ? "" : img.attr("alt");
        Element caption = figure.selectFirst("figcaption");

        figure.attr("class", "q-fig")
              .attr("data-src", src)
              .attr("data-alt", alt)
              .attr("data-caption", caption == null ? "" : caption.text());
    }

    private static void videoToEditor(Element block) {
        String id = block.attr("data-id");
        String caption = block.attr("data-caption");
        block.clearAttributes();
        block.attr("class", "q-vid").attr("data-id", id).attr("data-caption", caption);
    }

    private static void pdfToEditor(Element block) {
        Element link = block.selectFirst("a");
        Element label = block.selectFirst(".nm");
        String meta = firstNonBlank(block.attr("data-meta"), "");
        String name = firstNonBlank(block.attr("data-name"), label == null ? "" : label.text());
        String mediaId = block.attr("data-media-id");

        block.clearAttributes();
        block.attr("class", "q-pdf")
             .attr("data-url", link == null ? "" : link.attr("href"))
             .attr("data-media-id", mediaId)
             .attr("data-name", name)
             .attr("data-label", label == null ? name : label.text())
             .attr("data-meta", meta);
    }

    /**
     * Zdjecie bez altu nie moze trafic na blog: czytnik ekranu je pomija, a ocena
     * widocznosci liczy je jako brak. Odrzucamy zapis zamiast po cichu publikowac.
     */
    public void requireImageAlts(String publicationHtml) {
        Document doc = Jsoup.parseBodyFragment(publicationHtml == null ? "" : publicationHtml);
        // Miniatura filmu jest ozdobna i ma alt pusty CELOWO — opis niesie aria-label
        // calego bloku. Gdyby wpadla do tej kontroli, zaden wpis z filmem nie dalby sie zapisac.
        boolean missing = doc.select("img").stream()
                .filter(img -> img.closest(".yt-facade") == null)
                .anyMatch(img -> img.attr("alt").isBlank());
        if (missing) {
            throw new InvalidContentException("Uzupełnij tekst alternatywny zdjęcia");
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : (second == null ? "" : second);
    }
}
