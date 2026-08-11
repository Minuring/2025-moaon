package moaon.backend.article.repository;

import jakarta.annotation.Nullable;
import moaon.backend.article.dto.ArticleQueryCondition;

public interface CustomizedArticleRepository {

    ArticleSearchResult search(ArticleQueryCondition condition, @Nullable Long projectId);
}
