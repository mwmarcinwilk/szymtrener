package pl.szymtrener.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.config.AppProperties;

import java.util.Locale;

/**
 * Zarzadzanie kontami administratorow z poziomu panelu.
 *
 * Dzieki temu konto zalozone przy pierwszym wdrozeniu przestaje byc czymkolwiek
 * wyjatkowym: mozna dodac wlasne i skasowac tamto, bez grzebania w bazie.
 *
 * Dwie zasady, ktorych nie da sie ominac:
 *  - nie da sie usunac OSTATNIEGO konta (nikt nie wszedlby do panelu),
 *  - nie da sie usunac konta, na ktorym sie wlasnie pracuje (wylogowanie w polowie
 *    operacji jest myllace, a przy okazji chroni przed przypadkowym samobojstwem).
 */
@Controller
public class AdminUsersController {

    private static final Logger log = LoggerFactory.getLogger(AdminUsersController.class);

    /** Ten sam prog co przy zmianie hasla w panelu. */
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final AdminUserRepository accounts;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public AdminUsersController(AdminUserRepository accounts, PasswordEncoder encoder, AppProperties props) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.props = props;
    }

    @GetMapping("/admin/administratorzy")
    public String list(Model model, Authentication auth) {
        fill(model, auth);
        model.addAttribute("title", "Administratorzy");
        return "admin/admins";
    }

    @PostMapping("/admin/administratorzy")
    public String add(@RequestParam String email,
                      @RequestParam String password,
                      @RequestParam(required = false) String displayName,
                      RedirectAttributes flash) {

        String address = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (address.isBlank() || !address.contains("@")) {
            flash.addFlashAttribute("error", "Podaj poprawny adres e-mail.");
            return "redirect:/admin/administratorzy";
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            flash.addFlashAttribute("error", "Hasło musi mieć co najmniej " + MIN_PASSWORD_LENGTH + " znaków.");
            return "redirect:/admin/administratorzy";
        }
        if (accounts.findByEmailIgnoreCase(address).isPresent()) {
            flash.addFlashAttribute("error", "Konto o adresie " + address + " już istnieje.");
            return "redirect:/admin/administratorzy";
        }

        AdminUser account = new AdminUser();
        account.setEmail(address);
        account.setPasswordHash(encoder.encode(password));
        account.setDisplayName(displayName == null || displayName.isBlank() ? address : displayName.trim());
        account.setEnabled(true);
        accounts.save(account);

        log.info("Dodano konto administratora: {}", address);
        flash.addFlashAttribute("info", "Dodano konto " + address + ".");
        return "redirect:/admin/administratorzy";
    }

    @GetMapping("/admin/administratorzy/{id}")
    public String edit(@PathVariable Long id, Model model, Authentication auth) {
        AdminUser account = accounts.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma konta " + id));
        model.addAttribute("account", account);
        model.addAttribute("self", auth != null && account.getEmail().equalsIgnoreCase(auth.getName()));
        model.addAttribute("minLength", MIN_PASSWORD_LENGTH);
        model.addAttribute("title", "Konto: " + account.getEmail());
        return "admin/admin-edit";
    }

    /**
     * Zapis zmian. Haslo jest opcjonalne — puste pole zostawia dotychczasowe,
     * bo edycja nazwy nie moze wymuszac ustawiania hasla od nowa.
     */
    @PostMapping("/admin/administratorzy/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String email,
                         @RequestParam(required = false) String displayName,
                         @RequestParam(required = false) String password,
                         @RequestParam(defaultValue = "false") boolean enabled,
                         Authentication auth,
                         RedirectAttributes flash) {

        AdminUser account = accounts.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma konta " + id));
        boolean self = auth != null && account.getEmail().equalsIgnoreCase(auth.getName());

        String address = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (address.isBlank() || !address.contains("@")) {
            flash.addFlashAttribute("error", "Podaj poprawny adres e-mail.");
            return "redirect:/admin/administratorzy/" + id;
        }
        if (accounts.findByEmailIgnoreCase(address).filter(other -> !other.getId().equals(id)).isPresent()) {
            flash.addFlashAttribute("error", "Inne konto ma juz adres " + address + ".");
            return "redirect:/admin/administratorzy/" + id;
        }
        if (password != null && !password.isBlank() && password.length() < MIN_PASSWORD_LENGTH) {
            flash.addFlashAttribute("error", "Hasło musi mieć co najmniej " + MIN_PASSWORD_LENGTH + " znaków.");
            return "redirect:/admin/administratorzy/" + id;
        }
        // Wylaczenie wlasnego konta odcieloby od panelu w trakcie pracy.
        if (!enabled && self) {
            flash.addFlashAttribute("error", "Nie wyłączysz konta, na którym jesteś zalogowany.");
            return "redirect:/admin/administratorzy/" + id;
        }
        if (!enabled && account.isEnabled() && accounts.findAll().stream().filter(AdminUser::isEnabled).count() <= 1) {
            flash.addFlashAttribute("error", "To jedyne aktywne konto — najpierw włącz albo dodaj inne.");
            return "redirect:/admin/administratorzy/" + id;
        }

        boolean emailChanged = !account.getEmail().equalsIgnoreCase(address);
        account.setEmail(address);
        account.setDisplayName(displayName == null || displayName.isBlank() ? address : displayName.trim());
        account.setEnabled(enabled);
        if (password != null && !password.isBlank()) {
            account.setPasswordHash(encoder.encode(password));
        }
        accounts.save(account);
        log.info("Zaktualizowano konto administratora: {}", address);

        if (self && emailChanged) {
            // Adres jest loginem — dotychczasowa sesja wskazuje na nieistniejacy juz login.
            flash.addFlashAttribute("info", "Zapisano. Adres logowania się zmienił — zaloguj się ponownie.");
            return "redirect:/admin/wyloguj";
        }
        flash.addFlashAttribute("info", "Zapisano zmiany w koncie " + address + ".");
        return "redirect:/admin/administratorzy";
    }

    @PostMapping("/admin/administratorzy/{id}/usun")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        AdminUser account = accounts.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma konta " + id));

        if (accounts.count() <= 1) {
            flash.addFlashAttribute("error", "To jedyne konto administratora — najpierw dodaj inne.");
            return "redirect:/admin/administratorzy";
        }
        if (auth != null && account.getEmail().equalsIgnoreCase(auth.getName())) {
            flash.addFlashAttribute("error",
                    "Nie usuniesz konta, na którym jesteś zalogowany. Zaloguj się na inne i spróbuj ponownie.");
            return "redirect:/admin/administratorzy";
        }

        accounts.delete(account);
        log.info("Usunieto konto administratora: {}", account.getEmail());
        flash.addFlashAttribute("info", "Usunięto konto " + account.getEmail() + ".");
        return "redirect:/admin/administratorzy";
    }

    private void fill(Model model, Authentication auth) {
        model.addAttribute("accounts", accounts.findAll());
        model.addAttribute("currentUser", auth == null ? "" : auth.getName());
        model.addAttribute("minLength", MIN_PASSWORD_LENGTH);

        // Konto ze zmiennych srodowiskowych loguje niezaleznie od bazy, wiec musi byc
        // widoczne na liscie — inaczej „usunalem to konto, a dalej dziala" wygladaloby
        // jak awaria. Usuwa sie je wylacznie kasujac zmienna w Coolify.
        String envEmail = props.admin().email() == null ? "" : props.admin().email().trim();
        String envPassword = props.admin().password();
        boolean envActive = !envEmail.isBlank() && envPassword != null && !envPassword.isBlank();
        model.addAttribute("envActive", envActive);
        model.addAttribute("envEmail", envEmail);
    }
}
