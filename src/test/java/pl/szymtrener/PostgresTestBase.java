package pl.szymtrener;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.function.Supplier;

/**
 * Baza dla testow integracyjnych.
 *
 * Prawdziwy Postgres, nie H2: schemat uzywa tsvector, kolumny generowanej
 * (generated always as) i indeksu GIN — H2 nie zna zadnej z tych rzeczy, wiec
 * przepuscilby migracje, ktora na produkcji by sie wywrocila.
 *
 * Domyslnie baze podnosi Testcontainers. Gdy Docker jest niedostepny dla procesu
 * Javy (np. Docker Desktop z ograniczonym dostepem do gniazda), mozna wskazac
 * gotowa baze:
 *
 *   mvn test -Dtest.db.url=jdbc:postgresql://localhost:5433/szymtrener_test \
 *            -Dtest.db.user=szymtrener -Dtest.db.password=szymtrener
 *
 * Baza wskazana z zewnatrz musi byc PUSTA — Flyway zaklada na niej schemat od zera.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public abstract class PostgresTestBase {

    private static final String EXTERNAL_URL = property("test.db.url", "TEST_DB_URL");

    /**
     * Kontener jest statyczny i celowo NIE jest zamykany po klasie: Testcontainers
     * ubija go przez Ryuka po testach, a wspoldzielenie miedzy klasami oszczedza
     * kilkanascie sekund na kazdej.
     */
    private static final PostgreSQLContainer<?> POSTGRES =
            EXTERNAL_URL != null ? null : new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        if (POSTGRES != null) POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", constant(property("test.db.user", "TEST_DB_USER")));
            registry.add("spring.datasource.password", constant(property("test.db.password", "TEST_DB_PASSWORD")));
        }
    }

    private static Supplier<Object> constant(String value) {
        return () -> value;
    }

    private static String property(String systemProperty, String environmentVariable) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) value = System.getenv(environmentVariable);
        return (value == null || value.isBlank()) ? null : value;
    }
}
