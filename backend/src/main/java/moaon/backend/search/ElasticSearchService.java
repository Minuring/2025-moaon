package moaon.backend.search;

import lombok.RequiredArgsConstructor;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.repository.ArticleSearchResult;
import moaon.backend.project.domain.Project;
import moaon.backend.project.dto.ProjectArticleQueryCondition;
import moaon.backend.search.indexing.outbox.IndexEvent;
import moaon.backend.search.indexing.outbox.IndexEvent.Action;
import moaon.backend.search.indexing.outbox.IndexEventRepository;
import moaon.backend.search.indexing.outbox.IndexRequestedEvent;
import moaon.backend.search.query.ElasticQueryClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ElasticSearchService {

    private final ElasticQueryClient elasticQueryClient;
    private final IndexEventRepository indexEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ArticleSearchResult search(ArticleQueryCondition condition) {
        return elasticQueryClient.search(condition);
    }

    public ArticleSearchResult searchInProject(Project project, ProjectArticleQueryCondition condition) {
        return elasticQueryClient.searchInProject(project, condition.toArticleCondition());
    }

    public void requestIndex(Long articleId) {
        indexEventRepository.merge(new IndexEvent(articleId, Action.INDEXING));
        eventPublisher.publishEvent(new IndexRequestedEvent(articleId));
    }

    public void requestDelete(Long articleId) {
        indexEventRepository.merge(new IndexEvent(articleId, Action.DELETED));
        eventPublisher.publishEvent(new IndexRequestedEvent(articleId));
    }
}
