package pl.szymtrener.content;

import jakarta.persistence.*;

import java.time.Instant;

/** Stary adres opublikowanego wpisu — zrodlo przekierowania 301 po zmianie tytulu. */
@Entity
@Table(name = "post_slug_history")
public class PostSlugHistory {

    @Id
    private String slug;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected PostSlugHistory() {}

    public PostSlugHistory(String slug, Long postId) {
        this.slug = slug;
        this.postId = postId;
    }

    public String getSlug() { return slug; }
    public Long getPostId() { return postId; }
    public Instant getCreatedAt() { return createdAt; }
}
