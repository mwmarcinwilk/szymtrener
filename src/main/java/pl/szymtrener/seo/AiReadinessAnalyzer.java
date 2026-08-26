package pl.szymtrener.seo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import pl.szymtrener.content.Post;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Ocena wpisu wedlug bloku CONTENT siatki V4 (41 pkt), przeskalowana do 100.
 *
 * Swiadome ograniczenie zakresu: kategorie TECHNICAL i INFRASTRUCTURE (robots.txt,
 * SSR, schema, canonical, TTFB, hreflang) sa wlasciwoscia aplikacji, a nie
 * pojedynczego tekstu — aplikacja spelnia je dla kazdego wpisu tak samo, wiec
 * mieszanie ich do oceny redakcyjnej tylko zawyzaloby wynik. Tutaj oceniamy to,
 * na co autor faktycznie ma wplyw, piszac.
 */
@Service
public class AiReadinessAnalyzer {

    public enum State { OK, WARN, FAIL }

    public record Check(String label, State state, int points, int max, String hint) {}
    public record Report(int score, int rawPoints, int maxPoints, List<Check> checks) {}

    private static final Pattern NUMBER = Pattern.compile("\\d+([.,]\\d+)?\\s*(%|proc|kg|g|min|godz|lat|lata|roku|x|zl|zł)?");
    private static final Set<String> AUTHORITATIVE = Set.of(
            "pubmed.ncbi.nlm.nih.gov", "ncbi.nlm.nih.gov", "who.int", "gov.pl", "nih.gov",
            "cochrane.org", "nature.com", "thelancet.com", "bmj.com", "jamanetwork.com",
            "sciencedirect.com", "doi.org", "pzh.gov.pl", "gis.gov.pl");

    public Report analyse(Post post) {
        Document doc = Jsoup.parseBodyFragment(post.getContentHtml() == null ? "" : post.getContentHtml());
        List<Check> checks = new ArrayList<>();

        checks.add(answerFirst(post));
        checks.add(sectionAnswers(doc));
        checks.add(dataDensity(doc, post));
        checks.add(chunkFriendliness(doc));
        checks.add(citations(doc));
        checks.add(freshness(post));
        checks.add(questionHeadings(doc));
        checks.add(formatting(doc));
        checks.add(faq(post));
        checks.add(formatVariety(doc, post));

        int raw = checks.stream().mapToInt(Check::points).sum();
        int max = checks.stream().mapToInt(Check::max).sum();
        return new Report((int) Math.round(raw * 100.0 / max), raw, max, checks);
    }

    // 6 pkt — odpowiedz zaraz po H1
    private Check answerFirst(Post post) {
        int words = words(post.getLead());
        if (words >= 40 && words <= 90) {
            return new Check("Odpowiedź w leadzie", State.OK, 6, 6, "Lead ma " + words + " słów.");
        }
        if (words >= 20) {
            return new Check("Odpowiedź w leadzie", State.WARN, 4, 6,
                    "Lead ma " + words + " słów. Najlepiej działa 40–80: konkretna odpowiedź, zanim zacznie się wstęp.");
        }
        return new Check("Odpowiedź w leadzie", State.FAIL, 0, 6,
                "Dodaj lead 40–80 słów, który od razu odpowiada na tytuł.");
    }

    // 4 pkt — kazda sekcja zaczyna sie od odpowiedzi
    private Check sectionAnswers(Document doc) {
        List<Element> headings = doc.select("h2, h3");
        if (headings.isEmpty()) {
            return new Check("Odpowiedź w każdej sekcji", State.FAIL, 0, 4, "Podziel tekst nagłówkami H2.");
        }
        int good = 0;
        for (Element heading : headings) {
            Element next = heading.nextElementSibling();
            if (next != null && next.normalName().equals("p")) {
                int w = words(next.text());
                if (w >= 15 && w <= 80) good++;
            }
        }
        double share = good / (double) headings.size();
        int points = share >= 0.6 ? 4 : share >= 0.4 ? 3 : share >= 0.2 ? 2 : good >= 1 ? 1 : 0;
        State state = points >= 3 ? State.OK : points >= 1 ? State.WARN : State.FAIL;
        return new Check("Odpowiedź w każdej sekcji", state, points, 4,
                good + " z " + headings.size() + " sekcji zaczyna się zwięzłą odpowiedzią.");
    }

    // 8 pkt — liczby i statystyki
    private Check dataDensity(Document doc, Post post) {
        String text = post.getLead() + " " + doc.text();
        long numbers = NUMBER.matcher(text).results().count();
        int words = words(text);
        double per200 = words == 0 ? 0 : numbers * 200.0 / words;
        boolean sourced = !doc.select("a[href^=http]").isEmpty();

        int points;
        if (per200 >= 4) points = sourced ? 8 : 6;
        else if (per200 >= 2) points = sourced ? 6 : 5;
        else if (per200 >= 1) points = 3;
        else points = 0;
        State state = points >= 6 ? State.OK : points >= 3 ? State.WARN : State.FAIL;
        return new Check("Dane liczbowe", state, points, 8,
                "Około %.1f liczby na 200 słów%s.".formatted(per200, sourced ? " (ze źródłami)" : " — bez podanych źródeł"));
    }

    // 5 pkt — dlugosc akapitow, tabele i listy
    private Check chunkFriendliness(Document doc) {
        List<Element> paragraphs = doc.select("p");
        if (paragraphs.isEmpty()) return new Check("Fragmenty do cytowania", State.FAIL, 0, 5, "Brak treści.");
        long sweet = paragraphs.stream().filter(p -> { int w = words(p.text()); return w >= 40 && w <= 120; }).count();
        long walls = paragraphs.stream().filter(p -> words(p.text()) > 200).count();
        boolean structured = !doc.select("table, ul, ol").isEmpty();

        int points = (int) Math.round(3.0 * sweet / paragraphs.size());
        if (structured) points += 2;
        points = Math.max(0, Math.min(5, points - (int) walls));
        State state = points >= 4 ? State.OK : points >= 2 ? State.WARN : State.FAIL;
        String hint = walls > 0
                ? walls + " akapit(y) powyżej 200 słów — podziel je."
                : "Akapity w dobrym zakresie" + (structured ? " + tabele/listy." : ", brak tabel i list.");
        return new Check("Fragmenty do cytowania", state, points, 5, hint);
    }

    // 6 pkt — cytowania autorytatywne
    private Check citations(Document doc) {
        long authoritative = doc.select("a[href^=http]").stream()
                .map(a -> a.attr("href"))
                .filter(href -> AUTHORITATIVE.stream().anyMatch(href::contains))
                .count();
        boolean quotes = !doc.select("blockquote[cite], cite").isEmpty();
        int points = authoritative >= 3 ? 6 : authoritative == 2 ? 5 : authoritative == 1 ? 3 : 0;
        if (quotes && points < 6) points++;
        State state = points >= 5 ? State.OK : points >= 2 ? State.WARN : State.FAIL;
        return new Check("Odnośniki do źródeł", state, points, 6,
                authoritative == 0
                        ? "Dodaj 2–3 linki do badań lub instytucji (PubMed, WHO, gov.pl)."
                        : authoritative + " odnośnik(i) do autorytatywnych źródeł.");
    }

    // 8 pkt — swiezosc
    private Check freshness(Post post) {
        Instant modified = post.getUpdatedAt();
        if (modified == null) return new Check("Aktualność", State.FAIL, 0, 8, "Brak daty modyfikacji.");
        long days = Duration.between(modified, Instant.now()).toDays();
        int points = days <= 180 ? 8 : days <= 365 ? 6 : days <= 730 ? 3 : 1;
        State state = points >= 6 ? State.OK : State.WARN;
        return new Check("Aktualność", state, points, 8,
                days <= 1 ? "Zaktualizowany dzisiaj." : "Ostatnia zmiana " + days + " dni temu.");
    }

    // 1 pkt — pytania w naglowkach
    private Check questionHeadings(Document doc) {
        long questions = doc.select("h2, h3").stream().filter(h -> h.text().contains("?")).count();
        int points = questions >= 1 ? 1 : 0;
        return new Check("Pytania w nagłówkach", points == 1 ? State.OK : State.WARN, points, 1,
                points == 1 ? questions + " nagłówek w formie pytania." : "Sygnał drugorzędny — nie na siłę.");
    }

    // 1 pkt — hierarchia i formatowanie
    private Check formatting(Document doc) {
        boolean h2 = !doc.select("h2").isEmpty();
        boolean lists = !doc.select("ul, ol").isEmpty();
        int points = (h2 && lists) ? 1 : 0;
        return new Check("Formatowanie", points == 1 ? State.OK : State.WARN, points, 1,
                h2 ? (lists ? "H2 + listy obecne." : "Dodaj listę punktowaną.") : "Brak nagłówków H2.");
    }

    // 1 pkt — FAQ
    private Check faq(Post post) {
        int points = post.getFaq().isEmpty() ? 0 : 1;
        return new Check("Sekcja FAQ", points == 1 ? State.OK : State.WARN, points, 1,
                points == 1 ? post.getFaq().size() + " pytania — trafiają też do JSON-LD FAQPage."
                            : "Dodaj 2–3 pytania; generują FAQPage automatycznie.");
    }

    // 1 pkt — roznorodnosc formatu
    private Check formatVariety(Document doc, Post post) {
        int kinds = 0;
        if (!doc.select("img").isEmpty() || post.getCoverMediaId() != null) kinds++;
        if (!doc.select("table").isEmpty()) kinds++;
        if (post.isHasVideo()) kinds++;
        if (post.isHasPdf()) kinds++;
        int points = kinds >= 2 ? 1 : 0;
        return new Check("Różnorodność formatu", points == 1 ? State.OK : State.WARN, points, 1,
                kinds + " typ(y) treści poza tekstem.");
    }

    private static int words(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }
}
