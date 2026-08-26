package pl.szymtrener.submission;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prosty limit zgloszen na adres IP. Trzymany w pamieci — przy jednej instancji
 * aplikacji to wystarcza; przy skalowaniu poziomym trzeba go przeniesc do bazy.
 */
@Component
public class RateLimiter {

    private static final int MAX_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofHours(1);

    private record Bucket(int count, Instant windowStart) {}

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean allow(String key) {
        Instant now = Instant.now();
        Bucket updated = buckets.compute(key, (k, current) -> {
            if (current == null || current.windowStart().plus(WINDOW).isBefore(now)) {
                return new Bucket(1, now);
            }
            return new Bucket(current.count() + 1, current.windowStart());
        });
        if (buckets.size() > 10_000) buckets.clear();   // zabezpieczenie pamieci
        return updated.count() <= MAX_PER_WINDOW;
    }
}
