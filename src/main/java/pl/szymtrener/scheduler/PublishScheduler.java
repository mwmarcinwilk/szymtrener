package pl.szymtrener.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.szymtrener.content.Post;
import pl.szymtrener.content.PostRepository;
import pl.szymtrener.content.PostStatus;
import pl.szymtrener.seo.IndexNowService;

import java.time.Instant;
import java.util.List;

/** Co minute: wpisy zaplanowane na juz przechodza na opublikowane + ping IndexNow. */
@Component
public class PublishScheduler {

    private static final Logger log = LoggerFactory.getLogger(PublishScheduler.class);

    private final PostRepository posts;
    private final IndexNowService indexNow;

    public PublishScheduler(PostRepository posts, IndexNowService indexNow) {
        this.posts = posts;
        this.indexNow = indexNow;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void publishDue() {
        List<Post> due = posts.findByStatusAndPublishAtLessThanEqual(PostStatus.SCHEDULED, Instant.now());
        if (due.isEmpty()) return;

        for (Post post : due) {
            post.setStatus(PostStatus.PUBLISHED);
            post.setPublishedAt(post.getPublishAt() != null ? post.getPublishAt() : Instant.now());
        }
        posts.saveAll(due);
        log.info("Opublikowano zaplanowanych wpisow: {}", due.size());

        indexNow.submit(due.stream().map(p -> "/blog/" + p.getSlug()).toList());
    }
}
