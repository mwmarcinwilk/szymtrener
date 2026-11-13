package pl.szymtrener.content;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.szymtrener.PostgresTestBase;
import pl.szymtrener.media.MediaRepository;
import pl.szymtrener.media.ThumbnailFetcher;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Przepisanie starych wpisow z miniaturami z i.ytimg.com na prawdziwej bazie: lokalny adres, powiazanie
 * w bibliotece (pliku nie da sie usunac spod wpisu), data zmiany wpisu bez zmian, strona bez Google.
 */
@AutoConfigureMockMvc
class YoutubeThumbnailBackfillIT extends PostgresTestBase {

    private static final String ID = "LcXpFy7s-GQ";
    private static final String LEGACY = "<p>Film o treningu.</p><div class=\"art-video yt-facade\" data-id=\"" + ID + "\">"
            + "<img src=\"https://i.ytimg.com/vi/" + ID + "/hqdefault.jpg\" alt=\"\" loading=\"lazy\"></div>";

    // Atrapa zamiast sieci: kontekst z @MockitoBean jest osobny, wiec nie psuje innych IT.
    @MockitoBean ThumbnailFetcher fetcher;
    @Autowired YoutubeThumbnailBackfill backfill;
    @Autowired PostService postService;
    @Autowired PostRepository posts;
    @Autowired CategoryRepository categories;
    @Autowired MediaRepository library;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test
    @DisplayName("stary wpis: miniatura z naszego serwera, powiązana z wpisem, bez zmiany daty i bez Google na stronie")
    void rewritesLegacyPost() throws Exception {
        Post post = new Post();
        post.setTitle("Film o treningu po czterdziestce");
        post.setLead("Lead wpisu o treningu siłowym i długowieczności.");
        post.setContentHtml("<p>Film o treningu.</p>");
        post.setStatus(PostStatus.PUBLISHED);
        post.setCategory(categories.findAll().get(0));
        Post saved = postService.save(post, null);
        // Stan sprzed 11.2026: tresc zapisana z adresem Google, z pominieciem dzisiejszej lokalizacji.
        jdbc.update("update post set content_html = ? where id = ?", LEGACY, saved.getId());
        Timestamp updatedBefore = jdbc.queryForObject("select updated_at from post where id = ?", Timestamp.class, saved.getId());
        when(fetcher.fetch(ID)).thenReturn(Optional.of(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0}));

        assertThat(backfill.localizeLegacyPosts()).isEqualTo(1);

        String html = jdbc.queryForObject("select content_html from post where id = ?", String.class, saved.getId());
        assertThat(html).contains("/media/").doesNotContain("ytimg");
        assertThat(jdbc.queryForObject("select updated_at from post where id = ?", Timestamp.class, saved.getId()))
                .isEqualTo(updatedBefore);
        Long mediaId = library.findFirstByOriginalName("youtube-" + ID + ".jpg").orElseThrow().getId();
        assertThat(postService.postsUsing(mediaId)).isNotEmpty();

        String page = mvc.perform(get("/blog/" + saved.getSlug())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(page).contains("</html>").doesNotContain("ytimg");
        assertThat(backfill.localizeLegacyPosts()).as("drugi przebieg nic nie zmienia").isZero();
    }
}
