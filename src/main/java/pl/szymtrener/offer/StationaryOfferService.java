package pl.szymtrener.offer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.szymtrener.settings.SettingsService;

import java.util.ArrayList;
import java.util.List;

/**
 * Cennik treningow stacjonarnych gotowy do wyswietlenia.
 *
 * Brief „Ceny stacjonarne" punkt 5.3: cennik pojawia sie na stronie DWA razy
 * (sekcja oferty i FAQ) i obie instancje musza czerpac z jednego zrodla. Dlatego
 * zdania do FAQ powstaja tutaj, z tych samych liczb co karty — nie sa
 * przepisywane recznie do szablonu.
 */
@Service
public class StationaryOfferService {

    /** Zasady odwolan i pauzy. Teksty, bo Szymon ma je zmieniac bez programisty. */
    public static final String RULE_CANCEL = "stationary.rules.cancel";
    public static final String RULE_LATE = "stationary.rules.late";
    public static final String RULE_PAUSE = "stationary.rules.pause";

    private final StationaryPackageRepository packages;
    private final SettingsService settings;

    public StationaryOfferService(StationaryPackageRepository packages, SettingsService settings) {
        this.packages = packages;
        this.settings = settings;
    }

    /**
     * @param validity   „Ważny 16 tygodni" albo null dla wejscia pojedynczego
     * @param discount   „Taniej o 29%" albo null, gdy pakiet nie jest tanszy
     * @param totalLine  „Razem: 1 800 zł" albo null przy jednym treningu
     */
    public record PackageView(Long id, String name, int sessions,
                              String perSession, String totalLine,
                              String validity, String discount, boolean featured) {}

    @Transactional(readOnly = true)
    public List<PackageView> individual() {
        return views(StationaryKind.INDYWIDUALNY);
    }

    @Transactional(readOnly = true)
    public List<PackageView> pairs() {
        return views(StationaryKind.PARA);
    }

    private List<PackageView> views(StationaryKind kind) {
        List<StationaryPackage> found = packages.findByKindAndVisibleTrueOrderBySortOrderAsc(kind);
        // Rabat liczymy wzgledem wejscia pojedynczego TEGO SAMEGO rodzaju: cena pary
        // jest laczna za dwie osoby, wiec porownanie z cena indywidualna nie ma sensu.
        int singlePrice = found.stream()
                .filter(StationaryPackage::single)
                .mapToInt(StationaryPackage::getPricePerSessionGr)
                .findFirst().orElse(0);

        List<PackageView> out = new ArrayList<>();
        for (StationaryPackage p : found) {
            int discount = p.discountPercent(singlePrice);
            out.add(new PackageView(
                    p.getId(), p.getName(), p.getSessions(),
                    Money.format(p.getPricePerSessionGr()),
                    p.single() ? null : "Razem: " + Money.format(p.totalGr()),
                    validityLabel(p.getValidityWeeks()),
                    discount > 0 ? "Taniej o " + discount + "%" : null,
                    p.isFeatured()));
        }
        return out;
    }

    /** „Ważny 16 tygodni" — polska odmiana, bo „16 tygodni" i „4 tygodnie" różnią się. */
    static String validityLabel(Integer weeks) {
        if (weeks == null || weeks <= 0) return null;
        int last = weeks % 10, lastTwo = weeks % 100;
        String noun = (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) ? "tygodnie" : "tygodni";
        return "Ważny " + weeks + " " + noun;
    }

    /** Zasady odwolan i pauzy, w kolejnosci od najlagodniejszej. Puste pomijamy. */
    @Transactional(readOnly = true)
    public List<String> rules() {
        List<String> out = new ArrayList<>();
        for (String key : List.of(RULE_CANCEL, RULE_LATE, RULE_PAUSE)) {
            String value = settings.get(key, null);
            if (value != null && !value.isBlank()) out.add(value.trim());
        }
        return out;
    }

    /**
     * Zdanie o cenach do FAQ. Powstaje z tych samych danych co karty cennika —
     * inaczej po zmianie ceny cennik i FAQ pokazywalyby co innego.
     */
    @Transactional(readOnly = true)
    public String priceSentence() {
        String single = price(StationaryKind.INDYWIDUALNY, true);
        String cheapest = price(StationaryKind.INDYWIDUALNY, false);
        if (single == null) return null;

        StringBuilder out = new StringBuilder("Pojedynczy trening stacjonarny kosztuje " + single);
        if (cheapest != null) out.append(", a w największym pakiecie cena spada do ").append(cheapest).append(" za trening");
        out.append('.');

        // „od 200 do 240 zł", nie „od 200 zł do 240 zł" — jednostka raz, na koncu
        Integer pairCheapest = priceGr(StationaryKind.PARA, false);
        Integer pairSingle = priceGr(StationaryKind.PARA, true);
        if (pairCheapest != null && pairSingle != null) {
            out.append(" Treningi dla par to od ").append(Money.amount(pairCheapest))
               .append(" do ").append(Money.format(pairSingle)).append(" za sesję dla dwojga.");
        }
        return out.toString();
    }

    /** Najnizsza cena za trening dla pary — „ceny par zaczynaja sie od X". */
    @Transactional(readOnly = true)
    public String cheapestPair() {
        return price(StationaryKind.PARA, false);
    }

    /** Cena wejscia pojedynczego albo najnizsza cena za trening w pakiecie. */
    private String price(StationaryKind kind, boolean singleEntry) {
        Integer gr = priceGr(kind, singleEntry);
        return gr == null ? null : Money.format(gr);
    }

    private Integer priceGr(StationaryKind kind, boolean singleEntry) {
        return packages.findByKindAndVisibleTrueOrderBySortOrderAsc(kind).stream()
                .filter(p -> p.single() == singleEntry)
                .mapToInt(StationaryPackage::getPricePerSessionGr)
                .min()
                .stream().boxed()
                .findFirst()
                .orElse(null);
    }

    /** Najdluzsza waznosc w ofercie — do odpowiedzi „jak długo ważny jest pakiet". */
    @Transactional(readOnly = true)
    public String longestValidity() {
        return packages.findAll().stream()
                .filter(StationaryPackage::isVisible)
                .map(StationaryPackage::getValidityWeeks)
                .filter(w -> w != null && w > 0)
                .max(Integer::compareTo)
                .map(StationaryOfferService::validityLabel)
                .map(label -> label.replace("Ważny ", ""))
                .orElse(null);
    }
}
