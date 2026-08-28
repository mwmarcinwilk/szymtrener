package pl.szymtrener.offer;

/** Dla kogo jest pakiet stacjonarny. Cena PARY jest laczna za dwie osoby. */
public enum StationaryKind {

    INDYWIDUALNY("Trening indywidualny"),
    PARA("Trening dla pary");

    private final String label;

    StationaryKind(String label) { this.label = label; }

    public String label() { return label; }
}
