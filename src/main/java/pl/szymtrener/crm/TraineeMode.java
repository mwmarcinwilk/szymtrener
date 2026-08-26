package pl.szymtrener.crm;

public enum TraineeMode {
    ONLINE, ONSITE;

    public String label() {
        return switch (this) {
            case ONLINE -> "Online";
            case ONSITE -> "Stacjonarnie";
        };
    }
}
