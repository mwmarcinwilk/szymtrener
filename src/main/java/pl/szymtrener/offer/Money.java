package pl.szymtrener.offer;

/**
 * Formatowanie kwot dla calej oferty: online i stacjonarnej.
 *
 * Wydzielone, bo oba briefy wymagaja tego samego zapisu („1 074 zl", nie
 * „1074 zl"), a dwie kopie tej samej reguly rozjezdzaja sie przy pierwszej zmianie.
 */
public final class Money {

    /** Spacja NIEROZDZIELAJACA: „1 074 zl" nie moze sie zlamac na koncu wiersza. */
    private static final char NBSP = '\u00A0';

    private Money() {}

    /** Grosze → „1 074 zł". Bez koncowek groszowych, spacja jako separator tysiecy. */
    public static String format(int grosze) {
        return amount(grosze) + NBSP + "zł";
    }

    /** Sama kwota, bez jednostki — do zapisow typu „od 200 do 240 zł". */
    public static String amount(int grosze) {
        long zlote = Math.round(grosze / 100.0);
        String digits = Long.toString(zlote);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) out.append(NBSP);
            out.append(digits.charAt(i));
        }
        // NBSP takze przed jednostka: „1 074" nie moze zostac oddzielone od „zl"
        return out.toString();
    }
}
