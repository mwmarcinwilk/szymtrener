package pl.szymtrener.offer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.szymtrener.common.Plural;
import pl.szymtrener.settings.SettingsService;

import java.util.List;
import java.util.ArrayList;

/**
 * Oferta online w postaci gotowej do wyswietlenia.
 *
 * Kwoty formatuje TUTAJ, nie szablon: brief wymaga zapisu ze spacja jako
 * separatorem tysiecy („1 074 zl", nie „1074 zl"), a to samo formatowanie
 * potrzebne jest w mailu i w panelu. Jedno miejsce, jedna regula.
 */
@Service
public class OnlineOfferService {

    /** Cena Sciezki 1 z briefu, gdy nikt jej jeszcze nie zmienil w panelu. */
    public static final int DEFAULT_CONSULT_PRICE_GR = 34900;
    /** Cena jadlospisu z cennika z pazdziernika 2026, gdy nikt jej nie zmienil w panelu. */
    public static final int DEFAULT_DIET_PRICE_GR = 29900;
    /** Plan treningowy + jadlospis, jeden wspolny wywiad. */
    public static final int DEFAULT_COMBO_PRICE_GR = 54900;
    /** Aktualizacja planu albo jadlospisu po 3 miesiacach. */
    public static final int DEFAULT_UPDATE_PRICE_GR = 17900;

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

    /**
     * Produkt opisany jedna kwota i przelacznikiem widocznosci: konsultacja
     * z planem (Sciezka 1) i jadlospis. Za malo danych na wlasna tabele.
     */
    public record SimplePriceView(String price, boolean visible) {}

    public SimplePriceView consultation() {
        return new SimplePriceView(
                money(consultPriceGr()),
                settings.getBoolean(SettingsService.OFFER_CONSULT_VISIBLE, true));
    }

    public int consultPriceGr() {
        return settings.getInt(SettingsService.OFFER_CONSULT_PRICE_GR, DEFAULT_CONSULT_PRICE_GR);
    }

    /**
     * Jadlospis dietetyczny. Nie jest ani pakietem online, ani stacjonarnym —
     * jedna kwota bez waznosci i bez naboru, wiec tak jak konsultacja siedzi
     * w ustawieniach, a nie we wlasnej tabeli.
     */
    public SimplePriceView diet() {
        return new SimplePriceView(
                money(dietPriceGr()),
                settings.getBoolean(SettingsService.OFFER_DIET_VISIBLE, true));
    }

    public int dietPriceGr() {
        return settings.getInt(SettingsService.OFFER_DIET_PRICE_GR, DEFAULT_DIET_PRICE_GR);
    }

    public int comboPriceGr() {
        return settings.getInt(SettingsService.OFFER_COMBO_PRICE_GR, DEFAULT_COMBO_PRICE_GR);
    }

    public int updatePriceGr() {
        return settings.getInt(SettingsService.OFFER_UPDATE_PRICE_GR, DEFAULT_UPDATE_PRICE_GR);
    }

    /** Pozycja mini cennika produktow jednorazowych: „Plan treningowy - 349 zł · co zawiera”. */
    public record OneOffView(String name, String price, String contents) {}

    /** Pozycja mini cennika przed sformatowaniem, zeby „od X” liczyc z tych samych pozycji. */
    private record OneOff(String name, int priceGr, String contents) {}

    /**
     * Produkty jednorazowe w kolejnosci z cennika. Plan treningowy to dotychczasowa
     * „konsultacja + plan” (ta sama cena z panelu), jadlospis to ta sama kwota co
     * na karcie w sekcji „Zdalnie”. Jedno zrodlo kazdej ceny. Ukrycie planu albo
     * jadlospisu w panelu zdejmuje je tez stad, razem z zestawem, ktory z nich sie sklada.
     */
    private List<OneOff> visibleOneOff() {
        boolean plan = consultation().visible();
        boolean diet = diet().visible();
        List<OneOff> out = new ArrayList<>();
        if (plan) out.add(new OneOff("Plan treningowy", consultPriceGr(),
                "rozmowa 30 min przed startem, plan w PDF, 14 dni kontaktu mailowo lub przez WhatsApp"));
        if (diet) out.add(new OneOff("Jadłospis", dietPriceGr(),
                "wywiad, jadłospis, lista zakupów, 14 dni kontaktu"));
        if (plan && diet) out.add(new OneOff("Plan treningowy + jadłospis", comboPriceGr(),
                "jedno i drugie, jeden wspólny wywiad"));
        if (plan || diet) out.add(new OneOff("Aktualizacja po 3 miesiącach", updatePriceGr(),
                "przegląd postępów i nowa wersja planu albo jadłospisu"));
        return out;
    }

    public List<OneOffView> oneOff() {
        return visibleOneOff().stream().map(o -> new OneOffView(o.name(), money(o.priceGr()), o.contents())).toList();
    }

    /** Najnizsza cena widocznego produktu jednorazowego — „od 179 zł” pod mini cennikiem. Null, gdy brak. */
    public String oneOffFrom() {
        return visibleOneOff().stream().mapToInt(OneOff::priceGr).min().stream()
                .mapToObj(OnlineOfferService::money).findFirst().orElse(null);
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
        String seatsLine = starting && p.seatsLeft() > 0 ? seatsLine(p.seatsLeft(), p.getSeatsTotal()) : null;
        String conditionLine = starting
                ? "Cenę startową dostajesz w zamian za opinię po zakończeniu współpracy."
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

    /** „Zostały 3 z 5 miejsc w cenie startowej.” / „Zostało 1 z 5…” — czasownik idzie za liczbą. */
    static String seatsLine(int left, int total) {
        String verb = Plural.form(left, "Zostało", "Zostały", "Zostało");
        return verb + " " + left + " z " + total + " miejsc w cenie startowej.";
    }

    /** Grosze → „1 074 zł". Reguła jest wspólna dla całej oferty, patrz {@link Money}. */
    public static String money(int grosze) {
        return Money.format(grosze);
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
