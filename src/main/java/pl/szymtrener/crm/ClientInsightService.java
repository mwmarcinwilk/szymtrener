package pl.szymtrener.crm;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.szymtrener.offer.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Liczby, dla ktorych powstal ekran klientow.
 *
 * Handoff 8b: lista ma odpowiadac na trzy pytania — komu konczy sie pakiet,
 * kto ma trening w najblizszych dniach, z kim dawno nie bylo kontaktu. Wszystko
 * liczone z bazy; w makiecie te liczby sa przykladowe.
 */
@Service
public class ClientInsightService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    /** Prog ostrzezenia o koncu pakietu — tyle wejsc zostaje na rozmowe o kolejnym. */
    private static final int ENDING_SOON_LEFT = 2;

    private final TraineeRepository trainees;
    private final TrainingPackageRepository packages;
    private final TrainingSessionRepository sessions;
    private final MeasurementRepository measurements;

    public ClientInsightService(TraineeRepository trainees, TrainingPackageRepository packages,
                                TrainingSessionRepository sessions, MeasurementRepository measurements) {
        this.trainees = trainees;
        this.packages = packages;
        this.sessions = sessions;
        this.measurements = measurements;
    }

    /**
     * @param pct        procent wykorzystania paska
     * @param level      ''/warn/bad — kolor paska, zalezny od tego, ile ZOSTALO
     * @param note       zdanie pod paskiem albo null, gdy nie ma o czym mowic
     */
    public record PackageState(String name, int used, int total, int left, int pct,
                               String level, String note, boolean endingSoon, boolean none) {}

    /** Wiersz tabeli pomiarow: pierwszy pomiar, ostatni i roznica. */
    public record MeasurementRow(String metric, String first, String last,
                                 String delta, String direction, String arrow) {
        /** „−4,2 kg" czyta sie inaczej niz „4,2 kg" — znak niesie polowe informacji. */
        public String signedDelta() {
            return arrow.isEmpty() ? delta : arrow + delta;
        }
    }

    /** Kafelek na liscie klientow. */
    public record ClientRow(Trainee trainee, PackageState pack,
                            TrainingSession next, boolean stale) {}

    // ── Pakiety ──────────────────────────────────────────────────────────────

    /**
     * Stan aktywnego pakietu. Wejscia liczy sie z sesji, nie z licznika w kolumnie:
     * licznik trzeba by pamietac aktualizowac przy kazdej zmianie statusu sesji.
     */
    @Transactional(readOnly = true)
    public PackageState packageState(Long traineeId) {
        Optional<TrainingPackage> active = packages.findByTraineeIdAndActiveTrue(traineeId).stream().findFirst();
        if (active.isEmpty()) {
            return new PackageState(null, 0, 0, 0, 0, "", null, false, true);
        }
        TrainingPackage p = active.get();
        int used = usedSessions(traineeId, p.getId());
        int left = Math.max(0, p.getTotalSessions() - used);
        int pct = p.getTotalSessions() == 0 ? 0
                : Math.min(100, Math.round(used * 100f / p.getTotalSessions()));

        // Kolor zalezy od tego, ile ZOSTALO, nie od procentu: przy pakiecie
        // czterotreningowym 75% to jeszcze jedno wejscie, przy dwunastu — trzy.
        String level = left <= 1 ? "bad" : left <= ENDING_SOON_LEFT ? "warn" : "";
        String note = left == 0 ? "Pakiet wykorzystany"
                : left == 1 ? "Pakiet się kończy"
                : left <= ENDING_SOON_LEFT ? "Zapytaj o kontynuację"
                : null;

        return new PackageState(p.getName(), used, p.getTotalSessions(), left, pct,
                level, note, left <= ENDING_SOON_LEFT, false);
    }

    /**
     * Odwolany trening domyslnie NIE zuzywa pakietu (handoff 8b) — chyba ze trener
     * zaznaczyl inaczej dla konkretnej sesji.
     */
    private int usedSessions(Long traineeId, Long packageId) {
        return (int) sessions.findByTraineeIdOrderByStartsAtDesc(traineeId).stream()
                .filter(s -> packageId.equals(s.getPackageId()))
                .filter(s -> s.getStatus() == SessionStatus.DONE
                        || (s.getStatus() == SessionStatus.CANCELLED && s.isConsumesPackage()))
                .count();
    }

    @Transactional(readOnly = true)
    public List<Trainee> endingSoon() {
        return trainees.findAll().stream()
                .filter(t -> t.getStatus() == TraineeStatus.ACTIVE)
                .filter(t -> packageState(t.getId()).endingSoon())
                .toList();
    }

    /** Klienci, z ktorymi dawno nie bylo kontaktu. Nigdy = tez zaniedbany. */
    @Transactional(readOnly = true)
    public List<Trainee> staleContacts(int days) {
        return trainees.findAll().stream()
                .filter(t -> t.getStatus() == TraineeStatus.ACTIVE)
                .filter(t -> t.daysSinceContact() < 0 || t.daysSinceContact() >= days)
                .toList();
    }

    // ── Sesje ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<TrainingSession> nextSession(Long traineeId) {
        Instant now = Instant.now();
        return sessions.findByTraineeIdAndStatusOrderByStartsAtAsc(traineeId, SessionStatus.PLANNED).stream()
                .filter(s -> s.getStartsAt().isAfter(now))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public List<TrainingSession> log(Long traineeId, int limit) {
        List<TrainingSession> all = new ArrayList<>(sessions.findByTraineeIdOrderByStartsAtDesc(traineeId));
        // Najblizszy zaplanowany trening na gorze dziennika, potem historia malejaco.
        return all.stream().limit(limit).toList();
    }

    /** Treningi biezacego tygodnia, od poniedzialku do niedzieli. */
    @Transactional(readOnly = true)
    public List<TrainingSession> thisWeek() {
        LocalDate monday = LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant from = monday.atStartOfDay(ZONE).toInstant();
        Instant to = monday.plusWeeks(1).atStartOfDay(ZONE).toInstant();
        return sessions.findByStartsAtBetweenOrderByStartsAtAsc(from, to);
    }

    /**
     * Frekwencja: odbyte wobec wszystkich rozliczonych. Zaplanowane sie nie licza,
     * bo jeszcze nie wiadomo, czy dojda do skutku.
     */
    @Transactional(readOnly = true)
    public int attendancePct(Long traineeId) {
        List<TrainingSession> all = sessions.findByTraineeIdOrderByStartsAtDesc(traineeId);
        long done = all.stream().filter(s -> s.getStatus() == SessionStatus.DONE).count();
        long cancelled = all.stream().filter(s -> s.getStatus() == SessionStatus.CANCELLED).count();
        long total = done + cancelled;
        return total == 0 ? 0 : Math.round(done * 100f / total);
    }

    @Transactional(readOnly = true)
    public long doneSessions(Long traineeId) {
        return sessions.findByTraineeIdOrderByStartsAtDesc(traineeId).stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE).count();
    }

    @Transactional(readOnly = true)
    public long cancelledSessions(Long traineeId) {
        return sessions.findByTraineeIdOrderByStartsAtDesc(traineeId).stream()
                .filter(s -> s.getStatus() == SessionStatus.CANCELLED).count();
    }

    // ── Pieniadze ────────────────────────────────────────────────────────────

    /** Suma wszystkich pakietow klienta, w groszach. */
    @Transactional(readOnly = true)
    public int lifetimeValueGr(Long traineeId) {
        return packages.findByTraineeIdOrderByPurchasedAtDesc(traineeId).stream()
                .mapToInt(TrainingPackage::valueGr).sum();
    }

    @Transactional(readOnly = true)
    public String lifetimeValue(Long traineeId) {
        return Money.format(lifetimeValueGr(traineeId));
    }

    /** Sprzedaz pakietow w biezacym miesiacu — kafelek na liscie klientow. */
    @Transactional(readOnly = true)
    public String soldThisMonth() {
        LocalDate first = LocalDate.now(ZONE).withDayOfMonth(1);
        int sum = packages.findByPurchasedAtGreaterThanEqual(first).stream()
                .mapToInt(TrainingPackage::valueGr).sum();
        return Money.format(sum);
    }

    // ── Pomiary ──────────────────────────────────────────────────────────────

    /**
     * Pierwszy pomiar wobec ostatniego, per metryka. Kierunek „dobrej" zmiany
     * bierzemy z danych (`lowerIsBetter`), nie z nazwy metryki — inaczej kazda
     * nowa metryka wymagalaby zmiany w kodzie.
     */
    @Transactional(readOnly = true)
    public List<MeasurementRow> progress(Long traineeId) {
        Map<String, List<Measurement>> byMetric = new LinkedHashMap<>();
        measurements.findByTraineeIdOrderByMetricAscTakenOnAsc(traineeId)
                .forEach(m -> byMetric.computeIfAbsent(m.getMetric(), k -> new ArrayList<>()).add(m));

        List<MeasurementRow> rows = new ArrayList<>();
        byMetric.forEach((metric, list) -> {
            Measurement first = list.get(0);
            Measurement last = list.get(list.size() - 1);
            BigDecimal diff = last.getValue().subtract(first.getValue());
            int sign = diff.signum();

            String direction = sign == 0 ? "flat"
                    : (sign < 0) == first.isLowerIsBetter() ? "good" : "bad";
            String delta = sign == 0 ? "bez zmian"
                    : trim(diff.abs()) + " " + last.getUnit();

            String arrow = sign == 0 ? "" : (sign < 0 ? "−" : "+");
            rows.add(new MeasurementRow(metric,
                    trim(first.getValue()) + " " + first.getUnit(),
                    trim(last.getValue()) + " " + last.getUnit(),
                    delta, direction, arrow));
        });
        return rows;
    }

    /** Pierwsza metryka „w dol", zwykle masa ciala — kafelek na profilu. */
    @Transactional(readOnly = true)
    public MeasurementRow weightChange(Long traineeId) {
        return progress(traineeId).stream()
                .filter(r -> r.metric().toLowerCase(java.util.Locale.ROOT).contains("masa"))
                .findFirst().orElse(null);
    }

    /** „74,2" zamiast „74,20"; przecinek, bo tak zapisuje sie liczby po polsku. */
    private static String trim(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros()
                .toPlainString().replace('.', ',');
    }

    // ── Lista ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ClientRow> rows(List<Trainee> source, int staleDays) {
        return source.stream()
                .map(t -> new ClientRow(t, packageState(t.getId()), nextSession(t.getId()).orElse(null),
                        t.daysSinceContact() >= staleDays))
                .toList();
    }
}
