package pl.szymtrener.docimport;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;

/**
 * Dokumenty .docx budowane w locie przez POI.
 *
 * Docelowo warto wrzucic do src/test/resources trzy prawdziwe pliki od klienta —
 * dopiero one pokaza, jak jego Word nazywa style naglowkow. Do czasu, gdy beda
 * dostepne, fixtury odtwarzaja te same struktury, ktore konwerter ma obsluzyc.
 */
final class DocxFixtures {

    private DocxFixtures() {}

    static InputStream bytes(XWPFDocument doc) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        doc.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    /** Naglowki, akapity i formatowanie znakowe — angielskie nazwy stylow. */
    static XWPFDocument englishWord() {
        XWPFDocument doc = new XWPFDocument();

        heading(doc, "Heading1", "Tytuł rozdziału");
        plain(doc, "Zwykły akapit ze zdaniem.");
        heading(doc, "Heading2", "Podrozdział");

        XWPFParagraph p = doc.createParagraph();
        run(p, "pogrubione", true, false, false, false);
        run(p, " i ", false, false, false, false);
        run(p, "kursywa", false, true, false, false);
        run(p, " i ", false, false, false, false);
        run(p, "podkreślone", false, false, true, false);
        run(p, " i ", false, false, false, false);
        run(p, "przekreślone", false, false, false, true);

        return doc;
    }

    /** Polski Word nazywa styl naglowka „Nagwek1" (bez znakow diakrytycznych w ID). */
    static XWPFDocument polishWord() {
        XWPFDocument doc = new XWPFDocument();
        heading(doc, "Nagwek1", "Polski nagłówek pierwszego poziomu");
        plain(doc, "Akapit pod nagłówkiem.");
        heading(doc, "Nagwek2", "Polski nagłówek drugiego poziomu");
        plain(doc, "Drugi akapit.");
        return doc;
    }

    /** Listy punktowana i numerowana + tabela + cytat. */
    static XWPFDocument listsTableAndQuote() throws Exception {
        XWPFDocument doc = new XWPFDocument();

        BigInteger bullet = numbering(doc, STNumberFormat.BULLET, 0);
        listItem(doc, bullet, "Pierwszy punkt");
        listItem(doc, bullet, "Drugi punkt");

        plain(doc, "Akapit rozdzielający listy.");

        BigInteger decimal = numbering(doc, STNumberFormat.DECIMAL, 1);
        listItem(doc, decimal, "Krok pierwszy");
        listItem(doc, decimal, "Krok drugi");

        XWPFParagraph quote = doc.createParagraph();
        quote.setStyle("Quote");
        quote.createRun().setText("Zdanie zacytowane.");

        XWPFTable table = doc.createTable(2, 2);
        table.getRow(0).getCell(0).setText("Masa ciała");
        table.getRow(0).getCell(1).setText("Białko");
        table.getRow(1).getCell(0).setText("75 kg");
        table.getRow(1).getCell(1).setText("120 g");

        return doc;
    }

    /** Znaki, ktore musza zostac zescapowane, zeby nie rozjechac HTML-a. */
    static XWPFDocument htmlUnsafeText() {
        XWPFDocument doc = new XWPFDocument();
        plain(doc, "Ostrzeżenie: <script>alert('x')</script> oraz A & B < C > D");
        return doc;
    }

    static XWPFDocument empty() {
        return new XWPFDocument();
    }

    // ─── budulec ────────────────────────────────────────────────────

    private static void heading(XWPFDocument doc, String styleId, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle(styleId);
        p.createRun().setText(text);
    }

    private static void plain(XWPFDocument doc, String text) {
        doc.createParagraph().createRun().setText(text);
    }

    private static void run(XWPFParagraph p, String text,
                            boolean bold, boolean italic, boolean underline, boolean strike) {
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
        r.setItalic(italic);
        if (underline) r.setUnderline(UnderlinePatterns.SINGLE);
        r.setStrikeThrough(strike);
    }

    /** Prawdziwy Word zawsze zapisuje poziom listy (w:ilvl) — fixtura tez. */
    private static void listItem(XWPFDocument doc, BigInteger numId, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setNumID(numId);
        p.getCTP().getPPr().getNumPr().addNewIlvl().setVal(BigInteger.ZERO);
        p.createRun().setText(text);
    }

    /** Ten sam dokument, ale bez w:ilvl — sprawdza odczyt formatu z numbering.xml. */
    static XWPFDocument orderedListWithoutIlvl() throws Exception {
        XWPFDocument doc = new XWPFDocument();
        BigInteger decimal = numbering(doc, STNumberFormat.DECIMAL, 0);
        XWPFParagraph p = doc.createParagraph();
        p.setNumID(decimal);
        p.createRun().setText("Krok bez jawnego poziomu");
        return doc;
    }

    /**
     * Word wymaga definicji numeracji w numbering.xml — inaczej getNumFmt() nic
     * nie zwroci. Kazda definicja musi miec WLASNE abstractNumId; przy kolizji
     * obie listy rozwiazuja sie do tej samej (pierwszej) definicji.
     */
    private static BigInteger numbering(XWPFDocument doc, STNumberFormat.Enum format, int abstractId) {
        XWPFNumbering numbering = doc.createNumbering();
        CTAbstractNum abstractNum = CTAbstractNum.Factory.newInstance();
        abstractNum.setAbstractNumId(BigInteger.valueOf(abstractId));
        abstractNum.addNewLvl().setIlvl(BigInteger.ZERO);
        abstractNum.getLvlArray(0).addNewNumFmt().setVal(format);
        abstractNum.getLvlArray(0).addNewLvlText().setVal(format == STNumberFormat.BULLET ? "•" : "%1.");

        BigInteger registered = numbering.addAbstractNum(new XWPFAbstractNum(abstractNum));
        return numbering.addNum(registered);
    }

    /** Numeracja bez definicji formatu — sprawdza zachowanie awaryjne konwertera. */
    static XWPFDocument listWithoutNumberingDefinition() {
        XWPFDocument doc = new XWPFDocument();
        XWPFParagraph p = doc.createParagraph();
        p.setNumID(BigInteger.valueOf(99));
        p.createRun().setText("Punkt bez definicji numeracji");
        return doc;
    }
}
