package moaon.backend.article.api.crawl.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.api.crawl.exception.AiNoCostException;
import moaon.backend.article.api.crawl.exception.AiSummaryFailedException;
import moaon.backend.member.domain.Member;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiSummarizer {

    private static final String MODEL = "google/gemini-2.0-flash-001";

    private final AiSummaryClient aiSummaryClient;

    public AiSummarization summarize(String content, Member member) {
        if (member.isCrawlCountOvered()) {
            log.info("사용자 하루 토큰 횟수 한계입니다. memberId: {}", member.getId());
            return AiSummarization.nothing();
        }

        try {
            AiSummarization summarization = aiSummaryClient.summarize(content, MODEL);
            log.info("아티클을 요약했습니다. model: {}, memberId: {}", MODEL, member.getId());
            return summarization;

        } catch (AiNoCostException e) {
            log.warn("토큰 사용량이 한계에 달해 요약에 실패했습니다. model: {}", MODEL);
            return AiSummarization.nothing();

        } catch (AiSummaryFailedException e) {
            log.error("AI 요약에 실패했습니다. model:{}, status code: {}, message: {}",
                    MODEL, e.getResponseStatusCode(), e.getResponseMessage());
            return AiSummarization.nothing();

        } catch (Exception e) {
            log.error("아티클 요약에 실패했습니다.", e);
            return AiSummarization.nothing();
        }
    }
}
