package moaon.backend.search.indexing.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReindexJobLauncher {

    private final JobLauncher jobLauncher;
    private final Job articleReindexJob;
    private final JobExplorer jobExplorer;

    @Scheduled(cron = "${reindex.cron:0 0 3 * * *}")
    public void run() {
        try {
            var params = resolveJobParameters();
            jobLauncher.run(articleReindexJob, params);
        } catch (Exception e) {
            log.error("전체 재색인 실패", e);
        }
    }

    JobParameters resolveJobParameters() {
        var lastInstance = jobExplorer.getLastJobInstance("articleReindexJob");
        if (lastInstance != null) {
            var lastExecution = jobExplorer.getLastJobExecution(lastInstance);
            if (lastExecution != null && lastExecution.getStatus() == BatchStatus.FAILED) {
                log.info("이전 실패 실행 재시작");
                return lastExecution.getJobParameters();
            }
        }
        log.info("전체 재색인 시작");
        return new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();
    }
}
