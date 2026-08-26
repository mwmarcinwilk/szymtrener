package pl.szymtrener.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.szymtrener.config.AppProperties;

/**
 * Zaklada konto administratora przy PIERWSZYM starcie na podstawie zmiennych
 * srodowiskowych ADMIN_EMAIL / ADMIN_PASSWORD. Hash nie trafia do repozytorium.
 *
 * Gdy konto juz istnieje, zmiana ADMIN_PASSWORD niczego nie robi — hasla zmienia sie
 * wylacznie w panelu (/admin/haslo), zeby restart z nieaktualna zmienna nie cofal
 * zmiany zrobionej przez uzytkownika. Bez ostrzezenia wyglada to jak zepsute logowanie,
 * dlatego rozjazd miedzy ADMIN_PASSWORD a baza trafia do logu.
 */
@Configuration
public class AdminAccountInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    @Bean
    ApplicationRunner createAdminIfMissing(AdminUserRepository repo, PasswordEncoder encoder, AppProperties props) {
        return args -> {
            String email = props.admin().email();
            String password = props.admin().password();
            AdminUser existing = repo.findByEmailIgnoreCase(email).orElse(null);
            if (existing != null) {
                if (password != null && !password.isBlank()
                        && !encoder.matches(password, existing.getPasswordHash())) {
                    log.warn("ADMIN_PASSWORD rozni sie od hasla konta {} — konto juz istnieje, "
                            + "wiec zmienna nie zmienia hasla. Zaloguj sie starym haslem i zmien je w /admin/haslo.",
                            email);
                }
                return;
            }
            if (password == null || password.isBlank()) {
                log.warn("Brak konta administratora. Ustaw ADMIN_PASSWORD i uruchom ponownie.");
                return;
            }
            AdminUser u = new AdminUser();
            u.setEmail(email);
            u.setPasswordHash(encoder.encode(password));
            u.setDisplayName("Szymon Domagała");
            repo.save(u);
            log.info("Utworzono konto administratora: {}", email);
        };
    }
}
