package pl.szymtrener.admin;

import java.util.ArrayList;
import java.util.List;

/** Formularz edytora. Pola „strukturalne" (skrót, FAQ) sa listami, nie HTML-em. */
public class PostForm {
    private Long id;
    private String title = "";
    private String slug = "";
    private String lead = "";
    private String contentHtml = "";
    private String contentDelta;
    private Long categoryId;
    private Long coverMediaId;
    private String coverAlt = "";
    private String coverCaption = "";
    private String status = "DRAFT";
    private String publishAt;          // yyyy-MM-ddTHH:mm z <input type="datetime-local">
    private String tags = "";          // rozdzielone przecinkiem
    private String seoTitle = "";
    private String seoDescription = "";
    private List<String> summaryPoints = new ArrayList<>();
    private List<String> faqQuestions = new ArrayList<>();
    private List<String> faqAnswers = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getLead() { return lead; }
    public void setLead(String lead) { this.lead = lead; }
    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }
    public String getContentDelta() { return contentDelta; }
    public void setContentDelta(String contentDelta) { this.contentDelta = contentDelta; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getCoverMediaId() { return coverMediaId; }
    public void setCoverMediaId(Long coverMediaId) { this.coverMediaId = coverMediaId; }
    public String getCoverAlt() { return coverAlt; }
    public void setCoverAlt(String coverAlt) { this.coverAlt = coverAlt; }
    public String getCoverCaption() { return coverCaption; }
    public void setCoverCaption(String coverCaption) { this.coverCaption = coverCaption; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPublishAt() { return publishAt; }
    public void setPublishAt(String publishAt) { this.publishAt = publishAt; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    public List<String> getSummaryPoints() { return summaryPoints; }
    public void setSummaryPoints(List<String> summaryPoints) { this.summaryPoints = summaryPoints; }
    public List<String> getFaqQuestions() { return faqQuestions; }
    public void setFaqQuestions(List<String> faqQuestions) { this.faqQuestions = faqQuestions; }
    public List<String> getFaqAnswers() { return faqAnswers; }
    public void setFaqAnswers(List<String> faqAnswers) { this.faqAnswers = faqAnswers; }
}
