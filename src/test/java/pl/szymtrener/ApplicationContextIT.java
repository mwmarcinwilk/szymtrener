package pl.szymtrener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Najtanszy test o najwiekszym zasiegu: wstaje caly kontekst na prawdziwej bazie.
 * Wylapuje literowki w zapytaniach Spring Data, rozjazd encji ze schematem
 * (ddl-auto=validate) i bledy w migracjach Flyway — czyli wszystko, co inaczej
 * wychodzi dopiero przy starcie na serwerze.
 */
class ApplicationContextIT extends PostgresTestBase {

    @Autowired ApplicationContext context;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("kontekst wstaje: encje zgadzają się ze schematem, a repozytoria mają poprawne zapytania")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("wszystkie migracje Flyway przeszły")
    void migrationsApplied() {
        Integer failed = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = false", Integer.class);
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);

        assertThat(failed).isZero();
        assertThat(applied).isGreaterThanOrEqualTo(3);   // V1 schemat, V2 seed, V3 historia adresow
    }

    @Test
    @DisplayName("kolumna search_vector i indeks GIN istnieją — bez nich wyszukiwarka nie działa")
    void fullTextSearchIsWired() {
        Integer column = jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_name = 'post' and column_name = 'search_vector'""", Integer.class);
        Integer index = jdbc.queryForObject(
                "select count(*) from pg_indexes where tablename = 'post' and indexname = 'idx_post_search'",
                Integer.class);

        assertThat(column).isEqualTo(1);
        assertThat(index).isEqualTo(1);
    }

    @Test
    @DisplayName("dane startowe są na miejscu: autor i kategorie")
    void seedDataLoaded() {
        assertThat(jdbc.queryForObject("select count(*) from author", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from category", Integer.class)).isEqualTo(4);
    }
}
