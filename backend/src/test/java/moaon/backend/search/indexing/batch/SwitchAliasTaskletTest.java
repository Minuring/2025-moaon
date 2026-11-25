package moaon.backend.search.indexing.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SwitchAliasTaskletTest {

    private ElasticBatchClient batchClient;
    private SwitchAliasTasklet tasklet;

    @BeforeEach
    void setUp() {
        batchClient = mock(ElasticBatchClient.class);
        tasklet = new SwitchAliasTasklet(batchClient);
    }

    @Test
    void 기존_인덱스가_있으면_alias_교체_후_구_인덱스를_삭제한다() {
        when(batchClient.findIndexNamesByAlias(any())).thenReturn(Set.of("articles_idx-old"));

        tasklet.execute(mock(StepContribution.class), chunkContextWithNewIndex("articles_idx-123"));

        verify(batchClient).switchAlias(any(), any(), any());
        verify(batchClient).removeAlias(any(), any());
        verify(batchClient).deleteIndex(any());
    }

    @Test
    void 기존_인덱스가_없으면_alias_교체만_하고_삭제는_하지_않는다() {
        when(batchClient.findIndexNamesByAlias(any())).thenReturn(Set.of());

        tasklet.execute(mock(StepContribution.class), chunkContextWithNewIndex("articles_idx-123"));

        verify(batchClient).switchAlias(any(), any(), any());
        verify(batchClient, never()).removeAlias(any(), any());
        verify(batchClient, never()).deleteIndex(any());
    }

    @Test
    void alias가_가리키지_않는_인덱스는_새_인덱스를_빼고_삭제한다() {
        when(batchClient.findIndexNamesByAlias(any())).thenReturn(Set.of());
        when(batchClient.findIndexNamesByPattern("articles_idx-*"))
                .thenReturn(Set.of("articles_idx-123", "articles_idx-orphan"));

        tasklet.execute(mock(StepContribution.class), chunkContextWithNewIndex("articles_idx-123"));

        verify(batchClient).deleteIndex(IndexCoordinates.of("articles_idx-orphan"));
    }

    @Test
    void alias가_가리키지_않는_인덱스가_없으면_삭제하지_않는다() {
        when(batchClient.findIndexNamesByAlias(any())).thenReturn(Set.of());
        when(batchClient.findIndexNamesByPattern("articles_idx-*")).thenReturn(Set.of("articles_idx-123"));

        tasklet.execute(mock(StepContribution.class), chunkContextWithNewIndex("articles_idx-123"));

        verify(batchClient, never()).deleteIndex(any());
    }

    private ChunkContext chunkContextWithNewIndex(String newIndexName) {
        var jobExecution = new JobExecution(1L, new JobParameters());
        jobExecution.getExecutionContext().putString("newIndexName", newIndexName);
        var stepExecution = new StepExecution("switchAliasStep", jobExecution);
        return new ChunkContext(new StepContext(stepExecution));
    }
}
