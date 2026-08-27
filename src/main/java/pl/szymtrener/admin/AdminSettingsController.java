package pl.szymtrener.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.settings.SettingsService;

@Controller
public class AdminSettingsController {

    private final SettingsService settings;
    private final AppProperties props;

    /**
     * Poczta i adres serwisu siedza w .env, nie w bazie — zmiana wymaga restartu,
     * wiec panel ich nie edytuje. Wstrzykujemy je tylko po to, zeby POKAZAC, z jakiej
     * konfiguracji aplikacja faktycznie korzysta. Puste pole w panelu jest gorsze niz
     * brak pola: sugeruje, ze poczta nie jest ustawiona.
     */
    private final String smtpHost;
    private final String smtpPort;
    private final JavaMailSender mailSender;

    public AdminSettingsController(SettingsService settings, AppProperties props,
                                   @Value("${spring.mail.host:—}") String smtpHost,
                                   @Value("${spring.mail.port:—}") String smtpPort,
                                   JavaMailSender mailSender) {
        this.settings = settings;
        this.props = props;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.mailSender = mailSender;
    }

    @GetMapping("/admin/ustawienia")
    public String form(Model model) {
        model.addAttribute("pageSize", settings.getInt(SettingsService.BLOG_PAGE_SIZE, 9));
        model.addAttribute("recipient", settings.get(SettingsService.MAIL_RECIPIENT, props.mail().recipient()));
        model.addAttribute("notify", settings.getBoolean(SettingsService.MAIL_NOTIFY, true));
        model.addAttribute("autoReply", settings.getBoolean(SettingsService.MAIL_AUTOREPLY, props.mail().autoReply()));
        model.addAttribute("seoTitle", settings.get(SettingsService.SEO_TITLE, ""));
        model.addAttribute("seoDesc", settings.get(SettingsService.SEO_DESC, ""));
        model.addAttribute("smtpHost", smtpHost);
        model.addAttribute("smtpPort", smtpPort);
        model.addAttribute("mailFrom", props.mail().from());
        model.addAttribute("siteUrl", props.siteUrl());
        model.addAttribute("indexNow", props.indexnow().enabled());
        model.addAttribute("title", "Ustawienia");
        return "admin/settings";
    }

    /**
     * Test wysylki. Bez niego jedyna droga sprawdzenia poczty jest wypelnienie
     * formularza na stronie i czekanie — a blad i tak lezy wtedy w logu serwera,
     * nie przed oczami. Tutaj komunikat Gmaila wraca wprost do panelu.
     */
    @PostMapping("/admin/ustawienia/test-poczty")
    public String testMail(RedirectAttributes flash) {
        String recipient = settings.get(SettingsService.MAIL_RECIPIENT, props.mail().recipient());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(props.mail().from());
            message.setTo(recipient);
            message.setSubject("Test poczty ze strony szymtrener.pl");
            message.setText("Jeśli czytasz tę wiadomość, wysyłka z panelu działa poprawnie.");
            mailSender.send(message);
            flash.addFlashAttribute("info", "Wiadomość testowa wysłana na " + recipient + ".");
        } catch (Exception e) {
            // Pelny komunikat serwera pocztowego, bo to on niesie przyczyne
            // (np. 535-5.7.8 BadCredentials przy zlym hasle aplikacji Google).
            String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            flash.addFlashAttribute("error", "Nie udało się wysłać: " + cause);
        }
        return "redirect:/admin/ustawienia";
    }

    @PostMapping("/admin/ustawienia")
    public String save(@RequestParam int pageSize,
                       @RequestParam String recipient,
                       @RequestParam(defaultValue = "false") boolean notify,
                       @RequestParam(defaultValue = "false") boolean autoReply,
                       @RequestParam(required = false) String seoTitle,
                       @RequestParam(required = false) String seoDesc,
                       RedirectAttributes flash) {

        // Zakres z sensem: 1 wpis na stronie to bezsens, 48 to strona ladujaca sie wieczność.
        settings.set(SettingsService.BLOG_PAGE_SIZE, String.valueOf(Math.clamp(pageSize, 3, 48)));
        settings.set(SettingsService.MAIL_RECIPIENT, recipient.trim());
        settings.set(SettingsService.MAIL_NOTIFY, String.valueOf(notify));
        settings.set(SettingsService.MAIL_AUTOREPLY, String.valueOf(autoReply));
        settings.set(SettingsService.SEO_TITLE, seoTitle == null ? "" : seoTitle.trim());
        settings.set(SettingsService.SEO_DESC, seoDesc == null ? "" : seoDesc.trim());

        flash.addFlashAttribute("info", "Ustawienia zapisane.");
        return "redirect:/admin/ustawienia";
    }
}
