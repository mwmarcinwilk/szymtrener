package pl.szymtrener.consent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.szymtrener.PostgresTestBase;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Zapis zgody z historia, eksport, usuniecie razem z odslonami i retencja. */
class ConsentServiceIT extends PostgresTestBase {

    @Autowired ConsentService service;
    @Autowired CookieConsentRepository consents;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("delete from cookie_consent");
        jdbc.execute("delete from page_view");
    }

    @Test
    @DisplayName("nowa decyzja zakłada zgodę, zmiana dopisuje historię, ta sama decyzja niczego nie dopisuje")
    void recordsHistory() {
        UUID key = service.record(null, true, ConsentSource.BAR);
        UUID same = service.record(key, false, ConsentSource.DIALOG);
        service.record(key, false, ConsentSource.PAGE);

        assertThat(same).isEqualTo(key);
        assertThat(consents.findByConsentKey(key)).get().extracting(CookieConsent::isStatistics).isEqualTo(false);
        assertThat(jdbc.queryForList("select source from cookie_consent_change order by id", String.class))
                .containsExactly("BAR", "DIALOG");
    }

    @Test
    @DisplayName("identyfikator, którego nie ma w bazie, dostaje nową zgodę z nowym identyfikatorem")
    void unknownKeyStartsNewConsent() {
        UUID unknown = UUID.randomUUID();

        UUID key = service.record(unknown, true, ConsentSource.PAGE);

        assertThat(key).isNotEqualTo(unknown);
        assertThat(consents.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("eksport ma historię i tylko odsłony zapisane z kluczem tej zgody")
    @SuppressWarnings("unchecked")
    void exportsOwnData() {
        UUID key = service.record(null, true, ConsentSource.BAR);
        UUID other = service.record(null, true, ConsentSource.BAR);
        pageView("/blog", idOf(key));
        pageView("/", null);
        pageView("/oferta", idOf(other));

        Map<String, Object> data = service.export(key).orElseThrow();

        assertThat(data).containsEntry("identyfikator", key).containsEntry("statystyka", true);
        assertThat((List<Map<String, Object>>) data.get("historia")).hasSize(1)
                .first().satisfies(entry -> assertThat(entry).containsEntry("wersja_tresci", ConsentService.POLICY_VERSION));
        assertThat((List<Map<String, Object>>) data.get("odslony")).extracting(view -> view.get("adres")).containsExactly("/blog");
        assertThat(service.export(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("usunięcie kasuje zgodę, historię i odsłony za zgodą, reszta statystyki zostaje")
    void deletesEverythingLinked() {
        UUID key = service.record(null, true, ConsentSource.BAR);
        UUID other = service.record(null, true, ConsentSource.BAR);
        pageView("/", idOf(key));
        pageView("/", null);
        pageView("/", idOf(other));

        assertThat(service.delete(key)).isTrue();

        assertThat(consents.findByConsentKey(key)).isEmpty();
        assertThat(consents.findByConsentKey(other)).isPresent();
        assertThat(jdbc.queryForObject("select count(*) from cookie_consent_change", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from page_view", Long.class)).isEqualTo(2);
        assertThat(service.delete(key)).isFalse();
    }

    @Test
    @DisplayName("retencja kasuje tylko zgody bez zmiany dłużej niż próg, razem z ich odsłonami")
    void purgesStaleConsents() {
        UUID stale = service.record(null, true, ConsentSource.BAR);
        UUID fresh = service.record(null, true, ConsentSource.BAR);
        pageView("/", idOf(stale));
        pageView("/", idOf(fresh));
        jdbc.update("update cookie_consent set updated_at = now() - interval '400 days' where consent_key = ?", stale);

        int removed = service.purgeNotChangedSince(Instant.now().minus(Duration.ofDays(365)));

        assertThat(removed).isEqualTo(1);
        assertThat(consents.findByConsentKey(stale)).isEmpty();
        assertThat(consents.findByConsentKey(fresh)).isPresent();
        assertThat(jdbc.queryForList("select consent_id from page_view", Long.class)).containsExactly(idOf(fresh));
    }

    @Test
    @DisplayName("szuka po początku identyfikatora, a znaki spoza UUID dają pustą listę")
    void searchesByPrefix() {
        UUID key = service.record(null, true, ConsentSource.BAR);
        service.record(null, true, ConsentSource.BAR);

        assertThat(service.search("", 0).getTotalElements()).isEqualTo(2);
        assertThat(service.search(key.toString().substring(0, 8).toUpperCase(), 0).getContent())
                .extracting(ConsentService.Row::key).contains(key);
        assertThat(service.search(key.toString(), 0).getContent()).extracting(ConsentService.Row::key).containsExactly(key);
        assertThat(service.search("%", 0)).isEmpty();
        assertThat(service.search("' or 1=1 --", 0)).isEmpty();
    }

    private Long idOf(UUID key) {
        return consents.findByConsentKey(key).orElseThrow().getId();
    }

    private void pageView(String path, Long consentId) {
        jdbc.update("insert into page_view (path, session_hash, consent_id) values (?, 'dzienny', ?)", path, consentId);
    }
}
