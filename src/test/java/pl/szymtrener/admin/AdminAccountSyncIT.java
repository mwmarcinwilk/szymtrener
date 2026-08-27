package pl.szymtrener.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.szymtrener.PostgresTestBase;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.settings.SettingsService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Konto administratora musi nadazac za zmiennymi srodowiskowymi — to na nich
 * opiera sie dostep do panelu po wdrozeniu. Jednoczesnie haslo zmienione w panelu
 * nie moze znikac po restarcie. Te dwa wymagania latwo pogodzic zle, stad test.
 */
class AdminAccountSyncIT extends PostgresTestBase {

    @Autowired AdminUserRepository accounts;
    @Autowired PasswordEncoder encoder;
    @Autowired SettingsService settings;
    @Autowired AppProperties props;

    /** Odpala ten sam runner, ktory dziala przy starcie aplikacji. */
    private void sync(String email, String password) throws Exception {
        AppProperties custom = new AppProperties(
                props.siteUrl(), props.brandName(), props.mail(),
                new AppProperties.Admin(email, password),
                props.media(), props.indexnow(), props.analytics());
        ApplicationRunner runner = new AdminAccountInitializer()
                .syncAdminAccount(accounts, encoder, custom, settings);
        runner.run((ApplicationArguments) null);
    }

    @BeforeEach
    void clean() {
        accounts.deleteAll();
        settings.set(AdminAccountInitializer.FINGERPRINT_KEY, "");
    }

    @Test
    @DisplayName("pierwszy start zakłada konto ze zmiennych")
    void createsAccountOnFirstRun() throws Exception {
        sync("trener@example.test", "PierwszeHaslo123");

        AdminUser account = accounts.findByEmailIgnoreCase("trener@example.test").orElseThrow();
        assertThat(encoder.matches("PierwszeHaslo123", account.getPasswordHash())).isTrue();
        assertThat(account.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("zmiana ADMIN_PASSWORD aktualizuje hasło istniejącego konta")
    void updatesPassword() throws Exception {
        sync("trener@example.test", "PierwszeHaslo123");
        sync("trener@example.test", "NoweHaslo456789");

        AdminUser account = accounts.findByEmailIgnoreCase("trener@example.test").orElseThrow();
        assertThat(encoder.matches("NoweHaslo456789", account.getPasswordHash())).isTrue();
        assertThat(encoder.matches("PierwszeHaslo123", account.getPasswordHash())).isFalse();
        assertThat(accounts.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("zmiana ADMIN_EMAIL zmienia adres, a nie zakłada drugiego konta")
    void renamesInsteadOfDuplicating() throws Exception {
        sync("stary@example.test", "PierwszeHaslo123");
        sync("nowy@example.test", "PierwszeHaslo123");

        assertThat(accounts.count()).isEqualTo(1);
        assertThat(accounts.findByEmailIgnoreCase("nowy@example.test")).isPresent();
        assertThat(accounts.findByEmailIgnoreCase("stary@example.test")).isEmpty();
    }

    @Test
    @DisplayName("hasło zmienione w panelu przeżywa restart z niezmienionymi zmiennymi")
    void panelPasswordSurvivesRestart() throws Exception {
        sync("trener@example.test", "PierwszeHaslo123");

        // to samo, co robi AdminController.changePassword()
        AdminUser account = accounts.findByEmailIgnoreCase("trener@example.test").orElseThrow();
        account.setPasswordHash(encoder.encode("ZmienioneWPanelu99"));
        accounts.save(account);

        sync("trener@example.test", "PierwszeHaslo123");   // restart, zmienne bez zmian

        AdminUser after = accounts.findByEmailIgnoreCase("trener@example.test").orElseThrow();
        assertThat(encoder.matches("ZmienioneWPanelu99", after.getPasswordHash())).isTrue();
    }
}
