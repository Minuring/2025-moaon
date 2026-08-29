package moaon.backend.fixture;

import moaon.backend.global.config.QueryDslConfig;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@Import({QueryDslConfig.class, DataAccessLayerTest.DataAccessLayerTestConfig.class, RepositoryHelper.class})
public @interface DataAccessLayerTest {

    @ComponentScan(
            basePackages = "moaon.backend",
            includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Repository.class),
            useDefaultFilters = false
    )
    class DataAccessLayerTestConfig {
    }
}
