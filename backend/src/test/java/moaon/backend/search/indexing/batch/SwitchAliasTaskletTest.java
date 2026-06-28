package moaon.backend.search.indexing.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import moaon.backend.search.indexing.ArticleIndexRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;

class SwitchAliasTaskletTest {

    private ArticleIndexRepository indexRepository;
    private SwitchAliasTasklet tasklet;

    @BeforeEach
    void setUp() {
        indexRepository = mock(ArticleIndexRepository.class);
        tasklet = new SwitchAliasTasklet(indexRepository);
    }

    @Test
    void 기존_인덱스가_있으면_alias_교체_후_구_인덱스를_삭제한다() {
        when(indexRepository.findIndexNamesByAlias(any())).thenReturn(Set.of("articles_idx-old"));

        tasklet.execute(mock(StepContribution.class), chunkContextWithNewIndex("articles_idx-123"));

        verify(indexRepository).switchAlias(any(), any(), any());
        verify(indexRepository).removeAlias(any(), any());
        verify(indexRepository).deleteIndex(any());
    }

    @Test
    void 기존_인덱스가_없으면_alias_교체만_하고_삭제는_하지_않는다() {
        when(indexRepository.findIndexNamesByAlias(any())).thenReturn(Set.of());

        tasklet.execute(mock(StepContribution.class), chunkContextWithNewIndex("articles_idx-123"));

        verify(indexRepository).switchAlias(any(), any(), any());
        verify(indexRepository, never()).removeAlias(any(), any());
        verify(indexRepository, never()).deleteIndex(any());
    }

    private ChunkContext chunkContextWithNewIndex(String newIndexName) {
        var jobExecution = new JobExecution(1L, new JobParameters());
        jobExecution.getExecutionContext().putString("newIndexName", newIndexName);
        var stepExecution = new StepExecution("switchAliasStep", jobExecution);
        return new ChunkContext(new StepContext(stepExecution));
    }
}
