package pl.szymtrener.submission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Formatki mailowe renderuja sie dopiero przy prawdziwym zgloszeniu, wiec blad
 * w szablonie ujawnilby sie na produkcji jako „nie udalo sie wyslac" — i to
 * dopiero wtedy, gdy ktos faktycznie wypelni formularz. Ten test sprawdza je
 * przy kazdym buildzie.
 */
class MailTemplatesTest {

    private SpringTemplateEngine engine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        // SpringTemplateEngine, nie zwykly TemplateEngine: ten drugi liczy wyrazenia
        // przez OGNL, ktorego nie ma na sciezce klas — aplikacja uzywa SpEL-a.
        engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
    }

    private static Submission submission() {
        Submission s = new Submission();
        s.setType(SubmissionType.ONLINE);
        s.setName("Marta Kowalczyk");
        s.setEmail("marta@example.test");
        s.setPhone("601234567");
        s.setCity("Warszawa");
        s.setCurrentTraining("Ćwiczę sama dwa razy w tygodniu w domu.");
        s.setGoal("Chcę trenować regularnie i odzyskać siłę.");
        s.setEquipment("Hantle w domu");
        s.setInterest("Prowadzenie online");
        s.setSource("Instagram");
        return s;
    }

    private Context context(Submission s) {
        Context c = new Context(Locale.forLanguageTag("pl-PL"));
        c.setVariable("s", s);
        c.setVariable("panelUrl", "https://szymtrener.pl/admin/zgloszenia/1");
        c.setVariable("siteUrl", "https://szymtrener.pl");
        c.setVariable("siteHost", "szymtrener.pl");
        return c;
    }

    @Test
    @DisplayName("powiadomienie dla trenera renderuje się z kompletem danych zgłoszenia")
    void trainerNotificationRenders() {
        String html = engine.process("mail/notify-trainer", context(submission()));

        assertThat(html).contains("Marta Kowalczyk");
        assertThat(html).contains("marta@example.test");
        assertThat(html).contains("Prowadzenie online");
        assertThat(html).contains("Chcę trenować regularnie");
        assertThat(html).contains("https://szymtrener.pl/admin/zgloszenia/1");
        // styl musi byc w atrybutach: klienci pocztowi wycinaja <style> z naglowka
        assertThat(html).doesNotContain("<style");
    }

    @Test
    @DisplayName("puste pola są pomijane, nie pokazują się jako rząd myślników")
    void skipsEmptyFields() {
        Submission bare = new Submission();
        bare.setType(SubmissionType.CONTACT);
        bare.setName("Jan Nowak");
        bare.setEmail("jan@example.test");

        String html = engine.process("mail/notify-trainer", context(bare));

        assertThat(html).contains("Jan Nowak");
        assertThat(html).doesNotContain("Jak trenuje teraz");
        assertThat(html).doesNotContain("Co chce osiągnąć");
        assertThat(html).doesNotContain("Sprzęt");
    }

    @Test
    @DisplayName("potwierdzenie dla klienta zwraca się po imieniu i podaje telefon awaryjny")
    void clientConfirmationRenders() {
        String html = engine.process("mail/confirm-client", context(submission()));

        assertThat(html).contains("Marta Kowalczyk");
        assertThat(html).contains("odezwę się w ciągu 24 godzin");
        assertThat(html).contains("502 338 373");
        assertThat(html).contains("szymtrener.pl");
    }

    @Test
    @DisplayName("zgłoszenie bez pól opcjonalnych nie wywraca żadnej formatki")
    void bothTemplatesSurviveMinimalSubmission() {
        Submission bare = new Submission();
        bare.setType(SubmissionType.CONTACT);
        bare.setName("X");
        bare.setEmail("x@example.test");

        assertThatCode(() -> {
            engine.process("mail/notify-trainer", context(bare));
            engine.process("mail/confirm-client", context(bare));
        }).doesNotThrowAnyException();
    }
}
