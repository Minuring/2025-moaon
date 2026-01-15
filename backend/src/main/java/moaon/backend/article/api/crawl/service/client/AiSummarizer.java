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

    private static final String BLANK = "";
    private static final String FREE_MODEL = "meta-llama/llama-3.3-70b-instruct:free";
    private static final String NON_FREE_MODEL = "meta-llama/llama-3.3-70b-instruct";

    private final AiSummaryClient aiSummaryClient;

    public String summarize(String content, Member member) {
        if (member.isCrawlCountOvered()) {
            log.info("사용자 하루 토큰 횟수 한계입니다. memberId: {}", member.getId());
            return BLANK;
        }

        try {
            try {
                String summary = aiSummaryClient.summarize(content, FREE_MODEL);
                log.info("아티클을 요약했습니다. model: {}, memberId: {}", FREE_MODEL, member.getId());
                return summary;

            } catch (AiNoCostException | AiSummaryFailedException e) {
                try {
                    String summary = aiSummaryClient.summarize(content, NON_FREE_MODEL);
                    log.info("아티클을 요약했습니다. model: {}, memberId: {}", NON_FREE_MODEL, member.getId());
                    return summary;

                } catch (AiNoCostException e1) {
                    log.warn("토큰 사용량이 한계에 달해 요약에 실패했습니다. model: {}", NON_FREE_MODEL);
                    return BLANK;

                } catch (AiSummaryFailedException e1) {
                    log.error("AI 요약에 실패했습니다. model:{}, status code: {}, message: {}",
                            NON_FREE_MODEL, e1.getResponseStatusCode(), e1.getResponseMessage());
                    return BLANK;
                }
            }

        } catch (Exception e) {
            log.error("아티클 요약에 실패했습니다.", e);
            return BLANK;
        }
    }
}
