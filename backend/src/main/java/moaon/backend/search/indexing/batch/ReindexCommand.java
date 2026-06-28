package moaon.backend.search.indexing.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
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
    private final ApplicationContext context;

    /**
     * java -jar app.jar --reindex --spring.main.web-application-type=none
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("reindex")) {
            return;
        }

        var params = new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();

        var execution = jobLauncher.run(articleReindexJob, params);

        int exitCode = ExitStatus.COMPLETED.equals(execution.getExitStatus()) ? 0 : 1;
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }
}
