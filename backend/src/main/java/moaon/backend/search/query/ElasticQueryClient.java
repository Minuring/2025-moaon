package moaon.backend.search.query;

import lombok.RequiredArgsConstructor;
import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.dto.ArticleDto;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.repository.ArticleSearchResult;
import moaon.backend.global.util.Parsers;
import moaon.backend.project.domain.Project;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ElasticQueryClient {

    private static final IndexCoordinates ARTICLE_ALIAS = IndexCoordinates.of("articles");

    private final ElasticsearchOperations ops;

    public ArticleSearchResult search(ArticleQueryCondition condition) {
        NativeQuery esArticleQuery = new ElasticQueryBuilder()
                .withQueryCondition(condition)
                .build();
        SearchHits<ArticleDocument> searchHits = ops.search(esArticleQuery, ArticleDocument.class, ARTICLE_ALIAS);
        return wrapSearchHits(condition, searchHits);
    }

    public ArticleSearchResult searchInProject(Project project, ArticleQueryCondition condition) {
        NativeQuery esArticleQuery = new ElasticQueryBuilder()
                .withIds(project.getArticleIds())
                .withQueryCondition(condition)
                .build();

        SearchHits<ArticleDocument> searchHits = ops.search(esArticleQuery, ArticleDocument.class, ARTICLE_ALIAS);
        return wrapSearchHits(condition, searchHits);
    }

    public ArticleDocument save(ArticleDocument articleDocument) {
        return ops.withRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .save(articleDocument, ARTICLE_ALIAS);
    }

    private ArticleSearchResult wrapSearchHits(
            ArticleQueryCondition condition,
            SearchHits<ArticleDocument> searchHits
    ) {
        List<ArticleDto> documents = searchHits.getSearchHits().stream()
                .map(sh -> ArticleDto.from(sh.getContent(), sh.getHighlightFields()))
                .toList();
        boolean hasNext = documents.size() == condition.limit();
        ArticleCursor nextCursor = hasNext ? buildCursor(searchHits) : null;
        return new ArticleSearchResult(documents, searchHits.getTotalHits(), hasNext, nextCursor);
    }

    private ArticleCursor buildCursor(SearchHits<ArticleDocument> searchHits) {
        List<Object> sortValues = searchHits.getSearchHits().getLast().getSortValues();
        Object sortValue = sortValues.get(0);
        Long id = Parsers.parseLong(sortValues.get(1).toString());
        return new ArticleCursor(sortValue, id);
    }
}
