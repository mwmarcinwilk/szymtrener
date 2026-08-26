package pl.szymtrener.admin;

import org.springframework.stereotype.Component;
import pl.szymtrener.content.PostRepository;
import pl.szymtrener.content.PostStatus;
import pl.szymtrener.submission.SubmissionRepository;
import pl.szymtrener.submission.SubmissionStatus;

/**
 * Liczniki przy pozycjach menu panelu.
 *
 * Zwykly komponent wolany z szablonu przez {@code ${@adminNav...}}, a nie
 * {@code @ControllerAdvice}: advice jest wciagany do KAZDEGO wycinka
 * {@code @WebMvcTest}, takze testu kontrolera publicznego, ktory nie ma
 * repozytoriow JPA i przez to nie wstawal. Tak liczby powstaja dokladnie tam,
 * gdzie sa rysowane, i tylko wtedy, gdy rysowany jest panel.
 */
@Component("adminNav")
public class AdminNav {

    private final PostRepository posts;
    private final SubmissionRepository submissions;

    public AdminNav(PostRepository posts, SubmissionRepository submissions) {
        this.posts = posts;
        this.submissions = submissions;
    }

    /** Przy „Postach" liczba opublikowanych — tyle realnie stoi na blogu. */
    public long publishedPosts() {
        return posts.countByStatus(PostStatus.PUBLISHED);
    }

    /** Przy „Zgloszeniach" tylko nowe: tylko one wymagaja reakcji. */
    public long newSubmissions() {
        return submissions.countByStatus(SubmissionStatus.NEW);
    }
}
