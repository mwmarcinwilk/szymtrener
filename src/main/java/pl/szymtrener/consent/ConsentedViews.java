package pl.szymtrener.consent;

import java.time.Instant;
import java.util.List;

/**
 * Odslony zapisane za zgoda, do eksportu danych zgody. Tabela odslon nalezy do pakietu analytics,
 * ktory sam zalezy od zgody, wiec zgoda deklaruje tu, czego potrzebuje, a analytics to dostarcza.
 * Usuwac nie trzeba: {@code page_view.consent_id} ma kaskade od zgody.
 */
public interface ConsentedViews {

    record View(String path, Instant viewedAt) {}

    List<View> find(long consentId);
}
