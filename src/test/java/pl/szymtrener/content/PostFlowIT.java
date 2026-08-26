package pl.szymtrener.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import pl.szymtrener.PostgresTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Przeplywy, ktore najlatwiej zepsuc przy kolejnej zmianie: adres wpisu po zmianie
 * tytulu, wyszukiwarka, powiazania z plikami i archiwizacja.
 */
@AutoConfigureMockMvc
class PostFlowIT extends PostgresTestBase {

    @Autowired MockMvc mvc;
    @Autowired PostService postService;
    @Autowired PostRepository posts;
    @Autowired PostSlugHistoryRepository slugHistory;
    @Autowired PostMediaRepository postMedia;
    @Autowired CategoryRepository categories;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("delete from post_slug_history");
        jdbc.execute("delete from post_media");
        jdbc.execute("delete from post_summary_point");
        jdbc.execute("delete from post_faq");
        jdbc.execute("delete from post_tag");
        jdbc.execute("delete from post");
        jdbc.execute("delete from media_file");
    }

    private Post published(String title, String html) {
        return published(title, html, "trening");
    }

    private Post published(String title, String html, String categorySlug) {
        Post post = new Post();
        post.setTitle(title);
        post.setLead("Lead wpisu o treningu siłowym i długowieczności.");
        post.setContentHtml(html);
        post.setStatus(PostStatus.PUBLISHED);
        post.setCategory(categories.findBySlug(categorySlug).orElseThrow());
        return postService.save(post, null);
    }

    // ─── adres wpisu ────────────────────────────────────────────────

    @Test
    @DisplayName("zmiana tytułu opublikowanego wpisu zostawia stary adres w historii i odsyła 301")
    void oldSlugRedirectsPermanently() throws Exception {
        Post post = published("Mięśnie to polisa na życie", "<p>Treść.</p>");
        String oldSlug = post.getSlug();
        assertThat(oldSlug).isEqualTo("miesnie-to-polisa-na-zycie");

        post.setTitle("Dlaczego mięśnie to polisa na życie");
        Post updated = postService.save(post, null);

        assertThat(updated.getSlug()).isNotEqualTo(oldSlug);
        assertThat(slugHistory.findBySlug(oldSlug)).isPresent();

        mvc.perform(get("/blog/" + oldSlug))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("/blog/" + updated.getSlug()));

        mvc.perform(get("/blog/" + updated.getSlug())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("zmiana tytułu szkicu nie zaśmieca historii — nikt tego adresu nie widział")
    void draftSlugChangeLeavesNoHistory() {
        Post draft = new Post();
        draft.setTitle("Szkic pierwszy");
        draft.setContentHtml("<p>Treść.</p>");
        draft.setStatus(PostStatus.DRAFT);
        Post saved = postService.save(draft, null);

        saved.setTitle("Szkic drugi");
        postService.save(saved, null);

        assertThat(slugHistory.count()).isZero();
    }

    @Test
    @DisplayName("powrót do poprzedniego tytułu nie tworzy przekierowania w kółko")
    void revertingTitleDoesNotCreateSelfRedirect() throws Exception {
        Post post = published("Białko po czterdziestce", "<p>Treść.</p>");
        String original = post.getSlug();

        post.setTitle("Białko po pięćdziesiątce");
        postService.save(post, null);

        Post reverted = posts.findById(post.getId()).orElseThrow();
        reverted.setTitle("Białko po czterdziestce");
        Post back = postService.save(reverted, null);

        assertThat(back.getSlug()).isEqualTo(original);
        assertThat(slugHistory.findBySlug(original)).isEmpty();
        mvc.perform(get("/blog/" + original)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("nieznany adres, którego nie ma w historii, dalej daje 404")
    void unknownSlugStillReturns404() throws Exception {
        mvc.perform(get("/blog/takiego-wpisu-nigdy-nie-bylo"))
                .andExpect(status().isNotFound());
    }

    // ─── wyszukiwarka ───────────────────────────────────────────────

    @Test
    @DisplayName("wyszukiwarka znajduje wpis po słowie z tytułu i pomija niepasujące")
    void searchFindsByTitle() {
        published("Sarkopenia po pięćdziesiątce", "<p>Treść.</p>");
        published("Regeneracja i sen", "<p>Treść.</p>");

        List<String> found = postService.search("sarkopenia", 0, 10)
                .getContent().stream().map(PostView::title).toList();

        assertThat(found).containsExactly("Sarkopenia po pięćdziesiątce");
    }

    @Test
    @DisplayName("pusta fraza nie zwraca wszystkiego — zwraca nic")
    void emptyQueryReturnsNothing() {
        published("Sarkopenia po pięćdziesiątce", "<p>Treść.</p>");

        assertThat(postService.search("   ", 0, 10).getTotalElements()).isZero();
        assertThat(postService.search(null, 0, 10).getTotalElements()).isZero();
    }

    @Test
    @DisplayName("szkic nie pojawia się w wynikach wyszukiwania")
    void searchSkipsDrafts() {
        Post draft = new Post();
        draft.setTitle("Sarkopenia w szkicu");
        draft.setContentHtml("<p>Treść.</p>");
        draft.setStatus(PostStatus.DRAFT);
        postService.save(draft, null);

        assertThat(postService.search("sarkopenia", 0, 10).getTotalElements()).isZero();
    }

    // ─── powiazane wpisy ────────────────────────────────────────────

    @Test
    @DisplayName("powiązane uzupełniają się spoza kategorii zamiast podmieniać trafienia z kategorii")
    void relatedFillsInsteadOfReplacing() {
        Post fromCategory = published("Trening siłowy podstawy", "<p>Treść.</p>");
        published("Sen i regeneracja", "<p>Treść.</p>", "zdrowie");
        published("Białko w diecie", "<p>Treść.</p>", "odzywianie");

        Post current = published("Progresja obciążenia", "<p>Treść.</p>");

        List<String> related = postService.related(current).stream().map(PostView::title).toList();

        assertThat(related).hasSize(3);
        assertThat(related).contains(fromCategory.getTitle());   // trafienie z tej samej kategorii zostaje
        assertThat(related).doesNotContain(current.getTitle());   // nigdy sam siebie
        assertThat(related).doesNotHaveDuplicates();
    }

    // ─── powiazania z plikami ───────────────────────────────────────

    @Test
    @DisplayName("zapis wpisu odtwarza powiązania z plikami użytymi w treści i jako okładka")
    void savingPostRecordsMediaLinks() {
        Long mediaId = insertMedia("2026/08/zdjecie.jpg");

        Post post = new Post();
        post.setTitle("Wpis ze zdjęciem");
        post.setLead("Lead.");
        post.setContentHtml("<p><img src=\"/media/2026/08/zdjecie.jpg\" alt=\"Zdjęcie w treści\"></p>");
        post.setCoverMediaId(mediaId);
        post.setStatus(PostStatus.PUBLISHED);
        Post saved = postService.save(post, null);

        assertThat(postMedia.existsByMediaId(mediaId)).isTrue();
        assertThat(postService.postsUsing(mediaId)).containsExactly(saved.getTitle());
    }

    @Test
    @DisplayName("usunięcie zdjęcia z treści usuwa też powiązanie — plik znów wolno skasować")
    void removingImageDropsTheLink() {
        Long mediaId = insertMedia("2026/08/inne.jpg");

        Post post = new Post();
        post.setTitle("Wpis ze zdjęciem w treści");
        post.setLead("Lead.");
        post.setContentHtml("<p><img src=\"/media/2026/08/inne.jpg\" alt=\"Inne zdjęcie\"></p>");
        post.setStatus(PostStatus.PUBLISHED);
        Post saved = postService.save(post, null);
        assertThat(postService.postsUsing(mediaId)).isNotEmpty();

        saved.setContentHtml("<p>Już bez zdjęcia.</p>");
        postService.save(saved, null);

        assertThat(postService.postsUsing(mediaId)).isEmpty();
    }

    // ─── usuwanie wpisu ─────────────────────────────────────────────

    @Test
    @DisplayName("opublikowany wpis trafia do archiwum, a nie do kosza — adres nie może zniknąć")
    void publishedPostIsArchivedNotDeleted() {
        Post post = published("Wpis do archiwum", "<p>Treść.</p>");

        boolean removed = postService.deleteOrArchive(post);

        assertThat(removed).isFalse();
        assertThat(posts.findById(post.getId())).isPresent();
        assertThat(posts.findById(post.getId()).orElseThrow().getStatus()).isEqualTo(PostStatus.ARCHIVED);
    }

    @Test
    @DisplayName("szkic jest kasowany naprawdę")
    void draftIsDeleted() {
        Post draft = new Post();
        draft.setTitle("Szkic do skasowania");
        draft.setContentHtml("<p>Treść.</p>");
        draft.setStatus(PostStatus.DRAFT);
        Post saved = postService.save(draft, null);

        boolean removed = postService.deleteOrArchive(saved);

        assertThat(removed).isTrue();
        assertThat(posts.findById(saved.getId())).isEmpty();
    }

    // ─── unikalnosc adresow ─────────────────────────────────────────

    @Test
    @DisplayName("dwa wpisy o tym samym tytule dostają różne adresy")
    void duplicateTitlesGetDistinctSlugs() {
        Post first = published("Ten sam tytuł", "<p>Treść.</p>");
        Post second = published("Ten sam tytuł", "<p>Treść.</p>");

        assertThat(first.getSlug()).isEqualTo("ten-sam-tytul");
        assertThat(second.getSlug()).isEqualTo("ten-sam-tytul-2");
    }

    private Long insertMedia(String storageKey) {
        jdbc.update("""
                insert into media_file (storage_key, original_name, mime_type, kind, size_bytes)
                values (?, ?, 'image/jpeg', 'IMAGE', 1024)""",
                storageKey, storageKey.substring(storageKey.lastIndexOf('/') + 1));
        return jdbc.queryForObject("select id from media_file where storage_key = ?", Long.class, storageKey);
    }
}
