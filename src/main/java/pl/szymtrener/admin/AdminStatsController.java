package pl.szymtrener.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.szymtrener.analytics.AnalyticsView;
import pl.szymtrener.analytics.PageViewRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Wlasna statystyka — bez ciasteczek stron trzecich i bez skryptu na stronie
 * publicznej. Wizyty botow AI sa tu na wierzchu, bo to jedyny wskaznik
 * widocznosci w AI, ktory da sie zmierzyc z wlasnego serwera.
 */
@Controller
public class AdminStatsController {

    private static final List<Integer> RANGES = List.of(7, 30, 90, 365);
    private static final int TOP = 10;

    private final PageViewRepository views;
    private final AnalyticsView analytics;

    public AdminStatsController(PageViewRepository views, AnalyticsView analytics) {
        this.views = views;
        this.analytics = analytics;
    }

    @GetMapping("/admin/statystyki")
    public String stats(@RequestParam(defaultValue = "30") int dni, Model model) {
        int range = RANGES.contains(dni) ? dni : 30;
        Instant since = Instant.now().minus(Duration.ofDays(range));

        long visits = views.countViews(since);
        long sessions = views.countSessions(since);
        List<Object[]> bots = views.botVisits(since);
        long botTotal = bots.stream().mapToLong(row -> ((Number) row[1]).longValue()).sum();

        model.addAttribute("range", range);
        model.addAttribute("ranges", RANGES);
        model.addAttribute("visits", visits);
        model.addAttribute("sessions", sessions);
        model.addAttribute("botTotal", botTotal);
        model.addAttribute("perSession", sessions == 0 ? "0,0"
                : String.format(Locale.forLanguageTag("pl-PL"), "%.1f", (double) visits / sessions));

        model.addAttribute("visitsTrend", analytics.trendOverDays(range, views::countViewsBetween));
        model.addAttribute("sessionsTrend", analytics.trendOverDays(range, views::countSessionsBetween));

        model.addAttribute("bars", analytics.bars(since, range));
        model.addAttribute("paths", analytics.rows(views.topPaths(since), visits, TOP));
        model.addAttribute("referrers", analytics.rows(views.topReferrers(since), visits, TOP));
        model.addAttribute("devices", analytics.rows(views.deviceSplit(since), visits, 5));
        model.addAttribute("bots", analytics.rows(bots, botTotal, TOP));
        model.addAttribute("title", "Statystyki");
        return "admin/stats";
    }
}
