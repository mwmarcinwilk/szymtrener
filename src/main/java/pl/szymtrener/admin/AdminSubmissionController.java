package pl.szymtrener.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.common.SlugUtil;
import pl.szymtrener.crm.*;
import pl.szymtrener.settings.SettingsService;
import pl.szymtrener.submission.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/zgloszenia")
public class AdminSubmissionController {

    private static final Logger log = LoggerFactory.getLogger(AdminSubmissionController.class);
    private static final int PAGE_SIZE = 25;

    private final SubmissionRepository submissions;
    private final SubmissionNoteRepository notes;
    private final SubmissionService service;
    private final ObjectMapper json;
    private final SettingsService settings;
    private final MessageService messages;
    private final TraineeService trainees;

    public AdminSubmissionController(SubmissionRepository submissions, SubmissionNoteRepository notes,
                                     SubmissionService service, ObjectMapper json,
                                     SettingsService settings, MessageService messages,
                                     TraineeService trainees) {
        this.submissions = submissions;
        this.notes = notes;
        this.service = service;
        this.json = json.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.settings = settings;
        this.messages = messages;
        this.trainees = trainees;
    }

    @GetMapping
    public String list(@RequestParam(required = false) SubmissionStatus status,
                       @RequestParam(defaultValue = "0") int strona, Model model) {
        model.addAttribute("items", status == null
                ? submissions.findAllByOrderByCreatedAtDesc(PageRequest.of(strona, PAGE_SIZE))
                : submissions.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(strona, PAGE_SIZE)));
        model.addAttribute("activeStatus", status);
        model.addAttribute("statuses", SubmissionStatus.values());
        model.addAttribute("mailEnabled", settings.getBoolean(SettingsService.MAIL_ENABLED, true));
        model.addAttribute("countAll", submissions.count());
        model.addAttribute("countNew", submissions.countByStatus(SubmissionStatus.NEW));
        model.addAttribute("countContact", submissions.countByStatus(SubmissionStatus.IN_CONTACT));
        model.addAttribute("countClients", submissions.countByStatus(SubmissionStatus.CLIENT));
        model.addAttribute("countArchived", submissions.countByStatus(SubmissionStatus.ARCHIVED));
        model.addAttribute("page", strona);
        model.addAttribute("baseUrl", status == null ? "/admin/zgloszenia" : "/admin/zgloszenia?status=" + status);
        model.addAttribute("title", "Zgłoszenia");
        return "admin/submissions";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Submission submission = submissions.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma zgłoszenia " + id));
        List<SubmissionNote> all = notes.findBySubmissionIdOrderByPinnedDescCreatedAtDesc(id);

        model.addAttribute("item", submission);
        model.addAttribute("notes", all);
        // Karta „Na co uwazac": wyciag z notatek oznaczonych Zdrowie albo Wazne,
        // zeby przeciwwskazania byly widoczne bez czytania calosci.
        model.addAttribute("warnings", all.stream().filter(SubmissionNote::warning).limit(4).toList());
        model.addAttribute("thread", messages.thread(id));
        model.addAttribute("templates", messages.replyTemplates());
        model.addAttribute("statuses", SubmissionStatus.values());
        model.addAttribute("title", "Zgłoszenie: " + submission.getName());
        return "admin/submission-detail";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam SubmissionStatus status,
                               @RequestParam(required = false) String callAt) {
        service.changeStatus(id, status,
                (callAt == null || callAt.isBlank()) ? null
                        : LocalDateTime.parse(callAt).atZone(ZoneId.of("Europe/Warsaw")).toInstant());
        return "redirect:/admin/zgloszenia/" + id;
    }

    @PostMapping("/{id}/notatka")
    public String addNote(@PathVariable Long id, @RequestParam String body, Principal principal) {
        service.addNote(id, principal != null ? principal.getName() : "panel", body);
        return "redirect:/admin/zgloszenia/" + id;
    }

    /**
     * Zmiana etapu ze sciezki. Kazde przejscie zostawia slad w watku — inaczej
     * po tygodniu nie wiadomo, dlaczego zgloszenie stoi tam, gdzie stoi.
     */
    @PostMapping("/{id}/etap")
    public String stage(@PathVariable Long id, @RequestParam SubmissionStatus status,
                        RedirectAttributes flash) {
        Submission before = submissions.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma zgłoszenia " + id));
        if (before.getStatus() != status) {
            service.stage(id, status);
            messages.system(id, null, "Etap zmieniony na „" + status.label() + "”", false);
        }
        flash.addFlashAttribute("info", "Etap: " + status.label() + ".");
        return "redirect:/admin/zgloszenia/" + id;
    }

    /**
     * Wiadomosc do klienta albo zapis rozmowy telefonicznej. Tryb „telefon" NIC
     * nie wysyla — to tylko slad w watku.
     */
    @PostMapping("/{id}/wiadomosc")
    public String message(@PathVariable Long id,
                          @RequestParam String body,
                          @RequestParam(defaultValue = "mail") String way,
                          RedirectAttributes flash) {

        Submission s = submissions.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma zgłoszenia " + id));
        if (body == null || body.isBlank()) {
            flash.addFlashAttribute("error", "Pusta wiadomość nie ma czego przenieść.");
            return "redirect:/admin/zgloszenia/" + id;
        }

        if ("tel".equals(way)) {
            messages.logPhoneCall(id, null, body.trim());
            flash.addFlashAttribute("info", "Zapisano rozmowę. Do klienta nic nie poszło.");
        } else {
            MessageService.SendResult result =
                    messages.sendEmail(id, null, s.getEmail(), s.getName(), body.trim(), null);
            if (result.sent()) {
                flash.addFlashAttribute("info", "Wiadomość poszła do " + s.getEmail() + ".");
            } else {
                flash.addFlashAttribute("error", result.error());
            }
        }
        return "redirect:/admin/zgloszenia/" + id;
    }

    /** Wstawia szablon do pola odpowiedzi z podstawionym imieniem. */
    @GetMapping("/{id}/szablon/{code}")
    @ResponseBody
    public Map<String, String> template(@PathVariable Long id, @PathVariable String code) {
        Submission s = submissions.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma zgłoszenia " + id));
        String firstName = s.getName() == null ? "" : s.getName().trim().split("\\s+")[0];
        return Map.of("body", messages.fill(code, firstName, s.getCurrentTraining()));
    }

    /** Przypomnienie: „za ile" albo konkretna data. */
    @PostMapping("/{id}/przypomnienie")
    public String remind(@PathVariable Long id,
                         @RequestParam(required = false) String preset,
                         @RequestParam(required = false) String date,
                         RedirectAttributes flash) {

        java.time.ZoneId zone = ZoneId.of("Europe/Warsaw");
        java.time.Instant at = null;
        if (date != null && !date.isBlank()) {
            at = LocalDateTime.parse(date).atZone(zone).toInstant();
        } else if (preset != null && !preset.isBlank() && !"brak".equals(preset)) {
            // Przypomnienie o 9:00, nie o godzinie klikniecia — o 23:40 „za 3 dni"
            // powinno znaczyc rano trzeciego dnia, a nie w srodku nocy.
            int days = switch (preset) {
                case "jutro" -> 1;
                case "3dni" -> 3;
                case "tydzien" -> 7;
                default -> 0;
            };
            if (days > 0) {
                at = java.time.LocalDate.now(zone).plusDays(days).atTime(9, 0).atZone(zone).toInstant();
            }
        }
        service.remind(id, at);
        flash.addFlashAttribute("info", at == null
                ? "Przypomnienie wyłączone."
                : "Przypomnę Ci o tym zgłoszeniu.");
        return "redirect:/admin/zgloszenia/" + id;
    }

    /** Odhaczenie przypomnienia bez zmiany terminu. */
    @PostMapping("/{id}/przypomnienie/zalatwione")
    public String remindDone(@PathVariable Long id, RedirectAttributes flash) {
        service.remindDone(id);
        flash.addFlashAttribute("info", "Załatwione.");
        return "redirect:/admin/zgloszenia/" + id;
    }

    /** „Zrób klienta" — przenosi dane, watek i notatki, po czym otwiera profil. */
    @PostMapping("/{id}/konwertuj")
    public String convert(@PathVariable Long id, RedirectAttributes flash) {
        Trainee trainee = trainees.fromSubmission(id);
        messages.attachToTrainee(id, trainee.getId());
        notes.findBySubmissionIdOrderByPinnedDescCreatedAtDesc(id).forEach(n -> {
            n.setTraineeId(trainee.getId());
            notes.save(n);
        });
        messages.system(id, trainee.getId(), "Zgłoszenie zamienione na klienta", false);
        flash.addFlashAttribute("info", "Gotowe. To teraz klient.");
        return "redirect:/admin/klienci/" + trainee.getId();
    }

    /** Notatka z tagami. Puste tagi zapisujemy jako brak, nie jako pusty ciag. */
    @PostMapping("/{id}/notatka/tagi")
    public String addTaggedNote(@PathVariable Long id, @RequestParam String body,
                                @RequestParam(required = false) List<String> tags,
                                Principal principal, RedirectAttributes flash) {
        if (body == null || body.isBlank()) {
            flash.addFlashAttribute("error", "Pusta notatka nie zostanie zapisana.");
            return "redirect:/admin/zgloszenia/" + id;
        }
        service.addNote(id, null, principal != null ? principal.getName() : "panel", body.trim(),
                tags == null ? null : String.join(", ", tags));
        return "redirect:/admin/zgloszenia/" + id;
    }

        /**
     * Prawo dostepu do danych (RODO, art. 15) — komplet zgloszenia w JSON-ie,
     * gotowy do odeslania klientowi, ktory o niego poprosi.
     */
    @GetMapping("/{id}/dane")
    @ResponseBody
    public ResponseEntity<byte[]> export(@PathVariable Long id) throws Exception {
        Map<String, Object> data = service.export(id);
        byte[] body = json.writeValueAsBytes(data);
        String filename = "zgloszenie-" + id + "-" + SlugUtil.slugify(String.valueOf(data.get("imie"))) + ".json";

        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    /**
     * Prawo do usuniecia danych (RODO, art. 17). Kasujemy naprawde, nie archiwizujemy —
     * na zadanie klienta trzeba to zrobic w 30 dni i nie zostawiac kopii.
     */
    @PostMapping("/{id}/usun")
    public String delete(@PathVariable Long id, Principal principal) {
        Submission submission = submissions.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma zgłoszenia " + id));
        log.info("Usunięcie zgłoszenia {} ({}) przez {}", id, submission.getEmail(),
                principal != null ? principal.getName() : "panel");
        service.delete(id);
        return "redirect:/admin/zgloszenia?usunieto";
    }
}
