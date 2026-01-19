package moaon.backend.article.api.crawl.dto;

import java.util.Collections;
import java.util.List;
import moaon.backend.article.api.crawl.service.client.AiSummarization;
import moaon.backend.article.domain.Topic;

public record ArticleCrawlResult(
        String title,
        String summary,
        String content,
        String sector,
        List<String> topics,
        List<String> techStacks,
        boolean isSucceed
) {

    public static ArticleCrawlResult success(FinderCrawlResult crawlResult, AiSummarization aiSummarization) {
        return new ArticleCrawlResult(
                crawlResult.title(),
                enforceLength(aiSummarization.summary(), 0, 255),
                crawlResult.content(),
                aiSummarization.sector().getName(),
                aiSummarization.topics().stream().map(Topic::getName).toList(),
                aiSummarization.techStacks(),
                true
        );
    }

    public static ArticleCrawlResult withoutSummary(FinderCrawlResult result) {
        return new ArticleCrawlResult(result.title(), "", result.content(), "", Collections.emptyList(), Collections.emptyList(), false);
    }

    private static String enforceLength(String text, int min, int max) {
        if (text == null) {
            return "";
        }
        text = text.strip();
        if (text.length() <= max) {
            return text;
        }

        int lastSentenceEnd = text.lastIndexOf(".", max);
        if (lastSentenceEnd > min) {
            return text.substring(0, lastSentenceEnd + 1);
        }

        return text.substring(0, max);
    }
}
