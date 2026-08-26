package pl.szymtrener.submission;

public enum SubmissionStatus {
    NEW, IN_CONTACT, CALL_BOOKED, CLIENT, ARCHIVED;

    public String label() {
        return switch (this) {
            case NEW -> "Nowe";
            case IN_CONTACT -> "W kontakcie";
            case CALL_BOOKED -> "Rozmowa umówiona";
            case CLIENT -> "Klient";
            case ARCHIVED -> "Archiwum";
        };
    }

    /** Klasa plakietki z admin.css (.bg.new / .done / .arch / .plan). */
    public String badge() {
        return switch (this) {
            case NEW -> "new";
            case IN_CONTACT, CALL_BOOKED -> "plan";
            case CLIENT -> "done";
            case ARCHIVED -> "arch";
        };
    }
}
