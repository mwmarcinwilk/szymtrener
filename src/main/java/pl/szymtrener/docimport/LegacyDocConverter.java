package pl.szymtrener.docimport;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.List;

/**
 * Stary format .doc (Word 97-2003). Konwersja jest zgrubna — POI oddaje HTML
 * ze stylami inline, ktory i tak przechodzi potem przez HtmlSanitizer.
 * Zalecenie dla uzytkownika: zapisz w Wordzie jako .docx i wgraj ponownie.
 */
@Component
public class LegacyDocConverter {

    public ImportResult convert(InputStream in) throws Exception {
        HWPFDocument document = new HWPFDocument(in);
        WordToHtmlConverter converter = new WordToHtmlConverter(
                DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
        converter.processDocument(document);

        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "html");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(converter.getDocument()), new StreamResult(writer));

        String html = writer.toString();
        int bodyStart = html.indexOf("<body");
        if (bodyStart >= 0) {
            int contentStart = html.indexOf('>', bodyStart) + 1;
            int contentEnd = html.lastIndexOf("</body>");
            if (contentEnd > contentStart) html = html.substring(contentStart, contentEnd);
        }
        return new ImportResult(html, 0,
                List.of("Plik w starym formacie .doc — obrazki i czesc formatowania mogly nie przejsc. "
                      + "Dla najlepszego wyniku zapisz dokument jako .docx."));
    }
}
