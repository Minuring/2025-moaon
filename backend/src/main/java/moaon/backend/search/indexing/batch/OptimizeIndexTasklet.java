package moaon.backend.search.indexing.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.indexing.ArticleIndexRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

@Slf4j
@RequiredArgsConstructor
public class OptimizeIndexTasklet implements Tasklet {

    private final ArticleIndexRepository indexRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var newIndexName = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .getString("newIndexName");
        var newIndexCoords = IndexCoordinates.of(newIndexName);

        log.info("인덱스 refresh 복구: {}", newIndexName);
        indexRepository.updateRefreshInterval(newIndexCoords, "1s");

        log.info("forcemerge 시작: {}", newIndexName);
        indexRepository.forcemerge(newIndexCoords);
        log.info("forcemerge 완료: {}", newIndexName);

        return RepeatStatus.FINISHED;
    }
}
