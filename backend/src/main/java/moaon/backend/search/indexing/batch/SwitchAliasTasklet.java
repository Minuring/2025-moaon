package moaon.backend.search.indexing.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.query.ArticleDocument;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.elasticsearch.annotations.Alias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class SwitchAliasTasklet implements Tasklet {

    private static final Document DOCUMENT_ANNOTATION = ArticleDocument.class.getAnnotation(Document.class);

    private final ElasticBatchClient batchClient;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var newIndexName = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .getString("newIndexName");

        var newIndexCoords = IndexCoordinates.of(newIndexName);
        var aliasCoords = aliasWrapper();

        var oldIndexNames = batchClient.findIndexNamesByAlias(aliasCoords);
        batchClient.switchAlias(newIndexCoords, oldIndexNames, aliasCoords);
        log.info("Alias 교체 완료: {} → {}", oldIndexNames, newIndexName);

        if (!CollectionUtils.isEmpty(oldIndexNames)) {
            var oldIndicesCoords = IndexCoordinates.of(oldIndexNames.toArray(String[]::new));
            batchClient.removeAlias(oldIndicesCoords, aliasCoords);
            batchClient.deleteIndex(oldIndicesCoords);
            log.info("구 인덱스 삭제: {}", oldIndexNames);
        }

        deleteOrphanIndices(newIndexName);

        return RepeatStatus.FINISHED;
    }

    // 실패한 재색인이 남긴, alias가 가리키지 않는 인덱스를 지운다.
    private void deleteOrphanIndices(String newIndexName) {
        var orphanIndexNames = batchClient.findIndexNamesByPattern(DOCUMENT_ANNOTATION.indexName() + "-*")
                .stream()
                .filter(name -> !name.equals(newIndexName))
                .toList();
        if (orphanIndexNames.isEmpty()) {
            return;
        }

        batchClient.deleteIndex(IndexCoordinates.of(orphanIndexNames.toArray(String[]::new)));
        log.info("alias 없는 인덱스 삭제: {}", orphanIndexNames);
    }

    private IndexCoordinates aliasWrapper() {
        var aliasNames = Arrays.stream(DOCUMENT_ANNOTATION.aliases())
                .map(Alias::value)
                .toArray(String[]::new);
        return IndexCoordinates.of(aliasNames);
    }
}
