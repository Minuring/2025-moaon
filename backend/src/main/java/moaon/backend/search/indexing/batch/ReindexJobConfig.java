package moaon.backend.search.indexing.batch;

import lombok.RequiredArgsConstructor;
import moaon.backend.search.indexing.outbox.IndexEventRepository;
import moaon.backend.search.query.ArticleDocument;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class ReindexJobConfig {

    @Value("${reindex.chunk-size:500}")
    private int chunkSize;

    private final ElasticBatchClient batchClient;
    private final DataSource dataSource;
    private final BatchMetricsListener batchMetricsListener;
    private final IndexEventRepository indexEventRepository;

    @Bean
    public Job articleReindexJob(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ArticleJdbcPagingReader articleJdbcPagingReader,
            ArticleItemWriter articleItemWriter
    ) {
        var createNewIndexStep = new StepBuilder("createNewIndexStep", jobRepository)
                .tasklet(new CreateNewIndexTasklet(batchClient), transactionManager)
                .build();

        var indexArticlesStep = new StepBuilder("indexArticlesStep", jobRepository)
                .<ArticleDocument, IndexQuery>chunk(chunkSize, transactionManager)
                .reader(articleJdbcPagingReader)
                .processor(new ArticleItemProcessor())
                .writer(articleItemWriter)
                .listener(articleItemWriter)
                .build();

        var optimizeIndexStep = new StepBuilder("optimizeIndexStep", jobRepository)
                .tasklet(new OptimizeIndexTasklet(batchClient), transactionManager)
                .build();

        var switchAliasStep = new StepBuilder("switchAliasStep", jobRepository)
                .tasklet(new SwitchAliasTasklet(batchClient), transactionManager)
                .build();

        var replayOutboxStep = new StepBuilder("replayOutboxStep", jobRepository)
                .tasklet(new ReplayOutboxTasklet(indexEventRepository), transactionManager)
                .build();

        return new JobBuilder("articleReindexJob", jobRepository)
                .listener(batchMetricsListener)
                .start(createNewIndexStep)
                .next(indexArticlesStep)
                .next(optimizeIndexStep)
                .next(switchAliasStep)
                .next(replayOutboxStep)
                .build();
    }

    @Bean
    @StepScope
    public ArticleItemWriter articleItemWriter(
            @Value("#{jobExecutionContext['newIndexName']}") String newIndexName) {
        long totalCount = new JdbcTemplate(dataSource).queryForObject("SELECT COUNT(*) FROM article", Long.class);
        return new ArticleItemWriter(batchClient, IndexCoordinates.of(newIndexName), totalCount);
    }

    @Bean
    @StepScope
    public ArticleJdbcPagingReader articleJdbcPagingReader() {
        return new ArticleJdbcPagingReader(dataSource, chunkSize);
    }
}
