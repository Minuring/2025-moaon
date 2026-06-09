package moaon.backend.search.indexing.batch;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.indexing.outbox.IndexEventRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class ReplayOutboxTasklet implements Tasklet {

    private final IndexEventRepository indexEventRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        long runAt = chunkContext.getStepContext()
                .getStepExecution()
                .getJobParameters()
                .getLong("runAt");
        LocalDateTime since = LocalDateTime.ofInstant(Instant.ofEpochMilli(runAt), ZoneId.of("Asia/Seoul"));
        int count = indexEventRepository.incrementRequiredRevisionSince(since);
        log.info("전체 색인 중 발생한 Outbox 이벤트 {} 건 replay 예약 (since: {})", count, since);
        return RepeatStatus.FINISHED;
    }
}
