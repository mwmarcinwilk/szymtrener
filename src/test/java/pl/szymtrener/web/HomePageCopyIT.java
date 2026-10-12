package pl.szymtrener.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import pl.szymtrener.PostgresTestBase;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Strona glowna po cenniku i tekstach z 12.10.2026, na prawdziwej bazie po migracjach.
 * Tresc czytana jako UTF-8: domyslne getContentAsString() czyta ISO-8859-1 i kazda
 * negatywna asercja na polskim tekscie przechodzilaby zawsze.
 */
@AutoConfigureMockMvc
class HomePageCopyIT extends PostgresTestBase {

    /**
     * Kwoty idą z twardą spacją („1 080 zł”), ale Jsoup.text() normalizuje ją do
     * zwykłej, więc w asercjach na tekście z Jsoup stoi zwykła spacja.
     */
    private static final String NBSP = " ";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    private String home() throws Exception {
        String html = mvc.perform(get("/")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("</html>");
        return html;
    }

    @Test
    @DisplayName("nowy cennik: Opieka Longevity, para 270 zł, jadłospis 299 zł, produkty jednorazowe")
    void newPricesAreRendered() throws Exception {
        Document doc = Jsoup.parse(home());

        String offer = doc.getElementById("oferta").text();
        assertThat(offer).contains("Opieka Longevity", "135" + NBSP + "zł", "Taniej o 36%",
                "Razem: 1" + NBSP + "080" + NBSP + "zł / miesiąc (8 treningów)",
                "12 miesięcy, płatność co miesiąc", "Najlepszy wybór",
                "ok. 14" + NBSP + "400" + NBSP + "zł", "12" + NBSP + "960" + NBSP + "zł",
                "1" + NBSP + "440" + NBSP + "zł, czyli prawie 10 treningów gratis");

        String pairs = doc.getElementById("pary").text();
        assertThat(pairs).contains("270" + NBSP + "zł", "Taniej o 19%", "Taniej o 22%", "Taniej o 26%",
                "Opieka Longevity we dwoje", "Taniej o 33%",
                "Rok w pakietach po 12 to ok. 19" + NBSP + "200" + NBSP + "zł, tutaj 17" + NBSP + "280" + NBSP + "zł.",
                "W pakiecie 12 treningów każde z Was płaci więc 100" + NBSP + "zł za trening.")
                .doesNotContain("240" + NBSP + "zł");

        assertThat(doc.getElementById("zdalnie").text()).contains("299" + NBSP + "zł").doesNotContain("129");
        assertThat(doc.getElementById("online").text()).contains(
                "Plan treningowy + jadłospis", "549" + NBSP + "zł", "Aktualizacja po 3 miesiącach",
                "od 179" + NBSP + "zł", "Wybieram Start", "Zostały 3 z 5 miejsc w cenie startowej.",
                "Cena docelowa");
    }

    @Test
    @DisplayName("FAQ: nowe odpowiedzi, trzy tabele, pytanie o Opiekę, JSON-LD zgodny z widoczną treścią")
    void faqAndStructuredData() throws Exception {
        Document doc = Jsoup.parse(home());
        Element faq = doc.getElementById("faq");

        assertThat(faq.select("table.price-table")).hasSize(3);
        assertThat(faq.text()).contains(
                "Pojedynczy trening w studiu kosztuje 210" + NBSP + "zł. W pakietach cena spada do 150" + NBSP
                        + "zł, a w Opiece Longevity (rok współpracy, 2 treningi w tygodniu) do 135" + NBSP + "zł za trening.",
                "Trening we dwoje kosztuje od 180 do 270" + NBSP + "zł, łącznie za dwie osoby.",
                "plan treningowy kosztuje 349" + NBSP + "zł, a jadłospis 299" + NBSP + "zł.",
                "4 treningi wykorzystujesz w 6 tygodni, 8 w 10 tygodni, a 12 w 16 tygodni",
                "od 200" + NBSP + "zł za trening w pakietach i 180" + NBSP + "zł w Opiece Longevity",
                "Czym Opieka Longevity różni się od pakietu?");

        List<String> visibleQuestions = faq.select("h3.faq-q").eachText();
        JsonNode graph = null;
        for (Element script : doc.select("script[type=application/ld+json]")) {
            JsonNode node = json.readTree(script.data());
            if (node.has("@graph")) graph = node.get("@graph");
        }
        assertThat(graph).as("JSON-LD strony głównej musi być poprawnym JSON-em").isNotNull();

        List<String> structured = new ArrayList<>();
        for (JsonNode node : graph) {
            if ("FAQPage".equals(node.path("@type").asText())) {
                node.get("mainEntity").forEach(q -> structured.add(q.get("name").asText()));
                assertThat(node.toString()).doesNotContain("129");
            }
            assertThat(node.toString()).doesNotContain("Fit and Health");
        }
        assertThat(structured).containsExactlyElementsOf(visibleQuestions).hasSize(9);
    }

    @Test
    @DisplayName("reguły ogólne: bez starych nazw i przycisków, bez emoji, bez półpauz w tekście strony")
    void generalCopyRules() throws Exception {
        String html = home();
        Document doc = Jsoup.parse(html);

        assertThat(html).doesNotContain("Fit and Health", "Umów konsultację", "Umów bezpłatną",
                "🎙", "🏋", "🅿", "💬", "🌐", "✅", "⚠", "💻", "🥗", "▶ ");
        assertThat(doc.select("#main-nav .nav-links").text()).contains("Podcast").doesNotContain("Ekspert");
        assertThat(doc.select(".nav-cta").text()).isEqualTo("Umów rozmowę");

        // Treść opinii klientów zostaje w ich brzmieniu, wiec ja pomijamy.
        doc.select(".rv-body").remove();
        assertThat(doc.body().text()).doesNotContain("–", "—");
        assertThat(doc.title()).isEqualTo("Szymon Domagała - Trener Longevity | trener personalny Łódź");
    }

    @Test
    @DisplayName("opinia Wojtka jest na stronie z podpisem")
    void wojtekTestimonial() throws Exception {
        String reviews = Jsoup.parse(home()).getElementById("opinie").text();
        assertThat(reviews).contains("Wojtek · Katowice", "prowadzenie online · 40 lat",
                "zrzucić 25 zbędnych kilogramów w niespełna pół roku");
        // Tylko opinia Michała jest długa: ona dostaje kartę na dwa rzędy, krótkie stają obok.
        assertThat(Jsoup.parse(home()).select("#opinie figure.rv-long .rv-name").eachText())
                .containsExactly("Michał · Łódź");
    }

    @Test
    @DisplayName("panel: długość umowy zero albo nieliczbowa jest odrzucona i nic się nie zapisuje")
    void invalidTermIsRejected() throws Exception {
        Long id = jdbc.queryForObject(
                "select id from stationary_package where kind = 'INDYWIDUALNY' and sessions = 4", Long.class);

        for (String term : new String[] {"0", "abc", "61"}) {
            mvc.perform(post("/admin/oferta/stacjonarnie/" + id).with(csrf()).with(user("szymon").roles("ADMIN"))
                            .param("name", "Zmieniona nazwa").param("sessions", "4")
                            .param("pricePerSession", "170").param("validityWeeks", "6")
                            .param("visible", "true").param("termMonths", term))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));
        }

        String name = jdbc.queryForObject("select name from stationary_package where id = ?", String.class, id);
        Integer term = jdbc.queryForObject("select term_months from stationary_package where id = ?", Integer.class, id);
        assertThat(name).isEqualTo("Pakiet 4 treningów");
        assertThat(term).isNull();
    }

    @Test
    @DisplayName("JSON-LD zostaje poprawnym JSON-em, gdy nazwa Opieki z panelu ma cudzysłów, backslash i nową linię")
    void structuredDataSurvivesHostileName() throws Exception {
        String original = jdbc.queryForObject(
                "select name from stationary_package where kind = 'INDYWIDUALNY' and term_months is not null", String.class);
        String hostile = "Opieka \"X\" \\ </script><!--<script>\nY";
        jdbc.update("update stationary_package set name = ? where kind = 'INDYWIDUALNY' and term_months is not null", hostile);
        try {
            Document doc = Jsoup.parse(home());
            JsonNode graph = null;
            for (Element script : doc.select("script[type=application/ld+json]")) {
                JsonNode node = json.readTree(script.data());
                if (node.has("@graph")) graph = node.get("@graph");
            }
            assertThat(graph).isNotNull();
            String priceAnswer = null;
            for (JsonNode node : graph) {
                if ("FAQPage".equals(node.path("@type").asText())) {
                    priceAnswer = node.get("mainEntity").get(1).get("acceptedAnswer").get("text").asText();
                }
            }
            assertThat(priceAnswer).contains("w Opiece \"X\" \\ </script><!--<script>\nY (rok współpracy");
            assertThat(doc.select("script[type=application/ld+json]").outerHtml()).doesNotContain("<!--", "</script><");
        } finally {
            jdbc.update("update stationary_package set name = ? where kind = 'INDYWIDUALNY' and term_months is not null", original);
        }
    }
}
