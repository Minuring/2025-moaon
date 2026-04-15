package moaon.backend.search.indexing;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleReindexBatch {

    private final ArticleIndexer articleIndexer;

    @Value("${reindex.flag-path:/tmp/reindex-required.flag}")
    private String flagPath;

    @Value("${reindex.batch-size:1000}")
    private int batchSize;

    @Scheduled(cron = "${reindex.cron:0 0 3 * * *}")
    public void runIfRequired() {
        Path flag = Path.of(flagPath);
        if (!Files.exists(flag)) {
            return;
        }
        log.info("Reindex flag 감지, 전체 재색인 시작: {}", flag);
        try {
            articleIndexer.indexAll(batchSize);
            Files.delete(flag);
            log.info("전체 재색인 완료, flag 삭제: {}", flag);
        } catch (Exception e) {
            log.error("전체 재색인 실패 (flag 유지됨)", e);
        }
    }
}
