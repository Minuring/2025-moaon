package moaon.backend.search.indexing.batch;

import java.time.Instant;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.indexing.ArticleIndexRepository;
import moaon.backend.search.query.ArticleDocument;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.elasticsearch.annotations.Alias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

@Slf4j
@RequiredArgsConstructor
public class CreateNewIndexTasklet implements Tasklet {

    private static final Document DOCUMENT_ANNOTATION = ArticleDocument.class.getAnnotation(Document.class);

    private final ArticleIndexRepository indexRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var newIndexName = DOCUMENT_ANNOTATION.indexName() + "-" + Instant.now().toEpochMilli();
        var newIndexCoords = IndexCoordinates.of(newIndexName);

        indexRepository.createIndex(newIndexCoords, ArticleDocument.class);
        log.info("새 인덱스 생성: {}", newIndexName);

        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .putString("newIndexName", newIndexName);

        return RepeatStatus.FINISHED;
    }
}
