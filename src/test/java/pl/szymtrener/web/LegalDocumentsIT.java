package pl.szymtrener.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import pl.szymtrener.PostgresTestBase;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dokumenty prawne od 1.11.2026 (SPEC-2026-11-13-dokumenty-prawne-na-szymtrenerpl-regulamin-polit): regulamin i polityka jako HTML z PDF,
 * strona zgodna z Regulaminem Studia (odwolanie 12 h, bez przerwy w pakiecie), formularze bez zgody,
 * czcionki z naszego serwera. Tresc czytana jako UTF-8, inaczej negatywne asercje na polskim tekscie
 * przechodzilyby zawsze.
 */
@AutoConfigureMockMvc
class LegalDocumentsIT extends PostgresTestBase {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private String page(String path) throws Exception {
        String html = mvc.perform(get(path)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("</html>");
        return html;
    }

    @Test
    @DisplayName("regulamin: cała treść z załącznikami, numeracja jak w Wordzie i PDF do pobrania")
    void termsPage() throws Exception {
        Document doc = Jsoup.parse(page("/regulamin"));

        assertThat(doc.select("h2").eachText()).contains("§ 1. Postanowienia ogólne", "§ 11. Postanowienia końcowe");
        assertThat(doc.getElementById("zalacznik-1")).isNotNull();
        assertThat(doc.getElementById("zalacznik-2").text()).contains("Wzór formularza odstąpienia");
        // §5 ust. 5 odwoluje sie do „ust. 4”: czwarty ustep §5 to lista danych reklamacji.
        var complaints = doc.select("h2:contains(§ 5.) + ol.legal-list > li");
        assertThat(complaints.get(3).text()).startsWith("W celu usprawnienia rozpatrzenia reklamacji");
        // §3 ma podtytuly, ale ustepy leca dalej: „Opieka Longevity” zaczyna sie od ust. 27.
        assertThat(doc.select("h3:contains(Opieka Longevity) + ol.legal-list").attr("start")).isEqualTo("27");
        assertThat(doc.select("a[href^=/dokumenty/regulamin][href$=.pdf]")).hasSize(1);
        assertThat(doc.select("link[rel=canonical]").attr("href")).endsWith("/regulamin");
    }

    @Test
    @DisplayName("polityka prywatności: tekst z 11.2026 i zachowany opis cookies tej strony")
    void privacyPage() throws Exception {
        Document doc = Jsoup.parse(page("/polityka-prywatnosci"));
        String text = doc.text();

        assertThat(text).contains("formularza umożliwiającego umówienie rozmowy",
                "nie są przekazywane do państw trzecich", "__Host-st_zgoda", "JSESSIONID");
        assertThat(doc.select("h2:contains(Pliki cookies) + ol.legal-list").attr("start")).isEqualTo("22");
        assertThat(doc.select("a[href^=/dokumenty/polityka-prywatnosci][href$=.pdf]")).hasSize(1);
        assertThat(doc.select("footer a[href=/regulamin]")).hasSize(1);
    }

    @Test
    @DisplayName("stopka strony głównej i ustawień cookies linkuje regulamin, sitemap go zna")
    void termsAreLinked() throws Exception {
        for (String path : List.of("/", "/ustawienia-cookies")) {
            assertThat(Jsoup.parse(page(path)).select("footer a[href=/regulamin]")).as(path).hasSize(1);
        }
        String sitemap = mvc.perform(get("/sitemap.xml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(sitemap).contains("/regulamin</loc>");
    }

    @Test
    @DisplayName("PDF-y dokumentów: adres ze skrótem treści (bez starej wersji z cache), application/pdf, bez linków do localhost")
    void pdfsAreServed() throws Exception {
        for (String name : List.of("regulamin", "polityka-prywatnosci")) {
            String href = Jsoup.parse(page("/" + name)).select("a[download]").attr("href");
            assertThat(href).as(name).matches("/dokumenty/" + name + "-[0-9a-f]+\\.pdf");
            byte[] body = mvc.perform(get(href)).andExpect(status().isOk())
                    .andExpect(content().contentType("application/pdf"))
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(new String(body, 0, 5, StandardCharsets.US_ASCII)).as(href).isEqualTo("%PDF-");
            // Chrome --print-to-pdf rozwiazuje wzgledne linki wzgledem serwera, z ktorego drukowal.
            assertThat(new String(body, StandardCharsets.ISO_8859_1)).as(href).doesNotContain("localhost");
        }
    }

    @Test
    @DisplayName("formularze bez checkboxa zgody, z informacją o administratorze i linkiem do polityki")
    void formsInformInsteadOfAskingForConsent() throws Exception {
        Document doc = Jsoup.parse(page("/"));

        assertThat(doc.select("input[name=consent]")).isEmpty();
        assertThat(doc.select("p.of-consent a[href=/polityka-prywatnosci], p.form-consent a[href=/polityka-prywatnosci]"))
                .hasSize(2);
        assertThat(doc.select("p.of-consent").text()).contains("Administratorem Twoich danych jestem ja, Szymon Domagała");
    }

    @Test
    @DisplayName("zasady odwołań jak w Regulaminie Studia: 12 godzin, bez przerwy w pakiecie (także w FAQ)")
    void rulesFollowStudioTerms() throws Exception {
        String html = page("/");
        Document doc = Jsoup.parse(html);
        String rules = doc.select("div.rules").text();

        assertThat(rules).contains("co najmniej 12 godzin wcześniej", "nie zdążę oddać tej godziny komuś innemu",
                "raty zostają bez zmian");
        // „o czas przerwy” w zasadzie zamrozenia Opieki jest poprawne; zakazana jest przerwa w PAKIECIE.
        assertThat(rules).doesNotContain("24 godzin", "Raz na pakiet");
        assertThat(html).doesNotContain("przerwę do 3 tygodni");
        assertThat(jdbc.queryForObject("select count(*) from app_setting where \"key\" = 'stationary.rules.pause'",
                Integer.class)).isZero();
    }

    @Test
    @DisplayName("odsłona strony głównej i bloga nie woła i.ytimg.com ani youtube: miniatury z naszego serwera")
    void videoThumbnailsAreSelfHosted() throws Exception {
        for (String path : List.of("/", "/blog")) {
            assertThat(page(path)).as(path).doesNotContain("i.ytimg.com", "youtube-nocookie.com");
        }
        var thumbs = Jsoup.parse(page("/")).select("img.yt-thumb");
        assertThat(thumbs).hasSize(2);
        for (var img : thumbs) {
            assertThat(img.attr("src")).startsWith("/images/yt-");
            mvc.perform(get(img.attr("src"))).andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"));
        }
    }

    @Test
    @DisplayName("czcionki z naszego serwera: żadna strona publiczna nie woła Google Fonts")
    void fontsAreSelfHosted() throws Exception {
        String fontsHref = null;
        for (String path : List.of("/", "/regulamin", "/polityka-prywatnosci", "/ustawienia-cookies", "/blog")) {
            String html = page(path);
            assertThat(html).as(path).doesNotContain("fonts.googleapis.com", "fonts.gstatic.com");
            // Thymeleaf dokleja wersje z lancucha zasobow (/css/fonts-<hash>.css), wiec adres bierzemy ze strony.
            fontsHref = Jsoup.parse(html).select("link[rel=stylesheet][href*=/css/fonts]").attr("href");
            assertThat(fontsHref).as(path).startsWith("/css/fonts");
        }
        String css = mvc.perform(get(fontsHref)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        for (String font : css.split("url\\(")) {
            if (!font.startsWith("/fonts/")) continue;
            String file = font.substring(0, font.indexOf(')'));
            mvc.perform(get(file)).andExpect(status().isOk());
        }
        assertThat(css).contains("url(/fonts/montserrat-latin-ext-wght-normal.woff2)", "url(/fonts/lora-latin-wght-italic.woff2)");
    }
}
