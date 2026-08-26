package pl.szymtrener.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.common.SlugUtil;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.media.MediaRepository;
import pl.szymtrener.media.MediaService;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PostService {

    static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final int RELATED_COUNT = 3;
    private static final DateTimeFormatter PL_DATE =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl-PL"));
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PostRepository posts;
    private final CategoryRepository categories;
    private final ContentMetrics metrics;
    private final HtmlSanitizer sanitizer;
    private final EditorHtml editorHtml;
    private final MediaService media;
    private final MediaRepository mediaFiles;
    private final PostSlugHistoryRepository slugHistory;
    private final PostMediaRepository postMedia;
    private final AppProperties props;

    public PostService(PostRepository posts, CategoryRepository categories, ContentMetrics metrics,
                       HtmlSanitizer sanitizer, MediaService media, MediaRepository mediaFiles,
                       PostSlugHistoryRepository slugHistory, PostMediaRepository postMedia,
                       AppProperties props, EditorHtml editorHtml) {
        this.posts = posts;
        this.categories = categories;
        this.metrics = metrics;
        this.sanitizer = sanitizer;
        this.editorHtml = editorHtml;
        this.media = media;
        this.mediaFiles = mediaFiles;
        this.slugHistory = slugHistory;
        this.postMedia = postMedia;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public Page<PostView> published(String categorySlug, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        Page<Post> result = (categorySlug == null)
                ? posts.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, pageable)
                : posts.findByStatusAndCategorySlugOrderByPublishedAtDesc(PostStatus.PUBLISHED, categorySlug, pageable);
        return result.map(this::toCardView);
    }

    @Transactional(readOnly = true)
    public Post requirePublished(String slug) {
        return posts.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Nie ma wpisu: " + slug));
    }

    /**
     * Najpierw wpisy z tej samej kategorii; jesli jest ich mniej niz trzy,
     * DOKLADAMY najnowsze z reszty bloga zamiast podmieniac cala liste —
     * inaczej trafny wpis z kategorii wypadal na rzecz przypadkowego.
     */
    @Transactional(readOnly = true)
    public List<PostView> related(Post post) {
        List<Post> found = new ArrayList<>((post.getCategory() != null)
                ? posts.findTop3ByStatusAndCategoryIdAndIdNotOrderByPublishedAtDesc(
                        PostStatus.PUBLISHED, post.getCategory().getId(), post.getId())
                : List.of());

        if (found.size() < RELATED_COUNT) {
            Set<Long> already = found.stream().map(Post::getId).collect(java.util.stream.Collectors.toSet());
            already.add(post.getId());
            for (Post candidate : posts.findTop6ByStatusAndIdNotOrderByPublishedAtDesc(
                    PostStatus.PUBLISHED, post.getId())) {
                if (found.size() >= RELATED_COUNT) break;
                if (already.add(candidate.getId())) found.add(candidate);
            }
        }
        return found.stream().map(this::toCardView).toList();
    }

    /** Wyszukiwarka bloga — po kolumnie search_vector (tsvector + indeks GIN). */
    @Transactional(readOnly = true)
    public Page<PostView> search(String query, int page, int size) {
        String q = query == null ? "" : query.trim();
        if (q.isBlank()) return Page.empty(PageRequest.of(Math.max(0, page), size));
        return posts.search(q, PageRequest.of(Math.max(0, page), size)).map(this::toCardView);
    }

    /** Zapis licznika — wlasna transakcja, bo @Modifying bez niej nie przejdzie. */
    @Transactional
    public void registerView(Long postId) {
        posts.incrementViewCount(postId);
    }

    /** Wersja pelna — do widoku artykulu. Wymaga transakcji (leniwe kolekcje). */
    @Transactional(readOnly = true)
    public PostView toFullView(Post p) {
        List<PostView.FaqView> faq = p.getFaq().stream()
                .map(f -> new PostView.FaqView(f.getQuestion(), f.getAnswer()))
                .toList();
        String coverUrl = media.publicUrl(p.getCoverMediaId());
        return new PostView(
                p.getId(), p.getSlug(), p.getTitle(), p.getLead(), p.getContentHtml(),
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getCategory() != null ? p.getCategory().getSlug() : null,
                coverUrl,
                coverUrl != null ? props.absolute(coverUrl) : null,
                p.getCoverAlt(), p.getCoverCaption(),
                iso(p.getPublishedAt()), label(p.getPublishedAt()), iso(p.getUpdatedAt()),
                p.getReadingMinutes(),
                p.getAuthor() != null ? p.getAuthor().getName() : null,
                p.getAuthor() != null ? p.getAuthor().getBio() : null,
                p.getAuthor() != null ? p.getAuthor().getPhotoPath() : null,
                p.isHasVideo(), p.isHasPdf(),
                List.copyOf(p.getSummaryPoints()), faq);
    }

    /** Wersja skrocona — kafelek na liscie; bez tresci i kolekcji. */
    public PostView toCardView(Post p) {
        String coverUrl = media.publicUrl(p.getCoverMediaId());
        return new PostView(
                p.getId(), p.getSlug(), p.getTitle(), p.getLead(), null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getCategory() != null ? p.getCategory().getSlug() : null,
                coverUrl, coverUrl != null ? props.absolute(coverUrl) : null,
                p.getCoverAlt(), p.getCoverCaption(),
                iso(p.getPublishedAt()), label(p.getPublishedAt()), iso(p.getUpdatedAt()),
                p.getReadingMinutes(),
                p.getAuthor() != null ? p.getAuthor().getName() : null, null, null,
                p.isHasVideo(), p.isHasPdf(), List.of(), List.of());
    }

    // ─── zapis z panelu ──────────────────────────────────────────────

    @Transactional
    public Post save(Post post) {
        return save(post, post.getSlug());
    }

    /**
     * @param desiredSlug slug zadany w formularzu; pusty = wyliczany z tytulu.
     *                    Stary adres opublikowanego wpisu trafia do historii,
     *                    zeby dalo sie z niego odeslac 301 zamiast 404.
     */
    @Transactional
    public Post save(Post post, String desiredSlug) {
        String previousSlug = post.getSlug();
        boolean wasEverPublished = post.getPublishedAt() != null;

        String base = (desiredSlug == null || desiredSlug.isBlank())
                ? SlugUtil.slugify(post.getTitle())
                : SlugUtil.slugify(desiredSlug);
        post.setSlug(uniqueSlug(base, post.getId()));

        // Kolejnosc jest istotna: najpierw zamiana reprezentacji edytora na HTML
        // publikacji, dopiero potem bialalista. Odwrotnie sanitizer zdjalby atrybuty
        // data-*, z ktorych ta zamiana czyta.
        String publication = editorHtml.toPublication(post.getContentHtml());
        post.setContentHtml(sanitizer.clean(publication));
        editorHtml.requireImageAlts(post.getContentHtml());
        ContentMetrics.Result m = metrics.analyse(post.getContentHtml(), post.getLead());
        post.setWordCount(m.wordCount());
        post.setReadingMinutes(m.readingMinutes());
        post.setHasVideo(m.hasVideo());
        post.setHasPdf(m.hasPdf());
        post.setUpdatedAt(Instant.now());
        if (post.getStatus() == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }

        Post saved = posts.save(post);

        // Wpis zajmuje swoj aktualny adres — gdyby ten adres byl kiedys w historii,
        // przekierowanie wskazywaloby samo na siebie.
        slugHistory.deleteBySlug(saved.getSlug());
        if (wasEverPublished && previousSlug != null && !previousSlug.equals(saved.getSlug())) {
            slugHistory.save(new PostSlugHistory(previousSlug, saved.getId()));
        }

        syncMediaLinks(saved);
        return saved;
    }

    /**
     * Odtwarza powiazania wpis–plik na podstawie tresci. Bez tego nie da sie
     * odpowiedziec na pytanie „czy ten plik wolno usunac z biblioteki".
     */
    private void syncMediaLinks(Post post) {
        Set<Long> inline = new LinkedHashSet<>();
        Document doc = Jsoup.parseBodyFragment(post.getContentHtml() == null ? "" : post.getContentHtml());

        for (Element img : doc.select("img[src]")) {
            mediaIdFromUrl(img.attr("src")).ifPresent(inline::add);
        }
        for (Element node : doc.select("[data-media-id]")) {
            try {
                inline.add(Long.parseLong(node.attr("data-media-id").trim()));
            } catch (NumberFormatException ignored) { /* atrybut z recznej edycji */ }
        }
        for (Element link : doc.select("a[href]")) {
            mediaIdFromUrl(link.attr("href")).ifPresent(inline::add);
        }

        postMedia.deleteByPostId(post.getId());
        Set<Long> known = new LinkedHashSet<>();
        for (Long mediaId : inline) {
            if (mediaFiles.existsById(mediaId) && known.add(mediaId)) {
                postMedia.save(new PostMedia(post.getId(), mediaId, "INLINE"));
            }
        }
        Long cover = post.getCoverMediaId();
        if (cover != null && mediaFiles.existsById(cover)) {
            postMedia.save(new PostMedia(post.getId(), cover, "COVER"));
        }
    }

    /** /media/2026/08/abc.jpg oraz /pliki/12/plan.pdf -> id pliku w bibliotece. */
    private Optional<Long> mediaIdFromUrl(String url) {
        if (url == null || url.isBlank()) return Optional.empty();
        String path = url.startsWith("http") ? url.replaceFirst("^https?://[^/]+", "") : url;
        int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);

        if (path.startsWith("/media/")) {
            return mediaFiles.findByStorageKey(path.substring("/media/".length()))
                    .map(pl.szymtrener.media.MediaFile::getId);
        }
        if (path.startsWith("/pliki/")) {
            String rest = path.substring("/pliki/".length());
            int slash = rest.indexOf('/');
            try {
                return Optional.of(Long.parseLong(slash < 0 ? rest : rest.substring(0, slash)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public String uniqueSlug(String base, Long selfId) {
        String candidate = base.isBlank() ? "wpis" : base;
        int i = 2;
        while (true) {
            Optional<Post> existing = posts.findBySlug(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(selfId)) return candidate;
            candidate = base + "-" + i++;
        }
    }

    /**
     * Tytuly wpisow, ktore korzystaja z pliku — w tresci albo jako zdjecie glowne.
     * Pusta lista znaczy „mozna usunac".
     */
    @Transactional(readOnly = true)
    public List<String> postsUsing(Long mediaId) {
        Set<String> titles = new LinkedHashSet<>(postMedia.titlesUsingMedia(mediaId));
        titles.addAll(posts.titlesWithCover(mediaId));
        return List.copyOf(titles);
    }

    /**
     * Opublikowany wpis archiwizujemy zamiast kasowac: usuniety adres to 404 dla
     * bota, ktory go zacytowal. Szkic mozna skasowac — nikt go nie widzial.
     */
    @Transactional
    public boolean deleteOrArchive(Post post) {
        if (post.getPublishedAt() != null || post.getStatus() == PostStatus.PUBLISHED) {
            post.setStatus(PostStatus.ARCHIVED);
            post.setUpdatedAt(Instant.now());
            posts.save(post);
            return false;
        }
        postMedia.deleteByPostId(post.getId());
        posts.delete(post);
        return true;
    }

    public List<Category> categories() { return categories.findAllByOrderBySortOrderAsc(); }

    static String iso(Instant instant) {
        return instant == null ? null : ISO_DATE.format(instant.atZone(ZONE));
    }

    static String label(Instant instant) {
        return instant == null ? null : PL_DATE.format(instant.atZone(ZONE));
    }
}
