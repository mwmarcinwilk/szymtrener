package pl.szymtrener.content;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "post")
public class Post {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    /** Odpowiedz „answer-first" tuz po H1 — 40–80 slow. Wymagana przed publikacja. */
    @Column(name = "lead", columnDefinition = "text")
    private String lead;

    @Column(name = "content_html", nullable = false, columnDefinition = "text")
    private String contentHtml = "";

    /** Delta z Quilla — zrodlo prawdy edytora; HTML jest renderowana pochodna. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_delta")
    private String contentDelta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id")
    private Author author;

    @Column(name = "cover_media_id")
    private Long coverMediaId;

    @Column(name = "cover_alt")
    private String coverAlt;

    @Column(name = "cover_caption")
    private String coverCaption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "publish_at")
    private Instant publishAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "seo_title")
    private String seoTitle;

    @Column(name = "seo_description")
    private String seoDescription;

    @Column(name = "reading_minutes", nullable = false)
    private int readingMinutes;

    @Column(name = "word_count", nullable = false)
    private int wordCount;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "has_video", nullable = false)
    private boolean hasVideo;

    @Column(name = "has_pdf", nullable = false)
    private boolean hasPdf;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_tag", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    private Set<String> tags = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_summary_point", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "position")
    @Column(name = "text", columnDefinition = "text")
    private List<String> summaryPoints = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position asc")
    private List<PostFaq> faq = new ArrayList<>();

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }

    public boolean isPublished() { return status == PostStatus.PUBLISHED; }

    /**
     * Data w kolumnie listy: dla opublikowanego liczy sie data publikacji,
     * dla zaplanowanego termin, dla szkicu ostatnia zmiana. Format gotowy do
     * wyswietlenia — szablon nie formatuje dat samodzielnie.
     */
    @Transient
    public String dateLabel() {
        java.time.ZoneId zone = java.time.ZoneId.of("Europe/Warsaw");
        java.util.Locale pl = java.util.Locale.forLanguageTag("pl-PL");
        java.time.format.DateTimeFormatter date = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy", pl);
        java.time.format.DateTimeFormatter time = java.time.format.DateTimeFormatter.ofPattern("HH:mm", pl);

        if (status == PostStatus.PUBLISHED && publishedAt != null) {
            return date.format(publishedAt.atZone(zone));
        }
        if (status == PostStatus.SCHEDULED && publishAt != null) {
            return date.format(publishAt.atZone(zone)) + "<br>" + time.format(publishAt.atZone(zone));
        }
        return "zmiana<br>" + date.format(updatedAt.atZone(zone));
    }

    public void addFaq(PostFaq item) { item.setPost(this); item.setPosition(faq.size()); faq.add(item); }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLead() { return lead; }
    public void setLead(String lead) { this.lead = lead; }
    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }
    public String getContentDelta() { return contentDelta; }
    public void setContentDelta(String contentDelta) { this.contentDelta = contentDelta; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
    public Long getCoverMediaId() { return coverMediaId; }
    public void setCoverMediaId(Long coverMediaId) { this.coverMediaId = coverMediaId; }
    public String getCoverAlt() { return coverAlt; }
    public void setCoverAlt(String coverAlt) { this.coverAlt = coverAlt; }
    public String getCoverCaption() { return coverCaption; }
    public void setCoverCaption(String coverCaption) { this.coverCaption = coverCaption; }
    public PostStatus getStatus() { return status; }
    public void setStatus(PostStatus status) { this.status = status; }
    public Instant getPublishAt() { return publishAt; }
    public void setPublishAt(Instant publishAt) { this.publishAt = publishAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    public int getReadingMinutes() { return readingMinutes; }
    public void setReadingMinutes(int readingMinutes) { this.readingMinutes = readingMinutes; }
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public Integer getAiScore() { return aiScore; }
    public void setAiScore(Integer aiScore) { this.aiScore = aiScore; }
    public boolean isHasVideo() { return hasVideo; }
    public void setHasVideo(boolean hasVideo) { this.hasVideo = hasVideo; }
    public boolean isHasPdf() { return hasPdf; }
    public void setHasPdf(boolean hasPdf) { this.hasPdf = hasPdf; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Set<String> getTags() { return tags; }
    public List<String> getSummaryPoints() { return summaryPoints; }
    public List<PostFaq> getFaq() { return faq; }
}
