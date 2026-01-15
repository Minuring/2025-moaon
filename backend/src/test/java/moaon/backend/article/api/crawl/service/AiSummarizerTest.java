package moaon.backend.article.api.crawl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.net.URL;
import java.net.http.HttpClient;
import java.time.Duration;
import moaon.backend.article.api.crawl.dto.FinderCrawlResult;
import moaon.backend.article.api.crawl.service.client.AiSummarizer;
import moaon.backend.article.api.crawl.service.client.AiSummaryClient;
import moaon.backend.article.api.crawl.service.client.TistoryContentFinder;
import moaon.backend.member.domain.Member;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiSummarizerTest {

    private final AiSummaryClient aiSummaryClient = Mockito.mock(AiSummaryClient.class);
    private final AiSummarizer summarizer = new AiSummarizer(aiSummaryClient);

    @DisplayName("한 사용자가 하루 20번 초과 크롤링시 AI요약 없이 빈 summary를 반환한다.")
    @Test
    void crawlCountOver() {
        // given
        Member member = new Member(1L, "socialId", "abc@gmail.com", "poopo", 20);

        // when
        String summary = summarizer.summarize("본문", member);

        // then
        assertThat(summary).isEmpty();
        verify(aiSummaryClient, never()).summarize(eq("본문"), any());
    }

    @Test
    @Disabled
    void summarize() throws Exception {
        // given
        URL normalLink = new URL("https://tempdev.tistory.com/19");
        FinderCrawlResult crawlResult = new TistoryContentFinder(1000L).crawl(normalLink);

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        AiSummaryClient aiSummaryClient = new AiSummaryClient("api key here", httpClient, 5000L);
        AiSummarizer aiSummarizer = new AiSummarizer(aiSummaryClient);

        // when
        Member member = new Member(1L, "socialId", "abc@gmail.com", "poopo", 0);
        String summary = aiSummarizer.summarize(crawlResult.content(), member);

        // then
        assertAll(
                () -> assertThat(summary).isNotEmpty(),
                () -> assertThat(summary.length()).isLessThanOrEqualTo(200)
        );
    }
}
