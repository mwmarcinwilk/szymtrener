package pl.szymtrener.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ustawienia edytowalne z panelu. Czytane sa przy kazdym zadaniu (rozmiar strony
 * bloga, adres powiadomien), wiec trzymamy je w mapie i odswiezamy przy zapisie —
 * jedna instancja aplikacji, wiec nie ma czego uzgadniac miedzy wezlami.
 */
@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    /** Klucze znane aplikacji — ekran ustawien rysuje sie z tej listy. */
    public static final String BLOG_PAGE_SIZE = "blog.page.size";
    /**
     * Glowny wylacznik poczty. Wylaczony = zgloszenia nadal zapisuja sie w bazie
     * i widac je w panelu, ale nie leci zadna wiadomosc. Przydaje sie, zanim
     * skrzynka bedzie skonfigurowana — strona dziala, nic nie ginie.
     */
    public static final String MAIL_ENABLED = "mail.enabled";
    public static final String MAIL_RECIPIENT = "mail.notify.recipient";
    public static final String MAIL_NOTIFY = "mail.notify.trainer";
    public static final String MAIL_AUTOREPLY = "mail.autoreply";
    /**
     * Sciezka 1 z briefu (konsultacja + plan w PDF). Cena stala, poza mechanika
     * naboru zalozycielskiego — dlatego ustawienie, a nie wiersz w tabeli pakietow.
     * W GROSZACH, tak samo jak kwoty pakietow.
     */
    public static final String OFFER_CONSULT_PRICE_GR = "offer.consult.price.gr";
    public static final String OFFER_CONSULT_VISIBLE = "offer.consult.visible";
    /** Jadlospis dietetyczny: jedna kwota, tak samo jak konsultacja. W GROSZACH. */
    public static final String OFFER_DIET_PRICE_GR = "offer.diet.price.gr";
    public static final String OFFER_DIET_VISIBLE = "offer.diet.visible";
    /**
     * Stan odbioru odpowiedzi ze skrzynki (InboundMailService). Nie sa to ustawienia do edycji,
     * tylko pamiec postepu: ktory UID juz przeczytalismy i w jakiej numeracji skrzynki.
     */
    public static final String INBOX_UID_VALIDITY = "mail.inbox.uidvalidity";
    public static final String INBOX_LAST_UID = "mail.inbox.lastuid";
    /** Wynik ostatniego odbioru do wyswietlenia w Ustawieniach, np. „2026-09-14T19:30:00Z OK". */
    public static final String INBOX_LAST_CHECK = "mail.inbox.lastcheck";
    /** Wiadomosc, ktorej odbior sie wywraca, i liczba prob („uid:proby"), zeby nie blokowala skrzynki na zawsze. */
    public static final String INBOX_FAILED_UID = "mail.inbox.faileduid";
    public static final String SEO_TITLE = "seo.default.title";
    public static final String SEO_DESC = "seo.default.desc";

    private final AppSettingRepository repository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    public SettingsService(AppSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public String get(String key, String fallback) {
        ensureLoaded();
        String value = cache.get(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public int getInt(String key, int fallback) {
        String raw = get(key, null);
        if (raw == null) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Ustawienie {} nie jest liczba ({}), uzywam {}", key, raw, fallback);
            return fallback;
        }
    }

    public long getLong(String key, long fallback) {
        String raw = get(key, null);
        if (raw == null) return fallback;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Ustawienie {} nie jest liczba ({}), uzywam {}", key, raw, fallback);
            return fallback;
        }
    }

    public boolean getBoolean(String key, boolean fallback) {
        String raw = get(key, null);
        return raw == null ? fallback : Boolean.parseBoolean(raw.trim());
    }

    @Transactional
    public void set(String key, String value) {
        AppSetting setting = repository.findById(key).orElseGet(() -> new AppSetting(key, value));
        setting.setValue(value);
        repository.save(setting);
        ensureLoaded();
        if (value == null) cache.remove(key); else cache.put(key, value);
    }

    /** Wszystko, co jest w bazie — do wyswietlenia na ekranie ustawien. */
    @Transactional(readOnly = true)
    public Map<String, String> all() {
        ensureLoaded();
        return new LinkedHashMap<>(cache);
    }

    private void ensureLoaded() {
        if (loaded) return;
        synchronized (this) {
            if (loaded) return;
            repository.findAll().forEach(s -> {
                if (s.getValue() != null) cache.put(s.getKey(), s.getValue());
            });
            loaded = true;
        }
    }
}
