package pl.szymtrener.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.szymtrener.config.AppProperties;

/**
 * Logowanie do panelu.
 *
 * ZASADA: dane ze zmiennych srodowiskowych ADMIN_EMAIL i ADMIN_PASSWORD maja
 * BEZWARUNKOWE pierwszenstwo. Sprawdzamy je PRZED baza, przy kazdej probie
 * logowania — nie przy starcie, nie po synchronizacji, nie zaleznie od tego,
 * co akurat lezy w tabeli admin_user.
 *
 * Powod jest praktyczny: wczesniejsze podejscie synchronizowalo konto raz, przy
 * starcie aplikacji, i wystarczyla jedna nieoczekiwana sytuacja (pusta zmienna,
 * zdazony restart, zostawione stare konto), zeby wlasciciel stracil dostep do
 * wlasnego panelu. Tutaj zmienna dziala zawsze — wystarczy ja ustawic i wejsc.
 *
 * Konto w bazie zostaje: obsluguje zmiane hasla w panelu i konta zalozone recznie.
 * Gdy ADMIN_PASSWORD jest ustawione, logowanie nim dziala ROWNOLEGLE do hasla
 * z bazy — swiadomie, bo to jest wlasnie „awaryjne wejscie z env".
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserDetailsService.class);

    private final AdminUserRepository repository;
    private final AppProperties props;
    private final PasswordEncoder encoder;

    public AdminUserDetailsService(AdminUserRepository repository, AppProperties props, PasswordEncoder encoder) {
        this.repository = repository;
        this.props = props;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String login = trim(username);

        if (matchesEnvironment(login)) {
            log.info("Logowanie danymi ze zmiennych srodowiskowych: {}", login);
            return User.withUsername(login)
                    .password(encoder.encode(props.admin().password()))
                    .roles("ADMIN")
                    .build();
        }

        AdminUser account = repository.findByEmailIgnoreCase(login)
                .orElseThrow(() -> new UsernameNotFoundException("Nie ma konta: " + login));
        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .roles(account.getRole())
                .disabled(!account.isEnabled())
                .build();
    }

    /** Login zgadza sie z ADMIN_EMAIL, a ADMIN_PASSWORD jest ustawione. */
    private boolean matchesEnvironment(String login) {
        String email = trim(props.admin().email());
        String password = props.admin().password();
        return !email.isBlank()
                && email.equalsIgnoreCase(login)
                && password != null
                && !password.isBlank();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
