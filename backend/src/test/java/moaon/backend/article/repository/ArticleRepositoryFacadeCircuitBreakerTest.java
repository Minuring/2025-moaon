package moaon.backend.article.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.repository.db.ArticleDBRepository;
import moaon.backend.article.repository.es.ArticleDocumentRepository;
import moaon.backend.fixture.ArticleQueryConditionBuilder;
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
                "resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=5"
        })
class ArticleRepositoryFacadeCircuitBreakerTest {

    @MockitoBean
    private ArticleDocumentRepository elasticSearch;
    @MockitoBean
    private ArticleDBRepository database;
    @Autowired
    private ArticleRepositoryFacade repositoryFacade;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("articleSearchCB");
    }

    @AfterEach
    void afterEach() {
        circuitBreaker.reset();
    }

    @Test
    @DisplayName("정상 호출 시 CLOSED 유지")
    void normalCall_circuitBreakerClosed() {
        // given
        ArticleQueryCondition queryCondition = new ArticleQueryConditionBuilder().build();

        // when
        repositoryFacade.search(queryCondition);

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        verify(elasticSearch).search(queryCondition);
        verifyNoInteractions(database);
    }

    @Test
    @DisplayName("실패 시 fallback 호출")
    void failure_calls_fallback() {
        // given
        ArticleQueryCondition queryCondition = new ArticleQueryConditionBuilder().build();
        doThrow(RuntimeException.class).when(elasticSearch).search(queryCondition);

        // when
        repositoryFacade.search(queryCondition);

        // then
        InOrder inOrder = inOrder(elasticSearch, database);
        inOrder.verify(elasticSearch).search(queryCondition);
        inOrder.verify(database).findWithSearchConditions(queryCondition);
    }

    @Test
    @DisplayName("임계값 초과 시 회로 개방")
    void exceedThreshold_opensCircuitBreaker() {
        // given
        ArticleQueryCondition queryCondition = new ArticleQueryConditionBuilder().build();
        doThrow(RuntimeException.class).when(elasticSearch).search(queryCondition);

        // when
        for (int i = 0; i < 10; i++) {
            repositoryFacade.search(queryCondition);
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("개방 상태에서는 실제 메서드를 호출하지 않고 fallback 메서드만 실행")
    void openState_doesNotCallRealMethod() {
        // given
        circuitBreaker.transitionToOpenState();
        ArticleQueryCondition queryCondition = new ArticleQueryConditionBuilder().build();

        // when
        for (int i = 0; i < 10; i++) {
            repositoryFacade.search(queryCondition);
        }

        // then
        verify(elasticSearch, never()).search(queryCondition);
        verify(database, times(10)).findWithSearchConditions(queryCondition);
    }
}
