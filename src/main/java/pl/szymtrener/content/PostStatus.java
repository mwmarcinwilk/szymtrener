package pl.szymtrener.content;

public enum PostStatus {

    DRAFT("Szkic", "draft"),
    SCHEDULED("Zaplanowany", "plan"),
    PUBLISHED("Opublikowany", "pub"),
    ARCHIVED("Archiwum", "arch");

    private final String label;
    private final String badge;

    PostStatus(String label, String badge) {
        this.label = label;
        this.badge = badge;
    }

    /** Nazwa dla czlowieka — szablon nie tlumaczy statusow samodzielnie. */
    public String label() { return label; }

    /** Klasa plakietki z admin.css (.bg.pub / .plan / .draft / .arch). */
    public String badge() { return badge; }
}
