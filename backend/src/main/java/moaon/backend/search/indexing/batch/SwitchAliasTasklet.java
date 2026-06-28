package moaon.backend.search.indexing.batch;

import java.util.Arrays;
import java.util.Set;
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
import org.springframework.util.CollectionUtils;

@Slf4j
@RequiredArgsConstructor
public class SwitchAliasTasklet implements Tasklet {

    private static final Document DOCUMENT_ANNOTATION = ArticleDocument.class.getAnnotation(Document.class);

    private final ArticleIndexRepository indexRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var newIndexName = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .getString("newIndexName");

        var newIndexCoords = IndexCoordinates.of(newIndexName);
        var aliasCoords = aliasWrapper();

        var oldIndexNames = indexRepository.findIndexNamesByAlias(aliasCoords);
        indexRepository.switchAlias(newIndexCoords, oldIndexNames, aliasCoords);
        log.info("Alias 교체 완료: {} → {}", oldIndexNames, newIndexName);

        if (!CollectionUtils.isEmpty(oldIndexNames)) {
            var oldIndicesCoords = IndexCoordinates.of(oldIndexNames.toArray(String[]::new));
            indexRepository.removeAlias(oldIndicesCoords, aliasCoords);
            indexRepository.deleteIndex(oldIndicesCoords);
            log.info("구 인덱스 삭제: {}", oldIndexNames);
        }

        return RepeatStatus.FINISHED;
    }

    private IndexCoordinates aliasWrapper() {
        var aliasNames = Arrays.stream(DOCUMENT_ANNOTATION.aliases())
                .map(Alias::value)
                .toArray(String[]::new);
        return IndexCoordinates.of(aliasNames);
    }
}
