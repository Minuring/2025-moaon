package moaon.backend.article;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.draft.ArticleDraft;
import moaon.backend.article.draft.ArticleDraftRepository;
import moaon.backend.article.dto.ArticleCreateRequest;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.dto.ArticleListResponse;
import moaon.backend.article.repository.ArticleDBRepository;
import moaon.backend.article.repository.ArticleSearchResult;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.member.Member;
import moaon.backend.project.domain.Project;
import moaon.backend.project.dto.ProjectArticleQueryCondition;
import moaon.backend.project.dto.ProjectArticleResponse;
import moaon.backend.project.repository.ProjectRepository;
import moaon.backend.article.repository.SearchFacade;
import moaon.backend.techStack.TechStackResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleService {

    private final SearchFacade searchFacade;
    private final ArticleDBRepository articleDBRepository;
    private final ArticleDraftRepository articleDraftRepository;
    private final ProjectRepository projectRepository;
    private final TechStackResolver techStackResolver;

    public ArticleListResponse getPagedArticles(ArticleQueryCondition queryCondition) {
        ArticleSearchResult result = searchFacade.search(queryCondition);
        return ArticleListResponse.from(result);
    }

    public ProjectArticleResponse getByProjectId(long id, ProjectArticleQueryCondition condition) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
        ArticleSearchResult filteredArticles = searchFacade.searchInProject(project, condition);
        Map<Sector, Long> articleCountBySector = project.countArticlesGroupBySector();
        return ProjectArticleResponse.of(filteredArticles.articles(), articleCountBySector);
    }

    @Transactional
    public void increaseClicksCount(long id) {
        int modified = articleDBRepository.increaseClickCount(id);
        if (modified == 0) {
            throw new CustomException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        searchFacade.requestIndex(id);
    }

    @Transactional
    public void save(List<ArticleCreateRequest> requests, Member member) {
        for (ArticleCreateRequest request : requests) {
            Project project = projectRepository.findById(request.projectId()).orElseThrow(
                    () -> new CustomException(ErrorCode.PROJECT_NOT_FOUND)
            );
            if (!member.equals(project.getAuthor())) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_MEMBER);
            }

            ArticleDraft draft = articleDraftRepository.findById(request.draftId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_DRAFT_NOT_FOUND));
            if (!draft.isOwnedBy(member)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_MEMBER);
            }

            Article article = new Article(
                    request.title(),
                    request.summary(),
                    draft.getCrawledContent(),
                    draft.getUrl(),
                    LocalDateTime.now(),
                    project,
                    Sector.of(request.sector()),
                    request.topics().stream().map(Topic::of).toList(),
                    techStackResolver.resolve(request.techStacks())
            );
            Article saved = articleDBRepository.save(article);
            articleDraftRepository.delete(draft);
            searchFacade.requestIndex(saved.getId());
        }
    }
}
