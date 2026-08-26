package pl.szymtrener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cala konfiguracja domenowa w jednym miejscu, wstrzykiwana jako rekord. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String siteUrl,
        String brandName,
        Mail mail,
        Admin admin,
        Media media,
        IndexNow indexnow,
        Analytics analytics
) {
    public record Mail(String recipient, String from, boolean autoReply) {}
    public record Admin(String email, String password) {}
    public record Media(int maxImageWidth, float jpegQuality, String allowedMime) {}
    public record IndexNow(boolean enabled, String key) {}
    public record Analytics(boolean enabled, String salt) {}

    /** Absolutny URL kanoniczny — uzywany w canonical, OG, JSON-LD i sitemapie. */
    public String absolute(String path) {
        String base = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
        return path.startsWith("/") ? base + path : base + "/" + path;
    }
}
