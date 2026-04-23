package moaon.backend.global.config;

import com.p6spy.engine.spy.P6DataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"local", "dev"})
@Configuration
public class P6SpyConfig {

    @Bean
    public static BeanPostProcessor p6SpyDataSourceBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!"dataSource".equals(beanName)) {
                    return bean;
                }
                if (!(bean instanceof DataSource dataSource) || bean instanceof P6DataSource) {
                    return bean;
                }
                return new P6DataSource(dataSource);
            }
        };
    }
}
