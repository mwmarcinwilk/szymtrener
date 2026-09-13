package pl.szymtrener.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
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
 * Link rozbieramy recznie, nie przez `java.net.URI`: `URI` na niezakodowane `@`
 * w hasle albo `_` w nazwie hosta zwraca pusty host, a wtedy aplikacja po cichu
 * laczyla sie z localhost. Linku, w ktorym nie da sie jednoznacznie wskazac hosta,
 * nie zgadujemy: start sie zatrzymuje.
 *
 * Klasa MUSI byc zarejestrowana w `META-INF/spring.factories` — samo `@Component`
 * nie zadziala, bo musi wykonac sie ZANIM powstanie DataSource.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    /** Kolejnosc ma znaczenie: pierwsza ustawiona zmienna wygrywa. */
    private static final String[] SOURCES = {"DATABASE_URL", "SPRING_DATASOURCE_URL", "DB_URL"};
    private static final String[] SCHEMES = {"postgres://", "postgresql://"};
    private static final int DEFAULT_PORT = 5432;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (String name : SOURCES) {
            String value = environment.getProperty(name);
            if (value == null || value.isBlank()) continue;

            Map<String, Object> resolved;
            try {
                resolved = translate(value.trim());
            } catch (IllegalArgumentException e) {
                // Bez tego start szedlby dalej na domyslnym localhost i konczyl sie
                // „Connection refused" bez slowa o linku. Wartosci nie cytujemy: jest w niej haslo.
                throw new IllegalStateException("Zmienna " + name + " nie jest poprawnym linkiem do bazy: "
                        + e.getMessage() + ". Oczekiwany format: postgres://uzytkownik:haslo@host:5432/baza");
            }
            if (resolved.isEmpty()) continue;   // juz jest jdbc: albo to nie link postgresa

            // addFirst: ma przebic domyslne wartosci z application.yml
            environment.getPropertySources().addFirst(new MapPropertySource("database-url", resolved));
            return;
        }
    }

    /**
     * @return wlasciwosci `spring.datasource.*` albo pusta mapa, gdy przeklad
     *         jest zbedny (link juz jest w formacie JDBC albo to nie link postgresa)
     * @throws IllegalArgumentException link bez hosta, z blednym portem, niejednoznaczny
     *         albo z nieobslugiwanym schematem; komunikat nigdy nie zawiera hasla
     */
    static Map<String, Object> translate(String raw) {
        if (raw.startsWith("jdbc:")) return Map.of();
        String rest = stripScheme(raw);
        if (rest == null) {
            // `POSTGRES://`, `postgres:/` albo `mysql://` przepuszczone dalej konczylyby sie
            // cichym localhost albo linkiem z haslem w komunikacie sterownika.
            if (raw.contains("://") || raw.toLowerCase(Locale.ROOT).startsWith("postgres")) {
                throw new IllegalArgumentException("nieobslugiwany schemat");
            }
            return Map.of();
        }

        // Haslo moze zawierac '@' i ':', wiec dane logowania koncza sie na OSTATNIM '@'.
        int at = rest.lastIndexOf('@');
        // Ale '/', '?' albo '#' przed tym '@' oznacza, ze nie da sie zgadnac, gdzie konczy
        // sie host: `db/baza?app=x@prod` to baza na `db` albo login `db` i haslo `baza?app=x`
        // na `prod`. Zgadywanie wysyla dane logowania na obcy host, wiec odmawiamy
        // (w adresie URI te znaki i tak musza byc w danych logowania zakodowane).
        if (at >= 0 && rest.substring(0, at).chars().anyMatch(c -> "/?#".indexOf(c) >= 0)) {
            throw new IllegalArgumentException("niejednoznaczny link: znaki / ? # @ w hasle zakoduj jako %2F %3F %23 %40");
        }
        String userInfo = at < 0 ? null : rest.substring(0, at);
        String location = rest.substring(at + 1);
        // Link do postgresa nie ma fragmentu; bez tego `db#x` wchodziloby do nazwy hosta.
        int hash = location.indexOf('#');
        if (hash >= 0) location = location.substring(0, hash);

        String query = null;
        int questionMark = location.indexOf('?');
        if (questionMark >= 0) {
            query = location.substring(questionMark + 1);
            location = location.substring(0, questionMark);
        }

        int slash = location.indexOf('/');
        String authority = slash < 0 ? location : location.substring(0, slash);
        String database = slash < 0 ? "" : location.substring(slash + 1);

        String host = authority;
        int port = DEFAULT_PORT;
        int colon = authority.lastIndexOf(':');
        if (colon >= 0 && !authority.endsWith("]")) {   // ']' to koniec adresu IPv6 bez portu
            host = authority.substring(0, colon);
            port = parsePort(authority.substring(colon + 1));
        }
        // pgjdbc dzieli host na ',' (kilka serwerow), a pusta pozycja oznacza dla niego
        // localhost: `db,` po cichu dostaje zapasowe polaczenie z 127.0.0.1.
        for (String entry : host.split(",", -1)) {
            if (entry.isBlank()) throw new IllegalArgumentException("brak hosta");
        }

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                .append(host).append(':').append(port).append('/').append(database);
        // parametry (np. sslmode=require na bazie zewnetrznej) musza przejsc dalej
        if (query != null && !query.isBlank()) jdbc.append('?').append(query);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", jdbc.toString());

        if (userInfo != null && !userInfo.isBlank()) {
            // Login nie zawiera ':', haslo juz moze — dzielimy na PIERWSZYM.
            int separator = userInfo.indexOf(':');
            String user = separator < 0 ? userInfo : userInfo.substring(0, separator);
            String password = separator < 0 ? "" : userInfo.substring(separator + 1);
            properties.put("spring.datasource.username", decode(user));
            properties.put("spring.datasource.password", decode(password));
        }
        return properties;
    }

    private static String stripScheme(String raw) {
        for (String scheme : SCHEMES) {
            if (raw.regionMatches(true, 0, scheme, 0, scheme.length())) return raw.substring(scheme.length());
        }
        return null;
    }

    private static int parsePort(String value) {
        if (value.isEmpty()) return DEFAULT_PORT;
        try {
            int port = Integer.parseInt(value);
            if (port > 0 && port <= 65_535) return port;
        } catch (NumberFormatException ignored) {
            // spada do wyjatku ponizej
        }
        throw new IllegalArgumentException("port nie jest liczba z zakresu 1-65535");
    }

    /**
     * Haslo ze znakami specjalnymi bywa zakodowane procentowo (p@ss:word → p%40ss%3Aword)
     * i wtedy trzeba je odkodowac. `+` zostaje plusem: `URLDecoder` zrobilby z niego spacje.
     * Haslo z golym '%' (np. „50%off") nie jest poprawnym kodowaniem, wiec idzie bez zmian.
     */
    private static String decode(String value) {
        try {
            return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException notEncoded) {
            return value;
        }
    }
}
