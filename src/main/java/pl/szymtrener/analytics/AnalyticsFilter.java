package pl.szymtrener.analytics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.szymtrener.config.AppProperties;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Wlasna statystyka odwiedzin: bez ciasteczek, bez zewnetrznych skryptow,
 * bez zgody na analitykę. Identyfikator sesji to skrot z IP, przegladarki,
 * soli i daty — po dobie sam wygasa i nie da sie go powiazac z osoba.
 *
 * Boty AI zapisujemy osobno: liczba wizyt GPTBota czy ClaudeBota jest jedynym
 * sygnalem widocznosci, ktory da sie zmierzyc z wlasnego serwera.
 */
@Component
@Order(20)
public class AnalyticsFilter extends OncePerRequestFilter {

    private static final Map<String, String> AI_BOTS = Map.of(
            "gptbot", "GPTBot",
            "oai-searchbot", "OAI-SearchBot",
            "chatgpt-user", "ChatGPT-User",
            "claudebot", "ClaudeBot",
            "claude-searchbot", "Claude-SearchBot",
            "perplexitybot", "PerplexityBot",
            "google-extended", "Google-Extended",
            "bingbot", "Bingbot",
            "googlebot", "Googlebot");

    private final PageViewRepository repository;
    private final AppProperties props;

    public AnalyticsFilter(PageViewRepository repository, AppProperties props) {
        this.repository = repository;
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !props.analytics().enabled()
                || path.startsWith("/admin") || path.startsWith("/api")
                || path.startsWith("/css") || path.startsWith("/js")
                || path.startsWith("/images") || path.startsWith("/media")
                || path.startsWith("/actuator")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, response);
        if (response.getStatus() != 200) return;
        try {
            record(request);
        } catch (Exception ignored) {
            // statystyka nigdy nie moze wywrocic odpowiedzi
        }
    }

    private void record(HttpServletRequest request) {
        String userAgent = String.valueOf(request.getHeader("User-Agent")).toLowerCase(Locale.ROOT);
        String botName = AI_BOTS.entrySet().stream()
                .filter(e -> userAgent.contains(e.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(null);

        PageView view = new PageView();
        view.setPath(request.getRequestURI());
        view.setReferrer(referrerHost(request.getHeader("Referer")));
        view.setBot(botName != null || userAgent.contains("bot") || userAgent.contains("spider"));
        view.setBotName(botName);
        view.setDevice(userAgent.contains("mobi") ? "mobile" : "desktop");
        if (!view.isBot()) {
            view.setSessionHash(hash(clientIp(request) + userAgent + props.analytics().salt() + LocalDate.now()));
        }
        repository.save(view);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private static String referrerHost(String referrer) {
        if (referrer == null || referrer.isBlank()) return null;
        try {
            return java.net.URI.create(referrer).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes())).substring(0, 32);
        } catch (Exception e) {
            return null;
        }
    }
}
