package pl.szymtrener.docimport;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;
import pl.szymtrener.media.MediaFile;
import pl.szymtrener.media.MediaService;

import java.io.InputStream;
import java.util.*;

/**
 * Wlasny konwerter DOCX -> HTML.
 *
 * Dlaczego wlasny, a nie gotowa biblioteka: konwertery ogolnego przeznaczenia
 * (docx4j, xdocreport) produkuja HTML pelen stylow inline i tagow, ktorych blog.css
 * nie zna, wiec i tak trzeba by je czyscic. Tutaj mapujemy Worda od razu na te
 * kilkanascie tagow, ktore blog naprawde renderuje — i nic wiecej nie powstaje.
 *
 * Co przenosimy: naglowki (Heading 1-3), pogrubienie, kursywe, podkreslenie,
 * przekreslenie, listy punktowane i numerowane, hiperlacza, tabele (pierwszy
 * wiersz jako naglowek), obrazki (trafiaja do biblioteki mediow) i cytaty.
 * Czego nie przenosimy swiadomie: czcionek, rozmiarow, kolorow i wyrownania —
 * o wygladzie decyduje styl bloga, nie formatowanie z Worda.
 */
@Component
public class DocxToHtmlConverter {

    private final MediaService media;

    public DocxToHtmlConverter(MediaService media) {
        this.media = media;
    }

    public ImportResult convert(InputStream in, String sourceName) throws Exception {
        StringBuilder html = new StringBuilder();
        List<String> warnings = new ArrayList<>();
        int[] images = {0};

        try (XWPFDocument doc = new XWPFDocument(in)) {
            ListState list = new ListState();

            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph p) {
                    appendParagraph(doc, p, html, list, warnings, images);
                } else if (element instanceof XWPFTable table) {
                    list.close(html);
                    appendTable(table, html);
                }
            }
            list.close(html);
        }
        return new ImportResult(html.toString(), images[0], warnings);
    }

    // ─── akapity ────────────────────────────────────────────────────

    private void appendParagraph(XWPFDocument doc, XWPFParagraph p, StringBuilder html,
                                 ListState list, List<String> warnings, int[] images) {
        String inline = renderRuns(doc, p, warnings, images);
        boolean empty = inline.isBlank();

        if (isListItem(p)) {
            String tag = isOrdered(p) ? "ol" : "ul";
            list.open(html, tag);
            if (!empty) html.append("<li>").append(inline).append("</li>\n");
            return;
        }
        list.close(html);
        if (empty) return;

        int heading = headingLevel(p);
        if (heading > 0) {
            // H1 zostaje tytulem wpisu, wiec naglowki z Worda zaczynaja sie od H2
            int level = Math.min(4, heading + 1);
            html.append("<h").append(level).append('>').append(inline)
                .append("</h").append(level).append(">\n");
        } else if (isQuote(p)) {
            html.append("<blockquote>").append(inline).append("</blockquote>\n");
        } else {
            html.append("<p>").append(inline).append("</p>\n");
        }
    }

    private String renderRuns(XWPFDocument doc, XWPFParagraph p, List<String> warnings, int[] images) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : p.getRuns()) {
            String pictures = extractPictures(run, warnings, images);
            if (!pictures.isEmpty()) { sb.append(pictures); continue; }

            String text = run.text();
            if (text == null || text.isEmpty()) continue;
            String piece = escape(text);

            if (run.isBold())      piece = "<strong>" + piece + "</strong>";
            if (run.isItalic())    piece = "<em>" + piece + "</em>";
            if (run.getUnderline() != UnderlinePatterns.NONE) piece = "<u>" + piece + "</u>";
            if (run.isStrikeThrough()) piece = "<s>" + piece + "</s>";

            if (run instanceof XWPFHyperlinkRun link) {
                XWPFHyperlink target = link.getHyperlink(doc);
                if (target != null && target.getURL() != null) {
                    piece = "<a href=\"" + escape(target.getURL()) + "\">" + piece + "</a>";
                }
            }
            sb.append(piece);
        }
        return sb.toString().trim();
    }

    private String extractPictures(XWPFRun run, List<String> warnings, int[] images) {
        StringBuilder sb = new StringBuilder();
        for (XWPFPicture picture : run.getEmbeddedPictures()) {
            try {
                XWPFPictureData data = picture.getPictureData();
                String mime = switch (data.suggestFileExtension().toLowerCase(Locale.ROOT)) {
                    case "png" -> "image/png";
                    case "jpg", "jpeg" -> "image/jpeg";
                    case "webp" -> "image/webp";
                    default -> null;
                };
                if (mime == null) {
                    warnings.add("Pominieto obrazek w formacie " + data.suggestFileExtension());
                    continue;
                }
                MediaFile file = media.store(data.getData(), data.getFileName(), mime, null);
                sb.append("<figure><img src=\"").append(file.publicUrl())
                  .append("\" alt=\"\" loading=\"lazy\" decoding=\"async\"></figure>\n");
                images[0]++;
            } catch (Exception e) {
                warnings.add("Nie udalo sie przeniesc obrazka: " + e.getMessage());
            }
        }
        return sb.toString();
    }

    // ─── tabele ─────────────────────────────────────────────────────

    private void appendTable(XWPFTable table, StringBuilder html) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) return;
        html.append("<table>\n<thead>\n<tr>");
        for (XWPFTableCell cell : rows.get(0).getTableCells()) {
            html.append("<th scope=\"col\">").append(escape(cell.getText())).append("</th>");
        }
        html.append("</tr>\n</thead>\n<tbody>\n");
        for (int i = 1; i < rows.size(); i++) {
            html.append("<tr>");
            for (XWPFTableCell cell : rows.get(i).getTableCells()) {
                html.append("<td>").append(escape(cell.getText())).append("</td>");
            }
            html.append("</tr>\n");
        }
        html.append("</tbody>\n</table>\n");
    }

    // ─── rozpoznawanie stylow ───────────────────────────────────────

    private static int headingLevel(XWPFParagraph p) {
        String style = Optional.ofNullable(p.getStyleID()).orElse("").toLowerCase(Locale.ROOT);
        // dziala dla angielskiego Worda ("Heading1") i polskiego ("Nagwek1")
        for (int level = 1; level <= 4; level++) {
            if (style.contains("heading" + level) || style.contains("nag") && style.endsWith(String.valueOf(level))) {
                return level;
            }
        }
        return 0;
    }

    private static boolean isQuote(XWPFParagraph p) {
        String style = Optional.ofNullable(p.getStyleID()).orElse("").toLowerCase(Locale.ROOT);
        return style.contains("quote") || style.contains("cytat");
    }

    private static boolean isListItem(XWPFParagraph p) {
        return p.getNumID() != null;
    }

    private static boolean isOrdered(XWPFParagraph p) {
        String format = numberFormat(p);
        if (format == null) return false;   // w razie watpliwosci: lista punktowana
        String normalized = format.toLowerCase(Locale.ROOT);
        return !normalized.contains("bullet") && !normalized.equals("none");
    }

    /**
     * Format numeracji akapitu.
     *
     * XWPFParagraph.getNumFmt() zwraca null, gdy akapit nie ma jawnego poziomu
     * (w:ilvl) — Word zwykle go zapisuje, ale nie zawsze, a wtedy KAZDA lista
     * ladowala jako punktowana. Dlatego przy braku wyniku schodzimy do
     * numbering.xml i czytamy format wprost z definicji poziomu.
     */
    private static String numberFormat(XWPFParagraph p) {
        try {
            String direct = p.getNumFmt();
            if (direct != null && !direct.isBlank()) return direct;
        } catch (Exception ignored) {
            // uszkodzona numeracja — probujemy jeszcze raz nizej
        }

        try {
            XWPFNumbering numbering = p.getDocument().getNumbering();
            if (numbering == null || p.getNumID() == null) return null;

            XWPFNum num = numbering.getNum(p.getNumID());
            if (num == null) return null;

            XWPFAbstractNum abstractNum =
                    numbering.getAbstractNum(num.getCTNum().getAbstractNumId().getVal());
            if (abstractNum == null) return null;

            int wanted = p.getNumIlvl() == null ? 0 : p.getNumIlvl().intValue();
            var levels = abstractNum.getCTAbstractNum().getLvlList();
            for (var level : levels) {
                if (level.getNumFmt() == null) continue;
                if (level.getIlvl() != null && level.getIlvl().intValue() == wanted) {
                    return level.getNumFmt().getVal().toString();
                }
            }
            // Nie ma dokladnie tego poziomu — bierzemy pierwszy zdefiniowany.
            return levels.stream()
                    .filter(l -> l.getNumFmt() != null)
                    .findFirst()
                    .map(l -> l.getNumFmt().getVal().toString())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Pilnuje, zeby <ul>/<ol> otwieral i zamykal sie dokladnie raz. */
    private static final class ListState {
        private String open;
        void open(StringBuilder html, String tag) {
            if (tag.equals(open)) return;
            close(html);
            html.append('<').append(tag).append(">\n");
            open = tag;
        }
        void close(StringBuilder html) {
            if (open != null) { html.append("</").append(open).append(">\n"); open = null; }
        }
    }
}
