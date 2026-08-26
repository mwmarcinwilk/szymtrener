package pl.szymtrener.submission;

public enum SubmissionType {

    ONLINE("Prowadzenie online"),
    CONTACT("Kontakt ogólny");

    private final String label;

    SubmissionType(String label) { this.label = label; }

    /** Nazwa dla czlowieka — szablon nie tlumaczy typow samodzielnie. */
    public String label() { return label; }
}
