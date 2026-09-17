package pl.szymtrener.consent;

/** Miejsce, w ktorym zapadla decyzja. Nieznana wartosc z formularza to strona ustawien. */
public enum ConsentSource {
    BAR, DIALOG, PAGE;

    public static ConsentSource parse(String raw) {
        for (ConsentSource source : values()) {
            if (source.name().equalsIgnoreCase(raw)) return source;
        }
        return PAGE;
    }
}
