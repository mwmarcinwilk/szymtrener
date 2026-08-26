package pl.szymtrener.content;

import java.util.List;

/**
 * Model widoku dla Thymeleafa. Szablon nie dotyka encji ani leniwych kolekcji —
 * daty sa juz sformatowane po polsku, adresy plikow gotowe.
 */
public record PostView(
        Long id,
        String slug,
        String title,
        String lead,
        String contentHtml,
        String categoryName,
        String categorySlug,
        String coverUrl,
        String coverAbsoluteUrl,
        String coverAlt,
        String coverCaption,
        String publishedIso,
        String publishedLabel,
        String modifiedIso,
        int readingMinutes,
        String authorName,
        String authorBio,
        String authorPhotoUrl,
        boolean hasVideo,
        boolean hasPdf,
        List<String> summaryPoints,
        List<FaqView> faq
) {
    public record FaqView(String question, String answer) {}
}
