package moaon.backend.article.repository;

import moaon.backend.article.domain.ArticleContent;
import org.springframework.data.repository.Repository;

public interface ArticleContentRepository extends Repository<ArticleContent, Long> {

    ArticleContent save(ArticleContent articleContent);

    ArticleContent findById(Long articleId);
}
