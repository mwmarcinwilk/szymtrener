package pl.szymtrener.crm;

/** Czym przyszla albo poszla wiadomosc. */
public enum MessageChannel {

    FORM("Formularz"),
    EMAIL("E-mail"),
    PHONE("Telefon · notatka"),
    /** Cienka linia w watku: zmiana etapu, nieudana wysylka. Nie ma dymka. */
    SYSTEM("System");

    private final String label;

    MessageChannel(String label) { this.label = label; }

    public String label() { return label; }

    /** Klasa plakietki kanalu w dymku. */
    public String css() {
        return switch (this) {
            case FORM -> "form";
            case EMAIL -> "mail";
            case PHONE -> "tel";
            case SYSTEM -> "sys";
        };
    }
}
