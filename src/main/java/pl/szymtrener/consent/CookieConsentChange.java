package pl.szymtrener.consent;

import jakarta.persistence.*;
import java.time.Instant;

/** Jedna decyzja w historii zgody: dowod, na co i kiedy sie zgodzono. */
@Entity
@Table(name = "cookie_consent_change")
public class CookieConsentChange {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consent_id", nullable = false) private Long consentId;
    @Column(nullable = false) private boolean statistics;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10) private ConsentSource source;

    @Column(name = "policy_version", nullable = false, length = 20) private String policyVersion;
    @Column(name = "changed_at", nullable = false) private Instant changedAt = Instant.now();

    protected CookieConsentChange() {}

    CookieConsentChange(Long consentId, boolean statistics, ConsentSource source, String policyVersion) {
        this.consentId = consentId;
        this.statistics = statistics;
        this.source = source;
        this.policyVersion = policyVersion;
    }

    public boolean isStatistics() { return statistics; }
    public ConsentSource getSource() { return source; }
    public String getPolicyVersion() { return policyVersion; }
    public Instant getChangedAt() { return changedAt; }
}
