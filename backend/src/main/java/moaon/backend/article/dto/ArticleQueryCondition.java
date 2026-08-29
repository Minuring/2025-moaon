package moaon.backend.article.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.techStack.domain.TechStack;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Builder
@Accessors(fluent = true)
@Getter
@EqualsAndHashCode
@ToString
public final class ArticleQueryCondition {

    private SearchKeyword search;
    private Sector sector;
    private List<Topic> topics;
    private List<TechStack> techStacks;
    private ArticleSortType sortType;
    @Builder.Default private int limit = 20;
    private ArticleCursor cursor;

    public ArticleQueryCondition(SearchKeyword search, Sector sector, List<Topic> topics, List<TechStack> techStacks, ArticleSortType sortType, int limit, ArticleCursor cursor) {
        boolean cannotKeepRelevance = ArticleSortType.RELEVANCE == sortType && search == null;
        if (cannotKeepRelevance) {
            sortType = ArticleSortType.CREATED_AT;
        }
        this.search = search;
        this.sector = sector;
        this.topics = topics;
        this.techStacks = techStacks;
        this.sortType = sortType;
        this.limit = limit;
        this.cursor = cursor;
    }

    public ArticleQueryCondition() {
        this(null, null, null, null, null, 20, null);
    }

    public boolean hasSector() {
        return sector != null;
    }

    public boolean hasTopics() {
        return !CollectionUtils.isEmpty(topics);
    }

    public boolean hasTechStacks() {
        return !CollectionUtils.isEmpty(techStacks);
    }

    public boolean hasCursor() {
        return cursor != null;
    }
}
