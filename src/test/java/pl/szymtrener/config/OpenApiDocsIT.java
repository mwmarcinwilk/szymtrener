package pl.szymtrener.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import pl.szymtrener.PostgresTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Dokumentacja OpenAPI to mapa calego panelu, wiec widzi ja tylko admin. Sonda sprawdza tez adresy,
 * ktorych regula nie wymienia wprost: domyslne /v3/api-docs.yaml lezalo obok /v3/api-docs/** i bylo publiczne.
 */
@AutoConfigureMockMvc
class OpenApiDocsIT extends PostgresTestBase {

    private static final List<String> DOC_PATHS = List.of(
            "/admin/api-docs", "/admin/api-docs.yaml", "/admin/api-docs/swagger-config", "/admin/swagger-ui.html",
            "/admin/swagger-ui/index.html",
            "/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs.yaml/springdocDefault", "/v3/api-docs/swagger-config",
            "/v3/api-docs.json", "/swagger-ui.html", "/swagger-ui/index.html", "/swagger-ui/swagger-initializer.js",
            "/webjars/swagger-ui/index.html", "/swagger-resources", "/v2/api-docs");

    @Autowired MockMvc mvc;

    @Test
    @DisplayName("anonim nie dostaje żadnej postaci dokumentacji: ani JSON, ani YAML, ani Swagger UI")
    void docsHiddenFromAnonymous() throws Exception {
        for (String path : DOC_PATHS) {
            int status = mvc.perform(get(path)).andReturn().getResponse().getStatus();
            String body = mvc.perform(get(path)).andReturn().getResponse().getContentAsString();
            assertThat(status).as(path).isNotEqualTo(200);
            assertThat(body).as(path).doesNotContain("openapi").doesNotContain("zgoda-cookies").doesNotContain("swagger");
        }
    }

    @Test
    @DisplayName("admin widzi opisane endpointy zgody na cookies pod /admin/api-docs, w JSON i YAML")
    void adminSeesConsentEndpoints() throws Exception {
        var admin = user("admin@example.com").roles("ADMIN");
        mvc.perform(get("/admin/api-docs").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/zgoda-cookies'].post.summary").value("Zapis decyzji o cookies"))
                .andExpect(jsonPath("$.paths['/zgoda-cookies'].post.responses['429']").exists())
                .andExpect(jsonPath("$.paths['/ustawienia-cookies/moje-dane'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/admin/zgody-cookies/{key}/usun'].post.summary").value("Usunięcie zgody (RODO art. 17)"));
        mvc.perform(get("/admin/api-docs.yaml").with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/zgoda-cookies")));
        String ui = mvc.perform(get("/admin/swagger-ui.html").with(admin))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        mvc.perform(get(ui).with(admin)).andExpect(status().isOk());
        mvc.perform(get("/admin/api-docs/swagger-config").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/admin/api-docs"));
    }
}
