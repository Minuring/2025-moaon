package moaon.backend.article.draft.dto;

import java.util.stream.Collectors;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.draft.ArticleDraft;
import moaon.backend.member.Member;

public record ArticleDraftAnalyzeResponse(
        String summary,
        String sector,
        String topics,
        String techstacks,
        int remainingCount
) {

    public static ArticleDraftAnalyzeResponse from(ArticleDraft draft, Member member) {
        String summary = draft.getAnalyzedSummary() == null ? "" : draft.getAnalyzedSummary();
        String sector = draft.getAnalyzedSector() == null ? "" : draft.getAnalyzedSector().getName();
        String topics = draft.getAnalyzedTopics().stream()
                .map(Topic::getName)
                .collect(Collectors.joining(","));
        String techstacks = String.join(",", draft.getAnalyzedTechStacks());
        return new ArticleDraftAnalyzeResponse(summary, sector, topics, techstacks, member.getTodayRemainingTokens());
    }
}
