package moaon.backend.article.service;

import moaon.backend.article.ArticleService;
import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.draft.ArticleDraft;
import moaon.backend.article.draft.ArticleDraftRepository;
import moaon.backend.article.dto.ArticleCreateParams;
import moaon.backend.article.dto.ArticleDto;
import moaon.backend.article.dto.ArticleListResponse;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.fixture.Fixtures;
import moaon.backend.fixture.RepositoryHelper;
import moaon.backend.fixture.ServiceLayerTest;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.member.Member;
import moaon.backend.project.domain.Project;
import moaon.backend.project.dto.ProjectArticleQueryCondition;
import moaon.backend.project.dto.ProjectArticleResponse;
import moaon.backend.search.ElasticSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ServiceLayerTest
class ArticleServiceTest {

    @Autowired
    @InjectMocks
    private ArticleService articleService;

    @Autowired
    private RepositoryHelper repositoryHelper;

    @MockitoBean
    private ElasticSearchService elasticSearchService;

    @MockitoSpyBean
    private ArticleDraftRepository articleDraftRepository;

    @DisplayName("주어진 조건으로 아티클들을 조회한다")
    @Test
    void getPagedArticles() {
        // given
        Article a1 = repositoryHelper.saveAnyArticle();
        Article a2 = repositoryHelper.saveAnyArticle();

        // when
        ArticleQueryCondition condition = new ArticleQueryCondition();
        ArticleListResponse response = articleService.getPagedArticles(condition);

        // then
        assertAll(
                () -> assertThat(response.contents()).extracting(ArticleDto::id).contains(a1.getId(), a2.getId()),
                () -> assertThat(response.hasNext()).isFalse(),
                () -> assertThat(response.nextCursor()).isNull(),
                () -> assertThat(response.totalCount()).isEqualTo(2L)
        );
    }

    @DisplayName("특정 프로젝트 내 아티클들을 조회한다.")
    @Test
    void getByProjectId() {
        // given
        Project project = repositoryHelper.saveAnyProject();
        Article articleInProject1 = repositoryHelper.save(Fixtures.articleBuilder().project(project).build());
        Article articleInProject2 = repositoryHelper.save(Fixtures.articleBuilder().project(project).build());

        // when
        ProjectArticleQueryCondition condition = new ProjectArticleQueryCondition();
        ProjectArticleResponse response = articleService.getByProjectId(project.getId(), condition);

        // then
        assertThat(response.articles())
                .extracting(ArticleDto::id)
                .containsExactlyInAnyOrder(articleInProject1.getId(), articleInProject2.getId());
    }

    @DisplayName("특정 프로젝트 내 아티클들을 조회 시 응답에는 Sector 별 아티클 개수를 포함한다.")
    @Test
    void getByProjectId_withArticleSectorCounts() {
        // given
        Project project = repositoryHelper.saveAnyProject();
        Article fe1 = repositoryHelper.saveArticle(b -> b.project(project).sector(Sector.FE));
        Article fe2 = repositoryHelper.saveArticle(b -> b.project(project).sector(Sector.FE));
        Article be1 = repositoryHelper.saveArticle(b -> b.project(project).sector(Sector.BE));

        // when
        ProjectArticleQueryCondition condition = new ProjectArticleQueryCondition();
        ProjectArticleResponse response = articleService.getByProjectId(project.getId(), condition);

        // then
        assertThat(response.counts()).contains(
                new ProjectArticleResponse.ArticleSectorCount("all", 3L),
                new ProjectArticleResponse.ArticleSectorCount(Sector.FE.getName(), 2L),
                new ProjectArticleResponse.ArticleSectorCount(Sector.BE.getName(), 1L)
        );
    }

    @DisplayName("존재하지 않는 프로젝트 ID로 검색하면 예외가 발생한다.")
    @Test
    void getByProjectId_notFound() {
        ProjectArticleQueryCondition condition = new ProjectArticleQueryCondition();

        assertThatThrownBy(() -> articleService.getByProjectId(123L, condition))
                .isInstanceOf(CustomException.class);
    }

    @DisplayName("클릭 수를 증가시킨다.")
    @Test
    void increaseClicksCount_success() {
        // given
        Article article = repositoryHelper.saveAnyArticle();
        int clicksBefore = article.getClicks();

        // when
        articleService.increaseClicksCount(article.getId());
        int clicksAfter = repositoryHelper.getArticleById(article.getId()).getClicks();

        // then
        assertThat(clicksAfter).isEqualTo(clicksBefore + 1);
    }

    @DisplayName("존재하지 않는 아티클의 클릭 증가 시 예외 발생")
    @Test
    void increaseClicksCount_notFound() {
        assertThatThrownBy(() -> articleService.increaseClicksCount(999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ARTICLE_NOT_FOUND);
    }

    @DisplayName("프로젝트 작성자와 아티클 등록중인 사용자가 불일치하면 예외가 발생한다.")
    @Test
    void save_notSameProjectAuthor() {
        // given
        Project project = repositoryHelper.saveAnyProject();
        Member author = project.getAuthor();
        ArticleDraft draft = articleDraftRepository.save(new ArticleDraft(author, ".../url1", "제목1", "본문1"));

        List<ArticleCreateParams> params = List.of(
                new ArticleCreateParams(
                        project.getId(),
                        "제목1",
                        "요약1",
                        List.of(),
                        draft.getId(),
                        Sector.NON_TECH,
                        List.of(Topic.TEAM_CULTURE)
                )
        );

        // when & then
        Member otherMember = repositoryHelper.saveAnyMember();
        assertThatThrownBy(() -> articleService.save(params, otherMember))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED_MEMBER.getMessage());
    }

    @DisplayName("아티클 초안의 작성자와 아티클 등록중인 사용자가 불일치하면 예외가 발생한다.")
    @Test
    void save_notSameDraftAuthor() {
        // given
        Project project = repositoryHelper.saveAnyProject();
        Member projectAuthor = project.getAuthor();

        Member otherMember = repositoryHelper.saveAnyMember();
        ArticleDraft draft = articleDraftRepository.save(new ArticleDraft(otherMember, ".../url1", "제목1", "본문1"));

        List<ArticleCreateParams> params = List.of(
                new ArticleCreateParams(
                        project.getId(),
                        "제목1",
                        "요약1",
                        List.of(),
                        draft.getId(),
                        Sector.NON_TECH,
                        List.of(Topic.TEAM_CULTURE)
                )
        );

        // when & then
        assertThatThrownBy(() -> articleService.save(params, projectAuthor))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED_MEMBER.getMessage());
    }

    @DisplayName("ArticleCreateRequest의 갯수만큼 저장한다.")
    @Test
    void save_createsArticles() {
        // given
        Project project = repositoryHelper.saveAnyProject();
        Member projectAuthor = project.getAuthor();
        ArticleDraft draft1 = articleDraftRepository.save(new ArticleDraft(projectAuthor, ".../url1", "제목1", "본문1"));
        ArticleDraft draft2 = articleDraftRepository.save(new ArticleDraft(projectAuthor, ".../url2", "제목2", "본문2"));

        // when
        List<ArticleCreateParams> params = List.of(
                new ArticleCreateParams(project.getId(), "제목1", "요약1", List.of(), draft1.getId(), Sector.NON_TECH, List.of(Topic.TEAM_CULTURE)),
                new ArticleCreateParams(project.getId(), "제목2", "요약2", List.of(), draft2.getId(), Sector.NON_TECH, List.of(Topic.DESIGN))
        );
        articleService.save(params, projectAuthor);

        // then
        ProjectArticleResponse searched = articleService.getByProjectId(project.getId(), new ProjectArticleQueryCondition());
        assertThat(searched.articles()).hasSize(2)
                .extracting(
                        ArticleDto::title,
                        ArticleDto::url
                ).containsExactlyInAnyOrder(
                        tuple("제목1", draft1.getUrl()),
                        tuple("제목2", draft2.getUrl())
                );
    }

    @DisplayName("저장 후 색인 요청을 하며, Draft를 정리한다.")
    @Test
    void save_requestsIndexAndCleanUpDrafts() {
        // given
        Project project = repositoryHelper.saveAnyProject();
        Member projectAuthor = project.getAuthor();
        ArticleDraft draft1 = articleDraftRepository.save(new ArticleDraft(projectAuthor, ".../url1", "제목1", "본문1"));
        ArticleDraft draft2 = articleDraftRepository.save(new ArticleDraft(projectAuthor, ".../url2", "제목2", "본문2"));

        // when
        List<ArticleCreateParams> params = List.of(
                new ArticleCreateParams(project.getId(), "제목1", "요약1", List.of(), draft1.getId(), Sector.NON_TECH, List.of(Topic.TEAM_CULTURE)),
                new ArticleCreateParams(project.getId(), "제목2", "요약2", List.of(), draft2.getId(), Sector.NON_TECH, List.of(Topic.DESIGN))
        );
        articleService.save(params, projectAuthor);

        // then
        verify(elasticSearchService, times(2)).requestIndex(anyLong());
        verify(articleDraftRepository).delete(draft1);
        verify(articleDraftRepository).delete(draft2);
    }
}
