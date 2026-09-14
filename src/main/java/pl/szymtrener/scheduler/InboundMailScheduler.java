package pl.szymtrener.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.szymtrener.crm.ImapMailbox;
import pl.szymtrener.crm.InboundMailService;
import pl.szymtrener.crm.InboxProperties;
import pl.szymtrener.crm.Mailbox;
import pl.szymtrener.settings.SettingsService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Co dwie minuty: odpowiedzi klientow ze skrzynki do watkow rozmowy.
 *
 * Login i haslo bierzemy z nadawcy poczty (JavaMailSenderImpl), a nie wprost ze zmiennych:
 * MailConfig czysci tam haslo aplikacji Google ze spacji, wiec odbior dziala na tym samym
 * hasle co wysylka. Blad odbioru nie wychodzi poza ten watek: aplikacja i healthcheck zyja dalej,
 * a wynik ostatniej proby widac w Ustawieniach.
 */
@Component
public class InboundMailScheduler {

    private static final Logger log = LoggerFactory.getLogger(InboundMailScheduler.class);

    private final InboxProperties inbox;
    private final InboundMailService service;
    private final ObjectProvider<JavaMailSenderImpl> sender;
    private final SettingsService settings;

    public InboundMailScheduler(InboxProperties inbox, InboundMailService service,
                                ObjectProvider<JavaMailSenderImpl> sender, SettingsService settings) {
        this.inbox = inbox;
        this.service = service;
        this.sender = sender;
        this.settings = settings;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT2M")
    public void poll() {
        if (!inbox.enabled()) return;
        JavaMailSenderImpl mail = sender.getIfAvailable();
        String user = mail == null ? null : mail.getUsername();
        String password = mail == null ? null : mail.getPassword();
        if (user == null || user.isBlank() || password == null || password.isBlank()) {
            status("brak MAIL_USER/MAIL_PASSWORD");
            return;
        }
        try (Mailbox mailbox = ImapMailbox.open(inbox.host(), inbox.port(), user, password)) {
            InboundMailService.Result result = service.poll(mailbox, user);
            status("OK, nowe odpowiedzi: " + result.recorded());
        } catch (Exception | StackOverflowError e) {
            log.warn("Odbior poczty z {}:{} nie powiodl sie: {}", inbox.host(), inbox.port(), e.getMessage());
            status("błąd: " + e.getClass().getSimpleName());
        }
    }

    private void status(String text) {
        settings.set(SettingsService.INBOX_LAST_CHECK, Instant.now().truncatedTo(ChronoUnit.SECONDS) + " " + text);
    }
}
