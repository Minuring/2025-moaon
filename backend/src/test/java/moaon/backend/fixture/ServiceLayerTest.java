package moaon.backend.fixture;

import jakarta.servlet.Filter;
import moaon.backend.BackendApplication;
import moaon.backend.global.config.FilterConfig;
import moaon.backend.global.config.QueryDslConfig;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 서비스 계층 자체의 트랜잭션 경계(커밋/비동기 색인 등)를 그대로 검증하기 위해
// 테스트 메서드를 감싸는 @Transactional 롤백은 쓰지 않고, 매 테스트 종료 후
// 전체 테이블을 truncate하는 방식으로 데이터를 격리한다.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = ServiceLayerTest.ServiceLayerTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({QueryDslConfig.class, RepositoryHelper.class})
@ExtendWith(DatabaseCleanupExtension.class)
public @interface ServiceLayerTest {

    // BackendApplication과 동일하게 부트스트랩하되, Controller/ControllerAdvice/Filter 등
    // 웹 계층 빈만 컴포넌트 스캔에서 제외한다.
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            ElasticsearchRestClientAutoConfiguration.class,
            ElasticsearchClientAutoConfiguration.class
    })
    @AutoConfigurationPackage(basePackages = "moaon.backend")
    @EnableAsync
    @EnableJpaAuditing
    @EnableScheduling
    @ComponentScan(
            basePackages = "moaon.backend",
            excludeFilters = {
                    @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = {
                            Controller.class, RestController.class, ControllerAdvice.class, RestControllerAdvice.class
                    }),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                            Filter.class, BackendApplication.class, FilterConfig.class
                    })
            }
    )
    class ServiceLayerTestConfig {
    }
}
