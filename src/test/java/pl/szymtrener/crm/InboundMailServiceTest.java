package pl.szymtrener.crm;

import jakarta.mail.FolderClosedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.settings.SettingsService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Postep odbioru po UID: nic nie przepada przy zerwanym polaczeniu, nic nie wraca dwa razy,
 * a zepsuty mail nie blokuje kolejnych.
 */
class InboundMailServiceTest {

    private static final InboundMatcher.Target TARGET = new InboundMatcher.Target(7L, null, MatchedBy.REPLY);

    private final Map<String, String> state = new HashMap<>();
    private MessageService messages;
    private InboundMatcher matcher;
    private InboundMailService service;

    @BeforeEach
    void setUp() {
        SettingsService settings = mock(SettingsService.class);
        when(settings.get(anyString(), any())).thenAnswer(inv -> state.getOrDefault(inv.getArgument(0), inv.getArgument(1)));
        when(settings.getLong(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenAnswer(inv -> {
            String value = state.get(inv.<String>getArgument(0));
            return value == null ? inv.<Long>getArgument(1) : Long.parseLong(value);
        });
        org.mockito.Mockito.doAnswer(inv -> state.put(inv.getArgument(0), inv.getArgument(1)))
                .when(settings).set(anyString(), anyString());
        messages = mock(MessageService.class);
        matcher = mock(InboundMatcher.class);
        when(matcher.match(any())).thenReturn(Optional.of(TARGET));
        AppProperties props = new AppProperties("https://szymtrener.pl", "Szymtrener",
                new AppProperties.Mail("trener@szymtrener.pl", "kontakt@szymtrener.pl", true), null, null, null, null);
        service = new InboundMailService(messages, matcher, settings, props);
    }

    @Test
    @DisplayName("pierwsze uruchomienie zaczyna od końca skrzynki i nie importuje historii")
    void firstRunStartsAtEnd() throws Exception {
        FakeMailbox box = new FakeMailbox(42, mail(1, "a@example.test"), mail(2, "a@example.test"));

        InboundMailService.Result result = service.poll(box, "kontakt@szymtrener.pl");

        assertThat(result.recorded()).isZero();
        assertThat(state).containsEntry(SettingsService.INBOX_LAST_UID, "2")
                .containsEntry(SettingsService.INBOX_UID_VALIDITY, "42");
        verify(messages, never()).recordInbound(any(), any(), any());
    }

    @Test
    @DisplayName("nowe wiadomości trafiają do wątku, postęp idzie za ostatnim UID, powtórka niczego nie dubluje")
    void recordsNewAndAdvances() throws Exception {
        state.put(SettingsService.INBOX_UID_VALIDITY, "42");
        state.put(SettingsService.INBOX_LAST_UID, "1");
        FakeMailbox box = new FakeMailbox(42, mail(1, "a@example.test"), mail(2, "a@example.test"), mail(3, "b@example.test"));

        InboundMailService.Result first = service.poll(box, "kontakt@szymtrener.pl");
        InboundMailService.Result second = service.poll(box, "kontakt@szymtrener.pl");

        assertThat(first.recorded()).isEqualTo(2);
        assertThat(state).containsEntry(SettingsService.INBOX_LAST_UID, "3");
        assertThat(second.fetched()).isZero();
        verify(messages, times(2)).recordInbound(eq(TARGET), any(), anyString());
    }

    @Test
    @DisplayName("własna wysyłka, autoresponder, znany Message-ID i brak dopasowania nie tworzą wpisu")
    void skipsWhatIsNotAReply() throws Exception {
        state.put(SettingsService.INBOX_UID_VALIDITY, "42");
        state.put(SettingsService.INBOX_LAST_UID, "0");
        MimeMessage own = mail(1, "Kontakt@Szymtrener.pl");
        MimeMessage auto = mail(2, "a@example.test");
        auto.setHeader("Auto-Submitted", "auto-replied");
        MimeMessage known = mail(3, "a@example.test");
        MimeMessage stranger = mail(4, "obcy@example.test");
        when(messages.alreadyRecorded("<m3@example.test>")).thenReturn(true);
        when(matcher.match(org.mockito.ArgumentMatchers.argThat(m -> m != null && "obcy@example.test".equals(m.from()))))
                .thenReturn(Optional.empty());

        InboundMailService.Result result = service.poll(new FakeMailbox(42, own, auto, known, stranger), "kontakt@szymtrener.pl");

        assertThat(result.recorded()).isZero();
        assertThat(result.skipped()).isEqualTo(4);
        assertThat(state).containsEntry(SettingsService.INBOX_LAST_UID, "4");
        verify(messages, never()).recordInbound(any(), any(), any());
    }

    @Test
    @DisplayName("zepsuta wiadomość jest pomijana, a następne dalej trafiają do wątku")
    void brokenMessageDoesNotBlock() throws Exception {
        state.put(SettingsService.INBOX_UID_VALIDITY, "42");
        state.put(SettingsService.INBOX_LAST_UID, "0");
        when(matcher.match(org.mockito.ArgumentMatchers.argThat(m -> m != null && "zepsuty@example.test".equals(m.from()))))
                .thenThrow(new IllegalStateException("zepsuty"));

        InboundMailService.Result result = service.poll(
                new FakeMailbox(42, mail(1, "zepsuty@example.test"), mail(2, "a@example.test")), "kontakt@szymtrener.pl");

        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.recorded()).isEqualTo(1);
        assertThat(state).containsEntry(SettingsService.INBOX_LAST_UID, "2");
    }

    @Test
    @DisplayName("zerwane połączenie przerywa odbiór bez przesuwania postępu")
    void closedFolderKeepsProgress() throws Exception {
        state.put(SettingsService.INBOX_UID_VALIDITY, "42");
        state.put(SettingsService.INBOX_LAST_UID, "0");
        when(matcher.match(any())).thenAnswer(inv -> {
            throw new FolderClosedException(null, "rozlaczono");
        });

        assertThatThrownBy(() -> service.poll(new FakeMailbox(42, mail(1, "a@example.test")), "kontakt@szymtrener.pl"))
                .isInstanceOf(FolderClosedException.class);
        assertThat(state).containsEntry(SettingsService.INBOX_LAST_UID, "0");
    }

    @Test
    @DisplayName("chwilowy błąd IMAP przy wiadomości nie przesuwa postępu; po trzech próbach wiadomość jest pomijana")
    void transientErrorRetriedThenSkipped() throws Exception {
        state.put(SettingsService.INBOX_UID_VALIDITY, "42");
        state.put(SettingsService.INBOX_LAST_UID, "0");
        when(matcher.match(org.mockito.ArgumentMatchers.argThat(m -> m != null && "wolny@example.test".equals(m.from()))))
                .thenAnswer(inv -> { throw new MessagingException("NO serwer zajety"); });
        FakeMailbox box = new FakeMailbox(42, mail(1, "wolny@example.test"), mail(2, "a@example.test"));

        for (int attempt = 1; attempt < InboundMailService.MAX_ATTEMPTS; attempt++) {
            assertThatThrownBy(() -> service.poll(box, "kontakt@szymtrener.pl")).isInstanceOf(MessagingException.class);
            assertThat(state).containsEntry(SettingsService.INBOX_LAST_UID, "0");
        }
        InboundMailService.Result last = service.poll(box, "kontakt@szymtrener.pl");

        assertThat(last.failed()).isEqualTo(1);
        assertThat(last.recorded()).isEqualTo(1);
        assertThat(state).containsEntry(SettingsService.INBOX_LAST_UID, "2");
    }

    @Test
    @DisplayName("bomba quoted-printable (StackOverflowError) jest pomijana i nie zatrzymuje odbioru")
    void qpBombDoesNotBlockInbox() throws Throwable {
        state.put(SettingsService.INBOX_UID_VALIDITY, "42");
        state.put(SettingsService.INBOX_LAST_UID, "0");
        String raw = "X-Test-Uid: 2\r\nMessage-ID: <bomba@example.test>\r\nFrom: zly@example.test\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: quoted-printable\r\n\r\n"
                + "=\r\n".repeat(10_000) + "x\r\n";
        jakarta.mail.internet.MimeMessage bomb = new jakarta.mail.internet.MimeMessage(
                jakarta.mail.Session.getInstance(new java.util.Properties()),
                new java.io.ByteArrayInputStream(raw.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        FakeMailbox box = new FakeMailbox(42, mail(1, "a@example.test"), bomb, mail(3, "b@example.test"));

        // Stos 1 MB jak w watku harmonogramu; na domyslnym stosie testu bomba moglaby sie nie wywrocic.
        InboundMailService.Result[] result = new InboundMailService.Result[1];
        Throwable[] error = new Throwable[1];
        Thread worker = new Thread(null, () -> {
            try {
                result[0] = service.poll(box, "kontakt@szymtrener.pl");
            } catch (Throwable t) {
                error[0] = t;
            }
        }, "odbior", 1024 * 1024);
        worker.start();
        worker.join();

        if (error[0] != null) throw error[0];
        assertThat(result[0].recorded()).isEqualTo(2);
        assertThat(result[0].failed()).isEqualTo(1);
        assertThat(state).containsEntry(SettingsService.INBOX_LAST_UID, "3");
    }

    @Test
    @DisplayName("zmiana UIDVALIDITY zaczyna od nowego końca skrzynki")
    void uidValidityChange() throws Exception {
        state.put(SettingsService.INBOX_UID_VALIDITY, "41");
        state.put(SettingsService.INBOX_LAST_UID, "900");

        service.poll(new FakeMailbox(42, mail(1, "a@example.test"), mail(5, "a@example.test")), "kontakt@szymtrener.pl");

        assertThat(state).containsEntry(SettingsService.INBOX_LAST_UID, "5").containsEntry(SettingsService.INBOX_UID_VALIDITY, "42");
        verify(messages, never()).recordInbound(any(), any(), any());
    }

    private static MimeMessage mail(long uid, String from) throws MessagingException {
        return FakeMailbox.mail(uid, from);
    }
}
