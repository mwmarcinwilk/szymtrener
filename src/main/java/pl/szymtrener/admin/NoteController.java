package pl.szymtrener.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.szymtrener.submission.SubmissionService;

/** Operacje na notatkach wspolne dla zgloszen i klientow. */
@Controller
public class NoteController {

    private final SubmissionService service;

    public NoteController(SubmissionService service) {
        this.service = service;
    }

    /**
     * Przypina albo odpina notatke i wraca tam, skad przyszlo zadanie. Bez `back`
     * ten sam przycisk musialby istniec w dwoch wersjach — dla zgloszenia i dla klienta.
     */
    @PostMapping("/admin/notatki/{id}/przypnij")
    public String togglePin(@PathVariable Long id, @RequestParam String back) {
        service.togglePin(id);
        return "redirect:" + safe(back);
    }

    @PostMapping("/admin/notatki/{id}/usun")
    public String delete(@PathVariable Long id, @RequestParam String back) {
        service.deleteNote(id);
        return "redirect:" + safe(back);
    }

    /**
     * Adres powrotu przychodzi z formularza, wiec musi zostac w panelu — inaczej
     * byloby to otwarte przekierowanie na dowolna strone.
     */
    private static String safe(String back) {
        return back != null && back.startsWith("/admin/") && !back.startsWith("//")
                ? back : "/admin/zgloszenia";
    }
}
