package pl.szymtrener.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.crm.AttachmentService;
import pl.szymtrener.crm.MessageAttachment;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Nagłówki pobierania: plik od klienta nigdy nie otwiera się w przeglądarce jako strona. */
class AdminAttachmentControllerTest {

    private final AttachmentService service = mock(AttachmentService.class);
    private final AdminAttachmentController controller = new AdminAttachmentController(service);

    private void file(long id, String name, String mime, boolean previewable) {
        MessageAttachment a = mock(MessageAttachment.class);
        when(a.getOriginalName()).thenReturn(name);
        when(a.getMimeType()).thenReturn(mime);
        when(a.previewable()).thenReturn(previewable);
        when(a.getSha256()).thenReturn("abc" + id);
        when(service.describe(id)).thenReturn(Optional.of(a));
        when(service.load(id)).thenReturn(Optional.of(new AttachmentService.Loaded(a, new byte[]{1, 2})));
    }

    @Test
    @DisplayName("pobranie: attachment z nazwą UTF-8, nosniff, bez cache, sandbox")
    void downloadHeaders() {
        file(1, "wyniki badań\".pdf", "application/pdf", false);

        ResponseEntity<byte[]> response = controller.download(1L);

        HttpHeaders h = response.getHeaders();
        assertThat(h.getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment;").contains("filename*=UTF-8''");
        assertThat(h.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(h.getCacheControl()).contains("no-store").contains("private");
        assertThat(h.getFirst("Content-Security-Policy")).contains("sandbox");
    }

    @Test
    @DisplayName("podgląd tylko dla obrazów, z ETag i 304 bez czytania bajtów; PDF i nieistniejący plik dają 404")
    void previewOnlyImages() {
        file(2, "zdjecie.png", "image/png", true);
        file(3, "wyniki.pdf", "application/pdf", false);

        ResponseEntity<byte[]> first = controller.preview(2L, null);
        assertThat(first.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("inline;");
        assertThat(first.getHeaders().getETag()).isEqualTo("\"abc2\"");
        assertThat(first.getHeaders().getCacheControl()).contains("no-cache").contains("private");

        org.mockito.Mockito.clearInvocations(service);
        ResponseEntity<byte[]> again = controller.preview(2L, "\"abc2\"");
        assertThat(again.getStatusCode().value()).isEqualTo(304);
        org.mockito.Mockito.verify(service, org.mockito.Mockito.never()).load(2L);

        assertThatThrownBy(() -> controller.preview(3L, null)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> controller.download(99L)).isInstanceOf(NotFoundException.class);
    }
}
