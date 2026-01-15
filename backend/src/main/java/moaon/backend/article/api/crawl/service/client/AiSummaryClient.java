package moaon.backend.article.api.crawl.service.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.api.crawl.exception.AiNoCostException;
import moaon.backend.article.api.crawl.exception.AiSummaryFailedException;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.global.util.JsonExtractor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiSummaryClient {

    private static final URI OPEN_ROUTER_URI = URI.create("https://openrouter.ai/api/v1/chat/completions");

    private static final String SYSTEM_PROMPT = """
            반드시 다음 형식의 JSON 객체만 출력하라:
            {"summary":"..."}
            JSON 외 텍스트를 절대 출력하지 마라.
            
            summary 규칙:
            - 모든 문자는 한글
            - 공백 포함 200자 이하
            - 170~200자
            - 핵심 주제/문제/해결 포함
            - 쉼표, 마침표 외 문장부호 금지
            - 출력 직전 글자 수 확인 후 필요 시 재작성
            """;

    private final String apiKey;
    private final HttpClient httpClient;
    private final long readTimeoutMillis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiSummaryClient(
            @Value("${gpt.api-key}") String apiKey,
            @Qualifier("aiSummaryHttpClient") HttpClient client,
            @Value("${gpt.read-timeout-millis}") long readTimeoutMillis
    ) {
        this.apiKey = apiKey;
        this.httpClient = client;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @CircuitBreaker(name = "aiSummaryCircuitBreaker", fallbackMethod = "fallback")
    public String summarize(String content, String model) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        try {
            String requestBody = createRequestBody(model, content);
            HttpRequest request = createHttpRequest(requestBody);
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(JsonExtractor.extractJsonObject(response.body()));
            checkStatusCode(response.statusCode(), root.path("error"));

            JsonNode contentNode = root.path("choices").get(0).path("message").path("content");
            JsonNode parsedContent = objectMapper.readTree(contentNode.asText());
            return parsedContent.get("summary").asText("");

        } catch (IOException | InterruptedException e) {
            log.error("AI 요약 API 연결에서 실패했습니다.", e);
            throw new CustomException(ErrorCode.ARTICLE_CRAWL_FAILED, e);
        }
    }

    private String fallback(Exception e) {
        return "";
    }

    private String createRequestBody(final String model, final String userContent) throws JsonProcessingException {
        ArrayList<Object> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", userContent));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.2);
        requestBody.put("max_tokens", 500);
        if (!model.endsWith(":free")) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }
        return objectMapper.writeValueAsString(requestBody);
    }

    private HttpRequest createHttpRequest(final String requestJson) {
        return HttpRequest.newBuilder()
                .uri(OPEN_ROUTER_URI)
                .timeout(Duration.ofMillis(readTimeoutMillis))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(BodyPublishers.ofString(requestJson))
                .build();
    }

    private void checkStatusCode(final int statusCode, final JsonNode errorNode) {
        if (statusCode == 429 || statusCode == 402) {
            throw new AiNoCostException();
        }

        if (statusCode < 200 || statusCode >= 300) {
            JsonNode codeNode = errorNode.path("code");
            JsonNode messageNode = errorNode.path("message");
            String message = messageNode.asText();
            throw new AiSummaryFailedException(codeNode.asInt(), message);
        }
    }
}
