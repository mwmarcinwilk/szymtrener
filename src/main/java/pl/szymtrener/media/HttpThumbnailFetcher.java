package pl.szymtrener.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Jedyne wyjscie serwera do YouTube: host na sztywno, id sprawdzone wczesniej przez {@link YoutubeThumbnails},
 * bez przekierowan, limit czasu i rozmiaru. Wolane przy zapisie wpisu przez admina i przy jednorazowym
 * przepisaniu starych wpisow, nigdy z zadania anonimowego odwiedzajacego.
 */
@Component
class HttpThumbnailFetcher implements ThumbnailFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpThumbnailFetcher.class);
    static final int MAX_BYTES = 1_000_000;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public Optional<byte[]> fetch(String videoId) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(YoutubeThumbnails.SOURCE_PREFIX + videoId + "/hqdefault.jpg"))
                .timeout(Duration.ofSeconds(5)).GET().build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                boolean jpegType = response.headers().firstValue("content-type").orElse("").startsWith("image/jpeg");
                if (response.statusCode() != 200 || !jpegType) {
                    log.warn("Miniatura YouTube {}: status {}, bez zapisu", videoId, response.statusCode());
                    return Optional.empty();
                }
                return jpeg(body.readNBytes(MAX_BYTES + 1));
            }
        } catch (IOException e) {
            log.warn("Miniatura YouTube {}: blad pobrania ({})", videoId, e.getClass().getSimpleName());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /** Za duze albo bez sygnatury JPEG (FF D8 FF) odrzucamy, niezaleznie od naglowka. */
    static Optional<byte[]> jpeg(byte[] bytes) {
        boolean signature = bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        return signature && bytes.length <= MAX_BYTES ? Optional.of(bytes) : Optional.empty();
    }
}
