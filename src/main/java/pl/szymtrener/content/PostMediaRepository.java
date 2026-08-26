package pl.szymtrener.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostMediaRepository extends JpaRepository<PostMedia, PostMedia.Key> {

    @Modifying
    void deleteByPostId(Long postId);

    boolean existsByMediaId(Long mediaId);

    /** Tytuly wpisow uzywajacych pliku — komunikat „nie usune, bo jest w uzyciu". */
    @Query("""
           select p.title from PostMedia pm join Post p on p.id = pm.postId
           where pm.mediaId = :mediaId""")
    List<String> titlesUsingMedia(@Param("mediaId") Long mediaId);
}
