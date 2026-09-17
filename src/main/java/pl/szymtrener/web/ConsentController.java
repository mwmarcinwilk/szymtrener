package pl.szymtrener.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import pl.szymtrener.common.Downloads;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.consent.ConsentCookie;
import pl.szymtrener.consent.ConsentService;
import pl.szymtrener.consent.ConsentSource;
import pl.szymtrener.submission.RateLimiter;

import java.io.IOException;
import java.time.Duration;
import java.time.Year;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Zgoda na ciasteczka od strony odwiedzajacego. Wzorzec z ddd: formularz dziala bez JS
 * (POST i powrot na strone), a JS wysyla ten sam formularz w tle i dostaje 204.
 */
@Controller
@Tag(name = "Zgoda na cookies", description = "Panel zgody na stronie publicznej")
public class ConsentController {

    /**
     * Zapisy zgody z jednego adresu, nowe i zmiany razem: bez tego jedna zgoda i petla zmian
     * rozdmuchuja historie. Prog z zapasem na wspolny adres (siec komorkowa, biuro).
     */
    static final int WRITES_PER_CLIENT = 120;
    static final Duration WRITES_WINDOW = Duration.ofHours(1);

    private final ConsentService consents;
    private final ObjectMapper json;
    private final AppProperties props;
    /** Osobna pula: boty zapychajace tabele zgod nie moga zjesc limitu formularzy kontaktowych. */
    private final RateLimiter rateLimiter = new RateLimiter(WRITES_PER_CLIENT, WRITES_WINDOW);

    public ConsentController(ConsentService consents, ObjectMapper json, AppProperties props) {
        this.consents = consents;
        this.json = json;
        this.props = props;
    }

    @Operation(summary = "Strona ustawień cookies",
               description = "HTML: panel zgody, a przy zapisanej zgodzie jej identyfikator i link do pobrania danych.")
    @ApiResponse(responseCode = "200", description = "Strona HTML")
    @GetMapping("/ustawienia-cookies")
    public String settings(Model model) {
        model.addAttribute("year", Year.now(ZoneId.of("Europe/Warsaw")).getValue());
        model.addAttribute("canonical", props.absolute("/ustawienia-cookies"));
        return "ustawienia-cookies";
    }

    /**
     * Zapis wyboru.
     *
     * @param choice   {@code tak} i {@code nie} z przyciskow; {@code zapisz} bierze stan przelacznika.
     *                 Kazda inna wartosc to odmowa.
     * @param redirect sciezka powrotu; tylko lokalna, zeby formularz nie byl otwartym przekierowaniem
     */
    @Operation(summary = "Zapis decyzji o cookies",
               description = "Formularz (application/x-www-form-urlencoded) z tokenem CSRF. Nowa zgoda albo zmiana ustawia "
                       + "ciasteczko st_zgoda (po HTTPS __Host-st_zgoda); ta sama decyzja niczego nie zapisuje. "
                       + "Limit 120 zapisów na godzinę z jednego adresu.")
    @ApiResponse(responseCode = "302", description = "Bez nagłówka X-Requested-With: powrót na lokalną ścieżkę z pola powrot; "
            + "także po przekroczeniu limitu, wtedy bez zapisu")
    @ApiResponse(responseCode = "204", description = "Z nagłówkiem X-Requested-With: fetch, zapisano albo bez zmian")
    @ApiResponse(responseCode = "429", description = "Z nagłówkiem X-Requested-With: fetch, przekroczony limit; nic nie zapisano")
    @ApiResponse(responseCode = "403", description = "Brak albo nieważny token CSRF")
    @PostMapping("/zgoda-cookies")
    public Object consent(@Parameter(description = "tak = akceptacja, nie = tylko niezbędne, zapisz = stan przełącznika statystyka")
                          @RequestParam("wybor") String choice,
                          @Parameter(description = "Obecny (dowolna wartość) = zgoda na statystykę; liczy się tylko przy wybor=zapisz")
                          @RequestParam(name = "statystyka", required = false) String statistics,
                          @Parameter(description = "Ścieżka powrotu; tylko lokalna, spoza /admin i /error, inaczej /")
                          @RequestParam(name = "powrot", required = false) String redirect,
                          @Parameter(description = "Miejsce decyzji: BAR, DIALOG albo PAGE (domyślnie PAGE)")
                          @RequestParam(name = "zrodlo", required = false) String source,
                          @Parameter(description = "fetch = odpowiedź 204/429 zamiast przekierowania")
                          @RequestHeader(name = "X-Requested-With", required = false) String requestedWith,
                          HttpServletRequest request, HttpServletResponse response) {
        boolean granted = switch (choice) {
            case "tak" -> true;
            case "zapisz" -> statistics != null;
            default -> false;
        };
        boolean fetch = "fetch".equals(requestedWith);

        Optional<ConsentService.Status> existing = consents.current(request);
        // Adres z RemoteIpValve (forward-headers-strategy: native), nie surowy X-Forwarded-For, ktory klient podrobi.
        boolean allowed = rateLimiter.allow(request.getRemoteAddr());
        // Ta sama decyzja nie odnawia ciasteczka: zyje ono wtedy najwyzej pol roku od ostatniej zmiany,
        // a rekord rok, wiec ciasteczko nigdy nie przezyje rekordu usunietego przez retencje.
        boolean changed = existing.map(status -> status.statistics() != granted).orElse(true);
        if (allowed && changed) {
            UUID key = consents.record(existing.map(ConsentService.Status::key).orElse(null), granted, ConsentSource.parse(source));
            ConsentCookie.write(key, request, response);
        }
        if (fetch) return allowed ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        return "redirect:" + safeRedirect(redirect);
    }

    /** Dane zgody tej przegladarki (RODO art. 15). Tylko po identyfikatorze z jej ciasteczka. */
    @Operation(summary = "Pobranie danych własnej zgody (RODO art. 15)",
               description = "JSON do pobrania: identyfikator, stan, historia decyzji i odsłony zapisane za zgodą. "
                       + "Zgoda wyłącznie z ciasteczka tej przeglądarki, bez parametru z identyfikatorem.")
    @ApiResponse(responseCode = "200", description = "Plik zgoda-cookies.json (attachment, no-store)")
    @ApiResponse(responseCode = "404", description = "Brak ciasteczka albo zgody nie ma w bazie")
    @GetMapping("/ustawienia-cookies/moje-dane")
    public ResponseEntity<byte[]> myData(HttpServletRequest request) throws IOException {
        Optional<Map<String, Object>> data = consents.current(request)
                .flatMap(status -> consents.export(status.key()));
        if (data.isEmpty()) return ResponseEntity.notFound().build();
        return Downloads.json(json.writeValueAsBytes(data.get()), "zgoda-cookies.json");
    }

    static String safeRedirect(String path) {
        if (path == null || !path.startsWith("/") || path.startsWith("//") || path.contains("\\")
                || path.startsWith("/admin") || path.startsWith("/error") || path.chars().anyMatch(ConsentController::unsafeInRedirect)) {
            return "/";
        }
        return path;
    }

    /**
     * Znaki sterujace przegladarka wycina z adresu (TAB w {@code /<TAB>/host} daje {@code //host}),
     * klamry {@code redirect:} rozwija jako szablon URI i konczy sie bledem 500, a znak spoza ASCII
     * Tomcat wycina razem z naglowkiem Location. Sciezka z szablonu (getRequestURI) jest zawsze ASCII.
     */
    private static boolean unsafeInRedirect(int c) {
        return c < 0x20 || c > 0x7E || c == '{' || c == '}';
    }
}
