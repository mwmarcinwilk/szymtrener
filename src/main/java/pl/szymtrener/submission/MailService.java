package pl.szymtrener.submission;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.settings.SettingsService;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final Locale PL = Locale.forLanguageTag("pl-PL");

    private final JavaMailSender sender;
    private final TemplateEngine templates;
    private final AppProperties props;
    private final SubmissionRepository repository;
    private final SettingsService settings;

    public MailService(JavaMailSender sender, TemplateEngine templates, AppProperties props,
                       SubmissionRepository repository, SettingsService settings) {
        this.sender = sender;
        this.templates = templates;
        this.props = props;
        this.repository = repository;
        this.settings = settings;
    }

    /**
     * Poczta idzie po zapisie i asynchronicznie — zgloszenie jest juz w bazie,
     * wiec awaria SMTP nie oznacza utraty kontaktu, tylko wpis w mail_error.
     */
    @Async
    public void sendNotifications(Submission s) {
        if (!enabled()) {
            // Swiadomie NIE ustawiamy mailError: to nie awaria, tylko decyzja
            // w ustawieniach. Czerwony wpis na osi czasu bylby mylacy.
            log.info("Wysylka e-mail wylaczona w ustawieniach — zgloszenie {} zapisane bez powiadomien", s.getId());
            return;
        }
        try {
            if (settings.getBoolean(SettingsService.MAIL_NOTIFY, true)) {
                sender.send(trainerNotification(s));
            }
            if (settings.getBoolean(SettingsService.MAIL_AUTOREPLY, props.mail().autoReply())) {
                sender.send(autoReply(s));
            }
            s.setMailSent(true);
            s.setMailError(null);
        } catch (Exception e) {
            log.error("Nie udalo sie wyslac powiadomienia dla zgloszenia {}", s.getId(), e);
            s.setMailSent(false);
            s.setMailError(e.getMessage());
        }
        repository.save(s);
    }

    /**
     * Glowny wylacznik plus zabezpieczenie przed pusta konfiguracja: bez adresu
     * nadawcy JavaMail rzuca „Illegal address", co w logu wyglada jak awaria,
     * a jest zwyklym brakiem ustawien.
     */
    private boolean enabled() {
        if (!settings.getBoolean(SettingsService.MAIL_ENABLED, true)) return false;
        String from = props.mail().from();
        if (from == null || from.isBlank()) {
            log.warn("Brak adresu nadawcy (MAIL_FROM/MAIL_USER) — pomijam wysylke");
            return false;
        }
        return true;
    }

    /** Powiadomienie dla trenera. Reply-To na adres klienta: odpowiedz idzie prosto do niego. */
    private MimeMessage trainerNotification(Submission s) throws Exception {
        Context context = new Context(PL);
        context.setVariable("s", s);
        context.setVariable("panelUrl", props.absolute("/admin/zgloszenia/" + s.getId()));

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(props.mail().from());
        helper.setTo(settings.get(SettingsService.MAIL_RECIPIENT, props.mail().recipient()));
        helper.setReplyTo(s.getEmail());
        helper.setSubject((s.getType() == SubmissionType.ONLINE ? "Zgłoszenie ONLINE – " : "Zapytanie ze strony – ")
                          + s.getName());
        // Wersja tekstowa obok HTML-a: klienci bez HTML i filtry antyspamowe
        // traktuja wiadomosc wylacznie graficzna gorzej.
        helper.setText(plainTrainerNotification(s), templates.process("mail/notify-trainer", context));
        return message;
    }

    private MimeMessage autoReply(Submission s) throws Exception {
        Context context = new Context(PL);
        context.setVariable("s", s);
        context.setVariable("siteUrl", props.siteUrl());
        context.setVariable("siteHost", props.siteUrl().replaceFirst("^https?://", ""));

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(props.mail().from());
        helper.setTo(s.getEmail());
        helper.setSubject("Dostałem Twoje zgłoszenie – Szymon Domagała");
        helper.setText(plainAutoReply(s), templates.process("mail/confirm-client", context));
        return message;
    }

    private String plainAutoReply(Submission s) {
        return """
                Cześć %s,

                dziękuję za wiadomość. Dostałem Twoje zgłoszenie i odezwę się w ciągu 24 godzin.

                Jeśli sprawa jest pilna, zadzwoń: 502 338 373.

                Pozdrawiam,
                Szymon Domagała
                Trener personalny, Trener Longevity
                %s
                """.formatted(s.getName(), props.siteUrl());
    }

    private String plainTrainerNotification(Submission s) {
        StringBuilder sb = new StringBuilder();
        line(sb, "Imię", s.getName());
        line(sb, "E-mail", s.getEmail());
        line(sb, "Telefon", s.getPhone());
        line(sb, "Miejscowość", s.getCity());
        line(sb, "Trening teraz", s.getCurrentTraining());
        line(sb, "Cel", s.getGoal());
        line(sb, "Sprzęt", s.getEquipment());
        line(sb, "Zainteresowanie", s.getInterest());
        line(sb, "Skąd trafił", s.getSource());
        line(sb, "Wiadomość", s.getMessage());
        sb.append("\nZgoda RODO: ").append(s.getConsentAt());
        sb.append("\nPodgląd w panelu: ").append(props.absolute("/admin/zgloszenia/" + s.getId()));
        return sb.toString();
    }

    private static void line(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) sb.append(label).append(": ").append(value).append('\n');
    }
}
