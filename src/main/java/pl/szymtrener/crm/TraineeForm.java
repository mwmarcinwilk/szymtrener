package pl.szymtrener.crm;

import java.time.LocalDate;

public class TraineeForm {
    private Long id;
    private Long submissionId;
    private String name = "";
    private String city;
    private Integer age;
    private TraineeMode mode = TraineeMode.ONSITE;
    private LocalDate startedAt;
    private String planName;
    private int sessionCount;
    private TraineeStatus status = TraineeStatus.ACTIVE;
    private String email;
    private String phone;
    private String fixedSlots;
    private String source;
    private String goalNote;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getFixedSlots() { return fixedSlots; }
    public void setFixedSlots(String fixedSlots) { this.fixedSlots = fixedSlots; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getGoalNote() { return goalNote; }
    public void setGoalNote(String goalNote) { this.goalNote = goalNote; }
}
