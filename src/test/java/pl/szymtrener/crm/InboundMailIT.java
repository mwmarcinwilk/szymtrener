package pl.szymtrener.crm;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import pl.szymtrener.PostgresTestBase;
import pl.szymtrener.settings.SettingsService;
import pl.szymtrener.submission.Submission;
import pl.szymtrener.submission.SubmissionRepository;
import pl.szymtrener.submission.SubmissionType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Odbior na prawdziwej bazie: V11, dopasowanie po naglowku i po adresie, znacznik
 * „nowa odpowiedz" i unikalny Message-ID.
 */
class InboundMailIT extends PostgresTestBase {

    @Autowired InboundMailService inbound;
    @Autowired MessageService messages;
    @Autowired MessageRepository repository;
    @Autowired SubmissionRepository submissions;
    @Autowired SettingsService settings;

    @Test
    @DisplayName("odpowiedź na mail z panelu i nowy mail z adresu zgłoszenia trafiają do wątku jako nieprzeczytane")
    void repliesLandInThread() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Submission s = new Submission();
        s.setType(SubmissionType.CONTACT);
        s.setName("Jan Kowalski");
        s.setEmail("jan." + suffix + "@example.test");
        s = submissions.save(s);

        Message out = new Message();
        out.setSubmissionId(s.getId());
        out.setDirection(MessageDirection.OUT);
        out.setChannel(MessageChannel.EMAIL);
        out.setBody("Cześć Jan!");
        out.setMailStatus("SENT");
        out.setMailMessageId("<panel-" + suffix + "@szymtrener.pl>");
        repository.save(out);

        MimeMessage reply = FakeMailbox.mail(11, "obcy-adres-" + suffix + "@example.test");
        reply.setHeader("Message-ID", "<reply-" + suffix + "@example.test>");
        reply.setHeader("In-Reply-To", "<panel-" + suffix + "@szymtrener.pl>");
        MimeMessage fresh = FakeMailbox.mail(12, s.getEmail().toUpperCase());
        fresh.setHeader("Message-ID", "<fresh-" + suffix + "@example.test>");

        settings.set(SettingsService.INBOX_UID_VALIDITY, "777");
        settings.set(SettingsService.INBOX_LAST_UID, "10");
        InboundMailService.Result result = inbound.poll(new FakeMailbox(777, reply, fresh), "kontakt@szymtrener.pl");

        assertThat(result.recorded()).isEqualTo(2);
        List<Message> thread = messages.thread(s.getId());
        assertThat(thread).filteredOn(m -> m.getDirection() == MessageDirection.IN)
                .extracting(Message::getMatchedBy, Message::isUnread)
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple(MatchedBy.REPLY, true),
                        org.assertj.core.groups.Tuple.tuple(MatchedBy.ADDRESS, true));
        assertThat(messages.submissionsWithUnread(List.of(s.getId()))).containsExactly(s.getId());
        assertThat(repository.countSubmissionsWithUnread()).isGreaterThanOrEqualTo(1);

        // Otwarcie watku zwraca stan sprzed otwarcia (widok pokazuje „nowa") i gasi znacznik.
        assertThat(messages.openSubmissionThread(s.getId())).anyMatch(Message::isUnread);
        assertThat(messages.submissionsWithUnread(List.of(s.getId()))).isEmpty();

        // Ponowny odbior tych samych wiadomosci (np. po utracie stanu) nie dubluje wpisow.
        settings.set(SettingsService.INBOX_LAST_UID, "10");
        assertThat(inbound.poll(new FakeMailbox(777, reply, fresh), "kontakt@szymtrener.pl").recorded()).isZero();
        assertThat(messages.thread(s.getId())).filteredOn(m -> m.getDirection() == MessageDirection.IN).hasSize(2);
    }

    @Test
    @DisplayName("baza nie przyjmie drugiej wiadomości z tym samym Message-ID")
    void uniqueMessageId() {
        String id = "<dup-" + System.nanoTime() + "@example.test>";
        Submission s = new Submission();
        s.setType(SubmissionType.CONTACT);
        s.setName("Ewa");
        s.setEmail("ewa@example.test");
        Long submissionId = submissions.save(s).getId();

        repository.saveAndFlush(inboundMessage(submissionId, id));

        assertThatThrownBy(() -> repository.saveAndFlush(inboundMessage(submissionId, id)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Message inboundMessage(Long submissionId, String id) {
        Message m = new Message();
        m.setSubmissionId(submissionId);
        m.setDirection(MessageDirection.IN);
        m.setChannel(MessageChannel.EMAIL);
        m.setBody("x");
        m.setMailMessageId(id);
        return m;
    }
}
