package pl.szymtrener.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regresja: brakujacy plik statyczny (np. /service-worker.js, o ktory pyta stary
 * service worker w przegladarce) konczyl sie stack trace na poziomie ERROR i strona
 * 500. To jest 404 — ma isc zwykla sciezka bledow Spring Boota, bez halasu w logach.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("brakujacy zasob statyczny leci dalej do Springa (404), nie do obslugi 500")
    void rethrowsMissingStaticResource() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/service-worker.js");
        NoResourceFoundException missing = new NoResourceFoundException(HttpMethod.GET, "service-worker.js");

        assertThatThrownBy(() -> handler.handle(missing, request)).isSameAs(missing);
    }

    @Test
    @DisplayName("wyjatek z wlasnym @ResponseStatus nadal leci do Springa")
    void rethrowsAnnotatedException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/blog/nie-ma");
        NotFoundException notFound = new NotFoundException("nie ma");

        assertThatThrownBy(() -> handler.handle(notFound, request)).isSameAs(notFound);
    }

    @Test
    @DisplayName("prawdziwa awaria nadal daje strone 500 z numerem zgloszenia")
    void handlesRealFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

        Object result = handler.handle(new IllegalStateException("baza padla"), request);

        assertThat(result).isInstanceOf(ModelAndView.class);
        ModelAndView view = (ModelAndView) result;
        assertThat(view.getViewName()).isEqualTo("error/500");
        assertThat(view.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(view.getModel().get("incident")).isNotNull();
    }
}
