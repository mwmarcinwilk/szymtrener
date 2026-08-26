package pl.szymtrener.analytics;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "page_view")
public class PageView {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String path;
    private String referrer;

    /** sha-256(ip + user-agent + sol + data) — brak ciasteczek i danych osobowych. */
    @Column(name = "session_hash") private String sessionHash;

    private String device;
    @Column(name = "is_bot", nullable = false) private boolean bot;
    @Column(name = "bot_name") private String botName;
    @Column(name = "viewed_at", nullable = false) private Instant viewedAt = Instant.now();

    public Long getId() { return id; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
    public String getSessionHash() { return sessionHash; }
    public void setSessionHash(String sessionHash) { this.sessionHash = sessionHash; }
    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public boolean isBot() { return bot; }
    public void setBot(boolean bot) { this.bot = bot; }
    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }
    public Instant getViewedAt() { return viewedAt; }
}
