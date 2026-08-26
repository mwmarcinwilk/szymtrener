package pl.szymtrener.docimport;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.szymtrener.content.HtmlSanitizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DocImportService {

    private final DocxToHtmlConverter docx;
    private final LegacyDocConverter doc;
    private final HtmlSanitizer sanitizer;

    public DocImportService(DocxToHtmlConverter docx, LegacyDocConverter doc, HtmlSanitizer sanitizer) {
        this.docx = docx;
        this.doc = doc;
        this.sanitizer = sanitizer;
    }

    public ImportResult importDocument(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        ImportResult result;
        if (name.endsWith(".docx")) {
            result = docx.convert(file.getInputStream(), name);
        } else if (name.endsWith(".doc")) {
            result = doc.convert(file.getInputStream());
        } else {
            throw new IllegalArgumentException("Obslugiwane sa pliki .docx i .doc");
        }

        String clean = sanitizer.clean(result.html());
        List<String> warnings = new ArrayList<>(result.warnings());
        if (clean.isBlank()) warnings.add("Dokument nie zawieral tresci do przeniesienia.");
        return new ImportResult(clean, result.imageCount(), warnings);
    }
}
