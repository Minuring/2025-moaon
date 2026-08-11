package moaon.backend.search.indexing.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CreateNewIndexTaskletTest {

    @Test
    void 새_인덱스를_생성하고_이름을_execution_context에_저장한다() {
        var batchClient = mock(ElasticBatchClient.class);
        var tasklet = new CreateNewIndexTasklet(batchClient);

        var jobExecution = new JobExecution(1L, new JobParameters());
        var chunkContext = new ChunkContext(
                new StepContext(new StepExecution("createNewIndexStep", jobExecution)));

        tasklet.execute(mock(StepContribution.class), chunkContext);

        verify(batchClient).createIndex(any(), any());
        assertThat(jobExecution.getExecutionContext().getString("newIndexName"))
                .startsWith("articles_idx-");
    }
}
