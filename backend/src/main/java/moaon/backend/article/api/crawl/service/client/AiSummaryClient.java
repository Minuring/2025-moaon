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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.api.crawl.exception.AiNoCostException;
import moaon.backend.article.api.crawl.exception.AiSummaryFailedException;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
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
            반드시 다음 형식의 단일 JSON 객체만 출력하라:
            {
                "summary":"...",
                "sector": "..",
                "topics": "...",
                "techstacks": "..."
            }
            텍스트 블록, 마크다운 문법 등 JSON 외 텍스트를 절대 출력하지 마라.
            
            summary 규칙:
            - 한글 요약문
            - 공백 포함 150~200자 반드시 준수
            - 큰따옴표 금지
            
            sector 규칙:
            - 프론트엔드와 관련된 문서인 경우 FE
            - 백엔드와 관련된 문서의 경우 BE
            - 안드로이드와 관련된 문서의 경우 ANDROID
            - iOS와 관련된 문서의 경우 IOS
            - 클라우드, 아키텍처와 관련된 문서의 경우 INFRA
            - 기술과 관련된 문서가 아닌 경우 NON_TECH
            
            topics 규칙:
            - sector가 FE인 경우 TECHNOLOGY_ADOPTION, TROUBLESHOOTING, PERFORMANCE_OPTIMIZATION, TESTING, CODE_QUALITY, STATE_MANAGEMENT, UI_UX_IMPROVEMENT, BUNDLING, ETC 중 1~3개
            - sector가 BE인 경우 TECHNOLOGY_ADOPTION, TROUBLESHOOTING, PERFORMANCE_OPTIMIZATION, TESTING, CODE_QUALITY, SECURITY, ARCHITECTURE_DESIGN, API_DESIGN, DATABASE, DEPLOYMENT_AND_OPERATION, ETC 중 1~3개
            - sector가 ANDROID인 경우 BUILD, NATIVE, SDK, TECHNOLOGY_ADOPTION, TROUBLESHOOTING, PERFORMANCE_OPTIMIZATION, TESTING, CODE_QUALITY, UI_UX_IMPROVEMENT, ARCHITECTURE_DESIGN, ETC 중 1~3개
            - sector가 IOS인 경우 BUILD, NATIVE, SDK, TECHNOLOGY_ADOPTION, TROUBLESHOOTING, PERFORMANCE_OPTIMIZATION, TESTING, CODE_QUALITY, UI_UX_IMPROVEMENT, ARCHITECTURE_DESIGN, ETC 중 1~3개
            - sector가 INFRA인 경우 TECHNOLOGY_ADOPTION, TROUBLESHOOTING, PERFORMANCE_OPTIMIZATION, SECURITY, CI_CD, MONITORING_AND_LOGGING, NETWORK, ETC 중 1~3개
            - sector가 NON_TECH인 경우 TEAM_CULTURE, RETROSPECTIVE, PLANNING, DESIGN, ETC 중 1~3개
            
            techstacks 규칙:
            - 해당 문서에서 다루는 기술 스택 1~3개 ex) "java,spring"
            - sector가 NON_TECH인 경우 empty
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
    public AiSummarization summarize(String content, String model) {
        if (content == null || content.isEmpty()) {
            return AiSummarization.nothing();
        }

        try {
            String requestBody = createRequestBody(model, content);
            HttpRequest request = createHttpRequest(requestBody);
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(JsonExtractor.extractJsonObject(response.body()));
            checkStatusCode(response.statusCode(), root.path("error"));

            JsonNode contentNode = root.path("choices").get(0).path("message").path("content");
            JsonNode parsedContent = objectMapper.readTree(contentNode.asText());
            return createSummarization(parsedContent);

        } catch (IOException | InterruptedException e) {
            log.error("AI 요약 API 연결에서 실패했습니다.", e);
            throw new CustomException(ErrorCode.ARTICLE_CRAWL_FAILED, e);
        }
    }

    private AiSummarization fallback(Exception e) {
        return AiSummarization.nothing();
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

    private AiSummarization createSummarization(final JsonNode parsedContent) {
        String summary = parsedContent.get("summary").asText("");
        Sector sector = Sector.of(parsedContent.get("sector").asText(""));
        List<Topic> topics = extractListValue(parsedContent.get("topics")).stream()
                .map(String::toUpperCase)
                .map(Topic::valueOf)
                .toList();
        List<String> techStacks = extractListValue(parsedContent.get("techstacks"));
        return new AiSummarization(summary, sector, topics, techStacks);
    }

    private List<String> extractListValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return List.of();
        }
        if (node.isArray()) {
            return node.valueStream()
                    .map(n -> n.asText("").trim())
                    .filter(s -> !s.isBlank())
                    .toList();
        }

        String text = node.asText("").trim();
        if (text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
