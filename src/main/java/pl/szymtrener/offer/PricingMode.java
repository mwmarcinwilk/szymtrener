package pl.szymtrener.offer;

/** Ktora cena obowiazuje w danym pakiecie. Ustawiana osobno dla kazdego. */
public enum PricingMode {

    STARTOWA("Cena startowa"),
    DOCELOWA("Cena docelowa");

    private final String label;

    PricingMode(String label) { this.label = label; }

    public String label() { return label; }
}
