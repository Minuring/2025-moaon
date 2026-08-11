package moaon.backend.search.indexing.outbox;

import moaon.backend.article.domain.Article;
import moaon.backend.fixture.ArticleFixtureBuilder;
import moaon.backend.fixture.RepositoryHelper;
import moaon.backend.search.indexing.outbox.IndexEvent.Action;
import moaon.backend.search.query.ArticleDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Import(RepositoryHelper.class)
@SpringBootTest
class IndexEventWorkerTest {

    // 이게 없으면 doIndexOneAsync 호출이 즉시 리턴되고 실제 처리는 별도 스레드에서 끝나서,
    // 호출 직후의 verify()가 레이스 컨디션으로 실패한다.
    @TestConfiguration
    static class TestConfig implements AsyncConfigurer {
        @Override
        public Executor getAsyncExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    private IndexEventRepository indexEventRepository;

    @Autowired
    private RepositoryHelper repositoryHelper;

    @Autowired
    private IndexEventWorker indexEventWorker;

    @MockitoBean
    private ElasticIndexingClient indexingClient;

    @Test
    @DisplayName("doIndexOneAsync: ES 클러스터가 Red면 처리하지 않는다")
    void doIndexOneAsyncSkipsWhenClusterRed() throws IOException {
        // given
        Article article = repositoryHelper.save(new ArticleFixtureBuilder().build());
        indexEventRepository.merge(new IndexEvent(article.getId(), Action.INDEXING));

        stubEsClusterHealthy(false);

        // when
        indexEventWorker.doIndexOneAsync(new IndexRequestedEvent(article.getId()));

        // then
        verify(indexingClient, never()).upsert(any(ArticleDocument.class));
    }

    @Test
    @DisplayName("doIndexOneAsync: 이미 처리된 revision이면 건너뛴다")
    void doIndexOneAsyncSkipsWhenAlreadyProcessed() throws IOException {
        // given
        stubEsClusterHealthy(true);

        Article article = repositoryHelper.save(new ArticleFixtureBuilder().build());
        indexEventRepository.merge(new IndexEvent(article.getId(), Action.INDEXING));
        indexEventRepository.markAsProcessed(indexEventRepository.findByEntityId(article.getId()).orElseThrow());

        // when
        indexEventWorker.doIndexOneAsync(new IndexRequestedEvent(article.getId()));

        // then
        verify(indexingClient, never()).upsert(any(ArticleDocument.class));
    }

    @Test
    @DisplayName("doIndexOneAsync: Article이 존재하면 ES에 upsert하고 markAsProcessed까지 완료한다")
    void doIndexOneAsyncUpsertsExistingArticle() throws IOException {
        // given
        stubEsClusterHealthy(true);

        Article article = repositoryHelper.save(new ArticleFixtureBuilder().build());
        indexEventRepository.merge(new IndexEvent(article.getId(), Action.INDEXING));

        // when
        indexEventWorker.doIndexOneAsync(new IndexRequestedEvent(article.getId()));

        // then
        verify(indexingClient).upsert(any(ArticleDocument.class));
        assertThat(indexEventRepository.findByEntityId(article.getId()).orElseThrow().isProcessed()).isTrue();
    }

    @Test
    @DisplayName("doIndexOneAsync: INDEXING인데 Article이 DB에 없으면 삭제로 폴백한다")
    void doIndexOneAsyncFallsBackToDeleteWhenArticleMissing() throws IOException {
        // given
        stubEsClusterHealthy(true);

        long missingArticleId = 999_999L;
        indexEventRepository.merge(new IndexEvent(missingArticleId, Action.INDEXING));

        // when
        indexEventWorker.doIndexOneAsync(new IndexRequestedEvent(missingArticleId));

        // then
        verify(indexingClient).delete(any(Long.class));
        assertThat(indexEventRepository.findByEntityId(missingArticleId).orElseThrow().isProcessed()).isTrue();
    }

    @Test
    @DisplayName("doIndexOneAsync: DELETED 액션은 Article 조회 없이 삭제한다")
    void doIndexOneAsyncDeletesOnDeletedAction() throws IOException {
        // given
        stubEsClusterHealthy(true);

        Article article = repositoryHelper.save(new ArticleFixtureBuilder().build());
        indexEventRepository.merge(new IndexEvent(article.getId(), Action.DELETED));

        // when
        indexEventWorker.doIndexOneAsync(new IndexRequestedEvent(article.getId()));

        // then
        verify(indexingClient).delete(any(Long.class));
        assertThat(indexEventRepository.findByEntityId(article.getId()).orElseThrow().isProcessed()).isTrue();
    }

    @Test
    @DisplayName("doIndexOneAsync: ES 호출이 실패하면 markAsProcessed를 호출하지 않는다")
    void doIndexOneAsyncKeepsUnprocessedOnEsFailure() throws IOException {
        // given
        stubEsClusterHealthy(true);

        Article article = repositoryHelper.save(new ArticleFixtureBuilder().build());
        indexEventRepository.merge(new IndexEvent(article.getId(), Action.INDEXING));
        doThrow(new IOException("ES down")).when(indexingClient).upsert(any(ArticleDocument.class));

        // when
        indexEventWorker.doIndexOneAsync(new IndexRequestedEvent(article.getId()));

        // then
        assertThat(indexEventRepository.findByEntityId(article.getId()).orElseThrow().isProcessed()).isFalse();
    }

    @Test
    @DisplayName("doIndexIfRequired: 처리 대상이 없으면 아무것도 하지 않는다")
    void doIndexIfRequiredDoesNothingWhenNoEvents() throws IOException {
        stubEsClusterHealthy(true);

        indexEventWorker.doIndexIfRequired();

        verify(indexingClient, never()).upsert(any(ArticleDocument.class));
        verify(indexingClient, never()).delete(any(Long.class));
    }

    @Test
    @DisplayName("doIndexIfRequired: ES가 비정상이면 대상이 있어도 건너뛴다")
    void doIndexIfRequiredSkipsWhenClusterUnhealthy() throws IOException {
        // given
        Article article = repositoryHelper.save(new ArticleFixtureBuilder().build());
        indexEventRepository.merge(new IndexEvent(article.getId(), Action.INDEXING));

        stubEsClusterHealthy(false);

        // when
        indexEventWorker.doIndexIfRequired();

        // then
        verify(indexingClient, never()).upsert(any(ArticleDocument.class));
        assertThat(indexEventRepository.findByEntityId(article.getId()).orElseThrow().isProcessed()).isFalse();
    }

    @Test
    @DisplayName("doIndexIfRequired: 대상 이벤트를 색인하고 처리 완료로 남긴다")
    void doIndexIfRequiredProcessesDueEvents() throws IOException {
        // given
        stubEsClusterHealthy(true);

        Article article = repositoryHelper.save(new ArticleFixtureBuilder().build());
        indexEventRepository.merge(new IndexEvent(article.getId(), Action.INDEXING));

        // when
        indexEventWorker.doIndexIfRequired();

        // then
        verify(indexingClient).upsert(any(ArticleDocument.class));
        assertThat(indexEventRepository.findByEntityId(article.getId()).orElseThrow().isProcessed()).isTrue();
    }

    private void stubEsClusterHealthy(boolean healthy) {
        when(indexingClient.isHealthy()).thenReturn(healthy);
    }
}
