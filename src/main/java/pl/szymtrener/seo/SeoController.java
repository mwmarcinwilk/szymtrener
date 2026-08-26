package pl.szymtrener.seo;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.content.Post;
import pl.szymtrener.content.PostRepository;
import pl.szymtrener.content.PostStatus;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * robots.txt, sitemap.xml i klucz IndexNow generowane z aplikacji, nie z plikow
 * statycznych — dzieki temu nowy wpis pojawia sie w sitemapie od razu po publikacji.
 */
@RestController
public class SeoController {

    private static final DateTimeFormatter W3C = DateTimeFormatter.ISO_INSTANT;
    /** RSS wymaga daty w formacie RFC-822 — po angielsku, niezaleznie od locale serwera. */
    private static final DateTimeFormatter RFC_822 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final PostRepository posts;
    private final AppProperties props;

    public SeoController(PostRepository posts, AppProperties props) {
        this.posts = posts;
        this.props = props;
    }

    /**
     * Jawna lista Allow dla botow cytujacych. Sam brak pliku audyt traktuje jako
     * zaniedbanie (5/15 pkt), a jawne Allow dla >=3 botow Tier-1 daje pelne 15.
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                # robots.txt – szymtrener.pl

                # Boty cytujace (odpowiedzi w ChatGPT, Perplexity, Claude, Google AI Overviews)
                User-agent: OAI-SearchBot
                Allow: /

                User-agent: ChatGPT-User
                Allow: /

                User-agent: PerplexityBot
                Allow: /

                User-agent: Claude-SearchBot
                Allow: /

                User-agent: Claude-User
                Allow: /

                User-agent: Googlebot
                Allow: /

                User-agent: Bingbot
                Allow: /

                # Boty zbierajace dane treningowe
                User-agent: GPTBot
                Allow: /

                User-agent: ClaudeBot
                Allow: /

                User-agent: Google-Extended
                Allow: /

                User-agent: *
                Allow: /
                Disallow: /admin/
                Disallow: /api/

                Sitemap: %s
                """.formatted(props.absolute("/sitemap.xml"));
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        url(xml, props.absolute("/"), Instant.now(), "1.0");
        url(xml, props.absolute("/blog"), Instant.now(), "0.8");
        url(xml, props.absolute("/polityka-prywatnosci"), null, "0.2");
        for (Object[] row : posts.findSlugsForSitemap()) {
            url(xml, props.absolute("/blog/" + row[0]), (Instant) row[1], "0.7");
        }
        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void url(StringBuilder xml, String location, Instant lastMod, String priority) {
        xml.append("  <url>\n    <loc>").append(location).append("</loc>\n");
        if (lastMod != null) {
            xml.append("    <lastmod>").append(W3C.format(lastMod.atOffset(ZoneOffset.UTC))).append("</lastmod>\n");
        }
        xml.append("    <priority>").append(priority).append("</priority>\n  </url>\n");
    }


    /**
     * Kanal RSS. Czytelnicy i agregatory dostaja nowe wpisy bez odwiedzania strony;
     * lista jest ta sama, ktora widzi bot w sitemapie.
     */
    @GetMapping(value = "/feed.xml", produces = "application/rss+xml; charset=UTF-8")
    public String feed() {
        List<Post> latest = posts.findTop20ByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
           .append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n")
           .append("  <channel>\n")
           .append("    <title>").append(escape(props.brandName())).append("</title>\n")
           .append("    <link>").append(props.absolute("/blog")).append("</link>\n")
           .append("    <description>Trening silowy, dlugowiecznosc i regeneracja — blog Szymona Domagaly.</description>\n")
           .append("    <language>pl-pl</language>\n")
           .append("    <atom:link href=\"").append(props.absolute("/feed.xml"))
           .append("\" rel=\"self\" type=\"application/rss+xml\"/>\n");

        if (!latest.isEmpty() && latest.get(0).getPublishedAt() != null) {
            xml.append("    <lastBuildDate>").append(rfc822(latest.get(0).getPublishedAt())).append("</lastBuildDate>\n");
        }

        for (Post post : latest) {
            String link = props.absolute("/blog/" + post.getSlug());
            xml.append("    <item>\n")
               .append("      <title>").append(escape(post.getTitle())).append("</title>\n")
               .append("      <link>").append(link).append("</link>\n")
               .append("      <guid isPermaLink=\"true\">").append(link).append("</guid>\n");
            if (post.getLead() != null && !post.getLead().isBlank()) {
                xml.append("      <description>").append(escape(post.getLead())).append("</description>\n");
            }
            if (post.getCategory() != null) {
                xml.append("      <category>").append(escape(post.getCategory().getName())).append("</category>\n");
            }
            if (post.getPublishedAt() != null) {
                xml.append("      <pubDate>").append(rfc822(post.getPublishedAt())).append("</pubDate>\n");
            }
            xml.append("    </item>\n");
        }

        return xml.append("  </channel>\n</rss>\n").toString();
    }

    private static String rfc822(Instant instant) {
        return RFC_822.format(instant.atZone(java.time.ZoneId.of("Europe/Warsaw")));
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Plik weryfikacyjny IndexNow: /{klucz}.txt musi zwracac ten sam klucz. */
    @GetMapping(value = "/{key}.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String indexNowKey(@PathVariable String key) {
        String configured = props.indexnow().key();
        if (configured == null || configured.isBlank() || !configured.equals(key)) {
            throw new NotFoundException("Nie ma takiego pliku");
        }
        return configured;
    }

    /** llms.txt — dodany jako higiena; audyt V4 nie przyznaje za niego punktow. */
    @GetMapping(value = "/llms.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String llms() {
        List<Object[]> rows = posts.findSlugsForSitemap();
        StringBuilder sb = new StringBuilder("""
                # Szymon Domagała – Trener Longevity, Łódź

                > Trener personalny prowadzący treningi stacjonarne w Łodzi (Armii Krajowej 32a)
                > oraz online w całej Polsce. Specjalizacja: osoby 35–55 lat, trening siłowy
                > i długowieczność. Kontakt: +48 502 338 373, szymtrener@gmail.com

                ## Strony

                - [Strona główna](%s): oferta, cennik, studio, FAQ
                - [Blog](%s): trening siłowy, sarkopenia, regeneracja, odżywianie

                ## Wpisy

                """.formatted(props.absolute("/"), props.absolute("/blog")));
        for (Object[] row : rows) {
            sb.append("- ").append(props.absolute("/blog/" + row[0])).append('\n');
        }
        return sb.toString();
    }
}
