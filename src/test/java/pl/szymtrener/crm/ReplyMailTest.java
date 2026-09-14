package pl.szymtrener.crm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Odpowiedz z panelu: podstawienia w szablonie i formatka HTML, w ktorej wychodzi.
 * Tresc pisze trener, wiec formatka musi ja escapowac.
 */
class ReplyMailTest {

    private static final String FIRST = """
            Cześć {imie}!

            Dzięki za zgłoszenie.

            Napisałeś o swoim treningu: „{kontekst}”.

            Pasuje Ci czwartek o 18:00?

            Pozdrawiam,
            Szymon""";

    @Test
    @DisplayName("kontekst trafia do zdania bez własnej kropki na końcu")
    void fillsContext() {
        String body = MessageService.fillBody(FIRST, "Marta", "Ćwiczę sama w domu.");

        assertThat(body).startsWith("Cześć Marta!\n\nDzięki za zgłoszenie.");
        assertThat(body).contains("Napisałeś o swoim treningu: „Ćwiczę sama w domu”.");
    }

    @Test
    @DisplayName("pusty kontekst wycina całą linię razem z odstępem")
    void dropsLineWithEmptyContext() {
        String body = MessageService.fillBody(FIRST, "Jan", "  ");

        assertThat(body).isEqualTo("""
                Cześć Jan!

                Dzięki za zgłoszenie.

                Pasuje Ci czwartek o 18:00?

                Pozdrawiam,
                Szymon""");
    }

    @Test
    @DisplayName("pusty kontekst w akapicie z innymi zdaniami zabiera tylko swoje zdanie")
    void dropsOnlyContextSentence() {
        // Uklad z V8, ktory zostaje w szablonie poprawionym przez trenera (V10 go nie rusza).
        String v8 = "Cześć {imie}!\n\nPrzeczytałem, co napisałeś — {kontekst}. Proponuję 20 minut rozmowy. Bez zobowiązań.\n\nPozdrawiam,\nSzymon";

        assertThat(MessageService.fillBody(v8, "Jan", null))
                .isEqualTo("Cześć Jan!\n\nProponuję 20 minut rozmowy. Bez zobowiązań.\n\nPozdrawiam,\nSzymon");
        assertThat(MessageService.fillBody("Dzięki. Napisałeś: „{kontekst}”.\nProponuję rozmowę.", "Jan", ""))
                .isEqualTo("Dzięki.\nProponuję rozmowę.");
        assertThat(MessageService.fillBody("Cześć!\nNapisałeś: „{kontekst}”.\nDo usłyszenia.", "Jan", " "))
                .isEqualTo("Cześć!\nDo usłyszenia.");
    }

    @Test
    @DisplayName("bez imienia powitanie nie ma spacji ani przecinka przed wykrzyknikiem, ręczne pola zostają")
    void noNameAndManualPlaceholders() {
        assertThat(MessageService.fillBody("Cześć {imie}!\n\nPowód: {powod}.", null, null))
                .isEqualTo("Cześć!\n\nPowód: {powod}.");
        assertThat(MessageService.fillBody("Cześć, {imie}!", " ", null)).isEqualTo("Cześć!");
        assertThat(MessageService.fillBody("A\n\n\n\n \nB {kontekst}.\n\n\nC", "Jan", null)).isEqualTo("A\n\nC");
    }

    @Test
    @DisplayName("akapity dzieli pusta linia, pojedynczy enter zostaje w akapicie")
    void splitsParagraphs() {
        assertThat(MessageService.paragraphs("A\r\n\r\nB\nC\n \nD\n"))
                .isEqualTo(List.of(List.of("A"), List.of("B", "C"), List.of("D")));
    }

    @Test
    @DisplayName("formatka odpowiedzi escapuje treść i łamie wiersze")
    void rendersEscapedReply() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        Context c = new Context(Locale.forLanguageTag("pl-PL"));
        c.setVariable("paragraphs", MessageService.paragraphs("Cześć Jan!\n\n<script>alert(1)</script>\nPozdrawiam,\nSzymon"));
        c.setVariable("siteUrl", "https://szymtrener.pl");
        c.setVariable("siteHost", "szymtrener.pl");
        String html = engine.process("mail/reply", c);

        assertThat(html).contains("Cześć Jan!");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;<br>Pozdrawiam,<br>Szymon");
        assertThat(html).contains("Armii Krajowej 32a");
        assertThat(html).doesNotContain("<style");
    }
}
