package pl.szymtrener.crm;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.settings.SettingsService;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Watek rozmowy z klientem.
 *
 * Handoff, kryterium akceptacji: przycisk „Odpisz" NIGDY nie otwiera programu
 * pocztowego. Wiadomosc wychodzi stad przez JavaMail, a jej kopia zostaje
 * w watku — dzieki temu historia kontaktu jest w panelu, nie w czyjejs skrzynce.
 *
 * Wysylka jest tu SYNCHRONICZNA, inaczej niz powiadomienia o zgloszeniach:
 * trener stoi przy ekranie i musi od razu wiedziec, czy poszlo. Nieudana wysylka
 * zostaje w watku jako czerwona linia, a nie znika w logu.
 */
@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messages;
    private final ReplyTemplateRepository templates;
    private final JavaMailSender sender;
    private final AppProperties props;
    private final SettingsService settings;

    public MessageService(MessageRepository messages, ReplyTemplateRepository templates,
                          JavaMailSender sender, AppProperties props, SettingsService settings) {
        this.messages = messages;
        this.templates = templates;
        this.sender = sender;
        this.props = props;
        this.settings = settings;
    }

    /** Wynik wysylki: co pokazac trenerowi po kliknieciu „Wyślij". */
    public record SendResult(boolean sent, String error) {}

    @Transactional(readOnly = true)
    public List<Message> thread(Long submissionId) {
        return messages.findBySubmissionIdOrderBySentAtAsc(submissionId);
    }

    @Transactional(readOnly = true)
    public List<Message> traineeThread(Long traineeId) {
        return messages.findByTraineeIdOrderBySentAtAsc(traineeId);
    }

    @Transactional(readOnly = true)
    public List<ReplyTemplate> replyTemplates() {
        return templates.findAllByOrderBySortOrderAsc();
    }

    /**
     * Pierwsza pozycja watku: to, co klient wpisal w formularzu. Zapisujemy ja
     * przy zgloszeniu, zeby watek zaczynal sie od tresci, a nie od pustki.
     */
    @Transactional
    public void recordSubmission(Long submissionId, String body) {
        Message m = new Message();
        m.setSubmissionId(submissionId);
        m.setDirection(MessageDirection.IN);
        m.setChannel(MessageChannel.FORM);
        m.setBody(body);
        messages.save(m);
    }

    /**
     * Wysyla e-mail do klienta i zapisuje kopie w watku. Gdy wysylka sie nie uda,
     * wiadomosc i tak zostaje w watku ze statusem FAILED — trener widzi, ze
     * probowal, i moze ponowic. Cisza byla by tu gorsza niz czerwony wpis.
     */
    @Transactional
    public SendResult sendEmail(Long submissionId, Long traineeId, String to, String name,
                                String body, Long attachmentId) {
        Message m = new Message();
        m.setSubmissionId(submissionId);
        m.setTraineeId(traineeId);
        m.setDirection(MessageDirection.OUT);
        m.setChannel(MessageChannel.EMAIL);
        m.setBody(body);
        m.setAttachmentId(attachmentId);

        if (!mailEnabled()) {
            // Wylaczona poczta to decyzja w ustawieniach, nie awaria — ale trener
            // musi wiedziec, ze wiadomosc NIE wyszla do klienta.
            m.setMailStatus("FAILED");
            messages.save(m);
            return new SendResult(false, "Wysyłka e-mail jest wyłączona w Ustawieniach. Wiadomość zapisana w wątku, ale nie poszła do klienta.");
        }
        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(props.mail().from());
            helper.setTo(to);
            helper.setSubject("Wiadomość od Szymona Domagały");
            helper.setText(body, false);
            sender.send(mime);
            m.setMailStatus("SENT");
            messages.save(m);
            log.info("Wyslano wiadomosc do {} (zgloszenie {}, klient {})", to, submissionId, traineeId);
            return new SendResult(true, null);
        } catch (Exception e) {
            log.error("Nie udalo sie wyslac wiadomosci do {}", to, e);
            m.setMailStatus("FAILED");
            messages.save(m);
            system(submissionId, traineeId, "Wysyłka nie powiodła się: " + e.getMessage(), true);
            return new SendResult(false, "Nie udało się wysłać: " + e.getMessage());
        }
    }

    /** Zapis rozmowy telefonicznej. Trafia do watku i NIC nie wychodzi do klienta. */
    @Transactional
    public Message logPhoneCall(Long submissionId, Long traineeId, String body) {
        Message m = new Message();
        m.setSubmissionId(submissionId);
        m.setTraineeId(traineeId);
        m.setDirection(MessageDirection.OUT);
        m.setChannel(MessageChannel.PHONE);
        m.setBody(body);
        return messages.save(m);
    }

    /** Cienka linia w watku: zmiana etapu, nieudana wysylka. */
    @Transactional
    public void system(Long submissionId, Long traineeId, String body, boolean failure) {
        Message m = new Message();
        m.setSubmissionId(submissionId);
        m.setTraineeId(traineeId);
        m.setDirection(MessageDirection.OUT);
        m.setChannel(MessageChannel.SYSTEM);
        m.setBody(body);
        if (failure) m.setMailStatus("FAILED");
        messages.save(m);
    }

    /**
     * Podstawienia w szablonie. Robimy je po stronie serwera, przed wstawieniem
     * tekstu do pola — trener ma dostac gotowa wiadomosc, a nie klamry do recznego
     * wypelnienia. Nieuzupelnione miejsca zostaja widoczne jako {powod}, bo lepiej,
     * zeby rzucaly sie w oczy, niz zeby wyszly do klienta jako puste zdanie.
     */
    @Transactional(readOnly = true)
    public String fill(String code, String firstName, String context) {
        return templates.findByCode(code)
                .map(ReplyTemplate::getBody)
                .map(body -> body
                        .replace("{imie}", firstName == null ? "" : firstName)
                        // Kontekst wchodzi w srodek zdania zakonczonego kropka, wiec
                        // jego wlasna kropka dalaby „w domu..". Ucinamy ja przy wstawianiu.
                        .replace("{kontekst}", trimEnding(context)))
                .orElse("");
    }

    private static String trimEnding(String text) {
        if (text == null) return "";
        String trimmed = text.strip();
        while (trimmed.endsWith(".") || trimmed.endsWith("!")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).stripTrailing();
        }
        return trimmed;
    }

    /** Przenosi watek i notatki na klienta po konwersji zgloszenia. */
    @Transactional
    public void attachToTrainee(Long submissionId, Long traineeId) {
        messages.findBySubmissionIdOrderBySentAtAsc(submissionId).forEach(m -> {
            m.setTraineeId(traineeId);
            messages.save(m);
        });
    }

    private boolean mailEnabled() {
        if (!settings.getBoolean(SettingsService.MAIL_ENABLED, true)) return false;
        String from = props.mail().from();
        return from != null && !from.isBlank();
    }
}
