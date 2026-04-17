package moaon.backend.article.repository;

import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.project.domain.Project;
import moaon.backend.project.dto.ProjectArticleQueryCondition;

public interface SearchFacade {
    ArticleSearchResult search(ArticleQueryCondition condition);
    ArticleSearchResult searchInProject(Project project, ProjectArticleQueryCondition condition);
    void requestIndex(Long articleId);
    void requestDelete(Long articleId);
}
