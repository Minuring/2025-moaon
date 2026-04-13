package moaon.backend.project.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import moaon.backend.article.domain.Sector;
import moaon.backend.article.dto.ArticleDto;

public record ProjectArticleResponse(
        List<ArticleSectorCount> counts,
        List<ArticleDto> articles
) {

    public static ProjectArticleResponse of(
            List<ArticleDto> articles,
            Map<Sector, Long> articleCountBySector
    ) {
        List<ArticleSectorCount> articleSectorCounts = new ArrayList<>(articleCountBySector.entrySet().stream()
                .map(entry -> ArticleSectorCount.of(entry.getKey(), entry.getValue()))
                .toList());
        articleSectorCounts.add(ArticleSectorCount.all(articleCountBySector));
        return new ProjectArticleResponse(articleSectorCounts, articles);
    }

    public record ArticleSectorCount(
            String sector,
            long count
    ) {

        public static ArticleSectorCount of(Sector sector, long count) {
            return new ArticleSectorCount(sector.getName(), count);
        }

        public static ArticleSectorCount all(Map<Sector, Long> articleCountBySector) {
            long totalCount = articleCountBySector.values().stream()
                    .mapToLong(Long::longValue)
                    .sum();

            return new ArticleSectorCount("all", totalCount);
        }
    }
}
