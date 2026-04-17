package moaon.backend.search.query;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.dto.ArticleDto;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.repository.ArticleSearchResult;
import moaon.backend.global.parser.LongParser;
import moaon.backend.global.parser.Parser;
import moaon.backend.project.domain.Project;
import moaon.backend.search.log.domain.SearchHitLog;
import moaon.backend.search.log.domain.SearchLogCapture;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleDocumentRepository {

    private static final Parser<Long> ID_PARSER = new LongParser();
    private static final IndexCoordinates ARTICLE_ALIAS = IndexCoordinates.of("articles");

    private final ElasticsearchOperations ops;

    public SearchWithLog search(ArticleQueryCondition condition) {
        long startTime = System.currentTimeMillis();
        NativeQuery esArticleQuery = new ESArticleQueryBuilder()
                .withQueryCondition(condition)
                .build();
        SearchHits<ArticleDocument> searchHits = ops.search(esArticleQuery, ArticleDocument.class, ARTICLE_ALIAS);
        int queryTimeMs = (int) (System.currentTimeMillis() - startTime);

        ArticleSearchResult articleSearchResult = wrapSearchHits(condition, searchHits);

        String query = condition.search() != null ? condition.search().value() : null;

        List<SearchHitLog> hitLogs;
        if (query == null || query.isBlank()) {
            hitLogs = List.of();
        } else {
            hitLogs = new ArrayList<>();
            int rank = 1;
            for (SearchHit<ArticleDocument> hit : searchHits) {
                hitLogs.add(SearchHitLog.of(rank++, hit.getContent().getId(), hit.getContent().getTitle(),
                        hit.getScore(), hit.getHighlightFields()));
            }
        }

        SearchLogCapture searchLogCapture = new SearchLogCapture(
                query,
                (int) Math.min(searchHits.getTotalHits(), Integer.MAX_VALUE),
                queryTimeMs,
                hitLogs
        );

        return new SearchWithLog(articleSearchResult, searchLogCapture);
    }

    public ArticleSearchResult searchInProject(Project project, ArticleQueryCondition condition) {
        NativeQuery esArticleQuery = new ESArticleQueryBuilder()
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
        Long id = ID_PARSER.parse(sortValues.get(1).toString());
        return new ArticleCursor(sortValue, id);
    }
}
