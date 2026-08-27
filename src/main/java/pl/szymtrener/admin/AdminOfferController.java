package pl.szymtrener.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.offer.*;
import pl.szymtrener.settings.SettingsService;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ekran „Oferta online" — ceny, opinie i FAQ w jednym miejscu.
 *
 * Brief v2.2 stawia to jako wymaganie nr 1 punktu 2.5: „zmiana ceny lub etapu nie
 * moze wymagac deploya ani kontaktu z programista". Dlatego wszystko, co brief
 * przewiduje jako zmienne w czasie (kwoty, liczniki miejsc, tryb ceny, kolejnosc,
 * widocznosc), jest tu edytowalne.
 *
 * Kwoty wpisuje sie w ZLOTYCH, bo tak mysli o nich czlowiek; baza trzyma grosze.
 * Zamiana jest w jednym miejscu — {@link #grosze(String)}.
 */
@Controller
@RequestMapping("/admin/oferta")
public class AdminOfferController {

    private static final Logger log = LoggerFactory.getLogger(AdminOfferController.class);

    private final OnlinePackageRepository packages;
    private final TestimonialRepository testimonials;
    private final OnlineFaqRepository faq;
    private final SettingsService settings;
    private final OnlineOfferService offer;

    public AdminOfferController(OnlinePackageRepository packages, TestimonialRepository testimonials,
                                OnlineFaqRepository faq, SettingsService settings, OnlineOfferService offer) {
        this.packages = packages;
        this.testimonials = testimonials;
        this.faq = faq;
        this.settings = settings;
        this.offer = offer;
    }

    @GetMapping
    public String overview(Model model) {
        model.addAttribute("packages", packages.findAllByOrderBySortOrderAsc());
        model.addAttribute("testimonials", testimonials.findAllByOrderBySortOrderAsc());
        model.addAttribute("questions", faq.findAllByOrderBySortOrderAsc());
        model.addAttribute("consultPrice", zlote(offer.consultPriceGr()));
        model.addAttribute("consultVisible",
                settings.getBoolean(SettingsService.OFFER_CONSULT_VISIBLE, true));
        model.addAttribute("title", "Oferta online");
        return "admin/offer";
    }

    // ── Sciezka 1 — konsultacja + plan ───────────────────────────────────────

    @PostMapping("/konsultacja")
    public String saveConsultation(@RequestParam String price,
                                   @RequestParam(defaultValue = "false") boolean visible,
                                   RedirectAttributes flash) {
        Integer gr = grosze(price);
        if (gr == null) {
            flash.addFlashAttribute("error", "Cena konsultacji musi być liczbą, np. 349 albo 349,50.");
            return "redirect:/admin/oferta";
        }
        settings.set(SettingsService.OFFER_CONSULT_PRICE_GR, String.valueOf(gr));
        settings.set(SettingsService.OFFER_CONSULT_VISIBLE, String.valueOf(visible));
        flash.addFlashAttribute("info", "Zapisano ścieżkę „Konsultacja + Plan”.");
        return "redirect:/admin/oferta";
    }

    // ── Pakiety ──────────────────────────────────────────────────────────────

    @PostMapping("/pakiety")
    public String addPackage(@RequestParam String name, RedirectAttributes flash) {
        if (name == null || name.isBlank()) {
            flash.addFlashAttribute("error", "Podaj nazwę pakietu.");
            return "redirect:/admin/oferta";
        }
        OnlinePackage p = new OnlinePackage();
        p.setName(name.trim());
        p.setDurationLabel("—");
        p.setSortOrder(packages.findAll().size() + 1);
        // Nowy pakiet jest ukryty do czasu uzupelnienia kwot — inaczej na stronie
        // pojawilby sie natychmiast z cena 0 zl.
        p.setVisible(false);
        OnlinePackage saved = packages.save(p);
        flash.addFlashAttribute("info", "Dodano pakiet. Uzupełnij ceny i włącz widoczność.");
        return "redirect:/admin/oferta/pakiety/" + saved.getId();
    }

    @GetMapping("/pakiety/{id}")
    public String editPackage(@PathVariable Long id, Model model) {
        OnlinePackage p = packages.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma pakietu " + id));
        model.addAttribute("pkg", p);
        model.addAttribute("currentTotal", zlote(p.getCurrentTotalGr()));
        model.addAttribute("currentMonthly", zlote(p.getCurrentMonthlyGr()));
        model.addAttribute("targetTotal", zlote(p.getTargetTotalGr()));
        model.addAttribute("targetMonthly", zlote(p.getTargetMonthlyGr()));
        model.addAttribute("modes", PricingMode.values());
        model.addAttribute("title", "Pakiet: " + p.getName());
        return "admin/offer-package";
    }

    @PostMapping("/pakiety/{id}")
    public String savePackage(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam String durationLabel,
                              @RequestParam String currentTotal,
                              @RequestParam String currentMonthly,
                              @RequestParam String targetTotal,
                              @RequestParam String targetMonthly,
                              @RequestParam PricingMode pricingMode,
                              @RequestParam int seatsTaken,
                              @RequestParam int seatsTotal,
                              @RequestParam(required = false) String badgeText,
                              @RequestParam(defaultValue = "false") boolean badgeVisible,
                              @RequestParam(defaultValue = "false") boolean badgePromotional,
                              @RequestParam(defaultValue = "false") boolean highlighted,
                              @RequestParam(defaultValue = "0") int sortOrder,
                              @RequestParam(defaultValue = "false") boolean visible,
                              RedirectAttributes flash) {

        OnlinePackage p = packages.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma pakietu " + id));

        Integer ct = grosze(currentTotal), cm = grosze(currentMonthly);
        Integer tt = grosze(targetTotal), tm = grosze(targetMonthly);
        if (ct == null || cm == null || tt == null || tm == null) {
            flash.addFlashAttribute("error", "Wszystkie cztery kwoty muszą być liczbami, np. 597 albo 1074.");
            return "redirect:/admin/oferta/pakiety/" + id;
        }
        if (name.isBlank() || durationLabel.isBlank()) {
            flash.addFlashAttribute("error", "Nazwa i czas trwania nie mogą być puste.");
            return "redirect:/admin/oferta/pakiety/" + id;
        }
        if (seatsTotal < 0 || seatsTaken < 0 || seatsTaken > seatsTotal) {
            flash.addFlashAttribute("error", "Liczba zajętych miejsc musi mieścić się w liczbie miejsc razem.");
            return "redirect:/admin/oferta/pakiety/" + id;
        }

        p.setName(name.trim());
        p.setDurationLabel(durationLabel.trim());
        p.setCurrentTotalGr(ct);
        p.setCurrentMonthlyGr(cm);
        p.setTargetTotalGr(tt);
        p.setTargetMonthlyGr(tm);
        p.setPricingMode(pricingMode);
        p.setSeatsTaken(seatsTaken);
        p.setSeatsTotal(seatsTotal);
        p.setBadgeText(badgeText == null || badgeText.isBlank() ? null : badgeText.trim());
        p.setBadgeVisible(badgeVisible);
        p.setBadgePromotional(badgePromotional);
        p.setHighlighted(highlighted);
        p.setSortOrder(sortOrder);
        p.setVisible(visible);
        packages.save(p);

        log.info("Zaktualizowano pakiet online: {} ({})", p.getName(), p.effectiveMode());
        flash.addFlashAttribute("info", "Zapisano pakiet " + p.getName() + ".");
        return "redirect:/admin/oferta";
    }

    @PostMapping("/pakiety/{id}/usun")
    public String deletePackage(@PathVariable Long id, RedirectAttributes flash) {
        OnlinePackage p = packages.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma pakietu " + id));
        packages.delete(p);
        flash.addFlashAttribute("info", "Usunięto pakiet " + p.getName() + ".");
        return "redirect:/admin/oferta";
    }

    // ── Opinie ───────────────────────────────────────────────────────────────

    @PostMapping("/opinie")
    public String addTestimonial(@RequestParam String name, @RequestParam String body,
                                 RedirectAttributes flash) {
        if (name.isBlank() || body.isBlank()) {
            flash.addFlashAttribute("error", "Opinia potrzebuje imienia i treści.");
            return "redirect:/admin/oferta";
        }
        Testimonial t = new Testimonial();
        t.setName(name.trim());
        t.setBody(body.trim());
        t.setSortOrder(testimonials.findAll().size() + 1);
        Testimonial saved = testimonials.save(t);
        flash.addFlashAttribute("info", "Dodano opinię. Uzupełnij podpis, jeśli klient go zatwierdził.");
        return "redirect:/admin/oferta/opinie/" + saved.getId();
    }

    @GetMapping("/opinie/{id}")
    public String editTestimonial(@PathVariable Long id, Model model) {
        Testimonial t = testimonials.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma opinii " + id));
        model.addAttribute("op", t);
        model.addAttribute("title", "Opinia: " + t.getName());
        return "admin/offer-testimonial";
    }

    @PostMapping("/opinie/{id}")
    public String saveTestimonial(@PathVariable Long id,
                                  @RequestParam String name,
                                  @RequestParam(required = false) String city,
                                  @RequestParam(required = false) String cooperationFormat,
                                  @RequestParam(required = false) String durationLabel,
                                  @RequestParam String body,
                                  @RequestParam(defaultValue = "0") int sortOrder,
                                  @RequestParam(defaultValue = "false") boolean visible,
                                  RedirectAttributes flash) {

        Testimonial t = testimonials.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma opinii " + id));
        if (name.isBlank() || body.isBlank()) {
            flash.addFlashAttribute("error", "Opinia potrzebuje imienia i treści.");
            return "redirect:/admin/oferta/opinie/" + id;
        }
        t.setName(name.trim());
        t.setCity(blankToNull(city));
        t.setCooperationFormat(blankToNull(cooperationFormat));
        t.setDurationLabel(blankToNull(durationLabel));
        t.setBody(body.trim());
        t.setSortOrder(sortOrder);
        t.setVisible(visible);
        testimonials.save(t);
        flash.addFlashAttribute("info", "Zapisano opinię " + t.getName() + ".");
        return "redirect:/admin/oferta";
    }

    @PostMapping("/opinie/{id}/usun")
    public String deleteTestimonial(@PathVariable Long id, RedirectAttributes flash) {
        Testimonial t = testimonials.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma opinii " + id));
        testimonials.delete(t);
        flash.addFlashAttribute("info", "Usunięto opinię " + t.getName() + ".");
        return "redirect:/admin/oferta";
    }

    // ── FAQ ──────────────────────────────────────────────────────────────────

    @PostMapping("/faq")
    public String addQuestion(@RequestParam String question, RedirectAttributes flash) {
        if (question.isBlank()) {
            flash.addFlashAttribute("error", "Wpisz treść pytania.");
            return "redirect:/admin/oferta";
        }
        OnlineFaq q = new OnlineFaq();
        q.setQuestion(question.trim());
        q.setSortOrder(faq.findAll().size() + 1);
        faq.save(q);
        flash.addFlashAttribute("info", "Dodano pytanie. Pokaże się na stronie po wpisaniu odpowiedzi.");
        return "redirect:/admin/oferta";
    }

    /**
     * Zapis pytania odbywa sie wprost z listy — pytan jest kilkanascie i osobny
     * ekran na kazde byloby chodzeniem tam i z powrotem.
     */
    @PostMapping("/faq/{id}")
    public String saveQuestion(@PathVariable Long id,
                               @RequestParam String question,
                               @RequestParam(required = false) String answer,
                               @RequestParam(defaultValue = "0") int sortOrder,
                               @RequestParam(defaultValue = "false") boolean visible,
                               RedirectAttributes flash) {
        OnlineFaq q = faq.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma pytania " + id));
        if (question.isBlank()) {
            flash.addFlashAttribute("error", "Pytanie nie może być puste.");
            return "redirect:/admin/oferta";
        }
        q.setQuestion(question.trim());
        q.setAnswer(blankToNull(answer));
        q.setSortOrder(sortOrder);
        q.setVisible(visible);
        faq.save(q);
        flash.addFlashAttribute("info", q.answered()
                ? "Zapisano pytanie. Jest już widoczne na stronie."
                : "Zapisano pytanie. Bez odpowiedzi nie pokaże się na stronie.");
        return "redirect:/admin/oferta";
    }

    @PostMapping("/faq/{id}/usun")
    public String deleteQuestion(@PathVariable Long id, RedirectAttributes flash) {
        OnlineFaq q = faq.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma pytania " + id));
        faq.delete(q);
        flash.addFlashAttribute("info", "Usunięto pytanie.");
        return "redirect:/admin/oferta";
    }

    // ── Kwoty ────────────────────────────────────────────────────────────────

    /**
     * „597", „597,50", „1 074", „1074 zl" → grosze. Null, gdy nie da sie odczytac
     * liczby — wtedy kontroler pokazuje blad zamiast zapisac zero.
     */
    static Integer grosze(String input) {
        if (input == null) return null;
        String cleaned = input.replace(" ", "").replace(" ", "")
                .replace("zł", "").replace("zl", "").replace(',', '.').trim();
        if (cleaned.isEmpty()) return null;
        try {
            BigDecimal value = new BigDecimal(cleaned);
            if (value.signum() < 0) return null;
            return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    /** Grosze → wartosc do pola formularza: „597" albo „597,50". */
    static String zlote(int grosze) {
        BigDecimal value = BigDecimal.valueOf(grosze).movePointLeft(2);
        return (grosze % 100 == 0 ? value.setScale(0) : value.setScale(2)).toPlainString().replace('.', ',');
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
