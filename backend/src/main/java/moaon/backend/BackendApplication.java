package moaon.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// ElasticsearchClientConfig에서 조회/벌크용 ElasticsearchClient를 timeout을 다르게 하여 직접 구성하므로
// Spring Boot의 단일 ElasticsearchClient 자동구성은 끈다.
@SpringBootApplication(exclude = {
        ElasticsearchRestClientAutoConfiguration.class,
        ElasticsearchClientAutoConfiguration.class
})
@EnableAsync
@EnableJpaAuditing
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
