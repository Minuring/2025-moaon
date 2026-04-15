package moaon.backend.search.api;

import lombok.RequiredArgsConstructor;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.project.domain.Project;
import moaon.backend.project.dto.ProjectArticleQueryCondition;
import moaon.backend.search.admin.service.SearchLogService;
import moaon.backend.search.indexing.outbox.IndexEventWorker;
import moaon.backend.search.indexing.outbox.IndexEvent;
import moaon.backend.search.indexing.outbox.IndexEvent.Action;
import moaon.backend.search.indexing.outbox.IndexEventRepository;
import moaon.backend.search.query.ArticleDocumentRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchFacadeImpl implements SearchFacade {

    private final ArticleDocumentRepository articleDocumentRepository;
    private final IndexEventRepository indexEventRepository;
    private final SearchLogService searchLogService;
    private final IndexEventWorker indexEventWorker;

    @Override
    public ArticleSearchResult search(ArticleQueryCondition condition) {
        SearchWithLog result = articleDocumentRepository.search(condition);
        searchLogService.saveAsync(result.logCapture());
        return result.result();
    }

    @Override
    public ArticleSearchResult searchInProject(Project project, ProjectArticleQueryCondition condition) {
        return articleDocumentRepository.searchInProject(project, condition.toArticleCondition());
    }

    @Override
    public void requestIndex(Long articleId) {
        indexEventRepository.merge(new IndexEvent(articleId, Action.INDEXING));
        indexEventWorker.doIndexOneAsync(articleId);
    }

    @Override
    public void requestDelete(Long articleId) {
        indexEventRepository.merge(new IndexEvent(articleId, Action.DELETED));
        indexEventWorker.doIndexOneAsync(articleId);
    }
}
