package pl.szymtrener.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.szymtrener.docimport.DocImportService;
import pl.szymtrener.docimport.ImportResult;
import pl.szymtrener.media.MediaFile;
import pl.szymtrener.media.MediaService;

import java.util.List;
import java.util.Map;

/** Punkty wywolywane przez edytor: wgrywanie plikow i import z Worda. */
@RestController
@RequestMapping("/admin/api")
public class AdminApiController {

    private final MediaService media;
    private final DocImportService docImport;
    private final pl.szymtrener.media.MediaRepository repository;

    public AdminApiController(MediaService media, DocImportService docImport,
                              pl.szymtrener.media.MediaRepository repository) {
        this.media = media;
        this.docImport = docImport;
        this.repository = repository;
    }

    @PostMapping("/media")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(required = false) String alt) throws Exception {
        MediaFile saved = media.upload(file, alt);
        return ResponseEntity.ok(describe(saved));
    }

    @GetMapping("/media")
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "0") int strona) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(strona, 40))
                .map(AdminApiController::describe).getContent();
    }

    /** Import .docx/.doc — zwraca gotowy HTML do wklejenia w edytorze. */
    @PostMapping("/import-docx")
    public ResponseEntity<?> importDocument(@RequestParam("file") MultipartFile file) {
        try {
            ImportResult result = docImport.importDocument(file);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "html", result.html(),
                    "images", result.imageCount(),
                    "warnings", result.warnings()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("ok", false, "message", "Nie udało się wczytać dokumentu: " + e.getMessage()));
        }
    }

    private static Map<String, Object> describe(MediaFile file) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", file.getId());
        map.put("url", file.publicUrl());
        map.put("name", file.getOriginalName());
        map.put("kind", file.getKind().name());
        map.put("mime", file.getMimeType());
        map.put("size", file.humanSize());
        map.put("width", file.getWidth());
        map.put("height", file.getHeight());
        map.put("alt", file.getAltText());
        return map;
    }
}
