package pl.szymtrener.crm;

import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import pl.szymtrener.PostgresTestBase;
import pl.szymtrener.settings.SettingsService;
import pl.szymtrener.submission.Submission;
import pl.szymtrener.submission.SubmissionRepository;
import pl.szymtrener.submission.SubmissionType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Załącznik od klienta: zapis przy odbiorze, pobranie tylko po zalogowaniu, kasowanie razem ze zgłoszeniem. */
@AutoConfigureMockMvc
class AttachmentIT extends PostgresTestBase {

    @Autowired InboundMailService inbound;
    @Autowired MessageService messages;
    @Autowired SubmissionRepository submissions;
    @Autowired SettingsService settings;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test
    @DisplayName("formularz odpowiedzi: PDF zapisany przy wiadomości, exe udający PDF odrzucony bez wpisu w wątku")
    void replyFormWithFiles() throws Exception {
        Submission s = new Submission();
        s.setType(SubmissionType.CONTACT);
        s.setName("Ewa");
        s.setEmail("ewa." + System.nanoTime() + "@example.test");
        s = submissions.save(s);
        String url = "/admin/zgloszenia/" + s.getId() + "/wiadomosc";
        var admin = user("admin@example.com").roles("ADMIN");

        mvc.perform(multipart(url)
                        .file(new org.springframework.mock.web.MockMultipartFile("pliki", "faktura.pdf", "application/pdf", new byte[]{'M', 'Z', 0}))
                        .param("body", "Plik").param("way", "mail").with(csrf()).with(admin))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("niedozwolony typ")));
        assertThat(messages.openSubmissionThread(s.getId())).isEmpty();

        // Poczta w profilu testowym nie ma serwera, więc wysyłka kończy się FAILED, ale plik zostaje przy wiadomości.
        mvc.perform(multipart(url)
                        .file(new org.springframework.mock.web.MockMultipartFile("pliki", "plan.pdf", "application/pdf", "%PDF-1.7 plan".getBytes()))
                        .param("body", "Plan w załączniku").param("way", "mail").with(csrf()).with(admin))
                .andExpect(status().is3xxRedirection());
        List<Message> thread = messages.openSubmissionThread(s.getId());
        assertThat(messages.attachmentsOf(thread).values()).flatExtracting(v -> v)
                .extracting(a -> ((MessageAttachment) a).getOriginalName()).contains("plan.pdf");
    }

    @Test
    @DisplayName("PDF z odpowiedzi klienta: w wątku, do pobrania tylko dla admina, znika po usunięciu zgłoszenia")
    void attachmentLifecycle() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Submission s = new Submission();
        s.setType(SubmissionType.CONTACT);
        s.setName("Ola");
        s.setEmail("ola." + suffix + "@example.test");
        s = submissions.save(s);

        MimeBodyPart text = new MimeBodyPart();
        text.setText("Wyniki w załączniku.", "UTF-8");
        MimeBodyPart pdf = new MimeBodyPart();
        pdf.setDataHandler(new jakarta.activation.DataHandler(
                new jakarta.mail.util.ByteArrayDataSource("%PDF-1.7 morfologia".getBytes(), "application/pdf")));
        pdf.setFileName("morfologia.pdf");
        pdf.setDisposition("attachment");
        MimeMessage mail = FakeMailbox.mail(21, s.getEmail());
        mail.setContent(new MimeMultipart("mixed", text, pdf));
        mail.saveChanges();
        mail.setHeader("Message-ID", "<att-" + suffix + "@example.test>");
        mail.setHeader("X-Test-Uid", "21");

        settings.set(SettingsService.INBOX_UID_VALIDITY, "901");
        settings.set(SettingsService.INBOX_LAST_UID, "20");
        assertThat(inbound.poll(new FakeMailbox(901, mail), "kontakt@szymtrener.pl").recorded()).isEqualTo(1);

        List<Message> thread = messages.openSubmissionThread(s.getId());
        Map<Long, List<MessageAttachment>> files = messages.attachmentsOf(thread);
        MessageAttachment stored = files.values().stream().flatMap(List::stream).findFirst().orElseThrow();
        assertThat(stored.getMimeType()).isEqualTo("application/pdf");

        String url = "/admin/zalaczniki/" + stored.getId();
        mvc.perform(get(url)).andExpect(status().is3xxRedirection());
        mvc.perform(get(url).with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes("%PDF-1.7 morfologia".getBytes()));
        mvc.perform(get(url + "/podglad").with(user("admin@example.com").roles("ADMIN"))).andExpect(status().isNotFound());
        // Załączniki nie trafiają do publicznej biblioteki mediów.
        assertThat(jdbc.queryForObject("select count(*) from media_file where original_name = 'morfologia.pdf'", Integer.class)).isZero();

        submissions.deleteById(s.getId());
        assertThat(jdbc.queryForObject("select count(*) from message_attachment where id = ?", Integer.class, stored.getId())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from message_attachment_blob where attachment_id = ?", Integer.class, stored.getId())).isZero();
    }
}
