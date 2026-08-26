package pl.szymtrener.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import pl.szymtrener.analytics.AnalyticsView;
import pl.szymtrener.analytics.PageViewRepository;
import pl.szymtrener.content.Post;
import pl.szymtrener.content.PostRepository;
import pl.szymtrener.content.PostStatus;
import pl.szymtrener.submission.SubmissionRepository;
import pl.szymtrener.submission.SubmissionStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    /** Krotkie haslo to jedyne realne zabezpieczenie panelu — nie schodzimy nizej. */
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final PostRepository posts;
    private final SubmissionRepository submissions;
    private final PageViewRepository views;
    private final AdminUserRepository users;
    private final PasswordEncoder encoder;
    private final AnalyticsView analytics;

    public AdminController(PostRepository posts, SubmissionRepository submissions, PageViewRepository views,
                           AdminUserRepository users, PasswordEncoder encoder, AnalyticsView analytics) {
        this.posts = posts;
        this.submissions = submissions;
        this.views = views;
        this.users = users;
        this.encoder = encoder;
        this.analytics = analytics;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        int range = 30;
        Instant month = Instant.now().minus(Duration.ofDays(range));

        long visits = views.countViews(month);
        long sessions = views.countSessions(month);
        long newSubmissions = submissions.countByStatus(SubmissionStatus.NEW);
        long monthSubmissions = submissions.countByCreatedAtAfter(month);

        model.addAttribute("visits", visits);
        model.addAttribute("sessions", sessions);
        model.addAttribute("newSubmissions", newSubmissions);
        model.addAttribute("publishedPosts", posts.countByStatus(PostStatus.PUBLISHED));
        model.addAttribute("scheduledPosts", posts.countByStatus(PostStatus.SCHEDULED));

        // Konwersja liczona na SESJE, nie na odslony: jedna osoba czytajaca piec
        // podstron to nadal jedna szansa na zgloszenie, nie piec.
        model.addAttribute("conversion", sessions == 0 ? "—"
                : String.format(Locale.forLanguageTag("pl-PL"), "%.1f%%", monthSubmissions * 100.0 / sessions));

        model.addAttribute("visitsTrend", analytics.trendOverDays(range, views::countViewsBetween));
        model.addAttribute("bars", analytics.bars(Instant.now().minus(Duration.ofDays(14)), 14));
        model.addAttribute("referrers", analytics.rows(views.topReferrers(month), visits, 5));

        model.addAttribute("latestSubmissions", submissions.findTop5ByOrderByCreatedAtDesc());
        model.addAttribute("queue", queue());
        model.addAttribute("botVisits", views.botVisits(month));
        model.addAttribute("title", "Pulpit");
        return "admin/dashboard";
    }

    /**
     * Kolejka publikacji: najpierw to, co ma termin, potem swieze szkice.
     * Razem, bo z perspektywy pulpitu to jedno pytanie — „co czeka na wydanie".
     */
    private List<Post> queue() {
        List<Post> queue = new ArrayList<>(posts.findTop5ByStatusOrderByPublishAtAsc(PostStatus.SCHEDULED));
        posts.findTop5ByStatusOrderByUpdatedAtDesc(PostStatus.DRAFT).stream()
                .limit(Math.max(0, 5 - queue.size()))
                .forEach(queue::add);
        return queue;
    }

    @GetMapping("/admin/logowanie")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/admin/haslo")
    public String passwordForm(Model model) {
        model.addAttribute("form", new PasswordForm());
        model.addAttribute("title", "Zmiana hasła");
        model.addAttribute("minLength", MIN_PASSWORD_LENGTH);
        return "admin/password";
    }

    /**
     * Po udanej zmianie unieważniamy sesję i każemy zalogować się od nowa —
     * gdyby ktoś przejął sesję, zmiana hasła musi go z niej wyrzucić.
     */
    @PostMapping("/admin/haslo")
    public String changePassword(@ModelAttribute("form") PasswordForm form, Model model,
                                 HttpServletRequest request, HttpServletResponse response) {
        model.addAttribute("title", "Zmiana hasła");
        model.addAttribute("minLength", MIN_PASSWORD_LENGTH);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AdminUser user = auth == null ? null : users.findByEmailIgnoreCase(auth.getName()).orElse(null);
        if (user == null) return "redirect:/admin/logowanie";

        String error = validate(form, user);
        if (error != null) {
            log.warn("Nieudana próba zmiany hasła dla {}", user.getEmail());
            model.addAttribute("error", error);
            return "admin/password";
        }

        user.setPasswordHash(encoder.encode(form.getFresh()));
        users.save(user);
        log.info("Zmieniono hasło administratora {}", user.getEmail());

        new SecurityContextLogoutHandler().logout(request, response, auth);
        return "redirect:/admin/logowanie?haslo-zmienione";
    }

    private String validate(PasswordForm form, AdminUser user) {
        if (!encoder.matches(form.getCurrent(), user.getPasswordHash())) {
            return "Obecne hasło się nie zgadza.";
        }
        String fresh = form.getFresh() == null ? "" : form.getFresh();
        if (fresh.length() < MIN_PASSWORD_LENGTH) {
            return "Nowe hasło musi mieć co najmniej " + MIN_PASSWORD_LENGTH + " znaków.";
        }
        if (!fresh.equals(form.getRepeat())) {
            return "Powtórzone hasło jest inne niż nowe.";
        }
        if (encoder.matches(fresh, user.getPasswordHash())) {
            return "Nowe hasło jest takie samo jak obecne.";
        }
        return null;
    }
}
