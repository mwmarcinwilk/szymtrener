package pl.szymtrener.crm;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Odebrany mail na wpis w watku: tresc bez cytatu, bez HTML-a, z identyfikatorami odpowiedzi.
 * Wiadomosci budujemy w pamieci, tak jak wygladaja z Gmaila, Apple Mail i Outlooka.
 */
class InboundMailTest {

    private static final Instant NOW = Instant.parse("2026-09-14T19:00:00Z");

    private static MimeMessage message() throws Exception {
        MimeMessage m = new MimeMessage(Session.getInstance(new Properties()));
        m.setFrom(new InternetAddress("Jan Kowalski <Jan.Kowalski@Example.test>"));
        m.setSentDate(Date.from(NOW.minusSeconds(60)));
        return m;
    }

    @Nested
    class Text {

        @Test
        @DisplayName("odpowiedź z Gmaila po polsku traci cytat razem z nagłówkiem złamanym na dwie linie")
        void gmailPolishQuote() {
            String raw = """
                    Pasuje mi czwartek o 18.

                    Jan

                    W dniu pon., 14 wrz 2026 o 21:00 Szymon Domagała <kontakt@szymtrener.pl>
                    napisał(a):
                    > Cześć Jan!
                    > Pasuje Ci czwartek o 18:00?
                    """;

            assertThat(InboundMailText.clean(raw)).isEqualTo("Pasuje mi czwartek o 18.\n\nJan");
        }

        @Test
        @DisplayName("Apple Mail, Outlook i angielski Gmail: cytat ucięty od nagłówka")
        void otherClients() {
            assertThat(InboundMailText.clean("Tak.\n\nWiadomość napisana przez Szymon Domagała <k@s.pl> w dniu 14 wrz 2026, o godz. 21:00:\n\nCześć!"))
                    .isEqualTo("Tak.");
            assertThat(InboundMailText.clean("Yes.\r\n\r\nOn Mon, Sep 14, 2026 at 9:00 PM Szymon <k@s.pl> wrote:\r\n> Hi"))
                    .isEqualTo("Yes.");
            assertThat(InboundMailText.clean("Dobrze.\n\nOd: Szymon Domagała <k@s.pl>\nWysłano: poniedziałek\nTemat: Wiadomość"))
                    .isEqualTo("Dobrze.");
        }

        @Test
        @DisplayName("samo „Od:” w zdaniu klienta nie ucina wiadomości")
        void plainOdIsNotAQuote() {
            assertThat(InboundMailText.clean("Od: jutra mogę ćwiczyć rano.\nDzięki!"))
                    .isEqualTo("Od: jutra mogę ćwiczyć rano.\nDzięki!");
        }

        @Test
        @DisplayName("odpowiedź w tekście zostaje bez linii cytatu, a sam cytat nie daje pustego wpisu")
        void inlineAndOnlyQuote() {
            assertThat(InboundMailText.clean("> Pasuje czwartek?\nTak\n> A sprzęt?\nHantle")).isEqualTo("Tak\nHantle");
            assertThat(InboundMailText.clean("> tylko cytat")).isEqualTo("> tylko cytat");
        }

        @Test
        @DisplayName("bardzo długa treść jest skracana z dopiskiem")
        void limitsLength() {
            String body = InboundMailText.clean("a".repeat(InboundMailText.MAX_LENGTH + 500));

            assertThat(body).hasSizeLessThan(InboundMailText.MAX_LENGTH + 100).endsWith("wiadomość skrócona w panelu");
        }

        @Test
        @DisplayName("HTML zamienia się na tekst z akapitami, bez skryptów, stylów i cytatu Gmaila")
        void htmlToText() {
            String html = """
                    <html><head><style>p{color:red}</style></head><body>
                    <div>Cześć Szymon,</div><div><br></div><div>pasuje <b>czwartek</b>.<script>alert(1)</script></div>
                    <img src=x onerror="alert(2)">
                    <div class="gmail_quote">W dniu … napisał(a):<blockquote>stara wiadomość</blockquote></div>
                    </body></html>""";

            String text = InboundMailText.clean(InboundMailText.htmlToText(html));

            assertThat(text).isEqualTo("Cześć Szymon,\n\npasuje czwartek.");
        }
    }

    @Nested
    class Parse {

        @Test
        @DisplayName("tekst, nadawca małymi literami, identyfikatory odpowiedzi od najbliższej")
        void plainReply() throws Exception {
            MimeMessage m = message();
            m.setHeader("Message-ID", "<odp-1@example.test>");
            m.setHeader("In-Reply-To", "<nasza-2@szymtrener.pl>");
            m.setHeader("References", "<nasza-1@szymtrener.pl> <nasza-2@szymtrener.pl> <cudza@example.test>");
            m.setText("Pasuje.\n\nOn Mon, Sep 14, 2026 Szymon wrote:\n> Hej", "UTF-8");
            m.saveChanges();
            m.setHeader("Message-ID", "<odp-1@example.test>");

            InboundMail mail = InboundMail.parse(m, NOW);

            assertThat(mail.messageId()).isEqualTo("<odp-1@example.test>");
            assertThat(mail.from()).isEqualTo("jan.kowalski@example.test");
            assertThat(mail.replyTo()).containsExactly("<nasza-2@szymtrener.pl>", "<cudza@example.test>", "<nasza-1@szymtrener.pl>");
            assertThat(mail.body()).isEqualTo("Pasuje.");
            assertThat(mail.automatic()).isFalse();
            assertThat(mail.sentAt()).isEqualTo(NOW.minusSeconds(60));
        }

        @Test
        @DisplayName("multipart: tekst ma pierwszeństwo przed HTML, załączniki są liczone, nie czytane")
        void multipartWithAttachment() throws Exception {
            MimeBodyPart plain = new MimeBodyPart();
            plain.setText("Wysyłam wyniki.", "UTF-8");
            MimeBodyPart html = new MimeBodyPart();
            html.setContent("<p>Wysyłam <b>wyniki</b>.</p>", "text/html; charset=UTF-8");
            MimeMultipart alternative = new MimeMultipart("alternative", plain, html);
            MimeBodyPart body = new MimeBodyPart();
            body.setContent(alternative);
            MimeBodyPart pdf = new MimeBodyPart();
            pdf.setContent(new byte[]{1, 2, 3}, "application/pdf");
            pdf.setFileName("wyniki.pdf");
            MimeMessage m = message();
            m.setContent(new MimeMultipart("mixed", body, pdf));
            m.saveChanges();

            InboundMail mail = InboundMail.parse(m, NOW);

            assertThat(mail.body()).isEqualTo("Wysyłam wyniki.\n\n[pominięto załączniki: 1]");
        }

        @Test
        @DisplayName("sam HTML: treść jako tekst, znaczniki nie przechodzą")
        void htmlOnly() throws Exception {
            MimeMessage m = message();
            m.setContent("<div>Dzięki!</div><img src=x onerror=alert(1)><script>x()</script>", "text/html; charset=UTF-8");
            m.saveChanges();

            assertThat(InboundMail.parse(m, NOW).body()).isEqualTo("Dzięki!");
        }

        @Test
        @DisplayName("autoresponder i lista mailingowa są rozpoznane, data z przyszłości zamieniona na teraz")
        void automaticAndFutureDate() throws Exception {
            MimeMessage auto = message();
            auto.setHeader("Auto-Submitted", "auto-replied");
            auto.setText("Jestem na urlopie.");
            auto.setSentDate(Date.from(NOW.plusSeconds(86_400)));
            auto.saveChanges();
            MimeMessage list = message();
            list.setHeader("List-Id", "<newsletter.example.test>");
            list.setText("Promocja");
            list.saveChanges();
            MimeMessage human = message();
            human.setHeader("Auto-Submitted", "no");
            human.setText("Hej");
            human.saveChanges();

            InboundMail autoMail = InboundMail.parse(auto, NOW);
            assertThat(autoMail.automatic()).isTrue();
            assertThat(autoMail.sentAt()).isEqualTo(NOW);
            assertThat(InboundMail.parse(list, NOW).automatic()).isTrue();
            assertThat(InboundMail.parse(human, NOW).automatic()).isFalse();
        }

        @Test
        @DisplayName("toString nie wypisuje treści ani pełnego adresu")
        void toStringHidesContent() throws Exception {
            MimeMessage m = message();
            m.setText("Tajne dane zdrowotne");
            m.saveChanges();

            String text = InboundMail.parse(m, NOW).toString();

            assertThat(text).doesNotContain("Tajne").doesNotContain("jan.kowalski@");
        }
    }
}
