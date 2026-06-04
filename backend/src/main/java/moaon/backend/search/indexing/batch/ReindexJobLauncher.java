package moaon.backend.search.indexing.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReindexJobLauncher {

    private final JobLauncher jobLauncher;
    private final Job articleReindexJob;

    @Scheduled(cron = "${reindex.cron:0 0 3 * * *}")
    public void run() {
        log.info("전체 재색인 시작");
        try {
            var params = new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(articleReindexJob, params);
            log.info("전체 재색인 완료");
        } catch (Exception e) {
            log.error("전체 재색인 실패", e);
        }
    }
}
