package moaon.backend.search.indexing.batch;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.indexing.ArticleIndexRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

@Slf4j
public class ArticleItemWriter implements ItemWriter<IndexQuery> {

    private final ArticleIndexRepository indexRepository;
    private final IndexCoordinates targetIndex;
    private final long totalCount;
    private long indexedCount = 0;

    public ArticleItemWriter(ArticleIndexRepository indexRepository, IndexCoordinates targetIndex, long totalCount) {
        this.indexRepository = indexRepository;
        this.targetIndex = targetIndex;
        this.totalCount = totalCount;
    }

    @Override
    public void write(Chunk<? extends IndexQuery> chunk) {
        List<IndexQuery> items = new ArrayList<>(chunk.getItems());
        indexRepository.bulkIndex(items, targetIndex);
        indexedCount += items.size();
        int percent = totalCount > 0 ? (int) (indexedCount * 100 / totalCount) : 0;
        log.info("{}/{} ({}%) 색인 완료", indexedCount, totalCount, percent);
    }
}
