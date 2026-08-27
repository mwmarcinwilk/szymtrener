package pl.szymtrener.admin;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final AdminUserRepository admins;
    private final SubmissionRepository submissions;

    public AdminNav(PostRepository posts, SubmissionRepository submissions, AdminUserRepository admins) {
        this.posts = posts;
        this.submissions = submissions;
        this.admins = admins;
    }

    /** Przy „Postach" liczba opublikowanych — tyle realnie stoi na blogu. */
    public long publishedPosts() {
        return posts.countByStatus(PostStatus.PUBLISHED);
    }

    /** Przy „Zgloszeniach" tylko nowe: tylko one wymagaja reakcji. */
    public long newSubmissions() {
        return submissions.countByStatus(SubmissionStatus.NEW);
    }

    /**
     * Nazwa zalogowanej osoby do stopki menu. Bez tego pole „nazwa wyswietlana"
     * bylo zapisywane, ale nigdzie nie widoczne — a wiec bezuzyteczne.
     * Logowanie ze zmiennych srodowiskowych nie ma wiersza w bazie, wiec wtedy
     * zostaje sam adres.
     */
    public String currentName() {
        String login = currentLogin();
        return posts == null ? login : admins.findByEmailIgnoreCase(login)
                .map(AdminUser::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(login);
    }

    /** Inicjaly do kolka w stopce — z nazwy, a gdy jej brak, z adresu. */
    public String currentInitials() {
        String name = currentName();
        String[] parts = name.trim().split("[\\s@._-]+");
        if (parts.length >= 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(java.util.Locale.ROOT);
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase(java.util.Locale.ROOT);
    }

    private static String currentLogin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "" : auth.getName();
    }
}
