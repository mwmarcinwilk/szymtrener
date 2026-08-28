package pl.szymtrener.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.content.PostRepository;
import pl.szymtrener.offer.OnlineOfferService;
import pl.szymtrener.offer.StationaryOfferService;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class HomeController {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final DateTimeFormatter PL = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl-PL"));

    private final PostRepository posts;
    private final AppProperties props;
    private final OnlineOfferService offer;
    private final StationaryOfferService stationary;

    public HomeController(PostRepository posts, AppProperties props,
                          OnlineOfferService offer, StationaryOfferService stationary) {
        this.posts = posts;
        this.props = props;
        this.offer = offer;
        this.stationary = stationary;
    }

    @GetMapping("/")
    public String home(Model model) {
        // dateModified strony glownej: data ostatniej zmiany tresci w serwisie
        LocalDate modified = posts.findSlugsForSitemap().stream()
                .map(row -> ((Instant) row[1]).atZone(ZONE).toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now(ZONE));

        model.addAttribute("lastModified", modified.toString());
        model.addAttribute("lastModifiedLabel", PL.format(modified));
        model.addAttribute("year", Year.now(ZONE).getValue());
        model.addAttribute("canonical", props.absolute("/"));

        // Oferta online: ceny, opinie i FAQ pochodza z bazy, zeby Szymon mogl je
        // zmieniac z panelu. Pusta lista = sekcja chowa sie w calosci, bez placeholdera.
        model.addAttribute("packages", offer.packages());
        model.addAttribute("consult", offer.consultation());
        model.addAttribute("testimonials", offer.testimonials());
        model.addAttribute("onlineFaq", offer.faq());
        // Cena w danych strukturalnych musi zgadzac sie z ta na stronie — rozbieznosc
        // jest gorsza niz jej brak.
        model.addAttribute("lowestMonthly", offer.lowestMonthly());

        // Cennik stacjonarny: te same liczby zasilaja karty w sekcji oferty
        // i odpowiedz w FAQ (brief stacjonarny 5.3, jedno zrodlo cennika).
        model.addAttribute("individualPackages", stationary.individual());
        model.addAttribute("pairPackages", stationary.pairs());
        model.addAttribute("stationaryRules", stationary.rules());
        model.addAttribute("stationaryPrices", stationary.priceSentence());
        model.addAttribute("longestValidity", stationary.longestValidity());
        model.addAttribute("pairPrice", stationary.cheapestPair());
        return "index";
    }

    @GetMapping("/polityka-prywatnosci")
    public String privacy(Model model) {
        model.addAttribute("year", Year.now(ZONE).getValue());
        model.addAttribute("canonical", props.absolute("/polityka-prywatnosci"));
        return "polityka-prywatnosci";
    }
}
