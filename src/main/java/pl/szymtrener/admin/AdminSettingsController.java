package pl.szymtrener.admin;

import org.springframework.beans.factory.annotation.Value;
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

    public AdminSettingsController(SettingsService settings, AppProperties props,
                                   @Value("${spring.mail.host:—}") String smtpHost,
                                   @Value("${spring.mail.port:—}") String smtpPort) {
        this.settings = settings;
        this.props = props;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
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
