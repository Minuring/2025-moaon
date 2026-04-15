package moaon.backend.article.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import moaon.backend.article.domain.Article;
import moaon.backend.article.dto.ArticleCreateRequest;
import moaon.backend.article.repository.ArticleContentRepository;
import moaon.backend.article.repository.ArticleDBRepository;
import moaon.backend.fixture.ArticleFixtureBuilder;
import moaon.backend.fixture.Fixture;
import moaon.backend.fixture.ProjectFixtureBuilder;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.global.parser.URLParser;
import moaon.backend.member.domain.Member;
import moaon.backend.project.dto.ProjectArticleQueryCondition;
import moaon.backend.project.repository.ProjectRepository;
import moaon.backend.search.api.SearchFacade;
import moaon.backend.techStack.repository.TechStackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ArticleServiceTest {

    private final SearchFacade searchFacade = Mockito.mock(SearchFacade.class);
    private final ArticleDBRepository articleDBRepository = Mockito.mock(ArticleDBRepository.class);
    private final ArticleContentRepository articleContentRepository = Mockito.mock(ArticleContentRepository.class);
    private final ProjectRepository projectRepository = Mockito.mock(ProjectRepository.class);
    private final TechStackRepository techStackRepository = Mockito.mock(TechStackRepository.class);

    private final ArticleService articleService = new ArticleService(
            searchFacade,
            articleDBRepository,
            articleContentRepository,
            projectRepository,
            techStackRepository
    );

    @DisplayName("존재하지 않는 프로젝트 ID로 검색하면 예외가 발생한다.")
    @Test
    void getByProjectId_notFound() {
        when(projectRepository.findById(123L)).thenReturn(Optional.empty());
        ProjectArticleQueryCondition condition = mock(ProjectArticleQueryCondition.class);

        assertThatThrownBy(() -> articleService.getByProjectId(123L, condition))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
    }

    @DisplayName("클릭 수를 증가시킨다.")
    @Test
    void increaseClicksCount_success() {
        when(articleDBRepository.increaseClickCount(123L)).thenReturn(1);

        articleService.increaseClicksCount(123L);

        verify(articleDBRepository).increaseClickCount(123L);
        verify(searchFacade).requestIndex(123L);
    }

    @DisplayName("존재하지 않는 아티클의 클릭 증가 시 예외 발생")
    @Test
    void increaseClicksCount_notFound() {
        when(articleDBRepository.increaseClickCount(1L)).thenReturn(0);

        assertThatThrownBy(() -> articleService.increaseClicksCount(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ARTICLE_NOT_FOUND);
    }

    @DisplayName("ArticleCreateRequest의 갯수만큼 저장하고 각각 색인 요청한다.")
    @Test
    void save_createsArticleAndRequestsIndex() {
        Member author = new Member(1L, "socialId", "email", "name", 0);
        Article savedArticle = new ArticleFixtureBuilder().id(99L).build();
        when(projectRepository.findById(1L)).thenReturn(
                Optional.of(new ProjectFixtureBuilder().author(author).build())
        );
        when(articleDBRepository.save(any(Article.class))).thenReturn(savedArticle);

        List<ArticleCreateRequest> requests = List.of(
                articleCreateRequestWithProjectId(1L),
                articleCreateRequestWithProjectId(1L)
        );
        articleService.save(requests, author);

        verify(articleDBRepository, times(2)).save(any(Article.class));
        verify(searchFacade, times(2)).requestIndex(99L);
    }

    private ArticleCreateRequest articleCreateRequestWithProjectId(long projectId) {
        return new ArticleCreateRequest(
                projectId,
                "title",
                "summary",
                List.of(),
                URLParser.parse("https://example.com/article"),
                "non_tech",
                List.of("design")
        );
    }
}
