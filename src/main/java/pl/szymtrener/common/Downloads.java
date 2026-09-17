package pl.szymtrener.common;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** Odpowiedzi do pobrania: dane osobowe nigdy nie trafiaja do cache posrednika. */
public final class Downloads {

    private Downloads() {}

    public static ResponseEntity<byte[]> json(byte[] body, String filename) {
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
