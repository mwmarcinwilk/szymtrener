package pl.szymtrener.consent;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Ciasteczko niesie wylacznie identyfikator zgody. Sama decyzja zyje w bazie, wiec zgoda
 * usunieta przez admina przestaje dzialac od razu, a nie po wygasnieciu ciasteczka.
 *
 * Przez HTTPS nazwa ma prefiks {@code __Host-}: przegladarka przyjmie je tylko z {@code Secure},
 * bez {@code Domain} i ze sciezka {@code /}, wiec nie da sie go podrzucic po HTTP ani z subdomeny.
 * Po HTTPS czytamy wylacznie te nazwe. Zwykla nazwa zostaje dla lokalnego HTTP.
 */
public final class ConsentCookie {

    public static final String NAME = "st_zgoda";
    public static final String SECURE_NAME = "__Host-" + NAME;
    static final Duration MAX_AGE = Duration.ofDays(182);
    private static final Pattern UUID_FORMAT =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private ConsentCookie() {}

    static Optional<UUID> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        String name = name(request);
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null
                    && UUID_FORMAT.matcher(cookie.getValue()).matches()) {
                return Optional.of(UUID.fromString(cookie.getValue()));
            }
        }
        return Optional.empty();
    }

    /** Przez {@link ResponseCookie}, bo SameSite z {@code Cookie.setAttribute} nie kazdy kontener wypisuje. */
    public static void write(UUID key, HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(name(request), key.toString())
                .path("/")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .maxAge(MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static String name(HttpServletRequest request) {
        return request.isSecure() ? SECURE_NAME : NAME;
    }
}
