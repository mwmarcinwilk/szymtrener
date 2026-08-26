package pl.szymtrener.submission;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "submission")
public class Submission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SubmissionType type;

    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    private String phone;
    private String city;

    @Column(name = "current_training", columnDefinition = "text") private String currentTraining;
    @Column(columnDefinition = "text") private String goal;
    private String equipment;
    private String source;
    private String interest;
    @Column(columnDefinition = "text") private String message;

    @Column(name = "consent_at", nullable = false) private Instant consentAt = Instant.now();

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.NEW;

    @Column(name = "call_at") private Instant callAt;
    @Column(name = "ip_hash") private String ipHash;
    @Column(name = "user_agent") private String userAgent;
    @Column(name = "mail_sent", nullable = false) private boolean mailSent;
    @Column(name = "mail_error") private String mailError;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public SubmissionType getType() { return type; }
    public void setType(SubmissionType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCurrentTraining() { return currentTraining; }
    public void setCurrentTraining(String currentTraining) { this.currentTraining = currentTraining; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getInterest() { return interest; }
    public void setInterest(String interest) { this.interest = interest; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getConsentAt() { return consentAt; }
    public void setConsentAt(Instant consentAt) { this.consentAt = consentAt; }
    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public Instant getCallAt() { return callAt; }
    public void setCallAt(Instant callAt) { this.callAt = callAt; }
    public String getIpHash() { return ipHash; }
    public void setIpHash(String ipHash) { this.ipHash = ipHash; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public boolean isMailSent() { return mailSent; }
    public void setMailSent(boolean mailSent) { this.mailSent = mailSent; }
    public String getMailError() { return mailError; }
    public void setMailError(String mailError) { this.mailError = mailError; }
    public Instant getCreatedAt() { return createdAt; }

    /** Termin rozmowy w formacie <input type="datetime-local">, albo pusty. */
    @Transient
    public String callAtLocal() {
        if (callAt == null) return "";
        return java.time.LocalDateTime.ofInstant(callAt, java.time.ZoneId.of("Europe/Warsaw"))
                .withSecond(0).withNano(0).toString();
    }

    /** Inicjaly do kolka na liscie — imie i nazwisko, a gdy jednoczlonowe, dwie litery. */
    @Transient
    public String initials() {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(java.util.Locale.ROOT);
        }
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(java.util.Locale.ROOT);
    }

    /** Data gotowa do wyswietlenia — szablon nie formatuje dat samodzielnie. */
    @Transient
    public String getCreatedLabel() {
        return java.time.format.DateTimeFormatter
                .ofPattern("d MMM yyyy, HH:mm", java.util.Locale.forLanguageTag("pl-PL"))
                .format(createdAt.atZone(java.time.ZoneId.of("Europe/Warsaw")));
    }
}
