package pl.szymtrener.submission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.settings.SettingsService;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;
    private final AppProperties props;
    private final SubmissionRepository repository;
    private final SettingsService settings;

    public MailService(JavaMailSender sender, AppProperties props, SubmissionRepository repository,
                       SettingsService settings) {
        this.sender = sender;
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

    private SimpleMailMessage trainerNotification(Submission s) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(props.mail().from());
        m.setTo(settings.get(SettingsService.MAIL_RECIPIENT, props.mail().recipient()));
        m.setReplyTo(s.getEmail());
        m.setSubject((s.getType() == SubmissionType.ONLINE ? "Zgłoszenie ONLINE – " : "Zapytanie ze strony – ") + s.getName());
        m.setText(body(s));
        return m;
    }

    private SimpleMailMessage autoReply(Submission s) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(props.mail().from());
        m.setTo(s.getEmail());
        m.setSubject("Dostałem Twoje zgłoszenie – Szymon Domagała");
        m.setText("""
                Cześć %s,

                dziękuję za wiadomość. Dostałem Twoje zgłoszenie i odezwę się w ciągu 24 godzin.

                Jeśli sprawa jest pilna, zadzwoń: 502 338 373.

                Pozdrawiam,
                Szymon Domagała
                Trener personalny, Trener Longevity
                szymtrener.pl
                """.formatted(s.getName()));
        return m;
    }

    private String body(Submission s) {
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
        sb.append("\nPodgląd w panelu: /admin/zgloszenia/").append(s.getId());
        return sb.toString();
    }

    private static void line(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) sb.append(label).append(": ").append(value).append('\n');
    }
}
