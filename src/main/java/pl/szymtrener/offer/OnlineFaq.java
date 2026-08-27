package pl.szymtrener.offer;

import jakarta.persistence.*;

/** Pytanie w akordeonie sekcji oferty online. Osobne od FAQ wpisu blogowego. */
@Entity
@Table(name = "online_faq")
public class OnlineFaq {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String question;
    @Column(columnDefinition = "text") private String answer;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private boolean visible = true;

    /** Pytanie bez odpowiedzi nie trafia na strone — lepiej go nie pokazac. */
    @Transient
    public boolean answered() {
        return answer != null && !answer.isBlank();
    }

    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
