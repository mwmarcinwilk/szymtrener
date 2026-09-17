package pl.szymtrener.consent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CookieConsentChangeRepository extends JpaRepository<CookieConsentChange, Long> {

    List<CookieConsentChange> findByConsentIdOrderByChangedAtDesc(Long consentId);
}
