package moaon.backend.article.dto;

import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.draft.ArticleDraft;
import moaon.backend.project.domain.Project;
import moaon.backend.techStack.domain.TechStack;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleCreateParams(
        Long projectId,
        String title,
        String summary,
        List<TechStack> techStacks,
        Long draftId,
        Sector sector,
        List<Topic> topics
) {

    public Article createArticle(Project project, ArticleDraft draft) {
        return new Article(
                title,
                summary,
                draft.getUrl(),
                LocalDateTime.now(),
                project,
                sector,
                topics,
                techStacks
        );
    }
}
