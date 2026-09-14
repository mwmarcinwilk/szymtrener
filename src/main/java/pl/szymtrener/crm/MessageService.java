package pl.szymtrener.crm;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.settings.SettingsService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\R\\s*\\R");
    private static final Pattern LINE_BREAK = Pattern.compile("\\R");
    private static final Pattern EXTRA_BLANK_LINES = Pattern.compile("\\n(?:\\h*+\\n){2,}+");
    /** Pusty {imie} zabiera ze soba spacje i przecinek przed soba: „Cześć, {imie}!" daje „Cześć!". */
    private static final Pattern EMPTY_NAME = Pattern.compile("(?<![ ,])[ ,]*+\\{imie\\}");

    private final MessageRepository messages;
    private final ReplyTemplateRepository templates;
    private final JavaMailSender sender;
    private final AppProperties props;
    private final SettingsService settings;
    private final TemplateEngine mailTemplates;

    public MessageService(MessageRepository messages, ReplyTemplateRepository templates,
                          JavaMailSender sender, AppProperties props, SettingsService settings,
                          TemplateEngine mailTemplates) {
        this.messages = messages;
        this.templates = templates;
        this.sender = sender;
        this.props = props;
        this.settings = settings;
        this.mailTemplates = mailTemplates;
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
            // Tekst dokladnie taki, jak w panelu; HTML to ta sama formatka co potwierdzenie zgloszenia.
            helper.setText(body, replyHtml(body));
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

    private String replyHtml(String body) {
        Context context = new Context(Locale.forLanguageTag("pl-PL"));
        context.setVariable("paragraphs", paragraphs(body));
        context.setVariable("siteUrl", props.siteUrl());
        context.setVariable("siteHost", props.siteHost());
        return mailTemplates.process("mail/reply", context);
    }

    /** Akapity rozdziela pusta linia, pojedynczy enter to zlamanie wiersza w akapicie. */
    static List<List<String>> paragraphs(String body) {
        return PARAGRAPH_BREAK.splitAsStream(body.strip())
                .map(para -> LINE_BREAK.splitAsStream(para).map(String::strip).toList())
                .toList();
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
                .map(body -> fillBody(body, firstName, context))
                .orElse("");
    }

    static String fillBody(String body, String firstName, String context) {
        String name = firstName == null ? "" : firstName.strip();
        String filled = name.isEmpty()
                ? EMPTY_NAME.matcher(body).replaceAll("")
                : body.replace("{imie}", name);
        // Kontekst wchodzi w srodek zdania zakonczonego kropka, wiec
        // jego wlasna kropka dalaby „w domu..". Ucinamy ja przy wstawianiu.
        String ctx = trimEnding(context);
        if (ctx.isEmpty()) filled = dropContextSentences(filled);
        return filled.replace("{kontekst}", ctx).strip();
    }

    /**
     * Pusty kontekst dalby „napisałeś: „”.", wiec znika ZDANIE z {kontekst}, nie caly akapit:
     * w szablonie poprawionym przez trenera to zdanie moze stac obok propozycji rozmowy.
     * Linia, w ktorej nic wiecej nie zostalo, znika razem z nadmiarowym odstepem.
     */
    private static String dropContextSentences(String body) {
        String joined = LINE_BREAK.splitAsStream(body)
                .map(line -> {
                    if (!line.contains("{kontekst}")) return line;
                    while (line.contains("{kontekst}")) line = withoutSentenceAt(line, line.indexOf("{kontekst}"));
                    return line.isBlank() ? null : line.stripTrailing();
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining("\n"));
        return EXTRA_BLANK_LINES.matcher(joined).replaceAll("\n\n");
    }

    /** Wycina zdanie wokol pozycji: od konca poprzedniego zdania do kropki, wykrzyknika albo pytajnika. */
    private static String withoutSentenceAt(String line, int at) {
        int start = 0;
        for (int i = at - 1; i > 0; i--) {
            if (Character.isWhitespace(line.charAt(i)) && isSentenceEnd(line.charAt(i - 1))) {
                start = i + 1;
                break;
            }
        }
        int end = at;
        while (end < line.length() && !isSentenceEnd(line.charAt(end))) end++;
        if (end < line.length()) end++;
        while (end < line.length() && Character.isWhitespace(line.charAt(end))) end++;
        return line.substring(0, start) + line.substring(end);
    }

    private static boolean isSentenceEnd(char c) {
        return c == '.' || c == '!' || c == '?';
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
