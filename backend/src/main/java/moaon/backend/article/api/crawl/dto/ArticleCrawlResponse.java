package moaon.backend.article.api.crawl.dto;

import moaon.backend.member.domain.Member;

public record ArticleCrawlResponse(
        String title,
        String summary,
        String sector,
        String topics,
        String techstacks,
        int remainingCount
) {

    public static ArticleCrawlResponse from(ArticleCrawlResult crawlResult, Member member) {
        return new ArticleCrawlResponse(
                crawlResult.title(),
                crawlResult.summary(),
                crawlResult.sector(),
                String.join(",", crawlResult.topics()),
                String.join(",", crawlResult.techStacks()),
                member.getTodayRemainingTokens()
        );
    }
}
