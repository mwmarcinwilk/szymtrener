package pl.szymtrener.seo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.content.Post;
import pl.szymtrener.content.PostView;

import java.util.*;

/**
 * Dane strukturalne budujemy z tych samych obiektow, z ktorych renderuje sie HTML.
 * Dzieki temu JSON-LD nie moze opisywac czegos, czego na stronie nie ma —
 * spojnosc schema z widoczna trescia jest osobno punktowana w audycie.
 */
@Service
public class JsonLdService {

    private final AppProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonLdService(AppProperties props) {
        this.props = props;
    }

    public List<String> forBlogList(String canonical, List<PostView> posts) {
        Map<String, Object> blog = new LinkedHashMap<>();
        blog.put("@context", "https://schema.org");
        blog.put("@type", "Blog");
        blog.put("@id", canonical + "#blog");
        blog.put("url", canonical);
        blog.put("name", "Blog Szymona Domagały – trening i długowieczność");
        blog.put("inLanguage", "pl-PL");
        blog.put("publisher", organization());
        blog.put("blogPost", posts.stream().map(p -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("@type", "BlogPosting");
            item.put("headline", p.title());
            item.put("url", props.absolute("/blog/" + p.slug()));
            item.put("datePublished", p.publishedIso());
            item.put("author", person(p.authorName()));
            return item;
        }).toList());

        return List.of(write(blog), write(breadcrumbs(List.of(
                Map.entry("Strona główna", props.absolute("/")),
                Map.entry("Blog", props.absolute("/blog"))))));
    }

    public List<String> forPost(Post post, PostView view, String canonical) {
        List<String> out = new ArrayList<>();

        Map<String, Object> article = new LinkedHashMap<>();
        article.put("@context", "https://schema.org");
        article.put("@type", "BlogPosting");
        article.put("@id", canonical + "#article");
        article.put("mainEntityOfPage", canonical);
        article.put("headline", post.getTitle());
        article.put("description", view.lead());
        if (view.coverAbsoluteUrl() != null) article.put("image", view.coverAbsoluteUrl());
        article.put("datePublished", view.publishedIso());
        article.put("dateModified", view.modifiedIso());
        article.put("inLanguage", "pl-PL");
        article.put("wordCount", post.getWordCount());
        article.put("timeRequired", "PT" + post.getReadingMinutes() + "M");
        if (!post.getTags().isEmpty()) article.put("keywords", String.join(", ", post.getTags()));
        if (post.getCategory() != null) article.put("articleSection", post.getCategory().getName());
        article.put("author", authorNode(post));
        article.put("publisher", organization());
        out.add(write(article));

        List<Map.Entry<String, String>> trail = new ArrayList<>(List.of(
                Map.entry("Strona główna", props.absolute("/")),
                Map.entry("Blog", props.absolute("/blog"))));
        if (post.getCategory() != null) {
            trail.add(Map.entry(post.getCategory().getName(),
                    props.absolute("/blog/kategoria/" + post.getCategory().getSlug())));
        }
        trail.add(Map.entry(post.getTitle(), canonical));
        out.add(write(breadcrumbs(trail)));

        if (!view.faq().isEmpty()) {
            Map<String, Object> faq = new LinkedHashMap<>();
            faq.put("@context", "https://schema.org");
            faq.put("@type", "FAQPage");
            faq.put("@id", canonical + "#faq");
            faq.put("mainEntity", view.faq().stream().map(f -> {
                Map<String, Object> q = new LinkedHashMap<>();
                q.put("@type", "Question");
                q.put("name", f.question());
                q.put("acceptedAnswer", Map.of("@type", "Answer", "text", stripTags(f.answer())));
                return q;
            }).toList());
            out.add(write(faq));
        }
        return out;
    }

    private Map<String, Object> authorNode(Post post) {
        if (post.getAuthor() == null) return person("Szymon Domagała");
        Map<String, Object> author = new LinkedHashMap<>();
        author.put("@type", "Person");
        author.put("@id", props.absolute("/#" + post.getAuthor().getSlug()));
        author.put("name", post.getAuthor().getName());
        if (post.getAuthor().getJobTitle() != null) author.put("jobTitle", post.getAuthor().getJobTitle());
        author.put("url", props.absolute("/"));
        if (!post.getAuthor().getSameAs().isEmpty()) author.put("sameAs", post.getAuthor().getSameAs());
        return author;
    }

    private Map<String, Object> person(String name) {
        return new LinkedHashMap<>(Map.of("@type", "Person", "name", name == null ? "Szymon Domagała" : name));
    }

    private Map<String, Object> organization() {
        Map<String, Object> org = new LinkedHashMap<>();
        org.put("@type", "Organization");
        org.put("@id", props.absolute("/#organizacja"));
        org.put("name", "Fit and Health Studio Treningów Personalnych – Szymon Domagała");
        org.put("url", props.absolute("/"));
        org.put("logo", props.absolute("/images/szymon-portret.jpeg"));
        return org;
    }

    private Map<String, Object> breadcrumbs(List<Map.Entry<String, String>> trail) {
        Map<String, Object> crumbs = new LinkedHashMap<>();
        crumbs.put("@context", "https://schema.org");
        crumbs.put("@type", "BreadcrumbList");
        List<Map<String, Object>> items = new ArrayList<>();
        int position = 1;
        for (Map.Entry<String, String> entry : trail) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("@type", "ListItem");
            item.put("position", position++);
            item.put("name", entry.getKey());
            item.put("item", entry.getValue());
            items.add(item);
        }
        crumbs.put("itemListElement", items);
        return crumbs;
    }

    private static String stripTags(String html) {
        return org.jsoup.Jsoup.parse(html == null ? "" : html).text();
    }

    private String write(Object node) {
        try {
            // </script> w tresci rozbilby tag <script> — zabezpieczenie
            return mapper.writeValueAsString(node).replace("</", "<\\/");
        } catch (Exception e) {
            return "{}";
        }
    }
}
