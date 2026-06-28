package moaon.backend.article.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.Sector;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface ArticleDBRepository extends Repository<Article, Long> {

    Article save(Article a);

    Optional<Article> findById(Long id);

    @Query("select a from Article a")
    Stream<Article> streamAll();

    long count();

    @Modifying
    @Query("update Article a set a.clicks = a.clicks + 1 where a.id = :id")
    int increaseClickCount(Long id);
}
