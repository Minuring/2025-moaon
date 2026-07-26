package moaon.backend.article.draft.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {
                "resilience4j.circuitbreaker.configs.default.failureRateThreshold=50",
                "resilience4j.circuitbreaker.configs.default.slidingWindowSize=10",
                "resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=5",
                "resilience4j.circuitbreaker.configs.default.ignore-exceptions[0]=moaon.backend.article.draft.client.AiNoCostException",
                "resilience4j.circuitbreaker.configs.default.ignore-exceptions[1]=moaon.backend.article.draft.client.AiSummaryFailedException"
        }
)
class AiSummaryClientTest {

    @MockitoBean(name = "aiSummaryHttpClient")
    private HttpClient httpClient;

    @Autowired
    private AiSummaryClient aiSummaryClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Value("${resilience4j.circuitbreaker.configs.default.failureRateThreshold}")
    private int failureRateThreshold;

    @Value("${resilience4j.circuitbreaker.configs.default.slidingWindowSize}")
    private int slidingWindowSize;

    @AfterEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("aiSummaryCircuitBreaker").reset();
    }

    @Test
    @DisplayName("서킷 브레이커가 정의한 설정에 맞게 호출이 제한된다.")
    void circuitBreakerTest() throws IOException, InterruptedException {
        doThrow(IOException.class).when(httpClient).send(any(), any());

        executeNTimesIgnoringException(slidingWindowSize, () -> aiSummaryClient.summarize("본문", "llm-model"));

        int failureThreshold = (int) (slidingWindowSize * (failureRateThreshold * 0.01));
        verify(httpClient, atMost(failureThreshold + 1)).send(any(), any());
    }

    @Test
    @DisplayName("비즈니스 흐름 상 정상인 예외에 대해서는 회로 차단을 하지 않는다.")
    void circuitBreakerTest_ignoresLogicalException() throws IOException, InterruptedException {
        HttpResponse<String> mockedHttpResponse = Mockito.mock(HttpResponse.class);
        doReturn(429).when(mockedHttpResponse).statusCode();
        doReturn("{\"error\":{}}").when(mockedHttpResponse).body();
        doReturn(mockedHttpResponse).when(httpClient).send(any(), any());

        executeNTimesIgnoringException(slidingWindowSize, () -> aiSummaryClient.summarize("본문", "llm-model"));

        verify(httpClient, times(slidingWindowSize)).send(any(), any());
    }

    private void executeNTimesIgnoringException(int times, Runnable r) {
        for (int i = 0; i < times; i++) {
            try {
                r.run();
            } catch (Exception ignored) {
            }
        }
    }
}
