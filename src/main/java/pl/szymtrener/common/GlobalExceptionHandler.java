package pl.szymtrener.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.ModelAndView;

import java.time.Year;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * Ostatnia linia obrony. Wyjatek dostaje krotki identyfikator, ktory trafia
 * jednoczesnie do logu i na ekran — dzieki temu zgloszenie „nie dziala" da sie
 * powiazac z konkretnym stack trace zamiast szukac po godzinie w logach.
 *
 * Nie dotyka wyjatkow, ktore juz znaja swoj status HTTP: z wlasnym @ResponseStatus
 * (np. NotFoundException) oraz implementujacych ErrorResponse (NoResourceFoundException,
 * HttpRequestMethodNotSupportedException itp.). Te maja isc zwykla sciezka bledow
 * Spring Boota do error/404.html — brakujacy plik to 404, nie awaria serwera.
 */
@ControllerAdvice
@Order(0)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public Object handle(Exception exception, HttpServletRequest request) throws Exception {
        if (exception instanceof ErrorResponse
                || AnnotationUtils.findAnnotation(exception.getClass(), ResponseStatus.class) != null) {
            throw exception;   // np. NotFoundException, NoResourceFoundException — obsluguje je Spring
        }

        String incident = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Błąd [{}] przy {} {}", incident, request.getMethod(), request.getRequestURI(), exception);

        if (wantsJson(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false,
                                 "incident", incident,
                                 "message", "Coś poszło nie tak. Podaj numer " + incident + " przy zgłoszeniu."));
        }

        ModelAndView view = new ModelAndView("error/500");
        view.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        view.addObject("incident", incident);
        view.addObject("year", Year.now(ZoneId.of("Europe/Warsaw")).getValue());
        return view;
    }

    /** Formularze publiczne i edytor rozmawiaja JSON-em; reszta to zwykle strony. */
    private static boolean wantsJson(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/") || path.startsWith("/admin/api/")) return true;

        String accept = request.getHeader(org.springframework.http.HttpHeaders.ACCEPT);
        if (accept == null || accept.isBlank()) return false;
        return accept.contains(MediaType.APPLICATION_JSON_VALUE)
                && !accept.contains(MediaType.TEXT_HTML_VALUE);
    }
}
