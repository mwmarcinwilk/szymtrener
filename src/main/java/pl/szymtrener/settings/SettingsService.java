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
