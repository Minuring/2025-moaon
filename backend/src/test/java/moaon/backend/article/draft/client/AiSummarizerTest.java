package moaon.backend.article.draft.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.net.URL;
import java.net.http.HttpClient;
import java.time.Duration;
import moaon.backend.article.domain.Sector;
import moaon.backend.member.Member;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiSummarizerTest {

    private final AiSummaryClient aiSummaryClient = Mockito.mock(AiSummaryClient.class);
    private final AiSummarizer summarizer = new AiSummarizer(aiSummaryClient, "google/gemini-2.5-flash");

    @DisplayName("한 사용자가 하루 20번 초과 크롤링시 AI요약 없이 빈 summary를 반환한다.")
    @Test
    void crawlCountOver() {
        // given
        Member member = new Member(1L, "socialId", "abc@gmail.com", "poopo", 20);

        // when
        AiSummarization summarization = summarizer.summarize("본문", member);

        // then
        assertThat(summarization.isBlank()).isTrue();
        verify(aiSummaryClient, never()).summarize(eq("본문"), any());
    }

    @Test
    @Disabled
    void summarize() throws Exception {
        // given
        URL normalLink = new URL("https://velog.io/@jackjack/DB-%EA%B5%AC%EC%A1%B0-%EB%B0%8F-%EC%84%A4%EA%B3%84-RDB-vs-NoSQL");
        FinderCrawlResult crawlResult = new TistoryContentFinder(1000L).crawl(normalLink);

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        AiSummaryClient aiSummaryClient = new AiSummaryClient("api key here", httpClient, 5000L);
        AiSummarizer aiSummarizer = new AiSummarizer(aiSummaryClient, "google/gemini-2.5-flash");

        // when
        Member member = new Member(1L, "socialId", "abc@gmail.com", "poopo", 0);
        AiSummarization summarization = aiSummarizer.summarize(crawlResult.content(), member);

        // then
        assertAll(
                () -> assertThat(summarization.summary()).isNotEmpty(),
                () -> assertThat(summarization.summary().length()).isLessThanOrEqualTo(250),
                () -> assertThat(summarization.sector()).isInstanceOf(Sector.class),
                () -> assertThat(summarization.topics()).hasSizeBetween(1, 3),
                () -> assertThat(summarization.techStacks()).hasSizeBetween(1, 3)
        );
    }
}
