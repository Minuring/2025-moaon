package moaon.backend.search.indexing.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.ElasticsearchClusterClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import moaon.backend.article.repository.ArticleDBRepository;
import moaon.backend.search.indexing.outbox.IndexEvent.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class IndexEventWorkerTest {

    @Mock
    private IndexEventRepository indexEventRepository;

    @Mock
    private ArticleDBRepository articleRepository;

    @Mock
    private ElasticsearchClient esClient;

    @Mock
    private ElasticsearchClusterClient clusterClient;

    @Mock
    private HealthResponse healthResponse;

    @InjectMocks
    private IndexEventWorker scheduler;

    @BeforeEach
    void setUp() throws IOException {
        lenient().when(esClient.cluster()).thenReturn(clusterClient);
        lenient().when(clusterClient.health()).thenReturn(healthResponse);
        lenient().when(healthResponse.status()).thenReturn(HealthStatus.Green);
    }

    @Test
    @DisplayName("이벤트가 없으면 아무것도 처리하지 않는다")
    void doNothingWhenNoEvents() {
        // given
        when(indexEventRepository.findDueToProcess(any(PageRequest.class)))
                .thenReturn(List.of());

        // when
        scheduler.doIndexIfRequired();

        // then
        verify(indexEventRepository).findDueToProcess(any(PageRequest.class));
        verify(articleRepository, never()).findById(anyLong());
        verify(indexEventRepository, never()).markAsProcessed(any());
    }

    @Test
    @DisplayName("ES 클러스터가 Red 상태면 처리하지 않는다")
    void doNothingWhenClusterIsRed() {
        // given
        IndexEvent event = IndexEvent.builder()
                .entityId(100L)
                .action(Action.INDEXING)
                .requiredRevision(1)
                .processedRevision(0)
                .build();

        when(indexEventRepository.findDueToProcess(any(PageRequest.class)))
                .thenReturn(List.of(event));
        when(healthResponse.status()).thenReturn(HealthStatus.Red);

        // when
        scheduler.doIndexIfRequired();

        // then
        verify(indexEventRepository).findDueToProcess(any(PageRequest.class));
        verify(articleRepository, never()).findById(anyLong());
        verify(indexEventRepository, never()).markAsProcessed(any());
    }

    @Test
    @DisplayName("ES 클러스터 상태 확인 실패 시 처리하지 않는다")
    void doNothingWhenHealthCheckFails() throws IOException {
        // given
        IndexEvent event = IndexEvent.builder()
                .entityId(100L)
                .action(Action.INDEXING)
                .requiredRevision(1)
                .processedRevision(0)
                .build();

        when(indexEventRepository.findDueToProcess(any(PageRequest.class)))
                .thenReturn(List.of(event));
        when(clusterClient.health()).thenThrow(new IOException("Connection failed"));

        // when
        scheduler.doIndexIfRequired();

        // then
        verify(articleRepository, never()).findById(anyLong());
        verify(indexEventRepository, never()).markAsProcessed(any());
    }

    @Test
    @DisplayName("DELETED 액션의 이벤트를 처리하면 Article 조회 없이 처리한다")
    void processDeletedEvent() {
        // given
        IndexEvent event = IndexEvent.builder()
                .entityId(100L)
                .action(Action.DELETED)
                .requiredRevision(1)
                .processedRevision(0)
                .build();

        when(indexEventRepository.findDueToProcess(any(PageRequest.class)))
                .thenReturn(List.of(event));
        when(healthResponse.status()).thenReturn(HealthStatus.Green);

        // when
        scheduler.doIndexIfRequired();

        // then
        verify(articleRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("INDEXING 이벤트 처리 시 DB에 Article이 없으면 삭제한다.")
    void queryArticleWhenProcessingIndexingEvent() throws IOException {
        // given
        IndexEvent event = IndexEvent.builder()
                .entityId(100L)
                .action(Action.INDEXING)
                .requiredRevision(1)
                .processedRevision(0)
                .build();

        when(indexEventRepository.findDueToProcess(any(PageRequest.class)))
                .thenReturn(List.of(event));
        when(healthResponse.status()).thenReturn(HealthStatus.Green);
        when(articleRepository.findById(100L)).thenReturn(Optional.empty());

        // when
        scheduler.doIndexIfRequired();

        // then
        verify(esClient).delete(any(Function.class));
    }
}
