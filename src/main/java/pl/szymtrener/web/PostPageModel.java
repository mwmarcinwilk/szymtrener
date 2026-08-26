package pl.szymtrener.web;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.content.Post;
import pl.szymtrener.content.PostRepository;
import pl.szymtrener.content.PostService;
import pl.szymtrener.content.PostView;
import pl.szymtrener.seo.JsonLdService;

import java.time.Year;
import java.time.ZoneId;
import java.util.List;

/**
 * Model widoku artykulu. Wspolny dla strony publicznej i podgladu szkicu w panelu —
 * inaczej podglad pokazywalby cos innego niz to, co zobaczy czytelnik.
 *
 * Wpis wczytujemy TUTAJ, wewnatrz wlasnej transakcji. Aplikacja ma
 * open-in-view=false, wiec encja pobrana w kontrolerze byla juz odlaczona
 * i siegniecie po faq/summaryPoints konczylo sie LazyInitializationException.
 */
@Component
public class PostPageModel {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final PostRepository posts;
    private final PostService postService;
    private final JsonLdService jsonLd;
    private final AppProperties props;

    public PostPageModel(PostRepository posts, PostService postService,
                         JsonLdService jsonLd, AppProperties props) {
        this.posts = posts;
        this.postService = postService;
        this.jsonLd = jsonLd;
        this.props = props;
    }

    /**
     * @param preview tryb podgladu: bez JSON-LD i z noindex — szkic nie ma prawa
     *                trafic do wyszukiwarki ani udawac opublikowanego wpisu.
     */
    @Transactional(readOnly = true)
    public String fill(Long postId, Model model, boolean preview) {
        Post post = posts.findById(postId)
                .orElseThrow(() -> new NotFoundException("Nie ma wpisu " + postId));

        PostView view = postService.toFullView(post);
        String canonical = props.absolute("/blog/" + post.getSlug());

        model.addAttribute("post", view);
        model.addAttribute("related", postService.related(post));
        model.addAttribute("canonical", canonical);
        model.addAttribute("preview", preview);
        model.addAttribute("robots", preview ? "noindex, nofollow" : null);
        model.addAttribute("pageTitle",
                (post.getSeoTitle() != null && !post.getSeoTitle().isBlank() ? post.getSeoTitle() : post.getTitle())
                        + " | Szymon Domagała");
        model.addAttribute("pageDescription",
                post.getSeoDescription() != null && !post.getSeoDescription().isBlank()
                        ? post.getSeoDescription() : view.lead());
        model.addAttribute("jsonLd", preview ? List.of() : jsonLd.forPost(post, view, canonical));
        model.addAttribute("year", Year.now(ZONE).getValue());
        model.addAttribute("lastModified", view.modifiedIso());
        model.addAttribute("lastModifiedLabel", view.publishedLabel());
        return "blog/post";
    }
}
