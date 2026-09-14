package pl.szymtrener.crm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Do ktorego watku trafia odebrany mail. */
class InboundMatcherTest {

    private final MessageRepository messages = mock(MessageRepository.class);
    private final TraineeRepository trainees = mock(TraineeRepository.class);
    private final InboundMatcher matcher = new InboundMatcher(messages, trainees);

    private static InboundMail mail(List<String> replyTo, String from) {
        return new InboundMail("<x@example.test>", replyTo, from, false, Instant.now(), "Treść");
    }

    @Test
    @DisplayName("odpowiedź na wiadomość wysłaną ze zgłoszenia po konwersji trafia też do klienta")
    void replyToSubmissionMessageAfterConversion() {
        Message out = new Message();
        out.setSubmissionId(5L);
        when(messages.findFirstByDirectionAndMailMessageIdIn(eq(MessageDirection.OUT), any())).thenReturn(Optional.of(out));
        Trainee trainee = mock(Trainee.class);
        when(trainee.getId()).thenReturn(9L);
        when(trainees.findBySubmissionId(5L)).thenReturn(Optional.of(trainee));

        assertThat(matcher.match(mail(List.of("<panel@szymtrener.pl>"), "jan@example.test")))
                .contains(new InboundMatcher.Target(5L, 9L, MatchedBy.REPLY));
    }

    @Test
    @DisplayName("bez nagłówka odpowiedzi: najpierw klient, potem najnowsze zgłoszenie z adresu, obcy adres nigdzie")
    void addressFallback() {
        when(messages.latestSubmissionIdByEmail("lead@example.test")).thenReturn(Optional.of(3L));

        assertThat(matcher.match(mail(List.of(), "lead@example.test")))
                .contains(new InboundMatcher.Target(3L, null, MatchedBy.ADDRESS));
        assertThat(matcher.match(mail(List.of("<cudzy@example.test>"), "obcy@example.test"))).isEmpty();
        assertThat(matcher.match(mail(List.of(), null))).isEmpty();
    }
}
