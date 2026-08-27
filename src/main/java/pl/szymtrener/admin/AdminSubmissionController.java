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
import pl.szymtrener.settings.SettingsService;
import pl.szymtrener.submission.*;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    public AdminSubmissionController(SubmissionRepository submissions, SubmissionNoteRepository notes,
                                     SubmissionService service, ObjectMapper json,
                                     SettingsService settings) {
        this.submissions = submissions;
        this.notes = notes;
        this.service = service;
        this.json = json.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.settings = settings;
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
        model.addAttribute("item", submission);
        model.addAttribute("notes", notes.findBySubmissionIdOrderByCreatedAtDesc(id));
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
