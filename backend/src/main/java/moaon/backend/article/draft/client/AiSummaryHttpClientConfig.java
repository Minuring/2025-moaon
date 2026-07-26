package moaon.backend.article.draft.client;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiSummaryHttpClientConfig {

    @Bean(name = "aiSummaryHttpClient")
    public HttpClient aiSummaryHttpClient(@Value("${gpt.connect-timeout-millis}") long connectTimeoutMillis) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutMillis))
                .build();
    }
}
