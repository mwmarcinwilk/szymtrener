package pl.szymtrener.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rozbija jeden link do bazy (`DATABASE_URL`) na to, czego oczekuje Spring.
 *
 * Coolify — podobnie jak Heroku czy Render — podaje polaczenie jako jeden ciag:
 *   postgres://uzytkownik:haslo@host:5432/baza?sslmode=require
 *
 * Sterownik JDBC tego nie przyjmie: schemat musi brzmiec `jdbc:postgresql://`,
 * a login i haslo ida osobno. Wrzucenie surowego linku do `spring.datasource.url`
 * konczy sie bledem „Driver claims to not accept jdbcUrl".
 *
 * Klasa MUSI byc zarejestrowana w `META-INF/spring.factories` — samo `@Component`
 * nie zadziala, bo musi wykonac sie ZANIM powstanie DataSource.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    /** Kolejnosc ma znaczenie: pierwsza ustawiona zmienna wygrywa. */
    private static final String[] SOURCES = {"DATABASE_URL", "SPRING_DATASOURCE_URL", "DB_URL"};
    private static final int DEFAULT_PORT = 5432;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (String name : SOURCES) {
            String value = environment.getProperty(name);
            if (value == null || value.isBlank()) continue;

            Map<String, Object> resolved = translate(value.trim());
            if (resolved.isEmpty()) continue;   // juz jest jdbc: albo nie da sie rozebrac

            // addFirst: ma przebic domyslne wartosci z application.yml
            environment.getPropertySources().addFirst(new MapPropertySource("database-url", resolved));
            return;
        }
    }

    /**
     * @return wlasciwosci `spring.datasource.*` albo pusta mapa, gdy przeklad
     *         jest zbedny (link juz jest w formacie JDBC) lub niemozliwy
     */
    static Map<String, Object> translate(String raw) {
        if (raw.startsWith("jdbc:")) return Map.of();
        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) return Map.of();

        try {
            URI uri = new URI(raw);
            String host = uri.getHost();
            if (host == null) return Map.of();

            int port = uri.getPort() > 0 ? uri.getPort() : DEFAULT_PORT;
            String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");

            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                    .append(host).append(':').append(port).append('/').append(database);
            // parametry (np. sslmode=require na bazie zewnetrznej) musza przejsc dalej
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbc.append('?').append(uri.getQuery());
            }

            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("spring.datasource.url", jdbc.toString());

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                int separator = userInfo.indexOf(':');
                String user = separator < 0 ? userInfo : userInfo.substring(0, separator);
                String password = separator < 0 ? "" : userInfo.substring(separator + 1);
                // Haslo ze znakami specjalnymi jest w linku zakodowane procentowo
                // (p@ss:word → p%40ss%3Aword) — bez dekodowania logowanie odpada.
                properties.put("spring.datasource.username", decode(user));
                properties.put("spring.datasource.password", decode(password));
            }
            return properties;
        } catch (Exception e) {
            // Nie wywracamy startu aplikacji na parsowaniu — niech zadziala
            // zwykla konfiguracja, a blad polaczenia powie wprost, co jest nie tak.
            return Map.of();
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
