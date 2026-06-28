package moaon.backend.search.indexing.batch;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BatchMetricsListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        log.info("[배치 시작] 힙: {}MB / {}MB",
                heap.getUsed() / 1024 / 1024,
                heap.getMax() / 1024 / 1024);
    }

@Override
    public void afterJob(JobExecution jobExecution) {
        var duration = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime());
        var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        log.info("[배치 종료] 소요: {}초, 상태: {} | 힙: {}MB / {}MB",
                duration.getSeconds(),
                jobExecution.getExitStatus().getExitCode(),
                heap.getUsed() / 1024 / 1024,
                heap.getMax() / 1024 / 1024);
    }


}
