package moaon.backend.article.repository;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.domain.Article;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.repository.db.ArticleDBRepository;
import moaon.backend.article.repository.es.ArticleDocumentRepository;
import moaon.backend.article.repository.es.event.IndexEvent;
import moaon.backend.article.repository.es.event.IndexEvent.Action;
import moaon.backend.article.repository.es.event.IndexEventRepository;
import moaon.backend.project.domain.Project;
import moaon.backend.project.dto.ProjectArticleQueryCondition;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArticleRepositoryFacade {

    private final ArticleDBRepository database;
    private final ArticleDocumentRepository elasticSearch;
    private final IndexEventRepository indexEventRepository;

    public ArticleSearchResult search(ArticleQueryCondition condition) {
        return elasticSearch.search(condition);
    }

    public ArticleSearchResult searchInProject(Project project, ProjectArticleQueryCondition condition) {
        return elasticSearch.searchInProject(project, condition.toArticleCondition());
    }

    public Optional<Article> findById(Long id) {
        return database.findById(id);
    }

    public boolean updateClicksCount(Long id) {
        int modified = database.increaseClickCount(id);
        indexEventRepository.merge(new IndexEvent(id, Action.INDEXING));
        return modified != 0;
    }

    public Article save(Article article) {
        Article saved = database.save(article);
        indexEventRepository.merge(new IndexEvent(saved.getId(), Action.INDEXING));
        return saved;
    }
}
