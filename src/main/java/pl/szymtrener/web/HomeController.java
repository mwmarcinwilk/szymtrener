package pl.szymtrener.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.content.PostRepository;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class HomeController {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final DateTimeFormatter PL = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl-PL"));

    private final PostRepository posts;
    private final AppProperties props;

    public HomeController(PostRepository posts, AppProperties props) {
        this.posts = posts;
        this.props = props;
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
        return "index";
    }

    @GetMapping("/polityka-prywatnosci")
    public String privacy(Model model) {
        model.addAttribute("year", Year.now(ZONE).getValue());
        model.addAttribute("canonical", props.absolute("/polityka-prywatnosci"));
        return "polityka-prywatnosci";
    }
}
