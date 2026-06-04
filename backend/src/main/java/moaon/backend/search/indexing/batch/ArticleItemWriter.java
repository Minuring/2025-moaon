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
@RequiredArgsConstructor
public class ArticleItemWriter implements ItemWriter<IndexQuery> {

    private final ArticleIndexRepository indexRepository;
    private final IndexCoordinates targetIndex;

    @Override
    public void write(Chunk<? extends IndexQuery> chunk) {
        List<IndexQuery> items = new ArrayList<>(chunk.getItems());
        indexRepository.bulkIndex(items, targetIndex);
        log.info("{}개 문서 색인 완료 (인덱스: {})", items.size(), targetIndex.getIndexName());
    }
}
