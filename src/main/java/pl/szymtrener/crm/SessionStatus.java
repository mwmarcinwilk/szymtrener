package pl.szymtrener.crm;

public enum SessionStatus {

    PLANNED("Zaplanowany"),
    DONE("Odbyty"),
    CANCELLED("Odwołany");

    private final String label;

    SessionStatus(String label) { this.label = label; }

    public String label() { return label; }

    /** Klasa plakietki w dzienniku treningow. */
    public String badge() {
        return switch (this) {
            case PLANNED -> "plan";
            case DONE -> "done";
            case CANCELLED -> "arch";
        };
    }
}
