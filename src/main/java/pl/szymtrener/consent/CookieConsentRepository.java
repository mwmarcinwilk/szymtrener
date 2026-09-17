package pl.szymtrener.consent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CookieConsentRepository extends JpaRepository<CookieConsent, Long> {

    Optional<CookieConsent> findByConsentKey(UUID consentKey);

    /** Szukanie po poczatku identyfikatora: admin dostaje go zwykle przepisany z ekranu osoby. */
    @Query(value = "select * from cookie_consent where cast(consent_key as text) like :prefix || '%' order by updated_at desc",
           countQuery = "select count(*) from cookie_consent where cast(consent_key as text) like :prefix || '%'",
           nativeQuery = true)
    Page<CookieConsent> findByKeyPrefix(@Param("prefix") String prefix, Pageable pageable);

    @Modifying
    @Query("delete from CookieConsent c where c.updatedAt < :cutoff")
    int deleteNotChangedSince(@Param("cutoff") Instant cutoff);
}
