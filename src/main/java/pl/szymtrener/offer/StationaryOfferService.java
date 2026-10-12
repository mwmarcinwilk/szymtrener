package pl.szymtrener.offer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.szymtrener.common.Plural;
import pl.szymtrener.settings.SettingsService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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

    /** Zasady odwolan, pauzy i zamrozenia. Teksty, bo Szymon ma je zmieniac bez programisty. */
    public static final String RULE_CANCEL = "stationary.rules.cancel";
    public static final String RULE_LATE = "stationary.rules.late";
    public static final String RULE_PAUSE = "stationary.rules.pause";
    public static final String RULE_FREEZE = "stationary.rules.freeze";

    private final StationaryPackageRepository packages;
    private final SettingsService settings;

    public StationaryOfferService(StationaryPackageRepository packages, SettingsService settings) {
        this.packages = packages;
        this.settings = settings;
    }

    /**
     * @param validity    „Ważny 16 tygodni”, „12 miesięcy, płatność co miesiąc” albo null
     *                    dla wejscia pojedynczego
     * @param discount    „Taniej o 29%” albo null, gdy pakiet nie jest tanszy
     * @param totalLine   „Razem: 1 800 zł”, „Razem: 1 080 zł / miesiąc (8 treningów)”
     *                    albo null przy jednym treningu
     * @param badge       plakietka nad karta albo null
     * @param description zdanie pod cena albo null
     * @param total       sama kwota razem (miesieczna przy Opiece) do tabeli w FAQ
     * @param termShort   „6 tyg.”, „12 mies.” albo null — kolumna „ważność” w FAQ
     */
    public record PackageView(Long id, String name, int sessions,
                              String perSession, String totalLine,
                              String validity, String discount, boolean featured,
                              String badge, String description, boolean monthly,
                              String total, String termShort) {}

    /**
     * Rachunek „rok w pakietach kontra rok w Opiece”. Liczony z cen, nie wpisany do
     * szablonu: kwoty z dokumentu (14 400 / 12 960 / 1 440 zł) sa iloczynami cen
     * z cennika i po zmianie ceny w panelu musza zmienic sie same.
     *
     * @param packageSessions  wielkosc pakietu, z ktorym porownujemy („po 12”)
     * @param freeSessions     roznica przeliczona na treningi: „prawie 10 treningów”
     */
    public record YearCompare(int packageSessions, String packagesYear, String careYear,
                              String difference, String freeSessions) {}

    /**
     * Wszystko, czego strona glowna potrzebuje z cennika stacjonarnego, z JEDNEGO
     * odczytu tabeli. Pola moga byc null, gdy w cenniku brakuje danej pozycji —
     * szablon chowa wtedy zdanie albo blok.
     *
     * @param pairPackagePrice najnizsza cena za trening w pakiecie dla pary
     * @param pairCarePrice    cena za trening w Opiece dla pary
     * @param pairPerPerson    „W pakiecie 12 treningów każde z Was płaci więc 100 zł za trening.”
     * @param priceRange       „135-270 PLN” do danych strukturalnych
     */
    public record Page(List<PackageView> individual, List<PackageView> pairs, List<String> rules,
                       String priceSentence, String validitySentence,
                       String pairPackagePrice, String pairCarePrice, String pairPerPerson,
                       String priceRange, YearCompare careCompare, YearCompare pairCareCompare) {

        /**
         * Blok „Opieka Longevity” i pytanie P9 opisuja produkt — bez niego w cenniku znikaja.
         * Wystarczy Opieka w ktorymkolwiek cenniku, bo karta pary tez linkuje do bloku.
         */
        public boolean hasCare() {
            return Stream.concat(individual.stream(), pairs.stream()).anyMatch(PackageView::monthly);
        }
    }

    @Transactional(readOnly = true)
    public Page page() {
        List<StationaryPackage> visible = packages.findAllByOrderByKindAscSortOrderAsc().stream()
                .filter(StationaryPackage::isVisible)
                .toList();
        List<StationaryPackage> one = ofKind(visible, StationaryKind.INDYWIDUALNY);
        List<StationaryPackage> pair = ofKind(visible, StationaryKind.PARA);
        KindPrices pairPrices = KindPrices.of(pair);

        return new Page(views(one), views(pair), rules(),
                priceSentence(KindPrices.of(one), pairPrices),
                validitySentence(one),
                format(pairPrices.pack()), format(pairPrices.care()),
                pairPerPersonSentence(pair),
                priceRange(visible),
                yearCompare(one), yearCompare(pair));
    }

    private static List<StationaryPackage> ofKind(List<StationaryPackage> all, StationaryKind kind) {
        return all.stream().filter(p -> p.getKind() == kind).toList();
    }

    private static List<PackageView> views(List<StationaryPackage> found) {
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
                    totalLine(p),
                    p.isMonthly() ? termLabel(p.getTermMonths()) : validityLabel(p.getValidityWeeks()),
                    discount > 0 ? "Taniej o " + discount + "%" : null,
                    p.isFeatured(),
                    blankToNull(p.getBadgeText()),
                    blankToNull(p.getDescription()),
                    p.isMonthly(),
                    Money.format(p.totalGr()),
                    termShort(p)));
        }
        return out;
    }

    private static String totalLine(StationaryPackage p) {
        if (p.isMonthly()) {
            return "Razem: " + Money.format(p.totalGr()) + " / miesiąc (" + Plural.sessions(p.getSessions()) + ")";
        }
        return p.single() ? null : "Razem: " + Money.format(p.totalGr());
    }

    private static String termShort(StationaryPackage p) {
        Integer n = p.isMonthly() ? p.getTermMonths() : p.getValidityWeeks();
        if (n == null || n <= 0) return null;
        return n + (p.isMonthly() ? " mies." : " tyg.");
    }

    /** „12 miesięcy, płatność co miesiąc”. */
    static String termLabel(Integer months) {
        String payment = "płatność co miesiąc";
        return months == null || months <= 0 ? payment : Plural.months(months) + ", " + payment;
    }

    /** „Ważny 16 tygodni” — polska odmiana, bo „16 tygodni” i „4 tygodnie” różnią się. */
    static String validityLabel(Integer weeks) {
        if (weeks == null || weeks <= 0) return null;
        return "Ważny " + Plural.weeks(weeks);
    }

    /** Najwiekszy zwykly pakiet (nie wejscie pojedyncze, nie Opieka) — punkt odniesienia dla pary i Opieki. */
    private static Optional<StationaryPackage> biggestPack(List<StationaryPackage> found) {
        return found.stream()
                .filter(p -> !p.isMonthly() && !p.single())
                .max(Comparator.comparingInt(StationaryPackage::getSessions));
    }

    /**
     * Rok w Opiece kontra ten sam rok w najwiekszym pakiecie tego samego rodzaju.
     * Null, gdy ktoregos z nich nie ma na stronie — wtedy nie ma czego porownywac.
     */
    private static YearCompare yearCompare(List<StationaryPackage> found) {
        // Ta sama Opieka co w zdaniu o cenach (najtansza), zeby rachunek i zdanie mowily o jednej pozycji.
        Optional<StationaryPackage> care = Optional.ofNullable(KindPrices.of(found).careDetail());
        Optional<StationaryPackage> biggest = biggestPack(found);
        if (care.isEmpty() || biggest.isEmpty()) return null;
        return yearCompare(care.get(), biggest.get());
    }

    static YearCompare yearCompare(StationaryPackage care, StationaryPackage pack) {
        int sessions, packagesYear, careYear;
        try {
            sessions = Math.multiplyExact(care.getSessions(), care.getTermMonths());
            packagesYear = Math.multiplyExact(sessions, pack.getPricePerSessionGr());
            careYear = Math.multiplyExact(sessions, care.getPricePerSessionGr());
        } catch (ArithmeticException e) {
            // Absurdalne liczby z panelu nie moga wywrocic strony glownej — rachunek po prostu znika.
            return null;
        }
        int difference = packagesYear - careYear;
        if (difference <= 0) return null;
        return new YearCompare(pack.getSessions(),
                Money.format(packagesYear), Money.format(careYear), Money.format(difference),
                inSessions(difference, pack.getPricePerSessionGr()));
    }

    /**
     * Roznica przeliczona na treningi z pakietu: 1 440 zl przy 150 zl to 9,6, czyli
     * „prawie 10 treningów”. Rowna wielokrotnosc bez slowa „prawie”.
     */
    static String inSessions(int differenceGr, int perSessionGr) {
        if (perSessionGr <= 0) return null;
        int whole = differenceGr / perSessionGr;
        int rest = differenceGr % perSessionGr;
        if (rest == 0) return Plural.sessions(whole);
        return rest * 2 >= perSessionGr
                ? "prawie " + Plural.sessions(whole + 1)
                : "ponad " + Plural.sessions(whole);
    }

    /** Zasady odwolan, pauzy i zamrozenia, od najlagodniejszej. Puste pomijamy. */
    private List<String> rules() {
        List<String> out = new ArrayList<>();
        for (String key : List.of(RULE_CANCEL, RULE_LATE, RULE_PAUSE, RULE_FREEZE)) {
            String value = settings.get(key, null);
            if (value != null && !value.isBlank()) out.add(value.trim());
        }
        return out;
    }

    /**
     * Ceny jednego rodzaju w groszach: wejscie pojedyncze, najtanszy pakiet, Opieka.
     * Kazda moze byc null. {@code careDetail} to Opieka sama, potrzebna do zdania o cenach.
     */
    private record KindPrices(Integer single, Integer pack, Integer care, StationaryPackage careDetail) {

        static KindPrices of(List<StationaryPackage> found) {
            Integer single = found.stream().filter(StationaryPackage::single)
                    .map(StationaryPackage::getPricePerSessionGr).min(Integer::compareTo).orElse(null);
            Integer pack = found.stream().filter(p -> !p.single() && !p.isMonthly())
                    .map(StationaryPackage::getPricePerSessionGr).min(Integer::compareTo).orElse(null);
            StationaryPackage care = found.stream().filter(StationaryPackage::isMonthly)
                    .min(Comparator.comparingInt(StationaryPackage::getPricePerSessionGr)).orElse(null);
            return new KindPrices(single, pack, care == null ? null : care.getPricePerSessionGr(), care);
        }

        Integer lowest() {
            return Stream.of(single, pack, care).filter(Objects::nonNull).min(Integer::compareTo).orElse(null);
        }
    }

    /**
     * Zdanie o cenach do FAQ. Powstaje z tych samych danych co karty cennika —
     * inaczej po zmianie ceny cennik i FAQ pokazywalyby co innego.
     */
    private static String priceSentence(KindPrices one, KindPrices pair) {
        if (one.single() == null) return null;

        StringBuilder out = new StringBuilder("Pojedynczy trening w studiu kosztuje ")
                .append(Money.format(one.single())).append('.');
        if (one.pack() != null) {
            out.append(" W pakietach cena spada do ").append(Money.format(one.pack()));
            if (one.careDetail() != null) {
                out.append(", a w ").append(locative(one.careDetail().getName()))
                   .append(" (").append(careTerms(one.careDetail())).append(") do ")
                   .append(Money.format(one.care()));
            }
            out.append(" za trening.");
        }

        // „od 180 do 270 zł”, nie „od 180 zł do 270 zł” — jednostka raz, na koncu
        Integer pairLowest = pair.lowest();
        if (pairLowest != null && pair.single() != null && pairLowest < pair.single()) {
            out.append(" Trening we dwoje kosztuje od ").append(Money.amount(pairLowest))
               .append(" do ").append(Money.format(pair.single())).append(", łącznie za dwie osoby.");
        }
        return out.toString();
    }

    /**
     * „rok współpracy, 2 treningi w tygodniu” — z dlugosci umowy i liczby treningow
     * w miesiacu, zeby zdanie w FAQ nie rozjechalo sie z karta po zmianie w panelu.
     */
    static String careTerms(StationaryPackage care) {
        String term = care.getTermMonths() == 12 ? "rok" : Plural.months(care.getTermMonths());
        String rhythm = care.getSessions() % 4 == 0
                ? Plural.sessions(care.getSessions() / 4) + " w tygodniu"
                : Plural.sessions(care.getSessions()) + " w miesiącu";
        return term + " współpracy, " + rhythm;
    }

    /**
     * „Opieka Longevity” → „Opiece Longevity”. Nazwa pochodzi z panelu, wiec odmieniamy
     * tylko znany poczatek; inna nazwa zostaje w mianowniku zamiast udawac odmiane.
     * Brzmienie zdania z „Opiece” jest zatwierdzonym tekstem klienta.
     */
    static String locative(String name) {
        return name.startsWith("Opieka ") ? "Opiece " + name.substring("Opieka ".length()) : name;
    }

    /**
     * „4 treningi wykorzystujesz w 6 tygodni, 8 w 10 tygodni, a 12 w 16 tygodni” —
     * z pakietow indywidualnych z terminem waznosci. Null, gdy takich nie ma.
     */
    private static String validitySentence(List<StationaryPackage> one) {
        List<StationaryPackage> found = one.stream()
                .filter(p -> !p.single() && !p.isMonthly() && p.getValidityWeeks() != null && p.getValidityWeeks() > 0)
                .sorted(Comparator.comparingInt(StationaryPackage::getSessions))
                .toList();
        if (found.isEmpty()) return null;
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < found.size(); i++) {
            StationaryPackage p = found.get(i);
            String weeks = Plural.weeks(p.getValidityWeeks());
            parts.add(i == 0
                    ? Plural.sessions(p.getSessions()) + " wykorzystujesz w " + weeks
                    : p.getSessions() + " w " + weeks);
        }
        if (parts.size() == 1) return parts.get(0);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + ", a " + parts.get(parts.size() - 1);
    }

    /** Polowa ceny najwiekszego pakietu dla pary. Null, gdy takiego pakietu nie ma. */
    private static String pairPerPersonSentence(List<StationaryPackage> pair) {
        return biggestPack(pair)
                .map(p -> "W pakiecie " + Plural.sessions(p.getSessions()) + " każde z Was płaci więc "
                        + Money.format(p.getPricePerSessionGr() / 2) + " za trening.")
                .orElse(null);
    }

    /** Od najnizszej ceny za trening do najwyzszej, z obu cennikow. Null, gdy cennik jest pusty. */
    private static String priceRange(List<StationaryPackage> visible) {
        IntSummaryStatistics stats = visible.stream()
                .mapToInt(StationaryPackage::getPricePerSessionGr)
                .filter(gr -> gr > 0)
                .summaryStatistics();
        if (stats.getCount() == 0) return null;
        return Money.amount(stats.getMin()) + "-" + Money.amount(stats.getMax()) + " PLN";
    }

    private static String format(Integer gr) {
        return gr == null ? null : Money.format(gr);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
