package pl.szymtrener.content;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Powiazanie wpis–plik. Bez tego nie wiadomo, ktory plik wolno usunac
 * z biblioteki, a ktory jest w uzyciu i skasowanie go zepsuje wpis.
 */
@Entity
@Table(name = "post_media")
@IdClass(PostMedia.Key.class)
public class PostMedia {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Id
    @Column(name = "media_id")
    private Long mediaId;

    @Id
    @Column(name = "role")
    private String role;

    protected PostMedia() {}

    public PostMedia(Long postId, Long mediaId, String role) {
        this.postId = postId;
        this.mediaId = mediaId;
        this.role = role;
    }

    public Long getPostId() { return postId; }
    public Long getMediaId() { return mediaId; }
    public String getRole() { return role; }

    public static class Key implements Serializable {
        private Long postId;
        private Long mediaId;
        private String role;

        public Key() {}

        public Key(Long postId, Long mediaId, String role) {
            this.postId = postId;
            this.mediaId = mediaId;
            this.role = role;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(postId, key.postId)
                    && Objects.equals(mediaId, key.mediaId)
                    && Objects.equals(role, key.role);
        }

        @Override public int hashCode() { return Objects.hash(postId, mediaId, role); }
    }
}
