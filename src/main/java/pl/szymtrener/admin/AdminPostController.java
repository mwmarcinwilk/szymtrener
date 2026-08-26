package pl.szymtrener.admin;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.content.*;
import pl.szymtrener.content.EditorHtml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.szymtrener.seo.IndexNowService;
import pl.szymtrener.seo.AiReadinessAnalyzer;
import pl.szymtrener.seo.SeoScoreService;
import pl.szymtrener.web.PostPageModel;

import java.time.*;
import java.util.*;

@Controller
@RequestMapping("/admin/posty")
public class AdminPostController {

    private static final Logger log = LoggerFactory.getLogger(AdminPostController.class);
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final int PAGE_SIZE = 20;
    private static final java.time.format.DateTimeFormatter MONTH =
            java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("pl-PL"));

    private final PostRepository posts;
    private final PostService postService;
    private final CategoryRepository categories;
    private final AuthorRepository authors;
    private final AiReadinessAnalyzer analyzer;
    private final SeoScoreService seoScore;
    private final EditorHtml editorHtml;
    private final IndexNowService indexNow;
    private final PostPageModel postPage;

    public AdminPostController(PostRepository posts, PostService postService, CategoryRepository categories,
                               AuthorRepository authors, AiReadinessAnalyzer analyzer, SeoScoreService seoScore, EditorHtml editorHtml,
                               IndexNowService indexNow,
                               PostPageModel postPage) {
        this.posts = posts;
        this.postService = postService;
        this.categories = categories;
        this.authors = authors;
        this.analyzer = analyzer;
        this.seoScore = seoScore;
        this.editorHtml = editorHtml;
        this.indexNow = indexNow;
        this.postPage = postPage;
    }

    /** Kratka kalendarza: jeden dzien miesiaca z ewentualnymi znacznikami publikacji. */
    public record CalendarDay(int number, boolean muted, boolean today, boolean published, boolean scheduled) {}

    @GetMapping
    public String list(@RequestParam(required = false) PostStatus status,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int strona, Model model) {
        Pageable page = PageRequest.of(strona, PAGE_SIZE);
        boolean searching = q != null && !q.isBlank();
        model.addAttribute("posts", searching ? posts.searchByTitle(q.trim(), page)
                : status == null ? posts.findAllByOrderByUpdatedAtDesc(page)
                : posts.findByStatusOrderByUpdatedAtDesc(status, page));

        model.addAttribute("query", searching ? q.trim() : null);
        model.addAttribute("activeStatus", status);
        model.addAttribute("countAll", posts.count());
        model.addAttribute("countPublished", posts.countByStatus(PostStatus.PUBLISHED));
        model.addAttribute("countScheduled", posts.countByStatus(PostStatus.SCHEDULED));
        model.addAttribute("countDraft", posts.countByStatus(PostStatus.DRAFT));
        model.addAttribute("countArchived", posts.countByStatus(PostStatus.ARCHIVED));

        YearMonth month = YearMonth.now(ZONE);
        model.addAttribute("calendar", calendar(month));
        model.addAttribute("calendarLabel", MONTH.format(month));

        model.addAttribute("baseUrl", searching ? "/admin/posty?q=" + q.trim()
                : status == null ? "/admin/posty" : "/admin/posty?status=" + status);
        model.addAttribute("title", "Posty");
        return "admin/posts";
    }

    /**
     * Siatka kalendarza zaczyna sie od poniedzialku i jest dopelniona dniami
     * sasiednich miesiecy — inaczej kolumny nie trzymalyby sie dni tygodnia.
     */
    private List<CalendarDay> calendar(YearMonth month) {
        LocalDate first = month.atDay(1);
        LocalDate today = LocalDate.now(ZONE);
        LocalDate gridStart = first.minusDays(first.getDayOfWeek().getValue() - 1L);
        LocalDate gridEnd = gridStart.plusDays(41);

        Set<LocalDate> published = new HashSet<>();
        Set<LocalDate> scheduled = new HashSet<>();
        for (Object[] row : posts.calendar(gridStart.atStartOfDay(ZONE).toInstant(),
                                           gridEnd.plusDays(1).atStartOfDay(ZONE).toInstant())) {
            if (row[1] == null) continue;
            LocalDate day = ((Instant) row[1]).atZone(ZONE).toLocalDate();
            (row[0] == PostStatus.PUBLISHED ? published : scheduled).add(day);
        }

        // pelne tygodnie: 6 rzedow miesci kazdy uklad miesiaca, takze luty od niedzieli
        List<CalendarDay> days = new ArrayList<>(42);
        for (LocalDate day = gridStart; !day.isAfter(gridEnd); day = day.plusDays(1)) {
            days.add(new CalendarDay(day.getDayOfMonth(),
                    day.getMonth() != first.getMonth(),
                    day.equals(today),
                    published.contains(day),
                    scheduled.contains(day)));
        }
        return days;
    }

    @GetMapping("/nowy")
    public String create(Model model) {
        PostForm form = new PostForm();
        form.getSummaryPoints().addAll(List.of("", "", ""));
        model.addAttribute("form", form);
        model.addAttribute("categories", categories.findAllByOrderBySortOrderAsc());
        // Ocena pustego szkicu, zeby obie listy kryteriow byly widoczne od pierwszej sekundy
        // — autor ma wiedziec, czego sie od niego oczekuje, zanim zacznie pisac.
        model.addAttribute("score", seoScore.evaluate("", "", "", ""));
        model.addAttribute("title", "Nowy post");
        return "admin/post-editor";
    }

    /**
     * Transakcja jest tu konieczna: toForm() siega po tags, summaryPoints i faq,
     * a te sa leniwe i aplikacja ma open-in-view=false.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String edit(@PathVariable Long id, Model model) {
        Post post = posts.findById(id).orElseThrow(() -> new NotFoundException("Nie ma wpisu " + id));
        model.addAttribute("form", toForm(post));
        model.addAttribute("categories", categories.findAllByOrderBySortOrderAsc());
        model.addAttribute("score", seoScore.evaluate(post.getContentHtml(),
                post.getSeoTitle(), post.getSeoDescription(), post.getCoverAlt()));
        model.addAttribute("title", "Edycja: " + post.getTitle());
        return "admin/post-editor";
    }

    @PostMapping("/zapisz")
    @Transactional
    public String save(@ModelAttribute("form") PostForm form, Model model, RedirectAttributes flash) {
        Post saved;
        try {
            saved = persist(form);
        } catch (EditorHtml.InvalidContentException e) {
            // Wracamy na formularz z tym, co autor napisal — utrata tresci przy
            // odrzuconym zapisie bylaby gorsza niz sam brak altu.
            model.addAttribute("categories", categories.findAllByOrderBySortOrderAsc());
            model.addAttribute("score", seoScore.evaluate(form.getContentHtml(), form.getSeoTitle(),
                                                          form.getSeoDescription(), form.getCoverAlt()));
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", form.getId() == null ? "Nowy post" : "Edycja wpisu");
            return "admin/post-editor";
        }
        if (saved.getStatus() == PostStatus.PUBLISHED) {
            indexNow.submit(List.of("/blog/" + saved.getSlug(), "/blog"));
        }
        flash.addFlashAttribute("info", "Wpis zapisany.");
        return "redirect:/admin/posty/" + saved.getId();
    }

    /**
     * Autozapis z edytora (co 15 s, gdy sa zmiany). Zgodnie ze zleceniem pierwszy
     * autozapis nowego wpisu ZAKLADA szkic i zwraca jego id — kolejne ida juz na to id.
     *
     * Autozapis nigdy nie zmienia statusu: wpis opublikowany zapisuje sie dalej jako
     * opublikowany, ale nowy powstaje wylacznie jako szkic. Inaczej pisanie w tle
     * wypuszczaloby na produkcje polowe zdania.
     */
    @PostMapping({"/autozapis", "/{id}/autozapis"})
    @ResponseBody
    @Transactional
    public Map<String, Object> autosave(@PathVariable(required = false) Long id,
                                        @RequestBody PostForm form) {
        if (id != null) form.setId(id);
        if (form.getId() == null) form.setStatus(PostStatus.DRAFT.name());
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            return Map.of("ok", false, "reason", "Autozapis czeka na tytuł wpisu.");
        }
        try {
            Post saved = persist(form);
            return Map.of("ok", true,
                          "id", saved.getId(),
                          "savedAt", LocalDateTime.now(ZONE).withNano(0).toString());
        } catch (EditorHtml.InvalidContentException e) {
            // brak altu nie moze przerwac pisania — mowimy o tym i czekamy
            return Map.of("ok", false, "reason", e.getMessage());
        }
    }

    /** Jedyna sciezka zapisu wpisu — dziela ja formularz i autozapis. */
    private Post persist(PostForm form) {
        Post post = form.getId() == null ? new Post()
                : posts.findById(form.getId()).orElseThrow(() -> new NotFoundException("Nie ma wpisu"));

        post.setTitle(form.getTitle());
        // Slug ustawia PostService.save(): tylko on wie, jaki adres byl wczesniej,
        // i tylko on moze odlozyc stary do historii przekierowan.
        post.setLead(form.getLead());
        post.setContentHtml(form.getContentHtml());
        post.setContentDelta(form.getContentDelta());
        post.setCoverMediaId(form.getCoverMediaId());
        post.setCoverAlt(form.getCoverAlt());
        post.setCoverCaption(form.getCoverCaption());
        post.setSeoTitle(form.getSeoTitle());
        post.setSeoDescription(form.getSeoDescription());
        post.setCategory(form.getCategoryId() == null ? null
                : categories.findById(form.getCategoryId()).orElse(null));
        if (post.getAuthor() == null) post.setAuthor(authors.findAll().stream().findFirst().orElse(null));

        post.getTags().clear();
        if (form.getTags() != null) {
            Arrays.stream(form.getTags().split(",")).map(String::trim).filter(t -> !t.isEmpty())
                    .forEach(post.getTags()::add);
        }

        post.getSummaryPoints().clear();
        form.getSummaryPoints().stream().filter(s -> s != null && !s.isBlank())
                .forEach(post.getSummaryPoints()::add);

        post.getFaq().clear();
        for (int i = 0; i < form.getFaqQuestions().size(); i++) {
            String question = form.getFaqQuestions().get(i);
            String answer = i < form.getFaqAnswers().size() ? form.getFaqAnswers().get(i) : null;
            if (question != null && !question.isBlank() && answer != null && !answer.isBlank()) {
                PostFaq item = new PostFaq();
                item.setQuestion(question.trim());
                item.setAnswer(answer.trim());
                post.addFaq(item);
            }
        }

        PostStatus status = PostStatus.valueOf(form.getStatus());
        post.setStatus(status);
        if (status == PostStatus.SCHEDULED && form.getPublishAt() != null && !form.getPublishAt().isBlank()) {
            post.setPublishAt(LocalDateTime.parse(form.getPublishAt()).atZone(ZONE).toInstant());
        }

        Post saved = postService.save(post, form.getSlug());
        saved.setAiScore(analyzer.analyse(saved).score());
        return saved;
    }

    /**
     * Podglad szkicu dokladnie tak, jak wyglada wpis na stronie. Ten sam widok
     * i ten sam model co strona publiczna — rozni sie tylko paskiem u gory
     * i naglowkiem noindex, zeby szkic nie trafil do wyszukiwarki.
     */
    @GetMapping("/{id}/podglad")
    public String preview(@PathVariable Long id, Model model) {
        return postPage.fill(id, model, true);
    }

    /**
     * Opublikowanego wpisu nie kasujemy — archiwizujemy. Usuniety adres to 404
     * dla bota, ktory go zacytowal, a tego sie juz nie odkreci.
     */
    @PostMapping("/{id}/usun")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        Post post = posts.findById(id).orElseThrow(() -> new NotFoundException("Nie ma wpisu " + id));
        String title = post.getTitle();
        boolean removed = postService.deleteOrArchive(post);
        log.info("{} wpisu {} ({})", removed ? "Usuniecie" : "Archiwizacja", id, title);
        flash.addFlashAttribute("info", removed
                ? "Szkic \u201E" + title + "\u201D zosta\u0142 usuni\u0119ty."
                : "Wpis \u201E" + title + "\u201D trafi\u0142 do archiwum. Jego adres dalej dzia\u0142a.");
        return "redirect:/admin/posty";
    }

    /**
     * Ocena na zywo w prawej kolumnie edytora — bez zapisu wpisu.
     * Ta sama lista warunkow co w `score()` w admin-editor.js.
     */
    @PostMapping("/ocena")
    @ResponseBody
    public SeoScoreService.Result score(@RequestBody PostForm form) {
        return seoScore.evaluate(form.getContentHtml(), form.getSeoTitle(),
                                 form.getSeoDescription(), form.getCoverAlt());
    }

    /**
     * Wpis zlozony z tego, co jest teraz w formularzu — nigdy nie trafia do bazy.
     * Musi miec komplet pol czytanych przez obie siatki, inaczej ocena na zywo
     * rozjedzie sie z ocena policzona po zapisie.
     */
    private Post draft(PostForm form) {
        Post draft = new Post();
        draft.setTitle(form.getTitle());
        draft.setSlug(form.getSlug());
        draft.setLead(form.getLead());
        draft.setContentHtml(form.getContentHtml());
        draft.setCoverMediaId(form.getCoverMediaId());
        draft.setCoverAlt(form.getCoverAlt());
        draft.setSeoTitle(form.getSeoTitle());
        draft.setSeoDescription(form.getSeoDescription());
        draft.setStatus(PostStatus.valueOf(form.getStatus()));
        draft.setUpdatedAt(Instant.now());
        if (form.getCategoryId() != null) {
            categories.findById(form.getCategoryId()).ifPresent(draft::setCategory);
        }
        for (int i = 0; i < form.getFaqQuestions().size(); i++) {
            String question = form.getFaqQuestions().get(i);
            if (question != null && !question.isBlank()) {
                PostFaq item = new PostFaq();
                item.setQuestion(question);
                item.setAnswer(i < form.getFaqAnswers().size() ? form.getFaqAnswers().get(i) : "");
                draft.addFaq(item);
            }
        }
        return draft;
    }

    private PostForm toForm(Post post) {
        PostForm form = new PostForm();
        form.setId(post.getId());
        form.setTitle(post.getTitle());
        form.setSlug(post.getSlug());
        form.setLead(post.getLead());
        // Do edytora wraca postac z klasami q-*, inaczej Quill nie rozpozna blokow
        // i przy otwarciu wpisu skasowalby film, PDF i zrodlo zdjecia.
        form.setContentHtml(editorHtml.toEditor(post.getContentHtml()));
        form.setContentDelta(post.getContentDelta());
        form.setCategoryId(post.getCategory() != null ? post.getCategory().getId() : null);
        form.setCoverMediaId(post.getCoverMediaId());
        form.setCoverAlt(post.getCoverAlt());
        form.setCoverCaption(post.getCoverCaption());
        form.setStatus(post.getStatus().name());
        form.setSeoTitle(post.getSeoTitle());
        form.setSeoDescription(post.getSeoDescription());
        form.setTags(String.join(", ", post.getTags()));
        form.getSummaryPoints().addAll(post.getSummaryPoints());
        while (form.getSummaryPoints().size() < 3) form.getSummaryPoints().add("");
        post.getFaq().forEach(f -> {
            form.getFaqQuestions().add(f.getQuestion());
            form.getFaqAnswers().add(f.getAnswer());
        });
        if (post.getPublishAt() != null) {
            form.setPublishAt(LocalDateTime.ofInstant(post.getPublishAt(), ZONE).withSecond(0).withNano(0).toString());
        }
        return form;
    }
}
