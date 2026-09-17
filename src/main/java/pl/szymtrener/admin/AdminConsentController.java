package pl.szymtrener.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import pl.szymtrener.common.Downloads;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.consent.ConsentService;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Zgody na ciasteczka w panelu. Osoba podaje identyfikator ze strony „Ustawienia cookies",
 * a trener pobiera jej dane (RODO art. 15) albo je usuwa (art. 17).
 */
@Controller
@Tag(name = "Panel: zgody cookies", description = "Wymaga roli ADMIN")
public class AdminConsentController {

    private static final Logger log = LoggerFactory.getLogger(AdminConsentController.class);

    private final ConsentService consents;
    private final ObjectMapper json;

    public AdminConsentController(ConsentService consents, ObjectMapper json) {
        this.consents = consents;
        this.json = json;
    }

    @Operation(summary = "Lista zgód cookies", description = "HTML, 25 na stronę, od ostatnio zmienionej.")
    @ApiResponse(responseCode = "200", description = "Strona HTML")
    @ApiResponse(responseCode = "302", description = "Niezalogowany: przekierowanie na logowanie")
    @GetMapping("/admin/zgody-cookies")
    public String list(@Parameter(description = "Początek identyfikatora zgody (0-9, a-f, -); inne znaki dają pustą listę")
                       @RequestParam(required = false) String q,
                       @Parameter(description = "Numer strony od 0") @RequestParam(defaultValue = "0") int strona, Model model) {
        String query = q == null ? "" : q.trim();
        model.addAttribute("items", consents.search(query, strona));
        model.addAttribute("consentQuery", query);
        model.addAttribute("baseUrl", query.isEmpty() ? "/admin/zgody-cookies"
                : UriComponentsBuilder.fromPath("/admin/zgody-cookies").queryParam("q", query).encode().build().toUriString());
        model.addAttribute("title", "Zgody cookies");
        return "admin/consents";
    }

    @Operation(summary = "Pobranie danych zgody (RODO art. 15)", description = "Ten sam JSON, który osoba pobiera ze strony ustawień.")
    @ApiResponse(responseCode = "200", description = "Plik zgoda-cookies-{key}.json (attachment, no-store)")
    @ApiResponse(responseCode = "404", description = "Niepoprawny identyfikator albo zgody nie ma")
    @ApiResponse(responseCode = "302", description = "Niezalogowany: przekierowanie na logowanie")
    @GetMapping("/admin/zgody-cookies/{key}/dane")
    public ResponseEntity<byte[]> export(@Parameter(description = "Pełny identyfikator zgody (UUID)") @PathVariable String key) throws IOException {
        UUID consentKey = ConsentService.parseKey(key).orElseThrow(() -> notFound(key));
        Map<String, Object> data = consents.export(consentKey).orElseThrow(() -> notFound(key));
        return Downloads.json(json.writeValueAsBytes(data), "zgoda-cookies-" + consentKey + ".json");
    }

    @Operation(summary = "Usunięcie zgody (RODO art. 17)",
               description = "Twarde usunięcie zgody, jej historii i odsłon zapisanych za zgodą. Formularz z tokenem CSRF.")
    @ApiResponse(responseCode = "302", description = "Powrót na listę z komunikatem (usunięto albo zgody już nie było)")
    @ApiResponse(responseCode = "404", description = "Niepoprawny identyfikator")
    @ApiResponse(responseCode = "403", description = "Brak albo nieważny token CSRF")
    @PostMapping("/admin/zgody-cookies/{key}/usun")
    public String delete(@Parameter(description = "Pełny identyfikator zgody (UUID)") @PathVariable String key, Principal principal, RedirectAttributes flash) {
        UUID consentKey = ConsentService.parseKey(key).orElseThrow(() -> notFound(key));
        if (consents.delete(consentKey)) {
            log.info("Usunięcie zgody cookies {} przez {}", consentKey, principal != null ? principal.getName() : "panel");
            flash.addFlashAttribute("info", "Zgoda i powiązane z nią odsłony zostały usunięte.");
        } else {
            flash.addFlashAttribute("error", "Tej zgody już nie ma w bazie.");
        }
        return "redirect:/admin/zgody-cookies";
    }

    private static NotFoundException notFound(String key) {
        return new NotFoundException("Nie ma zgody " + key);
    }
}
