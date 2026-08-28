package pl.szymtrener.crm;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Pakiet kupiony przez klienta. Kwoty w groszach, tak jak w cenniku. */
@Entity
@Table(name = "training_package")
public class TrainingPackage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trainee_id", nullable = false) private Long traineeId;
    @Column(nullable = false) private String name;
    @Column(name = "total_sessions", nullable = false) private int totalSessions;
    @Column(name = "price_per_session_gr", nullable = false) private int pricePerSessionGr;
    @Column(name = "purchased_at", nullable = false) private LocalDate purchasedAt = LocalDate.now();
    @Column(nullable = false) private boolean active = true;

    @Transient
    public int valueGr() {
        return pricePerSessionGr * totalSessions;
    }

    public Long getId() { return id; }
    public Long getTraineeId() { return traineeId; }
    public void setTraineeId(Long traineeId) { this.traineeId = traineeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
    public int getPricePerSessionGr() { return pricePerSessionGr; }
    public void setPricePerSessionGr(int v) { this.pricePerSessionGr = v; }
    public LocalDate getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(LocalDate purchasedAt) { this.purchasedAt = purchasedAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
