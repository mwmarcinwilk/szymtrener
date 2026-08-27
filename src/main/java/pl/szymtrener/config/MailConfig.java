package pl.szymtrener.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Diagnostyka poczty przy starcie.
 *
 * Bez tego jedyna informacja przy bledzie logowania to „Authentication failed",
 * a pytanie „czy zmienne w ogole doszly, czy aplikacja loguje sie na inne konto"
 * zostaje bez odpowiedzi. Wypisujemy wiec, co REALNIE zostalo wczytane —
 * z zamaskowanym adresem i wylacznie DLUGOSCIA hasla, nigdy jego trescia.
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    /** Haslo aplikacji Google ma 16 znakow; Gmail pokazuje je w czterech grupach po cztery. */
    private static final int GOOGLE_APP_PASSWORD_LENGTH = 16;

    private final ObjectProvider<JavaMailSenderImpl> sender;
    private final String host;
    private final String port;
    private final String username;
    private final String password;

    public MailConfig(ObjectProvider<JavaMailSenderImpl> sender,
                      @Value("${spring.mail.host:}") String host,
                      @Value("${spring.mail.port:}") String port,
                      @Value("${spring.mail.username:}") String username,
                      @Value("${spring.mail.password:}") String password) {
        this.sender = sender;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @PostConstruct
    void reportConfiguration() {
        if (username.isBlank() || password.isBlank()) {
            log.warn("Poczta nieskonfigurowana: MAIL_USER {}, MAIL_PASSWORD {}. Zgloszenia beda zapisywane,"
                            + " ale bez powiadomien e-mail.",
                    username.isBlank() ? "puste" : "ustawione",
                    password.isBlank() ? "puste" : "ustawione");
            return;
        }

        log.info("Poczta: {}:{} jako {} (haslo: {} znakow)", host, port, mask(username), password.length());

        boolean gmail = host.contains("gmail.com");
        if (password.contains(" ")) {
            // Haslo aplikacji Google nigdy nie zawiera spacji — sa wylacznie separatorem
            // wizualnym w panelu Google. Usuwamy je, zeby wklejenie „jak widac" dzialalo,
            // ale mowimy o tym glosno, zeby przyczyna nie zostala ukryta.
            String cleaned = password.replace(" ", "");
            sender.ifAvailable(mail -> mail.setPassword(cleaned));
            log.warn("MAIL_PASSWORD zawieralo spacje ({} znakow) — usunalem je i uzywam {} znakow."
                    + " Google pokazuje haslo aplikacji w grupach po cztery, ale wpisuje sie je ciagiem.",
                    password.length(), cleaned.length());
        }
        if (gmail && !password.contains(" ") && password.length() != GOOGLE_APP_PASSWORD_LENGTH) {
            log.warn("MAIL_PASSWORD ma {} znakow, a haslo aplikacji Google ma dokladnie {}."
                    + " Zwykle haslo do konta Gmail nie zadziala — wygeneruj haslo aplikacji"
                    + " (Konto Google → Bezpieczenstwo → Hasla aplikacji).",
                    password.length(), GOOGLE_APP_PASSWORD_LENGTH);
        }
        if (gmail && !username.contains("@")) {
            log.error("MAIL_USER to '{}' — Gmail wymaga PELNEGO adresu z domena, np. jan.kowalski@gmail.com.",
                    mask(username));
        }
    }

    /** j***.k***@gmail.com — wystarczy, zeby rozpoznac konto, za malo zeby je wyciec. */
    private static String mask(String address) {
        int at = address.indexOf('@');
        if (at <= 1) return "***";
        return address.charAt(0) + "***" + address.substring(at);
    }
}
