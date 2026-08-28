package pl.szymtrener.offer;

import jakarta.persistence.*;

/**
 * Pakiet treningow stacjonarnych.
 *
 * Trzymamy tylko cene za JEDEN trening. Kwota „razem" i rabat wzgledem wejscia
 * pojedynczego sa liczone: trzy kolumny opisujace te sama cene rozjezdzaja sie
 * przy pierwszej zmianie, a brief wymaga jednego zrodla prawdy (punkt 5.3).
 */
@Entity
@Table(name = "stationary_package")
public class StationaryPackage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private StationaryKind kind = StationaryKind.INDYWIDUALNY;

    @Column(nullable = false) private String name;
    @Column(nullable = false) private int sessions = 1;
    @Column(name = "price_per_session_gr", nullable = false) private int pricePerSessionGr;

    /**
     * Waznosc w tygodniach. NULL dla wejscia pojedynczego — ono nie ma terminu,
     * a „—" w tabeli jest czytelniejsze niz zero.
     */
    @Column(name = "validity_weeks") private Integer validityWeeks;

    @Column(nullable = false) private boolean featured;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private boolean visible = true;

    @Transient
    public int totalGr() {
        return pricePerSessionGr * sessions;
    }

    /** Wejscie bez zobowiazania — punkt odniesienia dla rabatu pozostalych. */
    @Transient
    public boolean single() {
        return sessions <= 1;
    }

    /**
     * Rabat wzgledem ceny wejscia pojedynczego, w calych procentach. Zero, gdy
     * pakiet nie jest tanszy — wtedy nie ma czego chwalic i nie pokazujemy nic.
     */
    @Transient
    public int discountPercent(int singlePriceGr) {
        if (singlePriceGr <= 0 || pricePerSessionGr >= singlePriceGr) return 0;
        return Math.round((singlePriceGr - pricePerSessionGr) * 100f / singlePriceGr);
    }

    public Long getId() { return id; }
    public StationaryKind getKind() { return kind; }
    public void setKind(StationaryKind kind) { this.kind = kind; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSessions() { return sessions; }
    public void setSessions(int sessions) { this.sessions = sessions; }
    public int getPricePerSessionGr() { return pricePerSessionGr; }
    public void setPricePerSessionGr(int v) { this.pricePerSessionGr = v; }
    public Integer getValidityWeeks() { return validityWeeks; }
    public void setValidityWeeks(Integer validityWeeks) { this.validityWeeks = validityWeeks; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
