package pl.szymtrener.content;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

/** Autor tresci — byline na wpisie + zrodlo danych do JSON-LD Person (E-E-A-T). */
@Entity
@Table(name = "author")
public class Author {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String slug;
    @Column(nullable = false)
    private String name;
    @Column(name = "job_title")
    private String jobTitle;
    @Column(columnDefinition = "text")
    private String bio;
    @Column(name = "photo_path")
    private String photoPath;
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "author_same_as", joinColumns = @JoinColumn(name = "author_id"))
    @Column(name = "url")
    private Set<String> sameAs = new LinkedHashSet<>();

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Set<String> getSameAs() { return sameAs; }
}
