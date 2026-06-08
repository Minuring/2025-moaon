package moaon.backend.search.indexing.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;

class ReindexJobLauncherTest {

    private JobExplorer jobExplorer;
    private ReindexJobLauncher launcher;

    @BeforeEach
    void setUp() {
        jobExplorer = mock(JobExplorer.class);
        launcher = new ReindexJobLauncher(mock(JobLauncher.class), mock(Job.class), jobExplorer);
    }

    @Test
    void 이전_실행이_없으면_새_파라미터로_시작한다() {
        when(jobExplorer.getLastJobInstance("articleReindexJob")).thenReturn(null);

        long before = System.currentTimeMillis();
        var params = launcher.resolveJobParameters();
        long after = System.currentTimeMillis();

        assertThat(params.getLong("runAt")).isBetween(before, after);
    }

    @Test
    void 이전_실행이_COMPLETED이면_새_파라미터로_시작한다() {
        var instance = mock(JobInstance.class);
        var execution = mock(JobExecution.class);
        when(jobExplorer.getLastJobInstance("articleReindexJob")).thenReturn(instance);
        when(jobExplorer.getLastJobExecution(instance)).thenReturn(execution);
        when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);

        long before = System.currentTimeMillis();
        var params = launcher.resolveJobParameters();
        long after = System.currentTimeMillis();

        assertThat(params.getLong("runAt")).isBetween(before, after);
    }

    @Test
    void 이전_실행이_FAILED이면_같은_파라미터로_재시작한다() {
        var instance = mock(JobInstance.class);
        var execution = mock(JobExecution.class);
        JobParameters failedParams = new JobParametersBuilder()
                .addLong("runAt", 12345L)
                .toJobParameters();
        when(jobExplorer.getLastJobInstance("articleReindexJob")).thenReturn(instance);
        when(jobExplorer.getLastJobExecution(instance)).thenReturn(execution);
        when(execution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(execution.getJobParameters()).thenReturn(failedParams);

        var params = launcher.resolveJobParameters();

        assertThat(params.getLong("runAt")).isEqualTo(12345L);
    }
}
