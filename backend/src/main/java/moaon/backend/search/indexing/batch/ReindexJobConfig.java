package moaon.backend.search.indexing.batch;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import moaon.backend.article.domain.Article;
import moaon.backend.search.indexing.ArticleIndexRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ReindexJobConfig {

    private static final int CHUNK_SIZE = 500;

    private final ArticleIndexRepository indexRepository;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job articleReindexJob(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JpaCursorItemReader<Article> articleItemReader,
            ArticleItemWriter articleItemWriter
    ) {
        var createNewIndexStep = new StepBuilder("createNewIndexStep", jobRepository)
                .tasklet(new CreateNewIndexTasklet(indexRepository), transactionManager)
                .build();

        // JpaCursorItemReader: DB에서 커서 방식으로 Article을 스트리밍
        // - project는 JOIN FETCH로 즉시 로딩 (ArticleDocument 생성 시 필요)
        // - 나머지 연관관계(topics, techStacks, contentSeparated)는 열린 EntityManager 내에서 지연 로딩
        // - 알려진 한계: topics, techStacks 접근 시 N+1 쿼리 발생 (추후 개선 예정)
        var indexArticlesStep = new StepBuilder("indexArticlesStep", jobRepository)
                .<Article, IndexQuery>chunk(CHUNK_SIZE, transactionManager)
                .reader(articleItemReader)
                .processor(new ArticleItemProcessor())
                .writer(articleItemWriter)
                .build();

        var switchAliasStep = new StepBuilder("switchAliasStep", jobRepository)
                .tasklet(new SwitchAliasTasklet(indexRepository), transactionManager)
                .build();

        return new JobBuilder("articleReindexJob", jobRepository)
                .start(createNewIndexStep)
                .next(indexArticlesStep)
                .next(switchAliasStep)
                .build();
    }

    // @StepScope: Step 시작 시점에 jobExecutionContext에서 newIndexName을 읽어 주입
    @Bean
    @StepScope
    public ArticleItemWriter articleItemWriter(
            @Value("#{jobExecutionContext['newIndexName']}") String newIndexName) {
        return new ArticleItemWriter(indexRepository, IndexCoordinates.of(newIndexName));
    }

    @Bean
    @StepScope
    public JpaCursorItemReader<Article> articleItemReader() {
        return new JpaCursorItemReaderBuilder<Article>()
                .name("articleItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT a FROM Article a JOIN FETCH a.project")
                .build();
    }
}
