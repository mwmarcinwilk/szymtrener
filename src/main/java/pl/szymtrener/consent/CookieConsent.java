package pl.szymtrener.consent;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Aktualna decyzja jednej przegladarki. Historia decyzji lezy w {@link CookieConsentChange}. */
@Entity
@Table(name = "cookie_consent")
public class CookieConsent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consent_key", nullable = false, unique = true, updatable = false)
    private UUID consentKey;

    @Column(nullable = false) private boolean statistics;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected CookieConsent() {}

    CookieConsent(UUID consentKey, boolean statistics) {
        this.consentKey = consentKey;
        this.statistics = statistics;
    }

    void change(boolean statistics) {
        this.statistics = statistics;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getConsentKey() { return consentKey; }
    public boolean isStatistics() { return statistics; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
