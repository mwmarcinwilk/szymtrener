package pl.szymtrener.seo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.szymtrener.content.Post;
import pl.szymtrener.content.PostFaq;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ocena musi byc powtarzalna i monotoniczna: poprawienie tekstu nie moze obnizyc
 * wyniku. Inaczej autor przestaje jej ufac i przestaje z niej korzystac.
 */
class AiReadinessAnalyzerTest {

    private final AiReadinessAnalyzer analyzer = new AiReadinessAnalyzer();

    // ─── pomocnicze ─────────────────────────────────────────────────

    private static Post post(String lead, String html) {
        Post p = new Post();
        p.setTitle("Tytuł testowy");
        p.setLead(lead);
        p.setContentHtml(html);
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private static String lead(int words) {
        return ("słowo ".repeat(words)).trim();
    }

    @Test
    @DisplayName("ta sama treść daje zawsze ten sam wynik")
    void isDeterministic() {
        Post p = post(lead(50), "<h2>Nagłówek</h2><p>Treść akapitu z liczbą 30%.</p><ul><li>punkt</li></ul>");

        AiReadinessAnalyzer.Report first = analyzer.analyse(p);
        AiReadinessAnalyzer.Report second = analyzer.analyse(p);

        assertThat(first.score()).isEqualTo(second.score());
        assertThat(first.checks()).usingRecursiveComparison().isEqualTo(second.checks());
    }

    @Test
    @DisplayName("wynik zawsze mieści się w 0–100, a punkty w zadeklarowanym maksimum")
    void scoreStaysInRange() {
        for (Post p : new Post[]{
                post(null, null),
                post("", ""),
                post(lead(200), "<p>" + "słowo ".repeat(500) + "</p>"),
                post(lead(60), "<h2>A?</h2><p>" + lead(30) + "</p>")}) {

            AiReadinessAnalyzer.Report report = analyzer.analyse(p);

            assertThat(report.score()).isBetween(0, 100);
            assertThat(report.rawPoints()).isBetween(0, report.maxPoints());
            assertThat(report.checks()).allSatisfy(c ->
                    assertThat(c.points()).isBetween(0, c.max()));
        }
    }

    @Test
    @DisplayName("pusty wpis nie wywala się i dostaje niski wynik")
    void emptyPostScoresLow() {
        AiReadinessAnalyzer.Report report = analyzer.analyse(post(null, null));

        assertThat(report.score()).isLessThan(30);
        assertThat(report.checks()).isNotEmpty();
    }

    @Test
    @DisplayName("dobrze napisany wpis wypada wyraźnie lepiej niż słaby")
    void goodPostBeatsWeakPost() {
        Post weak = post("Krótko.", "<p>Jedno zdanie.</p>");

        Post strong = post(lead(60), """
                <h2>Ile białka potrzebujesz?</h2>
                <p>%s Badania wskazują 1,6 g na kg masy ciała, czyli około 120 g dziennie przy 75 kg.</p>
                <ul><li>1,6 g/kg</li><li>3 posiłki</li><li>30 g na porcję</li></ul>
                <table><tr><td>75 kg</td><td>120 g</td></tr></table>
                <p><a href="https://pubmed.ncbi.nlm.nih.gov/12345">Badanie 1</a>
                   <a href="https://www.who.int/raport">WHO</a>
                   <a href="https://doi.org/10.1000/xyz">Metaanaliza</a></p>
                """.formatted(lead(45)));
        strong.addFaq(faq());
        strong.setCoverMediaId(1L);

        assertThat(analyzer.analyse(strong).score())
                .isGreaterThan(analyzer.analyse(weak).score() + 25);
    }

    private static PostFaq faq() {
        PostFaq item = new PostFaq();
        item.setQuestion("Ile białka?");
        item.setAnswer("Około 1,6 g na kg.");
        return item;
    }

    @Nested
    @DisplayName("poszczególne kryteria")
    class Criteria {

        @Test
        @DisplayName("lead 40–80 słów dostaje komplet punktów, krótszy mniej")
        void leadLength() {
            assertThat(check(post(lead(60), "<p>x</p>"), "Odpowiedź w leadzie").state())
                    .isEqualTo(AiReadinessAnalyzer.State.OK);
            assertThat(check(post(lead(25), "<p>x</p>"), "Odpowiedź w leadzie").state())
                    .isEqualTo(AiReadinessAnalyzer.State.WARN);
            assertThat(check(post(lead(5), "<p>x</p>"), "Odpowiedź w leadzie").state())
                    .isEqualTo(AiReadinessAnalyzer.State.FAIL);
        }

        @Test
        @DisplayName("odnośniki do PubMed/WHO liczą się, a do losowego bloga nie")
        void onlyAuthoritativeCitationsCount() {
            String authoritative = """
                    <p><a href="https://pubmed.ncbi.nlm.nih.gov/1">A</a>
                       <a href="https://www.who.int/b">B</a>
                       <a href="https://doi.org/10.1/c">C</a></p>""";
            String random = """
                    <p><a href="https://blog.example.com/1">A</a>
                       <a href="https://forum.example.com/b">B</a>
                       <a href="https://sklep.example.com/c">C</a></p>""";

            assertThat(check(post(lead(50), authoritative), "Odnośniki do źródeł").points())
                    .isGreaterThan(check(post(lead(50), random), "Odnośniki do źródeł").points());
        }

        @Test
        @DisplayName("świeżo zmieniony wpis dostaje komplet za aktualność, sprzed dwóch lat — nie")
        void freshness() {
            Post fresh = post(lead(50), "<p>x</p>");
            Post stale = post(lead(50), "<p>x</p>");
            stale.setUpdatedAt(Instant.now().minus(800, ChronoUnit.DAYS));

            assertThat(check(fresh, "Aktualność").points()).isEqualTo(8);
            assertThat(check(stale, "Aktualność").points()).isLessThan(4);
        }

        @Test
        @DisplayName("FAQ podnosi punkt, gdy są pytania")
        void faqCheck() {
            Post without = post(lead(50), "<p>x</p>");
            Post with = post(lead(50), "<p>x</p>");
            with.addFaq(faq());

            assertThat(check(without, "Sekcja FAQ").points()).isZero();
            assertThat(check(with, "Sekcja FAQ").points()).isEqualTo(1);
        }

        @Test
        @DisplayName("nagłówek w formie pytania jest rozpoznawany")
        void questionHeading() {
            assertThat(check(post(lead(50), "<h2>Ile białka?</h2><p>x</p>"), "Pytania w nagłówkach").points())
                    .isEqualTo(1);
            assertThat(check(post(lead(50), "<h2>Białko</h2><p>x</p>"), "Pytania w nagłówkach").points())
                    .isZero();
        }

        @Test
        @DisplayName("ściana tekstu obniża ocenę fragmentów do cytowania")
        void wallOfTextIsPenalised() {
            Post wall = post(lead(50), "<p>" + lead(300) + "</p>");
            Post chunked = post(lead(50),
                    "<p>" + lead(60) + "</p><p>" + lead(60) + "</p><ul><li>punkt</li></ul>");

            assertThat(check(wall, "Fragmenty do cytowania").points())
                    .isLessThan(check(chunked, "Fragmenty do cytowania").points());
        }

        private AiReadinessAnalyzer.Check check(Post post, String label) {
            return analyzer.analyse(post).checks().stream()
                    .filter(c -> c.label().equals(label))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Brak kryterium: " + label));
        }
    }
}
