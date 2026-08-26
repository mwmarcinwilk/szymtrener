package pl.szymtrener.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pl.szymtrener.submission.FormRequests;
import pl.szymtrener.submission.RateLimiter;
import pl.szymtrener.submission.SubmissionService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/zgloszenia")
public class PublicFormController {

    private final SubmissionService submissions;
    private final RateLimiter rateLimiter;

    public PublicFormController(SubmissionService submissions, RateLimiter rateLimiter) {
        this.submissions = submissions;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/online")
    public ResponseEntity<?> online(@Valid @RequestBody FormRequests.OnlineForm form,
                                    BindingResult errors, HttpServletRequest request) {
        ResponseEntity<?> rejection = reject(errors, form.botcheck(), request);
        if (rejection != null) return rejection;
        submissions.acceptOnline(form, ip(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/kontakt")
    public ResponseEntity<?> contact(@Valid @RequestBody FormRequests.ContactForm form,
                                     BindingResult errors, HttpServletRequest request) {
        ResponseEntity<?> rejection = reject(errors, form.botcheck(), request);
        if (rejection != null) return rejection;
        submissions.acceptContact(form, ip(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private ResponseEntity<?> reject(BindingResult errors, Boolean honeypot, HttpServletRequest request) {
        // honeypot: pole ukryte przed czlowiekiem, zaznaczane przez boty.
        // Odpowiadamy sukcesem, zeby nie podpowiadac botowi, ze zostal wykryty.
        if (Boolean.TRUE.equals(honeypot)) return ResponseEntity.ok(Map.of("ok", true));

        if (!rateLimiter.allow(ip(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("ok", false, "message", "Za dużo zgłoszeń z tego adresu. Spróbuj później lub zadzwoń: 502 338 373."));
        }
        if (errors.hasErrors()) {
            Map<String, String> fields = new LinkedHashMap<>();
            errors.getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(Map.of("ok", false, "errors", fields));
        }
        return null;
    }

    private static String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
