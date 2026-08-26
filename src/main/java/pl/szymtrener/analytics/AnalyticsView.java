package pl.szymtrener.analytics;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Przeliczanie surowych wynikow z {@link PageViewRepository} na to, co rysuje panel.
 * Wydzielone z kontrolerow, bo pulpit i statystyki pokazuja te same slupki, te same
 * wiersze i ten sam trend — dwie kopie tej samej arytmetyki rozjechalyby sie
 * przy pierwszej zmianie progu.
 */
@Component
public class AnalyticsView {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final Locale PL = Locale.forLanguageTag("pl-PL");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", PL);

    /** Slupek wykresu: wysokosc w procentach, bo rysujemy go czystym CSS-em. */
    public record Bar(String label, long value, int percent, boolean weekend) {}

    /** Wiersz zestawienia: etykieta plus liczba i udzial w calosci. */
    public record Row(String label, long value, int percent) {}

    /**
     * Zmiana wzgledem poprzedniego okna tej samej dlugosci.
     *
     * @param up   true gdy w gore — decyduje o strzalce i kolorze
     * @param text gotowy podpis, np. „18,4%"
     */
    public record Trend(boolean up, boolean known, String text) {}

    private final PageViewRepository views;

    public AnalyticsView(PageViewRepository views) {
        this.views = views;
    }

    /**
     * Dni bez ruchu tez musza byc na wykresie — inaczej cztery slupki obok siebie
     * sugeruja cztery kolejne dni, a to moga byc cztery dni z calego kwartalu.
     */
    public List<Bar> bars(Instant since, int range) {
        int days = Math.min(range, 30);
        LocalDate from = LocalDate.now(ZONE).minusDays(days - 1L);

        long[] counts = new long[days];
        for (Object[] row : views.dailyViews(since)) {
            // date_trunc na timestamptz wraca jako Instant (Hibernate 6), nie java.sql.Timestamp
            LocalDate day = ((Instant) row[0]).atZone(ZONE).toLocalDate();
            int index = (int) ChronoUnit.DAYS.between(from, day);
            if (index >= 0 && index < days) counts[index] = ((Number) row[1]).longValue();
        }

        long max = 0;
        for (long count : counts) max = Math.max(max, count);

        List<Bar> bars = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate day = from.plusDays(i);
            int percent = max == 0 ? 0 : (int) Math.max(2, Math.round(counts[i] * 100.0 / max));
            bars.add(new Bar(DAY.format(day), counts[i], percent, day.getDayOfWeek().getValue() >= 6));
        }
        return bars;
    }

    public List<Row> rows(List<Object[]> source, long total, int limit) {
        return source.stream()
                .limit(limit)
                .map(row -> {
                    long value = ((Number) row[1]).longValue();
                    String label = row[0] == null ? "(bezpośrednie)" : row[0].toString();
                    return new Row(label, value, total == 0 ? 0 : (int) Math.round(value * 100.0 / total));
                })
                .toList();
    }

    /**
     * Trend liczony wzgledem poprzedniego okna tej samej dlugosci.
     * Przy zerowej bazie nie ma czego porownywac — mowimy o tym wprost zamiast
     * pokazywac efektowne, ale bez sensu „+100%".
     */
    public Trend trend(long current, long previous) {
        if (previous == 0) {
            return new Trend(current > 0, false, current > 0 ? "pierwsze dane" : "brak danych");
        }
        double change = (current - previous) * 100.0 / previous;
        return new Trend(change >= 0, true, String.format(PL, "%+.1f%%", change));
    }

    /** Okno „poprzednie tyle samo dni" — para wartosci do {@link #trend}. */
    public Trend trendOverDays(int days, java.util.function.BiFunction<Instant, Instant, Long> counter) {
        Instant now = Instant.now();
        Instant start = now.minus(Duration.ofDays(days));
        Instant earlier = start.minus(Duration.ofDays(days));
        return trend(counter.apply(start, now), counter.apply(earlier, start));
    }
}
