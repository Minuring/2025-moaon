package moaon.backend.search.query;

import lombok.RequiredArgsConstructor;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.repository.ArticleSearchResult;
import moaon.backend.article.repository.SearchFacade;
import moaon.backend.project.domain.Project;
import moaon.backend.project.dto.ProjectArticleQueryCondition;
import moaon.backend.search.log.service.SearchLogCaptureAssembler;
import moaon.backend.search.log.service.SearchLogService;
import moaon.backend.search.indexing.outbox.IndexEvent;
import moaon.backend.search.indexing.outbox.IndexEvent.Action;
import moaon.backend.search.indexing.outbox.IndexEventRepository;
import moaon.backend.search.indexing.outbox.IndexRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchFacadeImpl implements SearchFacade {

    private final ArticleDocumentRepository articleDocumentRepository;
    private final IndexEventRepository indexEventRepository;
    private final SearchLogService searchLogService;
    private final SearchLogCaptureAssembler searchLogCaptureAssembler;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ArticleSearchResult search(ArticleQueryCondition condition) {
        SearchWithLog raw = articleDocumentRepository.search(condition);
        searchLogCaptureAssembler.assemble(raw.hitLogs(), raw.result().totalCount(), condition, raw.queryTimeMs())
                .ifPresent(searchLogService::saveAsync);
        return raw.result();
    }

    @Override
    public ArticleSearchResult searchInProject(Project project, ProjectArticleQueryCondition condition) {
        return articleDocumentRepository.searchInProject(project, condition.toArticleCondition());
    }

    @Override
    public void requestIndex(Long articleId) {
        indexEventRepository.merge(new IndexEvent(articleId, Action.INDEXING));
        eventPublisher.publishEvent(new IndexRequestedEvent(articleId));
    }

    @Override
    public void requestDelete(Long articleId) {
        indexEventRepository.merge(new IndexEvent(articleId, Action.DELETED));
        eventPublisher.publishEvent(new IndexRequestedEvent(articleId));
    }
}
