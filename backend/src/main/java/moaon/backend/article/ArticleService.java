package moaon.backend.article;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.ArticleContent;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.draft.ArticleDraft;
import moaon.backend.article.draft.ArticleDraftRepository;
import moaon.backend.article.dto.ArticleCreateParams;
import moaon.backend.article.dto.ArticleListResponse;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.repository.ArticleContentRepository;
import moaon.backend.article.repository.ArticleRepository;
import moaon.backend.article.repository.ArticleSearchResult;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.member.Member;
import moaon.backend.project.domain.Project;
import moaon.backend.project.dto.ProjectArticleQueryCondition;
import moaon.backend.project.dto.ProjectArticleResponse;
import moaon.backend.project.repository.ProjectRepository;
import moaon.backend.search.ElasticSearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleService {

    private final ElasticSearchService elasticSearchService;
    private final ArticleRepository articleRepository;
    private final ArticleContentRepository articleContentRepository;
    private final ArticleDraftRepository articleDraftRepository;
    private final ProjectRepository projectRepository;
    private final TransactionTemplate transactionTemplate;

    @CircuitBreaker(name = "articleSearchCB", fallbackMethod = "getPagedArticlesFromDB")
    public ArticleListResponse getPagedArticles(ArticleQueryCondition queryCondition) {
        ArticleSearchResult result = elasticSearchService.search(queryCondition);
        return ArticleListResponse.from(result);
    }

    // CircuitBreaker의 fallbackMethod는 프록시가 아닌 원본 객체에 리플렉션으로 직접 호출되어
    // 클래스 레벨 @Transactional을 타지 않으므로, TransactionTemplate으로 직접 트랜잭션을 연다.
    private ArticleListResponse getPagedArticlesFromDB(ArticleQueryCondition queryCondition, Exception e) {
        return transactionTemplate.execute(status -> {
            ArticleSearchResult result = articleRepository.search(queryCondition, null);
            return ArticleListResponse.from(result);
        });
    }

    @CircuitBreaker(name = "articleSearchCB", fallbackMethod = "getByProjectIdFromDB")
    public ProjectArticleResponse getByProjectId(long id, ProjectArticleQueryCondition condition) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
        ArticleSearchResult filteredArticles = elasticSearchService.searchInProject(project, condition);
        Map<Sector, Long> articleCountBySector = project.countArticlesGroupBySector();
        return ProjectArticleResponse.of(filteredArticles.articles(), articleCountBySector);
    }

    private ProjectArticleResponse getByProjectIdFromDB(long id, ProjectArticleQueryCondition condition, Exception e) {
        return transactionTemplate.execute(status -> {
            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
            ArticleSearchResult filteredArticles = articleRepository.search(condition.toArticleCondition(), project.getId());
            Map<Sector, Long> articleCountBySector = project.countArticlesGroupBySector();
            return ProjectArticleResponse.of(filteredArticles.articles(), articleCountBySector);
        });
    }

    @Transactional
    public void increaseClicksCount(long id) {
        int modified = articleRepository.increaseClickCount(id);
        if (modified == 0) {
            throw new CustomException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        elasticSearchService.requestIndex(id);
    }

    @Transactional
    public void save(List<ArticleCreateParams> params, Member member) {
        for (ArticleCreateParams param : params) {
            Project project = projectRepository.getById(param.projectId());
            if (!project.isOwnedBy(member)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_MEMBER);
            }

            ArticleDraft draft = articleDraftRepository.getById(param.draftId());
            if (!draft.isOwnedBy(member)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_MEMBER);
            }

            Article article = articleRepository.save(param.createArticle(project, draft));
            articleContentRepository.save(new ArticleContent(article, draft.getCrawledContent()));
            articleDraftRepository.delete(draft);
            elasticSearchService.requestIndex(article.getId());
        }
    }
}
