package pl.szymtrener.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pulapki sa tu wszystkie znane z wdrozen na Coolify: haslo ze znakami specjalnymi,
 * brak portu, parametry polaczenia i link juz podany w formacie JDBC.
 */
class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    @DisplayName("zwykły link Coolify rozbija się na url, użytkownika i hasło")
    void translatesPlainUrl() {
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor
                .translate("postgres://szym:tajne@db-host:5432/szymtrener");

        assertThat(p.get("spring.datasource.url")).isEqualTo("jdbc:postgresql://db-host:5432/szymtrener");
        assertThat(p.get("spring.datasource.username")).isEqualTo("szym");
        assertThat(p.get("spring.datasource.password")).isEqualTo("tajne");
    }

    @Test
    @DisplayName("hasło ze znakami specjalnymi jest odkodowane — inaczej logowanie odpada")
    void decodesEncodedPassword() {
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor
                .translate("postgres://user:p%40ss%3Aword%2F99@host:5432/baza");

        assertThat(p.get("spring.datasource.password")).isEqualTo("p@ss:word/99");
    }

    @Test
    @DisplayName("brak portu oznacza domyślny 5432")
    void defaultsPort() {
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor
                .translate("postgres://user:pass@host/baza");

        assertThat(p.get("spring.datasource.url")).isEqualTo("jdbc:postgresql://host:5432/baza");
    }

    @Test
    @DisplayName("parametry połączenia (sslmode) przechodzą do adresu JDBC")
    void keepsQueryString() {
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor
                .translate("postgres://user:pass@host:5432/baza?sslmode=require");

        assertThat(p.get("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://host:5432/baza?sslmode=require");
    }

    @Test
    @DisplayName("schemat postgresql:// działa tak samo jak postgres://")
    void acceptsBothSchemes() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.translate("postgresql://u:p@h/db"))
                .containsEntry("spring.datasource.url", "jdbc:postgresql://h:5432/db");
    }

    @Test
    @DisplayName("gotowy adres JDBC przechodzi bez tłumaczenia")
    void leavesJdbcUrlAlone() {
        assertThat(DatabaseUrlEnvironmentPostProcessor
                .translate("jdbc:postgresql://localhost:5432/szymtrener")).isEmpty();
    }

    @Test
    @DisplayName("śmieciowy link nie wywraca startu aplikacji")
    void garbageIsIgnored() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.translate("to nie jest adres")).isEmpty();
        assertThat(DatabaseUrlEnvironmentPostProcessor.translate("postgres://")).isEmpty();
    }
}
