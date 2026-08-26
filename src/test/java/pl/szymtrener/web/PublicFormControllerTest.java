package pl.szymtrener.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.szymtrener.analytics.PageViewRepository;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.config.SecurityConfig;
import pl.szymtrener.submission.RateLimiter;
import pl.szymtrener.submission.SubmissionService;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicFormController.class)
@Import({SecurityConfig.class, PublicFormControllerTest.Config.class})
class PublicFormControllerTest {

    @TestConfiguration
    static class Config {
        @Bean AppProperties appProperties() {
            return new AppProperties(
                    "https://szymtrener.pl", "Szymon Domagała",
                    new AppProperties.Mail("kontakt@example.com", "kontakt@example.com", true),
                    new AppProperties.Admin("admin@example.com", null),
                    new AppProperties.Media(1600, 0.82f, "image/jpeg"),
                    new AppProperties.IndexNow(false, null),
                    new AppProperties.Analytics(false, "sol-testowa"));
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    // @MockitoBean, nie @Bean z atrapa: kontekst jest wspoldzielony miedzy testami,
    // a te atrapy musza sie resetowac po kazdym z nich.
    @MockitoBean SubmissionService submissions;
    @MockitoBean RateLimiter rateLimiter;

    /** AnalyticsFilter jest filtrem, wiec wchodzi do warstwy web razem z kontrolerem. */
    @MockitoBean PageViewRepository pageViews;

    @BeforeEach
    void allowByDefault() {
        when(rateLimiter.allow(anyString())).thenReturn(true);
    }

    private static Map<String, Object> validContact() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("name", "Anna Kowalska");
        form.put("email", "anna@example.com");
        form.put("phone", "500100200");
        form.put("interest", "Trening personalny");
        form.put("message", "Chciałabym zacząć trenować.");
        form.put("consent", true);
        form.put("botcheck", false);
        return form;
    }

    private static Map<String, Object> validOnline() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("name", "Jan Nowak");
        form.put("email", "jan@example.com");
        form.put("phone", "500100200");
        form.put("city", "Łódź");
        form.put("currentTraining", "Dwa razy w tygodniu siłownia.");
        form.put("goal", "Chcę zbudować siłę.");
        form.put("equipment", "Siłownia");
        form.put("source", "Google");
        form.put("consent", true);
        form.put("botcheck", false);
        return form;
    }

    @Test
    @DisplayName("poprawne zgłoszenie kontaktowe zostaje przyjęte i zapisane")
    void acceptsValidContactForm() throws Exception {
        mvc.perform(post("/api/zgloszenia/kontakt").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validContact())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(submissions).acceptContact(any(), any(), any());
    }

    @Test
    @DisplayName("poprawne zgłoszenie online zostaje przyjęte")
    void acceptsValidOnlineForm() throws Exception {
        mvc.perform(post("/api/zgloszenia/online").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validOnline())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(submissions).acceptOnline(any(), any(), any());
    }

    @Test
    @DisplayName("bez tokenu CSRF żądanie jest odrzucane")
    void rejectsRequestWithoutCsrfToken() throws Exception {
        mvc.perform(post("/api/zgloszenia/kontakt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validContact())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(submissions);
    }

    @Test
    @DisplayName("honeypot: bot dostaje odpowiedź sukcesu, ale nic nie trafia do bazy")
    void honeypotSilentlyDropsBotSubmissions() throws Exception {
        Map<String, Object> bot = validContact();
        bot.put("botcheck", true);

        mvc.perform(post("/api/zgloszenia/kontakt").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(bot)))
                // sukces celowo: nie podpowiadamy botowi, że został wykryty
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verifyNoInteractions(submissions);
    }

    @Test
    @DisplayName("honeypot działa nawet wtedy, gdy reszta formularza jest niepoprawna")
    void honeypotWinsOverValidation() throws Exception {
        Map<String, Object> bot = validContact();
        bot.put("botcheck", true);
        bot.put("email", "to-nie-jest-email");
        bot.put("name", "");

        mvc.perform(post("/api/zgloszenia/kontakt").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(bot)))
                .andExpect(status().isOk());

        verifyNoInteractions(submissions);
    }

    @Test
    @DisplayName("przekroczony limit zgłoszeń kończy się 429 i podpowiedzią telefonu")
    void rejectsWhenRateLimited() throws Exception {
        when(rateLimiter.allow(anyString())).thenReturn(false);

        mvc.perform(post("/api/zgloszenia/kontakt").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validContact())))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("502 338 373")));

        verifyNoInteractions(submissions);
    }

    @Test
    @DisplayName("brak zgody RODO blokuje zapis i wskazuje pole")
    void requiresConsent() throws Exception {
        Map<String, Object> form = validContact();
        form.put("consent", false);

        mvc.perform(post("/api/zgloszenia/kontakt").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(form)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.errors.consent").exists());

        verifyNoInteractions(submissions);
    }

    @Test
    @DisplayName("błędny e-mail i puste imię wracają jako błędy pól")
    void reportsFieldValidationErrors() throws Exception {
        Map<String, Object> form = validContact();
        form.put("email", "nie-email");
        form.put("name", "");

        mvc.perform(post("/api/zgloszenia/kontakt").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(form)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.name").value("Podaj imię i nazwisko"));

        verifyNoInteractions(submissions);
    }

    @Test
    @DisplayName("formularz online wymaga miejscowości i celu")
    void onlineFormRequiresItsOwnFields() throws Exception {
        Map<String, Object> form = validOnline();
        form.put("city", "");
        form.put("goal", "");

        mvc.perform(post("/api/zgloszenia/online").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(form)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.city").value("Podaj miejscowość"))
                .andExpect(jsonPath("$.errors.goal").exists());

        verifyNoInteractions(submissions);
    }

    @Test
    @DisplayName("limit jest sprawdzany przed walidacją — bot nie mapuje pól formularza")
    void rateLimitIsCheckedBeforeValidation() throws Exception {
        when(rateLimiter.allow(anyString())).thenReturn(false);
        Map<String, Object> form = validContact();
        form.put("email", "nie-email");

        mvc.perform(post("/api/zgloszenia/kontakt").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(form)))
                .andExpect(status().isTooManyRequests());
    }
}
