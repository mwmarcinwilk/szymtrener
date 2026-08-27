package pl.szymtrener.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.szymtrener.config.AppProperties;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Dane ze zmiennych srodowiskowych maja BEZWARUNKOWE pierwszenstwo przy logowaniu.
 *
 * To nie jest wygoda, tylko zabezpieczenie: wlasciciel musi miec pewna droge do
 * wlasnego panelu niezaleznie od tego, co stalo sie z tabela admin_user — pusta baza,
 * zostawione stare konto po zmianie adresu, nieudana synchronizacja przy starcie.
 */
class AdminUserDetailsServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private AdminUserDetailsService service(String envEmail, String envPassword, AdminUser... inDatabase) {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        when(repo.findByEmailIgnoreCase(anyString())).thenAnswer(call -> {
            String wanted = call.getArgument(0);
            return List.of(inDatabase).stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(wanted))
                    .findFirst();
        });
        AppProperties props = new AppProperties(
                "https://szymtrener.pl", "Szymon Domagała",
                new AppProperties.Mail("a@b.pl", "a@b.pl", true),
                new AppProperties.Admin(envEmail, envPassword),
                new AppProperties.Media(1600, 0.82f, "image/jpeg"),
                new AppProperties.IndexNow(false, null),
                new AppProperties.Analytics(false, "sol"));
        return new AdminUserDetailsService(repo, props, encoder);
    }

    private AdminUser account(String email, String password) {
        AdminUser u = new AdminUser();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setEnabled(true);
        return u;
    }

    @Test
    @DisplayName("dane z env logują, choć w bazie jest zupełnie inne konto")
    void environmentWinsOverDatabase() {
        AdminUserDetailsService service = service("zenv@example.test", "HasloZeZmiennej1",
                account("zbazy@example.test", "InneHaslo"));

        UserDetails user = service.loadUserByUsername("zenv@example.test");

        assertThat(encoder.matches("HasloZeZmiennej1", user.getPassword())).isTrue();
        assertThat(user.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("dane z env logują nawet przy PUSTEJ bazie — nie da się zablokować dostępu")
    void environmentWorksWithEmptyDatabase() {
        AdminUserDetailsService service = service("zenv@example.test", "HasloZeZmiennej1");

        UserDetails user = service.loadUserByUsername("zenv@example.test");

        assertThat(user.getUsername()).isEqualTo("zenv@example.test");
        assertThat(encoder.matches("HasloZeZmiennej1", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("adres ze spacjami i inną wielkością liter też trafia w zmienną")
    void loginIsTrimmedAndCaseInsensitive() {
        AdminUserDetailsService service = service("zenv@example.test", "HasloZeZmiennej1");

        assertThat(service.loadUserByUsername("  ZEnv@Example.TEST  ")).isNotNull();
    }

    @Test
    @DisplayName("bez ADMIN_PASSWORD logowanie z bazy działa jak wcześniej")
    void fallsBackToDatabaseWhenEnvIncomplete() {
        AdminUserDetailsService service = service("zenv@example.test", "",
                account("zbazy@example.test", "HasloZBazy"));

        UserDetails user = service.loadUserByUsername("zbazy@example.test");

        assertThat(encoder.matches("HasloZBazy", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("nieznany adres nadal odpada")
    void unknownAccountIsRejected() {
        AdminUserDetailsService service = service("zenv@example.test", "HasloZeZmiennej1");

        assertThatThrownBy(() -> service.loadUserByUsername("obcy@example.test"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
