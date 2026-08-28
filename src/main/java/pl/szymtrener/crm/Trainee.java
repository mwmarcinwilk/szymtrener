package pl.szymtrener.crm;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/** Klient, ktory zaczal trenowac. Zwykle powstaje z zgloszenia z formularza. */
@Entity
@Table(name = "trainee")
public class Trainee {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Zgloszenie, z ktorego powstal klient — zostawia slad, skad przyszedl. */
    @Column(name = "submission_id")
    private Long submissionId;

    @Column(nullable = false)
    private String name;

    private String city;
    private Integer age;

    @Enumerated(EnumType.STRING)
    private TraineeMode mode;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "session_count", nullable = false)
    private int sessionCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TraineeStatus status = TraineeStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    private String email;
    private String phone;

    /** Stale terminy w tygodniu, np. „pon 9:00 · czw 9:00". Jedno pole tekstowe: */
    /* trener wpisuje je po ludzku, a aplikacja i tak nie planuje za niego grafiku. */
    @Column(name = "fixed_slots") private String fixedSlots;

    /** Kiedy ostatnio byl kontakt. Lista klientow ostrzega, gdy minelo 14 dni. */
    @Column(name = "last_contact_at") private Instant lastContactAt;

    private String source;

    /** Cele i przeciwwskazania — kolumna boczna profilu. */
    @Column(name = "goal_note", columnDefinition = "text") private String goalNote;

    public Long getId() { return id; }
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public TraineeMode getMode() { return mode; }
    public void setMode(TraineeMode mode) { this.mode = mode; }
    public LocalDate getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDate startedAt) { this.startedAt = startedAt; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public int getSessionCount() { return sessionCount; }
    public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }
    public TraineeStatus getStatus() { return status; }
    public void setStatus(TraineeStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getFixedSlots() { return fixedSlots; }
    public void setFixedSlots(String fixedSlots) { this.fixedSlots = fixedSlots; }
    public Instant getLastContactAt() { return lastContactAt; }
    public void setLastContactAt(Instant lastContactAt) { this.lastContactAt = lastContactAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getGoalNote() { return goalNote; }
    public void setGoalNote(String goalNote) { this.goalNote = goalNote; }

    /** Inicjaly do kolka przy nazwisku. */
    @Transient
    public String initials() {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(java.util.Locale.ROOT);
        }
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * Ile dni bez kontaktu. -1, gdy kontaktu nigdy nie odnotowano — to co innego
     * niz „zero dni temu" i lista musi te dwa przypadki rozroznic.
     */
    @Transient
    public long daysSinceContact() {
        if (lastContactAt == null) return -1;
        return java.time.Duration.between(lastContactAt, Instant.now()).toDays();
    }

    /** Opis ostatniego kontaktu gotowy do wyswietlenia. */
    @Transient
    public String lastContactLabel() {
        long days = daysSinceContact();
        if (days < 0) return "brak kontaktu";
        if (days == 0) return "dzisiaj";
        if (days == 1) return "wczoraj";
        return days + " dni";
    }

    /** Data startu wspolpracy do naglowka profilu. */
    @Transient
    public String startedLabel() {
        if (startedAt == null) return null;
        return java.time.format.DateTimeFormatter
                .ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag("pl-PL")).format(startedAt);
    }

    /** Ile tygodni trwa wspolpraca — „klientka od 14 maja 2026 (15 tygodni)". */
    @Transient
    public Long weeksTogether() {
        if (startedAt == null) return null;
        return java.time.temporal.ChronoUnit.WEEKS.between(startedAt, java.time.LocalDate.now());
    }
}
