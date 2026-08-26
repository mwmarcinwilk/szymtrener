package pl.szymtrener.submission;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "submission_note")
public class SubmissionNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(nullable = false) private String author;
    @Column(nullable = false, columnDefinition = "text") private String body;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }

    @Transient
    public String getCreatedLabel() {
        return java.time.format.DateTimeFormatter
                .ofPattern("d MMM yyyy, HH:mm", java.util.Locale.forLanguageTag("pl-PL"))
                .format(createdAt.atZone(java.time.ZoneId.of("Europe/Warsaw")));
    }
}
