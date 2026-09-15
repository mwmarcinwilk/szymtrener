package pl.szymtrener.crm;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.settings.SettingsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Odpowiedz z panelu wychodzi jako multipart: tekst to dokladnie tresc z panelu,
 * HTML to formatka z escapowana trescia. Powrot do samego tekstu albo zamiana
 * argumentow setText nie przejdzie niezauwazona.
 */
class ReplySendTest {

    @Test
    @DisplayName("odpowiedź wychodzi jako tekst z panelu plus formatka HTML")
    void sendsMultipartReply() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        SettingsService settings = mock(SettingsService.class);
        when(settings.getBoolean(SettingsService.MAIL_ENABLED, true)).thenReturn(true);
        AppProperties props = new AppProperties("https://szymtrener.pl", "Szymtrener",
                new AppProperties.Mail("trener@szymtrener.pl", "kontakt@szymtrener.pl", true),
                null, null, null, null);

        MessageRepository repository = mock(MessageRepository.class);
        when(repository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        MessageService service = new MessageService(repository, mock(ReplyTemplateRepository.class),
                sender, props, settings, engine(), mock(TraineeRepository.class), mock(AttachmentService.class));

        String body = "Cześć Jan!\n\n<script>alert(1)</script>";
        AttachmentFile plan = new AttachmentFile("plan treningowy.pdf", "application/pdf", "%PDF-1.7 plan".getBytes());
        MessageService.SendResult result = service.sendEmail(1L, null, "jan@example.test", "Jan", body, List.of(plan));

        assertThat(result.sent()).isTrue();
        ArgumentCaptor<Message> saved = ArgumentCaptor.forClass(Message.class);
        verify(repository).save(saved.capture());
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(sent.capture());
        MimeMessage mime = sent.getValue();

        List<Part> leaves = new ArrayList<>();
        collect(mime, leaves);
        Part text = leaves.stream().filter(p -> isType(p, "text/plain")).findFirst().orElseThrow();
        Part pdf = leaves.stream().filter(p -> isType(p, "application/pdf")).findFirst().orElseThrow();
        assertThat(pdf.getFileName()).isEqualTo("plan treningowy.pdf");
        assertThat(pdf.getDisposition()).isEqualTo(Part.ATTACHMENT);
        Part html = leaves.stream().filter(p -> isType(p, "text/html")).findFirst().orElseThrow();

        assertThat(text.getContent()).isEqualTo(body);
        assertThat((String) html.getContent())
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>")
                .contains("Armii Krajowej 32a");
        // Po tym identyfikatorze odpowiedz klienta trafi do watku (In-Reply-To).
        assertThat(saved.getValue().getMailMessageId()).isEqualTo(mime.getMessageID())
                .matches("<[0-9a-f-]{36}@szymtrener\\.pl>");
    }

    private static boolean isType(Part part, String type) {
        try {
            return part.isMimeType(type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void collect(Part part, List<Part> leaves) throws Exception {
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) collect(multipart.getBodyPart(i), leaves);
        } else {
            leaves.add(part);
        }
    }

    private static SpringTemplateEngine engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        // SpringTemplateEngine, nie zwykly TemplateEngine: ten drugi liczy wyrazenia przez OGNL,
        // ktorego nie ma na sciezce klas.
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
