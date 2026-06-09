package moaon.backend.search.indexing.batch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import moaon.backend.search.indexing.outbox.IndexEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;

class ReplayOutboxTaskletTest {

    private IndexEventRepository indexEventRepository;
    private ReplayOutboxTasklet tasklet;

    @BeforeEach
    void setUp() {
        indexEventRepository = mock(IndexEventRepository.class);
        tasklet = new ReplayOutboxTasklet(indexEventRepository);
    }

    @Test
    void runAt_파라미터를_LocalDateTime으로_변환하여_incrementRequiredRevisionSince를_호출한다() {
        long runAt = System.currentTimeMillis();
        LocalDateTime expectedSince = LocalDateTime.ofInstant(Instant.ofEpochMilli(runAt), ZoneId.systemDefault());

        tasklet.execute(mock(StepContribution.class), chunkContextWith(runAt));

        verify(indexEventRepository).incrementRequiredRevisionSince(expectedSince);
    }

    private ChunkContext chunkContextWith(long runAt) {
        JobParameters params = new JobParametersBuilder()
                .addLong("runAt", runAt)
                .toJobParameters();
        var jobExecution = new JobExecution(1L, params);
        var stepExecution = new StepExecution("replayOutboxStep", jobExecution);
        return new ChunkContext(new StepContext(stepExecution));
    }
}
