package moaon.backend.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import java.util.Optional;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.repository.ArticleRepository;
import moaon.backend.article.repository.ArticleSearchResult;
import moaon.backend.fixture.ArticleQueryConditionBuilder;
import moaon.backend.fixture.ProjectArticleQueryConditionFixtureBuilder;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.project.dto.ProjectArticleQueryCondition;
import moaon.backend.project.repository.ProjectRepository;
import moaon.backend.search.ElasticSearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = WebEnvironment.NONE,
        properties = {
                "resilience4j.circuitbreaker.configs.default.failureRateThreshold=50",
                "resilience4j.circuitbreaker.configs.default.slidingWindowSize=10",
                "resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=5",
                "resilience4j.circuitbreaker.configs.default.ignore-exceptions[0]=moaon.backend.global.exception.custom.CustomException"
        })
class ArticleServiceCircuitBreakerTest {

    @MockitoBean
    private ElasticSearchService elasticSearchService;
    @MockitoBean
    private ArticleRepository articleRepository;
    @MockitoBean
    private ProjectRepository projectRepository;
    @Autowired
    private ArticleService articleService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("articleSearchCB");
        when(articleRepository.search(any(), any())).thenReturn(emptyResult());
    }

    @AfterEach
    void afterEach() {
        circuitBreaker.reset();
    }

    @Test
    @DisplayName("정상 호출 시 CLOSED 유지")
    void normalCall_circuitBreakerClosed() {
        ArticleQueryCondition queryCondition = new ArticleQueryConditionBuilder().build();
        when(elasticSearchService.search(queryCondition)).thenReturn(emptyResult());

        articleService.getPagedArticles(queryCondition);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        verify(elasticSearchService).search(queryCondition);
        verifyNoInteractions(articleRepository);
    }

    @Test
    @DisplayName("실패 시 fallback 호출")
    void failure_calls_fallback() {
        ArticleQueryCondition queryCondition = new ArticleQueryConditionBuilder().build();
        doThrow(RuntimeException.class).when(elasticSearchService).search(queryCondition);

        articleService.getPagedArticles(queryCondition);

        InOrder inOrder = inOrder(elasticSearchService, articleRepository);
        inOrder.verify(elasticSearchService).search(queryCondition);
        inOrder.verify(articleRepository).search(queryCondition, null);
    }

    @Test
    @DisplayName("임계값 초과 시 회로 개방")
    void exceedThreshold_opensCircuitBreaker() {
        ArticleQueryCondition queryCondition = new ArticleQueryConditionBuilder().build();
        doThrow(RuntimeException.class).when(elasticSearchService).search(queryCondition);

        for (int i = 0; i < 10; i++) {
            articleService.getPagedArticles(queryCondition);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("개방 상태에서는 실제 메서드를 호출하지 않고 fallback 메서드만 실행")
    void openState_doesNotCallRealMethod() {
        circuitBreaker.transitionToOpenState();
        ArticleQueryCondition queryCondition = new ArticleQueryConditionBuilder().build();

        for (int i = 0; i < 10; i++) {
            articleService.getPagedArticles(queryCondition);
        }

        verify(elasticSearchService, never()).search(queryCondition);
        verify(articleRepository, times(10)).search(queryCondition, null);
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트 조회 실패는 CircuitBreaker 실패로 집계되지 않는다")
    void projectNotFound_isIgnoredByCircuitBreaker() {
        ProjectArticleQueryCondition condition = new ProjectArticleQueryConditionFixtureBuilder().build();
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> articleService.getByProjectId(1L, condition))
                    .isInstanceOf(CustomException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        verifyNoInteractions(articleRepository);
    }

    private ArticleSearchResult emptyResult() {
        return new ArticleSearchResult(List.of(), 0, false, null);
    }
}
