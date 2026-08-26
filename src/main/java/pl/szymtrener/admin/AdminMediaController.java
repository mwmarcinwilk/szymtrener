package pl.szymtrener.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.content.PostService;
import pl.szymtrener.media.MediaFile;
import pl.szymtrener.media.MediaKind;
import pl.szymtrener.media.MediaRepository;
import pl.szymtrener.media.MediaService;

import java.util.List;

@Controller
public class AdminMediaController {

    private static final Logger log = LoggerFactory.getLogger(AdminMediaController.class);
    private static final int PAGE_SIZE = 40;

    private final MediaRepository media;
    private final MediaService mediaService;
    private final PostService postService;

    public AdminMediaController(MediaRepository media, MediaService mediaService, PostService postService) {
        this.media = media;
        this.mediaService = mediaService;
        this.postService = postService;
    }

    @GetMapping("/admin/media")
    public String library(@RequestParam(required = false) MediaKind rodzaj,
                          @RequestParam(defaultValue = "0") int strona, Model model) {
        model.addAttribute("files", rodzaj == null
                ? media.findAllByOrderByCreatedAtDesc(PageRequest.of(strona, PAGE_SIZE))
                : media.findByKindOrderByCreatedAtDesc(rodzaj, PageRequest.of(strona, PAGE_SIZE)));
        model.addAttribute("kinds", MediaKind.values());
        model.addAttribute("activeKind", rodzaj);
        model.addAttribute("countAll", media.count());
        model.addAttribute("countImages", media.countByKind(MediaKind.IMAGE));
        model.addAttribute("countPdfs", media.countByKind(MediaKind.PDF));
        model.addAttribute("countOther", media.countByKind(MediaKind.OTHER));
        model.addAttribute("totalSize", humanSize(media.totalBytes()));
        model.addAttribute("baseUrl", rodzaj == null ? "/admin/media" : "/admin/media?rodzaj=" + rodzaj);
        model.addAttribute("title", "Media");
        return "admin/media";
    }

    /** Ten sam format co MediaFile.humanSize(), ale dla sumy calej biblioteki. */
    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return Math.round(bytes / 1024.0) + " KB";
        if (bytes < 1024L * 1024 * 1024) return String.format(java.util.Locale.forLanguageTag("pl-PL"), "%.1f MB", bytes / (1024.0 * 1024));
        return String.format(java.util.Locale.forLanguageTag("pl-PL"), "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Usuniecie pliku z biblioteki. Najpierw sprawdzamy, czy plik nie jest uzyty
     * w zadnym wpisie — lepiej odmowic z komunikatem niz zostawic w opublikowanym
     * artykule dziure po obrazku, ktorej autor nie zauwazy.
     */
    @PostMapping("/admin/media/{id}/usun")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        MediaFile file = media.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie ma pliku " + id));

        List<String> inUse = postService.postsUsing(id);
        if (!inUse.isEmpty()) {
            flash.addFlashAttribute("error", "Nie usunąłem „" + file.getOriginalName()
                    + "”. Plik jest użyty w: " + String.join(", ", inUse)
                    + ". Najpierw usuń go z tych wpisów.");
            return "redirect:/admin/media";
        }

        mediaService.delete(id);
        log.info("Usunieto plik {} ({})", id, file.getOriginalName());
        flash.addFlashAttribute("info", "Usunięto plik „" + file.getOriginalName() + "”.");
        return "redirect:/admin/media";
    }
}
