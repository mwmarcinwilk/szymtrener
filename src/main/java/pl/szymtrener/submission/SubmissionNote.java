package pl.szymtrener.submission;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "submission_note")
public class SubmissionNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Notatka nalezy do zgloszenia albo do klienta; po konwersji dostaje oba. */
    @Column(name = "submission_id")
    private Long submissionId;

    @Column(name = "trainee_id")
    private Long traineeId;

    @Column(nullable = false) private String author;
    @Column(nullable = false, columnDefinition = "text") private String body;

    /** Przypieta notatka stoi na poczatku listy — to, o czym trzeba pamietac zawsze. */
    @Column(nullable = false) private boolean pinned;

    /** Etykiety po przecinku: Zdrowie, Rozmowa, Sprzedaz, Wazne. */
    @Column(length = 120) private String tags;

    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    public Long getTraineeId() { return traineeId; }
    public void setTraineeId(Long traineeId) { this.traineeId = traineeId; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }

    /** Tagi jako lista — szablon nie dzieli lancuchow samodzielnie. */
    @Transient
    public java.util.List<String> tagList() {
        if (tags == null || tags.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim).filter(t -> !t.isEmpty()).toList();
    }

    /**
     * Czy notatka trafia do karty „Na co uwazac". Wyciag z tagow Zdrowie i Wazne,
     * zeby trener widzial przeciwwskazania bez czytania calosci.
     */
    @Transient
    public boolean warning() {
        return tagList().stream().anyMatch(t -> t.equalsIgnoreCase("Zdrowie") || t.equalsIgnoreCase("Ważne"));
    }

    @Transient
    public String getCreatedLabel() {
        return java.time.format.DateTimeFormatter
                .ofPattern("d MMM, HH:mm", java.util.Locale.forLanguageTag("pl-PL"))
                .format(createdAt.atZone(java.time.ZoneId.of("Europe/Warsaw")));
    }
}
