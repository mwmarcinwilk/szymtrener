package pl.szymtrener.offer;

import jakarta.persistence.*;

/**
 * Pakiet prowadzenia online. Kazda kwota, ktora kiedykolwiek sie zmieni, jest tu
 * polem — brief wymaga, zeby zmiana ceny nie wymagala wdrozenia ani programisty.
 *
 * Kwoty w GROSZACH: int nie ma bledu zaokraglenia, ktory przy cenach potrafi
 * dolozyc grosz w podsumowaniu. Formatowanie „1 074 zl" nalezy do widoku.
 */
@Entity
@Table(name = "online_package")
public class OnlinePackage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String name;
    @Column(name = "duration_label", nullable = false) private String durationLabel;

    @Column(name = "current_total_gr", nullable = false) private int currentTotalGr;
    @Column(name = "current_monthly_gr", nullable = false) private int currentMonthlyGr;
    @Column(name = "target_total_gr", nullable = false) private int targetTotalGr;
    @Column(name = "target_monthly_gr", nullable = false) private int targetMonthlyGr;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_mode", nullable = false) private PricingMode pricingMode = PricingMode.STARTOWA;

    @Column(name = "seats_taken", nullable = false) private int seatsTaken;
    @Column(name = "seats_total", nullable = false) private int seatsTotal = 5;

    @Column(name = "badge_text") private String badgeText;
    @Column(name = "badge_visible", nullable = false) private boolean badgeVisible;
    /**
     * Czy plakietka mowi o cenie startowej. Promocyjna znika po zamknieciu naboru
     * (brief 2.4); „Najlepszy wybor" zostaje, bo opisuje pakiet, nie promocje.
     */
    @Column(name = "badge_promotional", nullable = false) private boolean badgePromotional = true;
    @Column(nullable = false) private boolean highlighted;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private boolean visible = true;

    /**
     * Cena startowa obowiazuje tylko dopoki sa wolne miejsca. Brief: „gdy
     * miejsca_wolne = 0 → automatyczne przelaczenie trybu na docelowa".
     */
    @Transient
    public PricingMode effectiveMode() {
        if (pricingMode == PricingMode.DOCELOWA) return PricingMode.DOCELOWA;
        return seatsLeft() > 0 ? PricingMode.STARTOWA : PricingMode.DOCELOWA;
    }

    @Transient
    public int seatsLeft() {
        return Math.max(0, seatsTotal - seatsTaken);
    }

    @Transient
    public boolean startingPrice() {
        return effectiveMode() == PricingMode.STARTOWA;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDurationLabel() { return durationLabel; }
    public void setDurationLabel(String durationLabel) { this.durationLabel = durationLabel; }
    public int getCurrentTotalGr() { return currentTotalGr; }
    public void setCurrentTotalGr(int v) { this.currentTotalGr = v; }
    public int getCurrentMonthlyGr() { return currentMonthlyGr; }
    public void setCurrentMonthlyGr(int v) { this.currentMonthlyGr = v; }
    public int getTargetTotalGr() { return targetTotalGr; }
    public void setTargetTotalGr(int v) { this.targetTotalGr = v; }
    public int getTargetMonthlyGr() { return targetMonthlyGr; }
    public void setTargetMonthlyGr(int v) { this.targetMonthlyGr = v; }
    public PricingMode getPricingMode() { return pricingMode; }
    public void setPricingMode(PricingMode pricingMode) { this.pricingMode = pricingMode; }
    public int getSeatsTaken() { return seatsTaken; }
    public void setSeatsTaken(int seatsTaken) { this.seatsTaken = seatsTaken; }
    public int getSeatsTotal() { return seatsTotal; }
    public void setSeatsTotal(int seatsTotal) { this.seatsTotal = seatsTotal; }
    public String getBadgeText() { return badgeText; }
    public void setBadgeText(String badgeText) { this.badgeText = badgeText; }
    public boolean isBadgeVisible() { return badgeVisible; }
    public void setBadgeVisible(boolean badgeVisible) { this.badgeVisible = badgeVisible; }
    public boolean isBadgePromotional() { return badgePromotional; }
    public void setBadgePromotional(boolean badgePromotional) { this.badgePromotional = badgePromotional; }
    public boolean isHighlighted() { return highlighted; }
    public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
