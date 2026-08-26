package pl.szymtrener.media;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pl.szymtrener.common.NotFoundException;

import java.time.Duration;

@RestController
public class MediaController {

    private final MediaService media;

    public MediaController(MediaService media) {
        this.media = media;
    }

    /** Zdjecia i pozostale pliki: /media/2026/08/abc123.jpg */
    @GetMapping("/media/**")
    public ResponseEntity<byte[]> serve(HttpServletRequest request) {
        String key = request.getRequestURI().substring("/media/".length());
        MediaFile file = media.byStorageKey(key)
                .orElseThrow(() -> new NotFoundException("Nie ma pliku: " + key));
        byte[] data = media.bytes(file.getId())
                .orElseThrow(() -> new NotFoundException("Brak zawartosci pliku " + file.getId()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                // klucz jest niezmienny — plik mozna trzymac w cache na zawsze
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .eTag(file.getChecksum())
                .body(data);
    }

    /** PDF-y: /pliki/12/plan-startowy.pdf — osobny adres, bo liczymy pobrania. */
    @GetMapping("/pliki/{id}/**")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        MediaFile file = media.byId(id)
                .orElseThrow(() -> new NotFoundException("Nie ma pliku " + id));
        byte[] data = media.bytes(id)
                .orElseThrow(() -> new NotFoundException("Brak zawartosci pliku " + id));
        media.countDownload(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getOriginalName(), java.nio.charset.StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(data);
    }
}
