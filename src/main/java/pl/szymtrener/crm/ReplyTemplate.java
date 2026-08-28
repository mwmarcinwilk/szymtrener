package pl.szymtrener.crm;

import jakarta.persistence.*;

/**
 * Gotowa odpowiedz do wstawienia w pole wiadomosci. W makiecie szablony siedza
 * w JavaScripcie; tutaj sa w bazie, zeby trener poprawial je sam, bez wdrozenia.
 */
@Entity
@Table(name = "reply_template")
public class ReplyTemplate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String label;
    @Column(nullable = false, columnDefinition = "text") private String body;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
