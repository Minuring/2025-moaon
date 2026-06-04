package moaon.backend.search.indexing.batch;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.indexing.ArticleIndexRepository;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

@Slf4j
public class ArticleItemWriter implements ItemWriter<IndexQuery>, StepExecutionListener {

    private final ArticleIndexRepository indexRepository;
    private final IndexCoordinates targetIndex;
    private final long totalCount;
    private long indexedCount = 0;
    private long peakHeapMb = 0;

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
        var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long heapUsedMb = heap.getUsed() / 1024 / 1024;
        peakHeapMb = Math.max(peakHeapMb, heapUsedMb);
        log.info("{}/{} ({}%) 색인 완료 | 힙: {}MB / {}MB",
                indexedCount, totalCount, percent,
                heapUsedMb,
                heap.getMax() / 1024 / 1024);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("색인 피크 힙: {}MB", peakHeapMb);
        return null;
    }
}
