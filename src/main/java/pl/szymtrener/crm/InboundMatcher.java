package pl.szymtrener.crm;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Do ktorego watku nalezy odebrany mail.
 *
 * Najpierw naglowek odpowiedzi: In-Reply-To/References wskazuje nasz Message-ID, ktorego nie
 * da sie zgadnac. Dopiero gdy go brak (klient napisal nowego maila), adres nadawcy. Adres
 * w naglowku From da sie podrobic, dlatego wynik niesie informacje, czym dopasowano.
 */
@Component
public class InboundMatcher {

    /** Watek docelowy: zgloszenie, klient albo oba po konwersji. */
    public record Target(Long submissionId, Long traineeId, MatchedBy matchedBy) {}

    private final MessageRepository messages;
    private final TraineeRepository trainees;

    public InboundMatcher(MessageRepository messages, TraineeRepository trainees) {
        this.messages = messages;
        this.trainees = trainees;
    }

    @Transactional(readOnly = true)
    public Optional<Target> match(InboundMail mail) {
        return byReply(mail).or(() -> byTrainee(mail)).or(() -> bySubmission(mail));
    }

    private Optional<Target> byReply(InboundMail mail) {
        if (mail.replyTo().isEmpty()) return Optional.empty();
        return messages.findFirstByDirectionAndMailMessageIdIn(MessageDirection.OUT, mail.replyTo())
                .map(m -> new Target(m.getSubmissionId(), traineeOf(m.getTraineeId(), m.getSubmissionId()), MatchedBy.REPLY));
    }

    private Optional<Target> byTrainee(InboundMail mail) {
        if (mail.from() == null) return Optional.empty();
        return trainees.findFirstByEmailIgnoreCaseOrderByIdDesc(mail.from())
                .map(t -> new Target(t.getSubmissionId(), t.getId(), MatchedBy.ADDRESS));
    }

    private Optional<Target> bySubmission(InboundMail mail) {
        if (mail.from() == null) return Optional.empty();
        return messages.latestSubmissionIdByEmail(mail.from())
                .map(id -> new Target(id, traineeOf(null, id), MatchedBy.ADDRESS));
    }

    /**
     * Klient watku. Wiadomosc wyslana ze strony zgloszenia juz po konwersji nie ma trainee_id,
     * a odpowiedz na nia ma trafic takze do profilu klienta.
     */
    private Long traineeOf(Long traineeId, Long submissionId) {
        if (traineeId != null || submissionId == null) return traineeId;
        return trainees.findBySubmissionId(submissionId).map(Trainee::getId).orElse(null);
    }
}
