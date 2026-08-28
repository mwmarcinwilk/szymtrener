package pl.szymtrener.crm;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Jeden pomiar. Kierunek „dobrej" zmiany trzymamy PRZY DANYCH, nie w widoku:
 * masa ciala i obwody maja spadac, sila i czas deski rosnac. Gdyby ta wiedza
 * siedziala w szablonie, kazdy nowy ekran musialby ja odtwarzac od zera.
 */
@Entity
@Table(name = "measurement")
public class Measurement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trainee_id", nullable = false) private Long traineeId;
    @Column(name = "taken_on", nullable = false) private LocalDate takenOn = LocalDate.now();
    @Column(nullable = false) private String metric;
    @Column(nullable = false) private BigDecimal value;
    @Column(nullable = false) private String unit;
    @Column(name = "lower_is_better", nullable = false) private boolean lowerIsBetter;

    public Long getId() { return id; }
    public Long getTraineeId() { return traineeId; }
    public void setTraineeId(Long traineeId) { this.traineeId = traineeId; }
    public LocalDate getTakenOn() { return takenOn; }
    public void setTakenOn(LocalDate takenOn) { this.takenOn = takenOn; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public boolean isLowerIsBetter() { return lowerIsBetter; }
    public void setLowerIsBetter(boolean lowerIsBetter) { this.lowerIsBetter = lowerIsBetter; }
}
