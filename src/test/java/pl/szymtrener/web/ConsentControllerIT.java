package pl.szymtrener.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import pl.szymtrener.PostgresTestBase;
import pl.szymtrener.consent.ConsentCookie;
import pl.szymtrener.consent.ConsentService;
import pl.szymtrener.consent.ConsentSource;
import pl.szymtrener.consent.CookieConsent;
import pl.szymtrener.consent.CookieConsentRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Zgoda na ciasteczka przez HTTP: pasek, zapis, pobranie swoich danych i panel admina.
 * Strony sprawdzane do {@code </html>}, bo blad w szablonie urywa widok przy statusie 200.
 */
@AutoConfigureMockMvc
class ConsentControllerIT extends PostgresTestBase {

    private static final String BAR = "class=\"consent-bar\"";

    @Autowired MockMvc mvc;
    @Autowired ConsentService service;
    @Autowired CookieConsentRepository consents;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("delete from cookie_consent");
        jdbc.execute("delete from page_view");
    }

    @Test
    @DisplayName("bez decyzji strona główna, polityka i ustawienia pokazują panel i renderują się do końca")
    void pagesRenderWithoutDecision() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString(BAR), containsString("consent-dialog"), containsString("</html>"))));
        mvc.perform(get("/polityka-prywatnosci")).andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString(BAR), containsString("st_zgoda"), containsString("</html>"))));
        mvc.perform(get("/ustawienia-cookies")).andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("consent-panel-page"), not(containsString(BAR)),
                        not(containsString("moje-dane")), containsString("</html>"))));
    }

    @Test
    @DisplayName("zapis bez tokenu CSRF jest odrzucony i nic nie trafia do bazy")
    void rejectsWithoutCsrf() throws Exception {
        mvc.perform(post("/zgoda-cookies").param("wybor", "tak")).andExpect(status().isForbidden());
        assertThat(consents.count()).isZero();
    }

    @Test
    @DisplayName("akceptacja zapisuje zgodę, zakłada bezpieczne ciasteczko i wraca na stronę")
    void acceptStoresConsentAndCookie() throws Exception {
        var result = mvc.perform(post("/zgoda-cookies").with(csrf())
                        .param("wybor", "tak").param("zrodlo", "BAR").param("powrot", "/blog"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/blog"))
                .andReturn();

        String header = result.getResponse().getHeader("Set-Cookie");
        assertThat(header).contains(ConsentCookie.NAME + "=").contains("HttpOnly").contains("SameSite=Lax").contains("Max-Age=");
        UUID key = UUID.fromString(result.getResponse().getCookie(ConsentCookie.NAME).getValue());
        assertThat(consents.findByConsentKey(key)).get().extracting(CookieConsent::isStatistics).isEqualTo(true);
        assertThat(jdbc.queryForObject("select source from cookie_consent_change", String.class)).isEqualTo("BAR");

        mvc.perform(get("/").cookie(cookie(key))).andExpect(status().isOk())
                .andExpect(content().string(allOf(not(containsString(BAR)), containsString("</html>"))));
    }

    @Test
    @DisplayName("zmiana decyzji z tą samą przeglądarką aktualizuje tę samą zgodę")
    void changeKeepsKey() throws Exception {
        UUID key = service.record(null, true, ConsentSource.BAR);

        mvc.perform(post("/zgoda-cookies").with(csrf()).cookie(cookie(key))
                        .param("wybor", "zapisz").param("zrodlo", "DIALOG").header("X-Requested-With", "fetch"))
                .andExpect(status().isNoContent())
                .andExpect(MockMvcResultMatchers.cookie().value(ConsentCookie.NAME, key.toString()));

        assertThat(consents.count()).isEqualTo(1);
        assertThat(consents.findByConsentKey(key)).get().extracting(CookieConsent::isStatistics).isEqualTo(false);

        // ta sama decyzja: bez ciasteczka i bez wpisu, żeby ciasteczko nie przeżyło rekordu
        mvc.perform(post("/zgoda-cookies").with(csrf()).cookie(cookie(key))
                        .param("wybor", "nie").header("X-Requested-With", "fetch"))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertThat(jdbc.queryForObject("select count(*) from cookie_consent_change", Long.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("powrót tylko na lokalną ścieżkę spoza panelu")
    void safeRedirect() {
        assertThat(ConsentController.safeRedirect("/blog/wpis?x=1")).isEqualTo("/blog/wpis?x=1");
        for (String unsafe : List.of("//evil.example", "https://evil.example", "/\t/evil.example", "/\\evil",
                "/admin/zgody-cookies", "/error", "/{x}", "/zażółć", "")) {
            assertThat(ConsentController.safeRedirect(unsafe)).as(unsafe).isEqualTo("/");
        }
        assertThat(ConsentController.safeRedirect(null)).isEqualTo("/");
    }

    @Test
    @DisplayName("własne dane pobiera tylko przeglądarka z ciasteczkiem istniejącej zgody")
    void myDataOnlyForOwnCookie() throws Exception {
        UUID key = service.record(null, false, ConsentSource.PAGE);

        mvc.perform(get("/ustawienia-cookies/moje-dane")).andExpect(status().isNotFound());
        mvc.perform(get("/ustawienia-cookies/moje-dane").cookie(cookie(UUID.randomUUID()))).andExpect(status().isNotFound());
        mvc.perform(get("/ustawienia-cookies/moje-dane").cookie(new Cookie(ConsentCookie.NAME, "' or 1=1")))
                .andExpect(status().isNotFound());

        mvc.perform(get("/ustawienia-cookies/moje-dane").cookie(cookie(key)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.identyfikator").value(key.toString()))
                .andExpect(jsonPath("$.historia[0].miejsce").value("PAGE"));

        mvc.perform(get("/ustawienia-cookies").cookie(cookie(key))).andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString(key.toString()), containsString("moje-dane"), containsString("</html>"))));
    }

    @Test
    @DisplayName("odsłona ze zgodą na statystykę wskazuje zgodę, a sesja zostaje dziennym skrótem jak bez zgody")
    void analyticsLinksConsent() throws Exception {
        UUID granted = service.record(null, true, ConsentSource.BAR);
        UUID denied = service.record(null, false, ConsentSource.BAR);

        mvc.perform(get("/polityka-prywatnosci").cookie(cookie(granted))).andExpect(status().isOk());
        mvc.perform(get("/polityka-prywatnosci").cookie(cookie(denied))).andExpect(status().isOk());

        var rows = jdbc.queryForList("select session_hash, consent_id from page_view order by id");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("consent_id")).isEqualTo(consents.findByConsentKey(granted).orElseThrow().getId());
        assertThat(rows.get(1).get("consent_id")).isNull();
        // ten sam klient i ten sam dzień: zgoda nie zmienia liczby sesji
        assertThat(rows.get(0).get("session_hash")).isEqualTo(rows.get(1).get("session_hash"));
    }

    @Test
    @DisplayName("powracający za zgodą to zgoda widziana w dwóch różnych dniach, nie w jednym")
    void countsReturningAcrossDays() throws Exception {
        long twoDays = consents.findByConsentKey(service.record(null, true, ConsentSource.BAR)).orElseThrow().getId();
        long oneDay = consents.findByConsentKey(service.record(null, true, ConsentSource.BAR)).orElseThrow().getId();
        String insert = "insert into page_view (path, session_hash, consent_id, viewed_at) values ('/', 'x', ?, now() - make_interval(days => ?))";
        jdbc.update(insert, twoDays, 1);
        jdbc.update(insert, twoDays, 3);
        jdbc.update(insert, oneDay, 2);
        jdbc.update(insert, oneDay, 2);

        mvc.perform(get("/admin/statystyki").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(content().string(containsString("Powracający za zgodą: 1")));
    }

    @Test
    @DisplayName("przez HTTPS ciasteczko ma prefiks __Host- i Secure, a zwykła nazwa jest ignorowana")
    void secureCookieUsesHostPrefix() throws Exception {
        var result = mvc.perform(post("/zgoda-cookies").secure(true).with(csrf()).param("wybor", "tak"))
                .andExpect(status().isFound()).andReturn();

        assertThat(result.getResponse().getHeader("Set-Cookie")).startsWith(ConsentCookie.SECURE_NAME + "=").contains("Secure");
        UUID key = UUID.fromString(result.getResponse().getCookie(ConsentCookie.SECURE_NAME).getValue());

        mvc.perform(get("/").secure(true).cookie(new Cookie(ConsentCookie.NAME, key.toString())))
                .andExpect(content().string(containsString(BAR)));
        mvc.perform(get("/").secure(true).cookie(new Cookie(ConsentCookie.SECURE_NAME, key.toString())))
                .andExpect(content().string(not(containsString(BAR))));
    }

    @Test
    @DisplayName("widok renderuje się w całości przed wysłaniem, bo stopka tworzy sesję dla tokenu CSRF")
    void viewIsBufferedBeforeCommit(@Autowired org.thymeleaf.spring6.view.ThymeleafViewResolver resolver) {
        assertThat(resolver.getProducePartialOutputWhileProcessing()).isFalse();
    }

    @Test
    @DisplayName("limit zapisów z jednego adresu nie zapisuje nadmiarowych rekordów")
    void limitsNewConsentsPerClient() throws Exception {
        RequestPostProcessor client = request -> {
            request.setRemoteAddr("203.0.113.77");
            return request;
        };
        for (int i = 0; i < ConsentController.WRITES_PER_CLIENT; i++) {
            mvc.perform(fetchConsent().with(client)).andExpect(status().isNoContent());
        }
        mvc.perform(fetchConsent().with(client)).andExpect(status().isTooManyRequests());

        assertThat(consents.count()).isEqualTo(ConsentController.WRITES_PER_CLIENT);
    }

    @Test
    @DisplayName("panel zgód wymaga logowania")
    void adminRequiresLogin() throws Exception {
        UUID key = service.record(null, true, ConsentSource.BAR);

        mvc.perform(get("/admin/zgody-cookies")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/logowanie"));
        mvc.perform(get("/admin/zgody-cookies/" + key + "/dane")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/logowanie"));
        mvc.perform(post("/admin/zgody-cookies/" + key + "/usun").with(csrf())).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/logowanie"));
        assertThat(consents.findByConsentKey(key)).isPresent();
    }

    @Test
    @DisplayName("admin wyszukuje zgodę, pobiera dane i usuwa ją; pasek wraca do tej przeglądarki")
    void adminSearchExportDelete() throws Exception {
        var admin = user("admin@example.com").roles("ADMIN");
        UUID key = service.record(null, true, ConsentSource.BAR);
        UUID other = service.record(null, false, ConsentSource.BAR);

        mvc.perform(get("/admin/zgody-cookies").param("q", key.toString().substring(0, 8)).with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString(key.toString()), not(containsString(other.toString())),
                        containsString("</html>"))));
        mvc.perform(get("/admin/zgody-cookies/" + key + "/dane").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identyfikator").value(key.toString()));
        mvc.perform(get("/admin/zgody-cookies/nie-uuid/dane").with(admin)).andExpect(status().isNotFound());
        mvc.perform(get("/admin/zgody-cookies/" + UUID.randomUUID() + "/dane").with(admin)).andExpect(status().isNotFound());

        mvc.perform(post("/admin/zgody-cookies/" + key + "/usun").with(admin).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/zgody-cookies"))
                .andExpect(flash().attributeExists("info"));

        assertThat(consents.findByConsentKey(key)).isEmpty();
        mvc.perform(get("/").cookie(cookie(key))).andExpect(content().string(containsString(BAR)));
    }

    @Test
    @DisplayName("statystyki w panelu renderują się z licznikiem powracających")
    void statsRender() throws Exception {
        mvc.perform(get("/admin/statystyki").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Powracający za zgodą: 0"), containsString("</html>"))));
    }

    private static MockHttpServletRequestBuilder fetchConsent() {
        return post("/zgoda-cookies").with(csrf()).param("wybor", "nie").header("X-Requested-With", "fetch");
    }

    private static Cookie cookie(UUID key) {
        return new Cookie(ConsentCookie.NAME, key.toString());
    }
}
