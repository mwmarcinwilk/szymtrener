package pl.szymtrener.crm;

/**
 * Polska odmiana liczebnikow.
 *
 * Bez tego panel pisze „1 odwolanych" i „1 pakietow". Trener czyta te liczby
 * codziennie, wiec bledna odmiana rzuca sie w oczy szybciej niz cokolwiek innego.
 */
public final class Plural {

    private Plural() {}

    /**
     * @param one   forma dla 1: „trening"
     * @param few   forma dla 2-4: „treningi"
     * @param many  forma dla 5+ i 12-14: „treningow"
     */
    public static String form(long n, String one, String few, String many) {
        long abs = Math.abs(n);
        if (abs == 1) return one;
        long last = abs % 10, lastTwo = abs % 100;
        return (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) ? few : many;
    }

    /** Liczba razem z odmieniona forma: „3 treningi". */
    public static String of(long n, String one, String few, String many) {
        return n + " " + form(n, one, few, many);
    }

    public static String sessions(long n) {
        return of(n, "trening", "treningi", "treningów");
    }

    public static String packages(long n) {
        return of(n, "pakiet", "pakiety", "pakietów");
    }

    public static String cancelled(long n) {
        return of(n, "odwołany", "odwołane", "odwołanych");
    }

    public static String clients(long n) {
        return of(n, "klientowi", "klientom", "klientom");
    }
}
