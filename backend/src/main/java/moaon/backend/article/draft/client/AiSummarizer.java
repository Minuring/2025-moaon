package moaon.backend.article.draft.client;

import lombok.extern.slf4j.Slf4j;
import moaon.backend.member.Member;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AiSummarizer {

    private final AiSummaryClient aiSummaryClient;
    private final String model;

    public AiSummarizer(AiSummaryClient aiSummaryClient, @Value("${gpt.model}") String model) {
        this.aiSummaryClient = aiSummaryClient;
        this.model = model;
    }

    public AiSummarization summarize(String content, Member member) {
        if (member.isCrawlCountOvered()) {
            log.info("사용자 하루 토큰 횟수 한계입니다. memberId: {}", member.getId());
            return AiSummarization.nothing();
        }

        try {
            AiSummarization summarization = aiSummaryClient.summarize(content, model);
            log.info("아티클을 요약했습니다. model: {}, memberId: {}", model, member.getId());
            return summarization;

        } catch (AiNoCostException e) {
            log.warn("토큰 사용량이 한계에 달해 요약에 실패했습니다. model: {}", model);
            return AiSummarization.nothing();

        } catch (AiSummaryFailedException e) {
            log.error("AI 요약에 실패했습니다. model:{}, status code: {}, message: {}",
                    model, e.getResponseStatusCode(), e.getResponseMessage());
            return AiSummarization.nothing();

        } catch (Exception e) {
            log.error("아티클 요약에 실패했습니다.", e);
            return AiSummarization.nothing();
        }
    }
}
