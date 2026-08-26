package pl.szymtrener.seo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.szymtrener.config.AppProperties;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;

/**
 * IndexNow: jedno zadanie POST po publikacji wpisu. Bing wspiera protokol
 * natywnie, a indeks Bing jest glownym zrodlem odkrywania URL-i dla ChatGPT Search.
 */
@Service
public class IndexNowService {

    private static final Logger log = LoggerFactory.getLogger(IndexNowService.class);
    private static final String ENDPOINT = "https://api.indexnow.org/indexnow";

    private final AppProperties props;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public IndexNowService(AppProperties props) {
        this.props = props;
    }

    @Async
    public void submit(List<String> paths) {
        if (!props.indexnow().enabled() || props.indexnow().key() == null || props.indexnow().key().isBlank()) return;

        String host = URI.create(props.siteUrl()).getHost();
        String urls = paths.stream()
                .map(p -> "\"" + props.absolute(p) + "\"")
                .reduce((a, b) -> a + "," + b).orElse("");
        String payload = """
                {"host":"%s","key":"%s","keyLocation":"%s","urlList":[%s]}
                """.formatted(host, props.indexnow().key(), props.absolute("/" + props.indexnow().key() + ".txt"), urls);

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("IndexNow: {} dla {} adresow", response.statusCode(), paths.size());
        } catch (Exception e) {
            log.warn("IndexNow nieudany: {}", e.getMessage());
        }
    }
}
