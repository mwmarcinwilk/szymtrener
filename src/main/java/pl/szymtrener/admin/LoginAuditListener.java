package pl.szymtrener.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Zapis prob logowania do panelu.
 *
 * Spring Security domyslnie nie loguje ani udanych, ani nieudanych logowan —
 * przy zgloszeniu „nie moge sie zalogowac" w logu nie ma NICZEGO, wiec nie da sie
 * odroznic zlego hasla od nieistniejacego konta ani sprawdzic, czy zadanie w ogole
 * doszlo do aplikacji.
 *
 * Rozroznienie „nie ma takiego konta" kontra „zle haslo" jest tu celowe i bezpieczne:
 * to log serwera, nie odpowiedz dla przegladarki. Uzytkownik na ekranie dalej widzi
 * jeden ogolny komunikat, wiec nie da sie tym wyliczyc istniejacych kont.
 */
@Component
public class LoginAuditListener {

    private static final Logger log = LoggerFactory.getLogger(LoginAuditListener.class);

    private final AdminUserRepository accounts;

    public LoginAuditListener(AdminUserRepository accounts) {
        this.accounts = accounts;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        log.info("Zalogowano do panelu: {}", event.getAuthentication().getName());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = String.valueOf(event.getAuthentication().getName());
        boolean exists = accounts.findByEmailIgnoreCase(username).isPresent();

        if (exists) {
            log.warn("Nieudane logowanie: konto {} ISTNIEJE, ale haslo sie nie zgadza."
                    + " Sprawdz ADMIN_PASSWORD (spacje, cudzyslowy) albo zmien haslo w /admin/haslo.", username);
        } else {
            log.warn("Nieudane logowanie: nie ma konta o adresie {}. W bazie sa: {}."
                    + " Jesli to nie ten adres, sprawdz ADMIN_EMAIL i zrestartuj aplikacje.",
                    username, existingAddresses());
        }
    }

    /** Same adresy, bez hasel — pozwala od razu zobaczyc, czy ADMIN_EMAIL zadzialal. */
    private String existingAddresses() {
        var all = accounts.findAll();
        if (all.isEmpty()) return "BRAK KONT — ustaw ADMIN_EMAIL i ADMIN_PASSWORD";
        return all.stream().map(AdminUser::getEmail).reduce((a, b) -> a + ", " + b).orElse("");
    }
}
