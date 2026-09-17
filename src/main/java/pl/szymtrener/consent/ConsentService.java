package pl.szymtrener.consent;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Zgoda na ciasteczka: zapis decyzji z historia, odczyt dla biezacego zadania,
 * eksport (RODO art. 15) i twarde usuniecie (art. 17).
 */
@Service
public class ConsentService {

    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);

    /** Wersja tekstu panelu i polityki, ktora osoba widziala. Zmien przy zmianie tresci zgody. */
    public static final String POLICY_VERSION = "2026-09-17";
    private static final int PAGE_SIZE = 25;
    private static final String REQUEST_ATTRIBUTE = ConsentService.class.getName() + ".current";
    private static final Pattern KEY_PREFIX = Pattern.compile("[0-9a-f-]{1,36}");

    /** Stan zgody widziany przez szablony i filtr statystyk; {@code id} trafia do {@code page_view.consent_id}. */
    public record Status(UUID key, long id, boolean statistics) {}

    /** Wiersz listy w panelu. */
    public record Row(UUID key, boolean statistics, Instant createdAt, Instant updatedAt) {}

    private final CookieConsentRepository consents;
    private final CookieConsentChangeRepository changes;
    private final ConsentedViews views;

    public ConsentService(CookieConsentRepository consents, CookieConsentChangeRepository changes,
                          ConsentedViews views) {
        this.consents = consents;
        this.changes = changes;
        this.views = views;
    }

    /**
     * Zgoda tej przegladarki albo pusto, gdy nie ma decyzji. Ciasteczko z identyfikatorem,
     * ktorego nie ma w bazie (usuniety przez admina, wygasly), to brak decyzji.
     * Wynik zostaje w atrybucie zadania, bo stopka i filtr statystyk pytaja o to samo.
     * Bez {@code @Transactional}: wizyta bez ciasteczka nie ma po co brac polaczenia z puli.
     */
    public Optional<Status> current(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Optional<Status> cached = (Optional<Status>) request.getAttribute(REQUEST_ATTRIBUTE);
        if (cached != null) return cached;
        Optional<Status> status = ConsentCookie.read(request)
                .flatMap(consents::findByConsentKey)
                .map(c -> new Status(c.getConsentKey(), c.getId(), c.isStatistics()));
        request.setAttribute(REQUEST_ATTRIBUTE, status);
        return status;
    }

    /**
     * Zapisuje decyzje. Istniejaca zgoda dostaje nowy wpis historii, brak zgody zaklada nowa
     * z losowym identyfikatorem. Ta sama decyzja co zapisana nie dopisuje niczego, wiec powtarzany
     * formularz nie rozdmuchuje historii ani nie odsuwa retencji. Zwraca identyfikator do ciasteczka.
     */
    @Transactional
    public UUID record(UUID existingKey, boolean statistics, ConsentSource source) {
        CookieConsent consent = existingKey == null ? null : consents.findByConsentKey(existingKey).orElse(null);
        if (consent != null && consent.isStatistics() == statistics) {
            return consent.getConsentKey();
        }
        if (consent != null) {
            consent.change(statistics);
        } else {
            consent = consents.save(new CookieConsent(UUID.randomUUID(), statistics));
        }
        changes.save(new CookieConsentChange(consent.getId(), statistics, source, POLICY_VERSION));
        log.debug("Zgoda {}: statystyka={}, zrodlo={}", consent.getConsentKey(), statistics, source);
        return consent.getConsentKey();
    }

    /** Komplet danych zgody w kolejnosci czytelnej dla czlowieka. Pusto, gdy zgody nie ma. */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> export(UUID key) {
        return consents.findByConsentKey(key).map(consent -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("identyfikator", consent.getConsentKey());
            data.put("statystyka", consent.isStatistics());
            data.put("udzielona", consent.getCreatedAt());
            data.put("ostatnia_zmiana", consent.getUpdatedAt());
            data.put("historia", changes.findByConsentIdOrderByChangedAtDesc(consent.getId()).stream()
                    .map(change -> {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("statystyka", change.isStatistics());
                        entry.put("miejsce", change.getSource());
                        entry.put("wersja_tresci", change.getPolicyVersion());
                        entry.put("data", change.getChangedAt());
                        return entry;
                    }).toList());
            data.put("odslony", views.find(consent.getId()).stream()
                    .map(view -> {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("adres", view.path());
                        entry.put("data", view.viewedAt());
                        return entry;
                    }).toList());
            return data;
        });
    }

    /** Twarde usuniecie zgody; historie i odslony za zgoda kasuje kaskada w bazie. */
    @Transactional
    public boolean delete(UUID key) {
        Optional<CookieConsent> consent = consents.findByConsentKey(key);
        if (consent.isEmpty()) return false;
        consents.delete(consent.get());
        log.info("Usunieto zgode {} razem z historia i odslonami", key);
        return true;
    }

    /** Strona listy w panelu; {@code query} to poczatek identyfikatora, inne znaki daja pusta liste. */
    @Transactional(readOnly = true)
    public Page<Row> search(String query, int page) {
        String prefix = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        PageRequest request = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        if (!prefix.isEmpty() && !KEY_PREFIX.matcher(prefix).matches()) {
            return Page.empty(request);
        }
        Page<CookieConsent> found = prefix.isEmpty()
                ? consents.findAll(request.withSort(Sort.by(Sort.Direction.DESC, "updatedAt")))
                : consents.findByKeyPrefix(prefix, request);
        return found.map(c -> new Row(c.getConsentKey(), c.isStatistics(), c.getCreatedAt(), c.getUpdatedAt()));
    }

    /** Retencja; odslony za zgoda znikaja razem ze zgoda (kaskada), nie zostaja bez wlasciciela. */
    @Transactional
    public int purgeNotChangedSince(Instant cutoff) {
        return consents.deleteNotChangedSince(cutoff);
    }

    /** Parsowanie identyfikatora z adresu w panelu; smieci to pusto, nie wyjatek. */
    public static Optional<UUID> parseKey(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
