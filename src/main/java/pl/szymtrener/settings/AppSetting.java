package pl.szymtrener.settings;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "app_setting")
public class AppSetting {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "value", columnDefinition = "text")
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected AppSetting() {}

    public AppSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; this.updatedAt = Instant.now(); }
    public Instant getUpdatedAt() { return updatedAt; }
}
