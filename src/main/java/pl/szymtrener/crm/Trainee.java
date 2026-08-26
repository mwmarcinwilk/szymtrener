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
}
