package moaon.backend.article.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.Topic;
import moaon.backend.search.query.ArticleDocument;
import moaon.backend.techStack.domain.TechStack;

public record ArticleDto(
        Long id,
        Long projectId,
        String projectTitle,
        int clicks,
        String title,
        String summary,
        List<String> techStacks,
        String url,
        String sector,
        List<String> topics,
        LocalDateTime createdAt,
        List<String> highlightTitle,
        List<String> highlightSummary
) {

    public static ArticleDto from(Article article) {
        return new ArticleDto(
                article.getId(),
                article.getProject().getId(),
                article.getProject().getTitle(),
                article.getClicks(),
                article.getTitle(),
                article.getSummary(),
                article.getTechStacks().stream().map(TechStack::getName).toList(),
                article.getArticleUrl(),
                article.getSector().getName(),
                article.getTopics().stream().map(Topic::getName).toList(),
                article.getCreatedAt(),
                null,
                null
        );
    }

    public static ArticleDto from(ArticleDocument document) {
        return ArticleDto.from(
                document,
                Map.of("title", List.of(""),
                        "summary", List.of(""),
                        "content", List.of(""))
        );
    }

    public static ArticleDto from(ArticleDocument document, Map<String, List<String>> highlights) {
        return new ArticleDto(
                document.getId(),
                document.getProjectId(),
                document.getProjectTitle(),
                document.getClicks(),
                document.getTitle(),
                document.getSummary(),
                List.copyOf(document.getTechStacks()),
                document.getUrl(),
                document.getSector().getName(),
                document.getTopics().stream().map(Topic::getName).toList(),
                document.getCreatedAt(),
                highlights.get("title"),
                highlights.get("summary")
        );
    }
}
