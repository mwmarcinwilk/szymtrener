package pl.szymtrener.crm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import pl.szymtrener.PostgresTestBase;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V10 poprawia szablony odpowiedzi tylko wtedy, gdy ich tresc jest dokladnie taka jak w V8.
 * Literowka w starej tresci sprawilaby, ze UPDATE po cichu nic nie zmienia, dlatego
 * sprawdzamy wynik na prawdziwej bazie po migracjach.
 */
class ReplyTemplateMigrationIT extends PostgresTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    @Test
    @DisplayName("wszystkie szablony z V8 dostały nową treść, bez półpauz i małej litery po powitaniu")
    void allSeededTemplatesUpdated() {
        var bodies = jdbc.queryForList("select body from reply_template order by sort_order", String.class);

        String greeting = "Cześć {imie}!\n\n";
        assertThat(bodies).hasSize(5).allSatisfy(body -> {
            assertThat(body).startsWith(greeting);
            assertThat(Character.isUpperCase(body.charAt(greeting.length()))).isTrue();
            assertThat(body).doesNotContain("—");
        });
    }

    @Test
    @DisplayName("szablon poprawiony przez trenera nie jest nadpisywany")
    void keepsTrainerEdits() throws Exception {
        String own = "Hej {imie}, moja wersja.";
        String before = jdbc.queryForObject("select body from reply_template where code = 'ping'", String.class);
        jdbc.update("update reply_template set body = ? where code = 'ping'", own);
        try {
            try (Connection connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection,
                        new ClassPathResource("db/migration/V10__reply_templates_copy.sql"));
            }
            assertThat(jdbc.queryForObject("select body from reply_template where code = 'ping'", String.class))
                    .isEqualTo(own);
        } finally {
            jdbc.update("update reply_template set body = ? where code = 'ping'", before);
        }
    }
}
