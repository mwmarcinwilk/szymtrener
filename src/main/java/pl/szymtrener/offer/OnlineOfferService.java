package pl.szymtrener.offer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.szymtrener.settings.SettingsService;

import java.util.List;

/**
 * Oferta online w postaci gotowej do wyswietlenia.
 *
 * Kwoty formatuje TUTAJ, nie szablon: brief wymaga zapisu ze spacja jako
 * separatorem tysiecy („1 074 zl", nie „1074 zl"), a to samo formatowanie
 * potrzebne jest w mailu i w panelu. Jedno miejsce, jedna regula.
 */
@Service
public class OnlineOfferService {

    /** Spacja NIEROZDZIELAJACA: „1 074 zl" nie moze sie zlamac na koncu wiersza. */
    private static final char NBSP = '\u00A0';

    /** Cena Sciezki 1 z briefu, gdy nikt jej jeszcze nie zmienil w panelu. */
    public static final int DEFAULT_CONSULT_PRICE_GR = 34900;

    private final OnlinePackageRepository packages;
    private final TestimonialRepository testimonials;
    private final OnlineFaqRepository faq;
    private final SettingsService settings;

    public OnlineOfferService(OnlinePackageRepository packages,
                              TestimonialRepository testimonials,
                              OnlineFaqRepository faq,
                              SettingsService settings) {
        this.packages = packages;
        this.testimonials = testimonials;
        this.faq = faq;
        this.settings = settings;
    }

    /** Sciezka 1 — jedna kwota i przelacznik widocznosci, wiec bez wlasnej tabeli. */
    public record ConsultationView(String price, boolean visible) {}

    public ConsultationView consultation() {
        return new ConsultationView(
                money(consultPriceGr()),
                settings.getBoolean(SettingsService.OFFER_CONSULT_VISIBLE, true));
    }

    public int consultPriceGr() {
        return settings.getInt(SettingsService.OFFER_CONSULT_PRICE_GR, DEFAULT_CONSULT_PRICE_GR);
    }

    /**
     * Pakiet gotowy do wyrenderowania.
     *
     * @param targetLine  linia ceny docelowej albo null, gdy obowiazuje juz cena
     *                    docelowa — wtedy zadnych oznaczen promocyjnych nie ma
     * @param seatsLine   licznik miejsc albo null poza naborem zalozycielskim
     */
    public record PackageView(Long id, String name, String durationLabel,
                              String price, String monthly,
                              String targetLine, String seatsLine, String conditionLine,
                              String badge, boolean highlighted, boolean startingPrice) {}

    @Transactional(readOnly = true)
    public List<PackageView> packages() {
        return packages.findByVisibleTrueOrderBySortOrderAsc().stream().map(OnlineOfferService::toView).toList();
    }

    /** Widoczne dla testow: to tu zapadaja decyzje o chowaniu oznaczen promocyjnych. */
    static PackageView toView(OnlinePackage p) {
        boolean starting = p.startingPrice();
        int total = starting ? p.getCurrentTotalGr() : p.getTargetTotalGr();
        int monthly = starting ? p.getCurrentMonthlyGr() : p.getTargetMonthlyGr();

        // Poza cena startowa znikaja: linia ceny docelowej, licznik miejsc, warunek
        // i plakietka promocyjna. Zostaje sama kwota, bez oznaczen.
        String targetLine = starting
                ? "Cena docelowa: " + money(p.getTargetTotalGr()) + " (" + money(p.getTargetMonthlyGr()) + "/mies.)"
                : null;
        String seatsLine = starting && p.seatsLeft() > 0
                ? "Zostało " + p.seatsLeft() + " z " + p.getSeatsTotal() + " miejsc w cenie startowej"
                : null;
        String conditionLine = starting
                ? "Cena startowa w zamian za opinię po zakończeniu współpracy."
                : null;
        // Plakietka promocyjna znika razem z cena startowa — po zamknieciu naboru
        // zostaje sama kwota, bez oznaczen (brief 2.4).
        boolean badgeShown = p.isBadgeVisible() && (starting || !p.isBadgePromotional());
        String badge = badgeShown ? p.getBadgeText() : null;

        return new PackageView(p.getId(), p.getName(), p.getDurationLabel(),
                money(total), money(monthly) + "/mies.",
                targetLine, seatsLine, conditionLine,
                badge, p.isHighlighted(), starting);
    }

    /** Grosze → „1 074 zł". Spacja nierozdzielajaca, bez koncowek groszowych. */
    public static String money(int grosze) {
        long zlote = Math.round(grosze / 100.0);
        String digits = Long.toString(zlote);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) out.append(NBSP);
            out.append(digits.charAt(i));
        }
        // NBSP takze przed jednostka: „1 074" nie moze zostac oddzielone od „zl"
        return out + "\u00A0zł";
    }

    /**
     * Najnizsza obowiazujaca cena miesieczna sposrod widocznych pakietow —
     * „prowadzenie online od X zl". Liczona z tych samych danych co karty, zeby
     * zadna kopia ceny nie zostala w HTML (brief 2.5: „NIE zaszyte w HTML
     * w kilku miejscach naraz"). Null, gdy nie ma zadnego pakietu.
     */
    @Transactional(readOnly = true)
    public String lowestMonthly() {
        return packages.findByVisibleTrueOrderBySortOrderAsc().stream()
                .mapToInt(p -> p.startingPrice() ? p.getCurrentMonthlyGr() : p.getTargetMonthlyGr())
                .min()
                .stream()
                .mapToObj(OnlineOfferService::money)
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Testimonial> testimonials() {
        return testimonials.findByVisibleTrueOrderBySortOrderAsc();
    }

    /**
     * Tylko pytania z odpowiedzia. Brief: odpowiedzi Szymon dostarczy pozniej,
     * a akordeon rozwijajacy sie na pustke wyglada jak usterka.
     */
    @Transactional(readOnly = true)
    public List<OnlineFaq> faq() {
        return faq.findByVisibleTrueOrderBySortOrderAsc().stream().filter(OnlineFaq::answered).toList();
    }
}
