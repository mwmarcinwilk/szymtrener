package pl.szymtrener.crm;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Tresc odebranego maila w postaci, ktora da sie pokazac w watku: sam tekst, bez cytatu
 * poprzedniej wiadomosci. HTML z maila nigdy nie trafia do panelu, zamieniamy go tu na tekst.
 *
 * Cytat ucinamy, bo klient pisze dwa zdania, a program pocztowy dokleja pod nimi cala
 * nasza wiadomosc. W watku ta wiadomosc juz jest, dwa razy to szum.
 */
final class InboundMailText {

    static final int MAX_LENGTH = 20_000;

    /**
     * Pierwsza linia cytatu. Gmail i Apple Mail po polsku i angielsku, Outlook i Thunderbird.
     * Naglowek Gmaila bywa zlamany na dwie linie, dlatego „W dniu"/„On" bez dwukropka
     * sprawdzamy razem z nastepna linia (patrz {@link #quoteStart}).
     */
    private static final List<Pattern> QUOTE_HEADERS = List.of(
            Pattern.compile("(?i)^\\s*(w dniu|dnia)\\b.*\\bnapisał(a|\\(a\\))?\\s*:\\s*$"),
            Pattern.compile("(?i)^\\s*wiadomość napisana przez\\b.*:\\s*$"),
            Pattern.compile("(?i)^\\s*on\\b.*\\bwrote\\s*:\\s*$"),
            Pattern.compile("(?i)^\\s*-{2,}\\s*(original message|wiadomość oryginalna|oryginalna wiadomość)\\s*-{2,}\\s*$"));
    /** Outlook: „Od: …" z „Wysłano:"/„Sent:" tuz pod spodem. Samo „Od:" moze byc zwyklym zdaniem klienta. */
    private static final Pattern OUTLOOK_FROM = Pattern.compile("(?i)^\\s*(od|from)\\s*:.+$");
    private static final Pattern OUTLOOK_SENT = Pattern.compile("(?i)^\\s*(wysłano|sent|data|date)\\s*:.+$");
    private static final Pattern SPLIT_HEADER_START = Pattern.compile("(?i)^\\s*(w dniu|dnia|on)\\b.*");
    private static final Pattern SPLIT_HEADER_END = Pattern.compile("(?i).*\\b(napisał(a|\\(a\\))?|wrote)\\s*:\\s*$");
    /** Trzy i wiecej pustych linii pod rzad zwijane do jednej przerwy; wspolne z MessageService. */
    static final Pattern EXTRA_BLANK_LINES = Pattern.compile("\\n(?:\\h*+\\n){2,}+");
    private static final Set<String> BLOCKS = Set.of("p", "div", "li", "tr", "h1", "h2", "h3", "h4", "h5", "h6", "table", "ul", "ol");

    private InboundMailText() {}

    /** Tekst gotowy do zapisu w watku. Gdy po ucieciu cytatu nic nie zostaje, oddaje calosc. */
    static String clean(String raw) {
        String text = normalize(raw);
        String reply = normalize(withoutQuote(text));
        String chosen = reply.isBlank() ? text : reply;
        if (chosen.length() > MAX_LENGTH) {
            chosen = chosen.substring(0, MAX_LENGTH).stripTrailing() + "\n[…] wiadomość skrócona w panelu";
        }
        return chosen;
    }

    /** HTML z maila na tekst: akapity i zlamania wierszy zostaja, cytaty i style znikaja. */
    static String htmlToText(String html) {
        Document doc = Jsoup.parse(html);
        doc.select("head, style, script, blockquote, .gmail_quote, .moz-cite-prefix, #appendonsend, #divRplyFwdMsg").remove();
        StringBuilder out = new StringBuilder();
        doc.body().traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode text) {
                    out.append(text.text());
                } else if (node instanceof Element el) {
                    if (el.normalName().equals("br")) out.append('\n');
                    else if (BLOCKS.contains(el.normalName())) newline(out);
                }
            }

            @Override
            public void tail(Node node, int depth) {
                if (node instanceof Element el && BLOCKS.contains(el.normalName())) newline(out);
            }
        });
        return out.toString();
    }

    private static void newline(StringBuilder out) {
        if (!out.isEmpty() && out.charAt(out.length() - 1) != '\n') out.append('\n');
    }

    private static String withoutQuote(String text) {
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        int cut = quoteStart(lines);
        List<String> kept = lines.subList(0, cut == -1 ? lines.size() : cut);
        // Linie „> ..." to cytat w stylu zwyklego tekstu; przy odpowiedzi „w tekscie" zostaja same odpowiedzi.
        return String.join("\n", kept.stream().filter(line -> !line.stripLeading().startsWith(">")).toList());
    }

    private static int quoteStart(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (Pattern header : QUOTE_HEADERS) {
                if (header.matcher(line).matches()) return i;
            }
            if (OUTLOOK_FROM.matcher(line).matches()) {
                for (int j = i + 1; j < Math.min(i + 3, lines.size()); j++) {
                    if (OUTLOOK_SENT.matcher(lines.get(j)).matches()) return i;
                }
            }
            if (SPLIT_HEADER_START.matcher(line).matches() && i + 1 < lines.size()
                    && SPLIT_HEADER_END.matcher(lines.get(i + 1)).matches()) {
                return i;
            }
        }
        return -1;
    }

    private static String normalize(String text) {
        String unified = text.replace("\r\n", "\n").replace('\r', '\n').replace(' ', ' ');
        String trimmedLines = String.join("\n", unified.lines().map(String::stripTrailing).toList());
        return EXTRA_BLANK_LINES.matcher(trimmedLines).replaceAll("\n\n").strip();
    }
}
