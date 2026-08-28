package pl.szymtrener.submission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.settings.SettingsService;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Jedna wiadomosc rano z zalegloscami.
 *
 * Handoff nazywa zapomniane zgloszenia glownym problemem panelu. Rozwiazaniem
 * nie jest kolejne powiadomienie przy kazdym zdarzeniu, tylko JEDNO zestawienie
 * dziennie — inaczej trener przestanie je czytac po tygodniu.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final SubmissionRepository submissions;
    private final JavaMailSender sender;
    private final AppProperties props;
    private final SettingsService settings;

    public ReminderScheduler(SubmissionRepository submissions, JavaMailSender sender,
                             AppProperties props, SettingsService settings) {
        this.submissions = submissions;
        this.sender = sender;
        this.props = props;
        this.settings = settings;
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "Europe/Warsaw")
    @Transactional(readOnly = true)
    public void sendDueReminders() {
        List<Submission> due = due();
        if (due.isEmpty()) return;

        if (!settings.getBoolean(SettingsService.MAIL_ENABLED, true)) {
            // Plakietka w menu i tak pokaze zaleglosci — poczta jest tu dodatkiem,
            // nie jedynym kanalem.
            log.info("Poczta wylaczona — {} przypomnien widocznych tylko w panelu", due.size());
            return;
        }
        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setFrom(props.mail().from());
            helper.setTo(settings.get(SettingsService.MAIL_RECIPIENT, props.mail().recipient()));
            helper.setSubject("Przypomnienie: " + due.size() + " zgłoszeń czeka na Ciebie");
            helper.setText(body(due), false);
            sender.send(mime);
            log.info("Wyslano przypomnienie o {} zgloszeniach", due.size());
        } catch (Exception e) {
            log.error("Nie udalo sie wyslac przypomnienia", e);
        }
    }

    /** Zgloszenia z terminem, ktory juz minal i ktorych trener nie odhaczyl. */
    @Transactional(readOnly = true)
    public List<Submission> due() {
        return submissions.findByRemindDoneFalseAndRemindAtLessThanEqual(Instant.now());
    }

    private String body(List<Submission> due) {
        StringBuilder sb = new StringBuilder("Zgłoszenia, do których chciałeś wrócić:\n\n");
        for (Submission s : due) {
            sb.append("• ").append(s.getName());
            if (s.getCity() != null) sb.append(" (").append(s.getCity()).append(')');
            sb.append(" — ").append(s.getStatus().label()).append('\n');
            sb.append("  ").append(props.absolute("/admin/zgloszenia/" + s.getId())).append("\n\n");
        }
        sb.append("Otwórz panel, żeby odpisać albo przesunąć termin.\n");
        return sb.toString();
    }
}
