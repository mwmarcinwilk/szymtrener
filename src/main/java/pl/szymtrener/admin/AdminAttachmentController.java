package pl.szymtrener.admin;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.crm.AttachmentService;
import pl.szymtrener.crm.MessageAttachment;

import java.nio.charset.StandardCharsets;

/**
 * Pobieranie zalacznikow z watku rozmowy. Tylko pod /admin, czyli po zalogowaniu (SecurityConfig):
 * to dane osobowe klientow, czesto wyniki badan, wiec bez publicznego adresu i bez cache.
 *
 * Kazda odpowiedz ma nosniff, a typ pochodzi z rozpoznania tresci przy zapisie. Inline oddajemy
 * wylacznie obrazy, ktore da sie pokazac jako miniature; wszystko inne idzie jako „attachment",
 * wiec plik od klienta nie otworzy sie w przegladarce trenera jako strona.
 */
@Controller
public class AdminAttachmentController {

    private final AttachmentService attachments;

    public AdminAttachmentController(AttachmentService attachments) {
        this.attachments = attachments;
    }

    /** GET /admin/zalaczniki/{id} — plik do zapisania na dysku. 404, gdy nie istnieje. */
    @GetMapping("/admin/zalaczniki/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        AttachmentService.Loaded file = find(id);
        return respond(file, ContentDisposition.attachment(), CacheControl.noStore().cachePrivate(), null);
    }

    /** GET /admin/zalaczniki/{id}/podglad — miniatura w watku, tylko dla obrazow rozpoznanych po tresci. */
    @GetMapping("/admin/zalaczniki/{id}/podglad")
    public ResponseEntity<byte[]> preview(@PathVariable Long id,
                                          @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        MessageAttachment meta = attachments.describe(id)
                .filter(MessageAttachment::previewable)
                .orElseThrow(() -> new NotFoundException("Brak podglądu dla załącznika " + id));
        // Miniatury laduja sie przy kazdym otwarciu watku: bez cache (dane osobowe), ale z ETag,
        // wiec przegladarka dostaje 304 bez czytania bajtow z bazy.
        String etag = "\"" + meta.getSha256() + "\"";
        CacheControl revalidate = CacheControl.noCache().cachePrivate();
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(revalidate).build();
        }
        return respond(find(id), ContentDisposition.inline(), revalidate, etag);
    }

    private AttachmentService.Loaded find(Long id) {
        return attachments.load(id).orElseThrow(() -> new NotFoundException("Nie ma załącznika " + id));
    }

    private static ResponseEntity<byte[]> respond(AttachmentService.Loaded file, ContentDisposition.Builder disposition,
                                                  CacheControl cache, String etag) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (etag != null) response.eTag(etag);
        return response
                .contentType(MediaType.parseMediaType(file.attachment().getMimeType()))
                .contentLength(file.data().length)
                .cacheControl(cache)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition.filename(file.attachment().getOriginalName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(file.data());
    }
}
