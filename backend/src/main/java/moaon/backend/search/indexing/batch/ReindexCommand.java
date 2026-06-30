package moaon.backend.search.indexing.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReindexCommand implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job articleReindexJob;
    private final JobExplorer jobExplorer;
    private final ApplicationContext context;

    /**
     * java -jar app.jar --reindex --spring.main.web-application-type=none
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("reindex")) {
            return;
        }

        var params = resolveJobParameters();
        var execution = jobLauncher.run(articleReindexJob, params);

        int exitCode = ExitStatus.COMPLETED.equals(execution.getExitStatus()) ? 0 : 1;
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }

    private JobParameters resolveJobParameters() {
        var lastInstance = jobExplorer.getLastJobInstance("articleReindexJob");
        if (lastInstance != null) {
            var lastExecution = jobExplorer.getLastJobExecution(lastInstance);
            if (lastExecution != null
                    && (lastExecution.getStatus() == BatchStatus.FAILED
                    || lastExecution.getStatus() == BatchStatus.STOPPED)) {
                log.info("이전 실패/중단 실행 재시작 (status: {}, runAt: {})",
                        lastExecution.getStatus(),
                        lastExecution.getJobParameters().getLong("runAt"));
                return lastExecution.getJobParameters();
            }
        }
        log.info("전체 재색인 시작");
        return new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();
    }
}
