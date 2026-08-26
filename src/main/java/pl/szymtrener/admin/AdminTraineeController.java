package pl.szymtrener.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.crm.*;
import pl.szymtrener.submission.SubmissionRepository;

@Controller
@RequestMapping("/admin/klienci")
public class AdminTraineeController {

    private static final int PAGE_SIZE = 25;

    private final TraineeRepository trainees;
    private final TraineeService service;
    private final SubmissionRepository submissions;

    public AdminTraineeController(TraineeRepository trainees, TraineeService service,
                                  SubmissionRepository submissions) {
        this.trainees = trainees;
        this.service = service;
        this.submissions = submissions;
    }

    @GetMapping
    public String list(@RequestParam(required = false) TraineeStatus status,
                       @RequestParam(defaultValue = "0") int strona, Model model) {
        model.addAttribute("items", status == null
                ? trainees.findAllOrdered(PageRequest.of(strona, PAGE_SIZE))
                : trainees.findByStatusOrdered(status, PageRequest.of(strona, PAGE_SIZE)));
        model.addAttribute("statuses", TraineeStatus.values());
        model.addAttribute("activeStatus", status);
        model.addAttribute("activeCount", trainees.countByStatus(TraineeStatus.ACTIVE));
        model.addAttribute("countAll", trainees.count());
        model.addAttribute("countOnline", trainees.countByMode(TraineeMode.ONLINE));
        model.addAttribute("countOnsite", trainees.countByMode(TraineeMode.ONSITE));
        model.addAttribute("baseUrl", status == null ? "/admin/klienci" : "/admin/klienci?status=" + status);
        model.addAttribute("title", "Klienci");
        return "admin/clients";
    }

    @GetMapping("/nowy")
    public String create(Model model) {
        model.addAttribute("form", new TraineeForm());
        model.addAttribute("title", "Nowy klient");
        return form(model, null);
    }

    @GetMapping("/{id}")
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
        return form;
    }
}
