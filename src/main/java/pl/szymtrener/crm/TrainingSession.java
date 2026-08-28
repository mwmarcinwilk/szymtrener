package pl.szymtrener.crm;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Jeden trening w dzienniku: zaplanowany, odbyty albo odwolany. */
@Entity
@Table(name = "training_session")
public class TrainingSession {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trainee_id", nullable = false) private Long traineeId;
    @Column(name = "package_id") private Long packageId;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "text") private String note;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SessionStatus status = SessionStatus.PLANNED;

    /**
     * Czy trening zuzywa wejscie z pakietu. Odwolany domyslnie nie zuzywa, ale
     * trener moze to zmienic, gdy klient odwolal na ostatnia chwile.
     */
    @Column(name = "consumes_package", nullable = false) private boolean consumesPackage = true;

    /** Dzien do kolka z data w dzienniku: „31". */
    @Transient
    public String dayLabel() {
        return DateTimeFormatter.ofPattern("d").format(startsAt.atZone(ZONE));
    }

    /** Miesiac do kolka z data: „sie". */
    @Transient
    public String monthLabel() {
        return DateTimeFormatter.ofPattern("LLL", Locale.forLanguageTag("pl-PL"))
                .format(startsAt.atZone(ZONE)).replace(".", "");
    }

    /** „poniedziałek 9:00" — pelny opis terminu pod tytulem sesji. */
    @Transient
    public String whenLabel() {
        return DateTimeFormatter.ofPattern("EEEE H:mm", Locale.forLanguageTag("pl-PL"))
                .format(startsAt.atZone(ZONE));
    }

    /** „pon 31 sie, 9:00" — skrocony termin na liscie klientow. */
    @Transient
    public String shortLabel() {
        return DateTimeFormatter.ofPattern("EEE d MMM, H:mm", Locale.forLanguageTag("pl-PL"))
                .format(startsAt.atZone(ZONE)).replace(".", "");
    }

    /** Wartosc pola <input type="datetime-local"> przy edycji. */
    @Transient
    public String startsAtLocal() {
        return java.time.LocalDateTime.ofInstant(startsAt, ZONE).withSecond(0).withNano(0).toString();
    }

    public Long getId() { return id; }
    public Long getTraineeId() { return traineeId; }
    public void setTraineeId(Long traineeId) { this.traineeId = traineeId; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public boolean isConsumesPackage() { return consumesPackage; }
    public void setConsumesPackage(boolean consumesPackage) { this.consumesPackage = consumesPackage; }
}
