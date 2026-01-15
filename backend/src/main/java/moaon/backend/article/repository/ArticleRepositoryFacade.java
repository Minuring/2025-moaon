package moaon.backend.article.repository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

    @CircuitBreaker(name = "articleSearchCB", fallbackMethod = "searchWithDB")
    public ArticleSearchResult search(ArticleQueryCondition condition) {
        return elasticSearch.search(condition);
    }

    public ArticleSearchResult searchWithDB(ArticleQueryCondition condition, Exception e) {
        log.error("검색엔진이 실패하였습니다. 데이터베이스로 검색을 시도합니다 : {}", e.getMessage());
        return database.findWithSearchConditions(condition);
    }

    public ArticleSearchResult searchInProject(Project project, ProjectArticleQueryCondition condition) {
        return elasticSearch.searchInProject(project, condition.toArticleCondition());
    }

    public Optional<Article> findById(Long id) {
        return database.findById(id);
    }

    public void updateClicksCount(Article article) {
        database.increaseClickCount(article.getId());
        indexEventRepository.merge(new IndexEvent(article, Action.INDEXING));
    }

    public Article save(Article article) {
        Article saved = database.save(article);
        indexEventRepository.merge(new IndexEvent(saved, Action.INDEXING));
        return saved;
    }
}
