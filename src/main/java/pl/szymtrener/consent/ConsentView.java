package pl.szymtrener.consent;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Stan zgody dla szablonow: {@code ${@cookieConsent.current()}}.
 * Zwykly komponent, nie {@code @ControllerAdvice}, z tego samego powodu co {@code AdminNav}:
 * advice wchodzi do kazdego wycinka {@code @WebMvcTest}.
 */
@Component("cookieConsent")
public class ConsentView {

    private final ConsentService service;

    public ConsentView(ConsentService service) {
        this.service = service;
    }

    /** Biezaca zgoda albo {@code null}, gdy osoba jeszcze nie zdecydowala. */
    public ConsentService.Status current() {
        HttpServletRequest request = request();
        return request == null ? null : service.current(request).orElse(null);
    }

    /**
     * Sciezka, na ktora wraca formularz bez JS. Thymeleaf 3.1 nie daje szablonom obiektu zadania,
     * a kontroler i tak przepuszcza ja przez {@code safeRedirect}.
     */
    public String returnPath() {
        HttpServletRequest request = request();
        return request == null ? "/" : request.getRequestURI();
    }

    private static HttpServletRequest request() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest() : null;
    }
}
