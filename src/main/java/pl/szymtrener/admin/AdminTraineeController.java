package pl.szymtrener.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.crm.*;
import pl.szymtrener.submission.SubmissionNote;
import pl.szymtrener.submission.SubmissionNoteRepository;
import pl.szymtrener.submission.SubmissionRepository;
import pl.szymtrener.submission.SubmissionService;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Controller
@RequestMapping("/admin/klienci")
public class AdminTraineeController {

    private static final int PAGE_SIZE = 25;

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    /** Po tylu dniach bez kontaktu lista zaczyna ostrzegac. */
    private static final int STALE_DAYS = 14;

    private final TraineeRepository trainees;
    private final TraineeService service;
    private final SubmissionRepository submissions;
    private final ClientInsightService insight;
    private final MessageService messages;
    private final TrainingPackageRepository packages;
    private final TrainingSessionRepository sessions;
    private final MeasurementRepository measurements;
    private final SubmissionNoteRepository notes;
    private final SubmissionService noteService;

    public AdminTraineeController(TraineeRepository trainees, TraineeService service,
                                  SubmissionRepository submissions, ClientInsightService insight,
                                  MessageService messages, TrainingPackageRepository packages,
                                  TrainingSessionRepository sessions, MeasurementRepository measurements,
                                  SubmissionNoteRepository notes, SubmissionService noteService) {
        this.trainees = trainees;
        this.service = service;
        this.submissions = submissions;
        this.insight = insight;
        this.messages = messages;
        this.packages = packages;
        this.sessions = sessions;
        this.measurements = measurements;
        this.notes = notes;
        this.noteService = noteService;
    }

    /**
     * Lista klientow ma odpowiadac na trzy pytania (handoff 8b): komu konczy sie
     * pakiet, kto ma trening w najblizszych dniach, z kim dawno nie bylo kontaktu.
     * Dlatego filtruje sie tutaj w pamieci, po policzonym stanie pakietu — zapytanie
     * SQL nie zna liczby wykorzystanych wejsc bez tej samej reguly zapisanej drugi raz.
     */
    @GetMapping
    public String list(@RequestParam(required = false) String filtr, Model model) {
        List<Trainee> all = trainees.findAll().stream()
                .sorted(java.util.Comparator.comparing(Trainee::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<ClientInsightService.ClientRow> rows = insight.rows(all, STALE_DAYS);
        List<ClientInsightService.ClientRow> shown = switch (filtr == null ? "" : filtr) {
            case "online" -> rows.stream().filter(r -> r.trainee().getMode() == TraineeMode.ONLINE).toList();
            case "stacjonarnie" -> rows.stream().filter(r -> r.trainee().getMode() == TraineeMode.ONSITE).toList();
            case "pakiet" -> rows.stream().filter(r -> r.pack().endingSoon() && !r.pack().none()).toList();
            case "kontakt" -> rows.stream().filter(ClientInsightService.ClientRow::stale).toList();
            case "przerwa" -> rows.stream().filter(r -> r.trainee().getStatus() == TraineeStatus.PAUSED).toList();
            default -> rows;
        };

        List<ClientInsightService.ClientRow> ending =
                rows.stream().filter(r -> r.pack().endingSoon() && !r.pack().none()).toList();

        model.addAttribute("rows", shown);
        model.addAttribute("ending", ending);
        model.addAttribute("filtr", filtr == null ? "" : filtr);
        model.addAttribute("countAll", rows.size());
        model.addAttribute("countOnline", rows.stream().filter(r -> r.trainee().getMode() == TraineeMode.ONLINE).count());
        model.addAttribute("countOnsite", rows.stream().filter(r -> r.trainee().getMode() == TraineeMode.ONSITE).count());
        model.addAttribute("countEnding", ending.size());
        model.addAttribute("countStale", rows.stream().filter(ClientInsightService.ClientRow::stale).count());
        model.addAttribute("countPaused", rows.stream().filter(r -> r.trainee().getStatus() == TraineeStatus.PAUSED).count());
        model.addAttribute("activeCount", trainees.countByStatus(TraineeStatus.ACTIVE));

        List<TrainingSession> week = insight.thisWeek();
        model.addAttribute("weekCount", week.size());
        model.addAttribute("weekLeft", week.stream()
                .filter(x -> x.getStartsAt().isAfter(Instant.now()))
                .filter(x -> x.getStatus() == SessionStatus.PLANNED).count());
        model.addAttribute("soldThisMonth", insight.soldThisMonth());
        model.addAttribute("title", "Klienci");
        return "admin/clients";
    }

    /** Treningi biezacego tygodnia — „kto ma trening w najblizszych dniach". */
    @GetMapping("/tydzien")
    public String week(Model model) {
        List<TrainingSession> week = insight.thisWeek();
        model.addAttribute("sessions", week);
        model.addAttribute("names", week.stream()
                .map(TrainingSession::getTraineeId).distinct()
                .collect(java.util.stream.Collectors.toMap(id -> id,
                        id -> trainees.findById(id).map(Trainee::getName).orElse("—"))));
        model.addAttribute("title", "Tydzień treningów");
        return "admin/clients-week";
    }

    @GetMapping("/nowy")
    public String create(Model model) {
        model.addAttribute("form", new TraineeForm());
        model.addAttribute("title", "Nowy klient");
        return form(model, null);
    }

    /** Profil klienta: dziennik, pomiary, rozmowa i kolumna z pakietem. */
    @GetMapping("/{id}")
    public String profile(@PathVariable Long id, Model model) {
        Trainee trainee = trainees.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma klienta " + id));

        List<SubmissionNote> all = notes.findByTraineeIdOrderByPinnedDescCreatedAtDesc(id);

        model.addAttribute("t", trainee);
        model.addAttribute("pack", insight.packageState(id));
        model.addAttribute("packages", packages.findByTraineeIdOrderByPurchasedAtDesc(id));
        model.addAttribute("log", insight.log(id, 12));
        model.addAttribute("progress", insight.progress(id));
        model.addAttribute("weight", insight.weightChange(id));
        model.addAttribute("thread", messages.traineeThread(id));
        model.addAttribute("notes", all);
        model.addAttribute("warnings", all.stream().filter(SubmissionNote::warning).limit(4).toList());
        model.addAttribute("done", insight.doneSessions(id));
        model.addAttribute("cancelled", insight.cancelledSessions(id));
        model.addAttribute("attendance", insight.attendancePct(id));
        model.addAttribute("lifetime", insight.lifetimeValue(id));
        model.addAttribute("statuses", SessionStatus.values());
        model.addAttribute("title", trainee.getName());
        return "admin/client-profile";
    }

    /** Formularz danych klienta. Profil jest pod /{id}, wiec edycja ma wlasny adres. */
    @GetMapping("/{id}/edycja")
    public String edit(@PathVariable Long id, Model model) {
        Trainee trainee = trainees.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma klienta " + id));
        model.addAttribute("form", toForm(trainee));
        model.addAttribute("title", trainee.getName());
        return form(model, trainee);
    }

    @PostMapping("/zapisz")
    public String save(@ModelAttribute("form") TraineeForm form, RedirectAttributes flash) {
        Trainee saved = service.save(form);
        flash.addFlashAttribute("info", "Zapisano dane klienta.");
        return "redirect:/admin/klienci/" + saved.getId();
    }

    @PostMapping("/{id}/usun")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        service.delete(id);
        flash.addFlashAttribute("info", "Klient usunięty.");
        return "redirect:/admin/klienci";
    }

    // ── Dziennik treningow, pomiary, pakiety ─────────────────────────────────

    /**
     * Dodanie albo zmiana sesji. Odwolany trening domyslnie nie zuzywa pakietu —
     * przelacznik pozwala trenerowi zdecydowac inaczej, gdy klient odwolal
     * na ostatnia chwile.
     */
    @PostMapping("/{id}/sesja")
    public String saveSession(@PathVariable Long id,
                              @RequestParam(required = false) Long sessionId,
                              @RequestParam String startsAt,
                              @RequestParam String title,
                              @RequestParam(required = false) String note,
                              @RequestParam SessionStatus status,
                              @RequestParam(defaultValue = "false") boolean consumesPackage,
                              RedirectAttributes flash) {

        Trainee trainee = trainees.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma klienta " + id));
        if (title.isBlank() || startsAt.isBlank()) {
            flash.addFlashAttribute("error", "Trening potrzebuje terminu i nazwy.");
            return "redirect:/admin/klienci/" + id;
        }

        TrainingSession s = sessionId == null ? new TrainingSession()
                : sessions.findById(sessionId).orElseThrow(() -> new NotFoundException("Nie ma sesji " + sessionId));
        s.setTraineeId(id);
        s.setStartsAt(LocalDateTime.parse(startsAt).atZone(ZONE).toInstant());
        s.setTitle(title.trim());
        s.setNote(note == null || note.isBlank() ? null : note.trim());
        s.setStatus(status);
        // Odbyty trening zawsze zuzywa wejscie; przelacznik dotyczy tylko odwolanych.
        s.setConsumesPackage(status == SessionStatus.CANCELLED ? consumesPackage : true);
        if (s.getPackageId() == null) {
            packages.findByTraineeIdAndActiveTrue(id).stream().findFirst()
                    .ifPresent(p -> s.setPackageId(p.getId()));
        }
        sessions.save(s);

        flash.addFlashAttribute("info", "Zapisano trening: " + s.getTitle() + ".");
        return "redirect:/admin/klienci/" + trainee.getId();
    }

    @PostMapping("/{id}/sesja/{sessionId}/usun")
    public String deleteSession(@PathVariable Long id, @PathVariable Long sessionId,
                                RedirectAttributes flash) {
        sessions.deleteById(sessionId);
        flash.addFlashAttribute("info", "Trening usunięty z dziennika.");
        return "redirect:/admin/klienci/" + id;
    }

    /**
     * Nowy pomiar. „Mniej znaczy lepiej" zapisujemy PRZY pomiarze, bo kierunek
     * dobrej zmiany zalezy od metryki: masa ciala w dol, martwy ciag w gore.
     */
    @PostMapping("/{id}/pomiar")
    public String addMeasurement(@PathVariable Long id,
                                 @RequestParam String metric,
                                 @RequestParam String value,
                                 @RequestParam String unit,
                                 @RequestParam(required = false) String takenOn,
                                 @RequestParam(defaultValue = "false") boolean lowerIsBetter,
                                 RedirectAttributes flash) {
        if (metric.isBlank()) {
            flash.addFlashAttribute("error", "Podaj nazwę parametru.");
            return "redirect:/admin/klienci/" + id;
        }
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            flash.addFlashAttribute("error", "Wartość pomiaru musi być liczbą, np. 74,2.");
            return "redirect:/admin/klienci/" + id;
        }

        Measurement m = new Measurement();
        m.setTraineeId(id);
        m.setMetric(metric.trim());
        m.setValue(parsed);
        m.setUnit(unit.isBlank() ? "" : unit.trim());
        m.setTakenOn(takenOn == null || takenOn.isBlank() ? LocalDate.now(ZONE) : LocalDate.parse(takenOn));
        m.setLowerIsBetter(lowerIsBetter);
        measurements.save(m);

        flash.addFlashAttribute("info", "Zapisano pomiar: " + m.getMetric() + ".");
        return "redirect:/admin/klienci/" + id;
    }

    /**
     * Sprzedaz pakietu. Poprzedni aktywny zamykamy: dwa aktywne pakiety naraz
     * uniemozliwilyby policzenie, z ktorego schodza treningi.
     */
    @PostMapping("/{id}/pakiet")
    public String sellPackage(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam int totalSessions,
                              @RequestParam String pricePerSession,
                              RedirectAttributes flash) {
        Integer price = AdminOfferController.grosze(pricePerSession);
        if (price == null || totalSessions < 1) {
            flash.addFlashAttribute("error", "Podaj liczbę treningów i cenę za trening, np. 150.");
            return "redirect:/admin/klienci/" + id;
        }
        packages.findByTraineeIdAndActiveTrue(id).forEach(p -> {
            p.setActive(false);
            packages.save(p);
        });

        TrainingPackage p = new TrainingPackage();
        p.setTraineeId(id);
        p.setName(name.isBlank() ? "Pakiet " + totalSessions : name.trim());
        p.setTotalSessions(totalSessions);
        p.setPricePerSessionGr(price);
        p.setPurchasedAt(LocalDate.now(ZONE));
        packages.save(p);

        flash.addFlashAttribute("info", "Sprzedano " + p.getName() + ".");
        return "redirect:/admin/klienci/" + id;
    }

    /** Wiadomosc do klienta — ten sam mechanizm co przy zgloszeniu. */
    @PostMapping("/{id}/wiadomosc")
    public String message(@PathVariable Long id, @RequestParam String body,
                          @RequestParam(defaultValue = "mail") String way,
                          RedirectAttributes flash) {
        Trainee t = trainees.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma klienta " + id));
        if (body == null || body.isBlank()) {
            flash.addFlashAttribute("error", "Pusta wiadomość nie ma czego przenieść.");
            return "redirect:/admin/klienci/" + id;
        }
        if ("tel".equals(way)) {
            messages.logPhoneCall(null, id, body.trim());
            flash.addFlashAttribute("info", "Zapisano rozmowę. Do klienta nic nie poszło.");
        } else if (t.getEmail() == null || t.getEmail().isBlank()) {
            flash.addFlashAttribute("error", "Ten klient nie ma zapisanego adresu e-mail. Uzupełnij dane albo zapisz rozmowę telefoniczną.");
            return "redirect:/admin/klienci/" + id;
        } else {
            MessageService.SendResult result =
                    messages.sendEmail(null, id, t.getEmail(), t.getName(), body.trim(), null);
            flash.addFlashAttribute(result.sent() ? "info" : "error",
                    result.sent() ? "Wiadomość poszła do " + t.getEmail() + "." : result.error());
        }
        // Kazdy kontakt odswieza licznik „ostatni kontakt" na liscie klientow.
        t.setLastContactAt(Instant.now());
        trainees.save(t);
        return "redirect:/admin/klienci/" + id;
    }

    @PostMapping("/{id}/notatka")
    public String addNote(@PathVariable Long id, @RequestParam String body,
                          @RequestParam(required = false) List<String> tags,
                          Principal principal, RedirectAttributes flash) {
        if (body == null || body.isBlank()) {
            flash.addFlashAttribute("error", "Pusta notatka nie zostanie zapisana.");
            return "redirect:/admin/klienci/" + id;
        }
        noteService.addNote(null, id, principal != null ? principal.getName() : "panel", body.trim(),
                tags == null ? null : String.join(", ", tags));
        return "redirect:/admin/klienci/" + id;
    }

    /** Wolane z ekranu zgloszenia — przycisk „Zrób z tego klienta". */
    @PostMapping("/ze-zgloszenia/{submissionId}")
    public String fromSubmission(@PathVariable Long submissionId, RedirectAttributes flash) {
        Trainee trainee = service.fromSubmission(submissionId);
        flash.addFlashAttribute("info", "Zgłoszenie zamienione na klienta. Uzupełnij plan i datę startu.");
        return "redirect:/admin/klienci/" + trainee.getId();
    }

    private String form(Model model, Trainee trainee) {
        model.addAttribute("modes", TraineeMode.values());
        model.addAttribute("statuses", TraineeStatus.values());
        model.addAttribute("source", trainee == null || trainee.getSubmissionId() == null
                ? null
                : submissions.findById(trainee.getSubmissionId()).orElse(null));
        return "admin/client-edit";
    }

    private TraineeForm toForm(Trainee trainee) {
        TraineeForm form = new TraineeForm();
        form.setId(trainee.getId());
        form.setSubmissionId(trainee.getSubmissionId());
        form.setName(trainee.getName());
        form.setCity(trainee.getCity());
        form.setAge(trainee.getAge());
        form.setMode(trainee.getMode());
        form.setStartedAt(trainee.getStartedAt());
        form.setPlanName(trainee.getPlanName());
        form.setSessionCount(trainee.getSessionCount());
        form.setStatus(trainee.getStatus());
        form.setEmail(trainee.getEmail());
        form.setPhone(trainee.getPhone());
        form.setFixedSlots(trainee.getFixedSlots());
        form.setSource(trainee.getSource());
        form.setGoalNote(trainee.getGoalNote());
        return form;
    }
}
