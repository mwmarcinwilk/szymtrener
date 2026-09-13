package pl.szymtrener.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("tekst, który nie jest linkiem postgresa, zostaje pominięty")
    void nonPostgresValueIsIgnored() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.translate("to nie jest adres")).isEmpty();
    }

    @Test
    @DisplayName("prawdziwy link z Coolify: długie hasło i host będący identyfikatorem zasobu")
    void translatesRealCoolifyUrl() {
        String password = "Xk9mQ2vLp7RtY4wZ8nB3cF6hJ1sD5gA0eU2iO9pL3kM7nB1vC4xZ6qW8eR5tY2uI";
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor
                .translate("postgres://postgres:" + password + "@ok0gs8wcg8kk4oc4wg8gc0kc:5432/postgres");

        assertThat(p).containsEntry("spring.datasource.url", "jdbc:postgresql://ok0gs8wcg8kk4oc4wg8gc0kc:5432/postgres")
                .containsEntry("spring.datasource.username", "postgres")
                .containsEntry("spring.datasource.password", password);
    }

    @ParameterizedTest(name = "hasło {0}")
    @ValueSource(strings = {"p@ss", "pa:ss", "p@ss:x@y", "50%off", "a+b", "pa;ss", "pa&ss"})
    @DisplayName("niezakodowane @ : % + w haśle nie gubią hosta")
    void handlesUnencodedPassword(String password) {
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor
                .translate("postgres://szym:" + password + "@db:5432/baza");

        assertThat(p).containsEntry("spring.datasource.url", "jdbc:postgresql://db:5432/baza")
                .containsEntry("spring.datasource.username", "szym")
                .containsEntry("spring.datasource.password", password);
    }

    @Test
    @DisplayName("podkreślnik w nazwie hosta (nazwa usługi Dockera) jest dozwolony")
    void acceptsUnderscoreInHost() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.translate("postgres://u:p@my_db:5432/baza"))
                .containsEntry("spring.datasource.url", "jdbc:postgresql://my_db:5432/baza");
    }

    @ParameterizedTest
    @ValueSource(strings = {"postgres://", "postgres://u:tajnehaslo@:5432/baza", "postgres://u:tajnehaslo@db:abc/baza",
            "postgres://u:tajnehaslo@db/baza?ApplicationName=app@prod",
            "postgres://u:tajnehaslo@db/baza?x=a@prod/x",
            "postgres://u:tajnehaslo@db/baza?options=a@prod:6543/evil",
            "postgres://u:tajnehaslo@db?ApplicationName=app@prod",
            "postgres://u:tajnehaslo@db:5432?sslmode=require&ApplicationName=a@prod:5432",
            "postgres://u:tajnehaslo@db/baza#f@prod/x",
            "postgres://u:p@ss#1/x@db:5432/baza",
            "postgres://u:pa#tajnehaslo@db/baza",
            "postgres://u:pa/tajnehaslo@db/baza",
            "postgres://u:pa?tajnehaslo@db/baza",
            "postgres://db:5432/app?user=app&password=tajnehaslo&ApplicationName=svc@prod",
            "postgres://db/app?sslmode=require&ApplicationName=me@evil",
            "postgres://db/my@evil",
            "postgres://db#frag@evil",
            "postgres:/u:tajnehaslo@db/baza",
            "mysql://u:tajnehaslo@db/baza",
            "JDBC:postgresql://db/baza?password=tajnehaslo",
            "postgres://u:tajnehaslo@,/baza",
            "postgres://u:tajnehaslo@db,/baza",
            "postgres://u:tajnehaslo@,db/baza",
            "postgres://u:tajnehaslo@db,,db2/baza"})
    @DisplayName("zepsuty link postgresa zatrzymuje start zamiast łączyć z localhost, bez hasła w komunikacie")
    void brokenPostgresUrlFailsLoudly(String raw) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of("DATABASE_URL", raw)));

        assertThatThrownBy(() -> new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DATABASE_URL")
                .hasMessageNotContaining("tajnehaslo");
    }

    @Test
    @DisplayName("zepsuty DATABASE_URL zatrzymuje start nawet przy poprawnym DB_URL — nie zgadujemy, który link jest właściwy")
    void brokenDatabaseUrlDoesNotFallBackToDbUrl() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("env", Map.of(
                "DATABASE_URL", "postgres://",
                "DB_URL", "postgres://u:p@db:5432/baza")));

        assertThatThrownBy(() -> new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DATABASE_URL");
    }

    @Test
    @DisplayName("% z dwiema cyframi szesnastkowymi to kodowanie, dosłowny procent trzeba zapisać jako %25")
    void percentFollowedByHexIsDecoded() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.translate("postgres://u:abc%12def@db/baza"))
                .containsEntry("spring.datasource.password", "abc\u0012def");
        assertThat(DatabaseUrlEnvironmentPostProcessor.translate("postgres://u:abc%2512def@db/baza"))
                .containsEntry("spring.datasource.password", "abc%12def");
    }

    @Test
    @DisplayName("kilka hostów po przecinku przechodzi, gdy żadna pozycja nie jest pusta")
    void acceptsMultipleHosts() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.translate("postgres://u:p@db1,db2/baza"))
                .containsEntry("spring.datasource.url", "jdbc:postgresql://db1,db2:5432/baza");
    }

    @Test
    @DisplayName("wielkość liter w schemacie nie ma znaczenia")
    void schemeIsCaseInsensitive() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.translate("POSTGRES://u:p@db/baza"))
                .containsEntry("spring.datasource.url", "jdbc:postgresql://db:5432/baza");
    }

    @Test
    @DisplayName("fragment po # nie wchodzi do nazwy hosta")
    void dropsFragment() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.translate("postgres://u:p@db#cokolwiek"))
                .containsEntry("spring.datasource.url", "jdbc:postgresql://db:5432/");
    }

    @Test
    @DisplayName("zakodowany @ w parametrach nie myli hosta")
    void encodedAtInQueryKeepsHost() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.translate("postgres://u:p@db/baza?ApplicationName=app%40prod"))
                .containsEntry("spring.datasource.url", "jdbc:postgresql://db:5432/baza?ApplicationName=app%40prod")
                .containsEntry("spring.datasource.password", "p");
    }

    @Test
    @DisplayName("DATABASE_URL trafia do spring.datasource i wygrywa z domyślnymi wartościami")
    void postProcessorOverridesDatasource() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("yml",
                Map.of("spring.datasource.url", "jdbc:postgresql://localhost:5432/szymtrener")));
        environment.getPropertySources().addFirst(new MapPropertySource("env",
                Map.of("DATABASE_URL", "postgres://szym:p@ss@db:5432/baza")));

        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://db:5432/baza");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("p@ss");
    }
}
