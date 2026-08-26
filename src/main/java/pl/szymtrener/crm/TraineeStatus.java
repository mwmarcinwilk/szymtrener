package pl.szymtrener.crm;

public enum TraineeStatus {
    ACTIVE, PAUSED, FINISHED;

    public String label() {
        return switch (this) {
            case ACTIVE -> "Aktywny";
            case PAUSED -> "Pauza";
            case FINISHED -> "Zakończony";
        };
    }

    /** Klasa plakietki w tabeli — te same kolory co statusy zgloszen. */
    public String badge() {
        return switch (this) {
            case ACTIVE -> "done";
            case PAUSED -> "plan";
            case FINISHED -> "arch";
        };
    }
}
