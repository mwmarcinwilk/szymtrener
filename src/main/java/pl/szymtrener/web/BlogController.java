package pl.szymtrener.web;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.content.*;
import pl.szymtrener.seo.JsonLdService;
import pl.szymtrener.settings.SettingsService;

import java.time.Year;
import java.time.ZoneId;
import java.util.List;

@Controller
public class BlogController {

    private static final int DEFAULT_PAGE_SIZE = 9;

    private final PostService postService;
    private final CategoryRepository categories;
    private final PostSlugHistoryRepository slugHistory;
    private final PostRepository posts;
    private final JsonLdService jsonLd;
    private final PostPageModel postPage;
    private final SettingsService settings;
    private final AppProperties props;

    public BlogController(PostService postService, CategoryRepository categories,
                          PostSlugHistoryRepository slugHistory, PostRepository posts,
                          JsonLdService jsonLd, PostPageModel postPage,
                          SettingsService settings, AppProperties props) {
        this.postService = postService;
        this.categories = categories;
        this.slugHistory = slugHistory;
        this.posts = posts;
        this.jsonLd = jsonLd;
        this.postPage = postPage;
        this.settings = settings;
        this.props = props;
    }

    /** Polska odmiana liczebnika: 1 wpis, 2–4 wpisy, 5+ wpisow (i 12–14 wpisow). */
    static String foundLabel(long count) {
        long lastTwo = count % 100;
        long last = count % 10;
        String noun = (count == 1) ? "wpis"
                : (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) ? "wpisy"
                : "wpisów";
        return "Znaleziono " + count + " " + noun + ".";
    }

    private int pageSize() {
        return settings.getInt(SettingsService.BLOG_PAGE_SIZE, DEFAULT_PAGE_SIZE);
    }

    @GetMapping("/blog")
    public String list(@RequestParam(defaultValue = "0") int strona, Model model) {
        return renderList(null, strona, model);
    }

    @GetMapping("/blog/kategoria/{slug}")
    public String byCategory(@PathVariable String slug, @RequestParam(defaultValue = "0") int strona, Model model) {
        Category category = categories.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Nie ma kategorii: " + slug));
        model.addAttribute("activeCategory", category);
        return renderList(slug, strona, model);
    }

    private String renderList(String categorySlug, int page, Model model) {
        Page<PostView> result = postService.published(categorySlug, page, pageSize());
        List<PostView> items = result.getContent();

        PostView featured = null;
        if (page == 0 && categorySlug == null && !items.isEmpty()) {
            featured = items.get(0);
            items = items.subList(1, items.size());
        }

        String base = categorySlug == null ? "/blog" : "/blog/kategoria/" + categorySlug;
        String canonical = props.absolute(page == 0 ? base : base + "?strona=" + page);

        model.addAttribute("posts", items);
        model.addAttribute("featured", featured);
        model.addAttribute("categories", categories.findAllByOrderBySortOrderAsc());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("nextPageUrl", base + "?strona=" + (page + 1));
        model.addAttribute("canonical", canonical);
        model.addAttribute("pageTitle", settings.get(SettingsService.SEO_TITLE,
                "Blog o treningu i długowieczności | Szymon Domagała"));
        model.addAttribute("pageDescription", settings.get(SettingsService.SEO_DESC,
                "Blog trenera Szymona Domagały: trening siłowy po 40-tce, sarkopenia, regeneracja i długowieczność."));
        model.addAttribute("blogLead",
                "Piszę o tym, co realnie działa: treningu siłowym po 40-tce, zatrzymywaniu utraty mięśni, regeneracji i długowieczności. Bez mitów, bez skrótów.");
        model.addAttribute("siteUrl", props.siteUrl());
        model.addAttribute("jsonLd", jsonLd.forBlogList(canonical, result.getContent()));
        model.addAttribute("year", Year.now(ZoneId.of("Europe/Warsaw")).getValue());
        model.addAttribute("lastModified", items.isEmpty() ? null : items.get(0).modifiedIso());
        model.addAttribute("lastModifiedLabel", items.isEmpty() ? null : items.get(0).publishedLabel());
        if (!model.containsAttribute("activeCategory")) model.addAttribute("activeCategory", null);
        return "blog/list";
    }

    /** Wyniki wyszukiwania — ta sama siatka co lista, ale bez indeksowania. */
    @GetMapping("/blog/szukaj")
    public String search(@RequestParam(name = "q", required = false) String query,
                         @RequestParam(defaultValue = "0") int strona, Model model) {
        String phrase = query == null ? "" : query.trim();
        Page<PostView> result = postService.search(phrase, strona, pageSize());

        model.addAttribute("posts", result.getContent());
        model.addAttribute("featured", null);
        model.addAttribute("categories", categories.findAllByOrderBySortOrderAsc());
        model.addAttribute("activeCategory", null);
        model.addAttribute("searchQuery", phrase);
        model.addAttribute("resultLabel", foundLabel(result.getTotalElements()));
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("nextPageUrl", "/blog/szukaj?q=" + java.net.URLEncoder.encode(
                phrase, java.nio.charset.StandardCharsets.UTF_8) + "&strona=" + (strona + 1));
        model.addAttribute("canonical", props.absolute("/blog"));
        model.addAttribute("robots", "noindex, follow");
        model.addAttribute("pageTitle", (phrase.isBlank() ? "Szukaj na blogu" : "Szukaj: " + phrase)
                + " | Szymon Domagała");
        model.addAttribute("pageDescription", "Wyszukiwarka wpisów na blogu Szymona Domagały.");
        model.addAttribute("blogLead", phrase.isBlank()
                ? "Wpisz, czego szukasz. Przeszukam tytuły i zajawki wpisów."
                : "Znalezione wpisy dla frazy „" + phrase + "”.");
        model.addAttribute("siteUrl", props.siteUrl());
        model.addAttribute("jsonLd", List.of());
        model.addAttribute("year", Year.now(ZoneId.of("Europe/Warsaw")).getValue());
        model.addAttribute("lastModified", null);
        model.addAttribute("lastModifiedLabel", null);
        return "blog/list";
    }

    @GetMapping("/blog/{slug}")
    public ModelAndView post(@PathVariable String slug, Model model) {
        Long id = posts.findIdBySlugAndStatus(slug, PostStatus.PUBLISHED).orElse(null);
        if (id == null) {
            return new ModelAndView(redirectFromOldSlug(slug));
        }
        String view = postPage.fill(id, model, false);
        postService.registerView(id);
        return new ModelAndView(view);
    }

    /**
     * Adres zmieniony po publikacji: odsylamy 301, a nie 404. Bot cytujacy trzyma
     * stary adres i 404 kasuje cytat; przekierowanie stale przenosi go na nowy.
     */
    private RedirectView redirectFromOldSlug(String slug) {
        Post target = slugHistory.findBySlug(slug)
                .flatMap(history -> posts.findById(history.getPostId()))
                .filter(Post::isPublished)
                .orElseThrow(() -> new NotFoundException("Nie ma wpisu: " + slug));

        RedirectView redirect = new RedirectView("/blog/" + target.getSlug());
        redirect.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return redirect;
    }

}
