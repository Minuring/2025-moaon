package moaon.backend.article.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.techStack.domain.TechStack;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static moaon.backend.article.domain.QArticle.article;

@RequiredArgsConstructor
class ArticleRetrievalQuery {

    private static final int FETCH_EXTRA_FOR_HAS_NEXT = 1;
    private static final String BLANK = " ";

    /**
     * title·summary와 content를 각자의 FULLTEXT 인덱스로 검색한 뒤 article id별로 점수를 합산한다.
     * 두 점수를 한 식으로 더해 비교하면 어느 인덱스도 타지 못해 전체 스캔이 되므로 따로 구해 합친다.
     * FULLTEXT 점수는 0 이상이라 합산 결과는 "MATCH(title, summary) + MATCH(content) > 0"과 같은 집합·점수를 갖는다.
     */
    private static final String MATCHED_SCORES = """
            SELECT id, SUM(score) AS score
            FROM (
                SELECT id, MATCH(title, summary) AGAINST(:keyword IN BOOLEAN MODE) AS score
                FROM article
                WHERE MATCH(title, summary) AGAINST(:keyword IN BOOLEAN MODE) > 0
                UNION ALL
                SELECT id, MATCH(content) AGAINST(:keyword IN BOOLEAN MODE) AS score
                FROM article_content
                WHERE MATCH(content) AGAINST(:keyword IN BOOLEAN MODE) > 0
            ) matched_by_column
            GROUP BY id
            """;

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager em;

    public List<Article> search(ArticleQueryCondition condition) {
        return search(condition, null);
    }

    public List<Article> search(ArticleQueryCondition condition, @Nullable Long projectId) {
        SearchKeyword search = condition.search();
        return search != null
                ? fetchWithScore(condition, projectId, search)
                : fetchWithoutScore(condition, projectId);
    }

    private List<Article> fetchWithoutScore(ArticleQueryCondition condition, Long projectId) {
        return jpaQueryFactory
                .selectFrom(article)
                .where(
                        baseConditions(condition, projectId),
                        cursorClause(condition)
                )
                .orderBy(toOrderBy(condition.sortType()))
                .limit(condition.limit() + FETCH_EXTRA_FOR_HAS_NEXT)
                .fetch();
    }

    private List<Article> fetchWithScore(ArticleQueryCondition condition, Long projectId, SearchKeyword search) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", formatSearchKeyword(search));
        List<String> where = searchFilters(condition, projectId, params);
        ArticleSortType sortType = condition.sortType();
        boolean needsArticle = !where.isEmpty() || sortType != ArticleSortType.RELEVANCE;
        String idColumn = needsArticle ? "a.id" : "m.id";

        if (condition.hasCursor()) {
            where.add(searchCursorClause(condition.cursor(), sortType, idColumn, params));
        }

        String sql = "SELECT " + idColumn + ", m.score FROM (" + MATCHED_SCORES + ") m"
                + (needsArticle ? " JOIN article a ON a.id = m.id" : "")
                + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                + " ORDER BY " + searchSortColumn(sortType) + " DESC, " + idColumn + " DESC";
        Query query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);
        query.setMaxResults(condition.limit() + FETCH_EXTRA_FOR_HAS_NEXT);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<Long> ids = rows.stream().map(row -> ((Number) row[0]).longValue()).toList();
        Map<Long, Article> articlesById = jpaQueryFactory
                .selectFrom(article)
                .where(article.id.in(ids))
                .fetch()
                .stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));

        return rows.stream()
                .map(row -> {
                    Article found = articlesById.get(((Number) row[0]).longValue());
                    found.setScore(((Number) row[1]).doubleValue());
                    return found;
                })
                .toList();
    }

    public long count(ArticleQueryCondition condition, Long projectId) {
        if (condition.search() != null) {
            return countBySearch(condition, projectId);
        }
        if (hasFilter(condition, projectId)) {
            return countByIntersection(condition, projectId);
        }

        Long count = jpaQueryFactory
                .select(article.count())
                .from(article)
                .where(baseConditions(condition, projectId))
                .fetchOne();
        return count == null ? 0 : count;
    }

    private long countBySearch(ArticleQueryCondition condition, Long projectId) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", formatSearchKeyword(condition.search()));
        List<String> where = searchFilters(condition, projectId, params);

        String sql = "SELECT COUNT(*) FROM (" + MATCHED_SCORES + ") m"
                + (where.isEmpty() ? "" : " JOIN article a ON a.id = m.id WHERE " + String.join(" AND ", where));
        Query query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).longValue();
    }

    private List<String> searchFilters(ArticleQueryCondition condition, Long projectId, Map<String, Object> params) {
        List<String> where = new ArrayList<>();

        if (projectId != null) {
            where.add("a.project_id = :projectId");
            params.put("projectId", projectId);
        }
        if (condition.hasSector()) {
            where.add("a.sector = :sector");
            params.put("sector", condition.sector().name());
        }
        if (condition.hasTopics()) {
            List<String> topicNames = condition.topics().stream()
                    .map(Topic::name)
                    .distinct()
                    .toList();
            for (int i = 0; i < topicNames.size(); i++) {
                where.add("EXISTS (SELECT 1 FROM article_topics t WHERE t.article_id = a.id AND t.topics = :topic" + i + ")");
                params.put("topic" + i, topicNames.get(i));
            }
        }
        if (condition.hasTechStacks()) {
            List<String> techStackNames = condition.techStacks().stream()
                    .map(TechStack::getName)
                    .distinct()
                    .toList();
            for (int i = 0; i < techStackNames.size(); i++) {
                where.add("EXISTS (SELECT 1 FROM article_tech_stack ats JOIN tech_stack ts ON ts.id = ats.tech_stack_id"
                        + " WHERE ats.article_id = a.id AND ts.name = :techStack" + i + ")");
                params.put("techStack" + i, techStackNames.get(i));
            }
        }
        return where;
    }

    private String searchSortColumn(ArticleSortType sortType) {
        if (sortType == ArticleSortType.RELEVANCE) {
            return "m.score";
        }
        if (sortType == ArticleSortType.CLICKS) {
            return "a.clicks";
        }
        return "a.created_at";
    }

    private String searchCursorClause(ArticleCursor cursor, ArticleSortType sortType, String idColumn, Map<String, Object> params) {
        String sortColumn = searchSortColumn(sortType);
        if (sortType == ArticleSortType.RELEVANCE) {
            params.put("cursorValue", cursor.getSortValueAsDouble());
        } else if (sortType == ArticleSortType.CLICKS) {
            params.put("cursorValue", cursor.getSortValueAsInt());
        } else {
            params.put("cursorValue", cursor.getSortValueAsLocalDateTime());
        }
        params.put("cursorId", cursor.getLastId());
        return "(" + sortColumn + " < :cursorValue OR (" + sortColumn + " = :cursorValue AND " + idColumn + " < :cursorId))";
    }

    private boolean hasFilter(ArticleQueryCondition condition, Long projectId) {
        return projectId != null
                || condition.hasSector()
                || condition.hasTopics()
                || condition.hasTechStacks();
    }

    /**
     * 필터별 article_id 집합을 각 테이블의 커버링 인덱스로 구해 교집합을 센다.
     * 연관 테이블은 article FK라 article 조인 없이 직접 조회해도 결과가 같고, article 클러스터드 인덱스 접근을 피할 수 있다.
     */
    private long countByIntersection(ArticleQueryCondition condition, Long projectId) {
        List<String> idSetQueries = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (projectId != null) {
            idSetQueries.add("SELECT id FROM article WHERE project_id = :projectId");
            params.put("projectId", projectId);
        }
        if (condition.hasSector()) {
            idSetQueries.add("SELECT id FROM article WHERE sector = :sector");
            params.put("sector", condition.sector().name());
        }
        if (condition.hasTopics()) {
            List<String> topicNames = condition.topics().stream()
                    .map(Topic::name)
                    .distinct()
                    .toList();
            for (int i = 0; i < topicNames.size(); i++) {
                idSetQueries.add("SELECT article_id FROM article_topics WHERE topics = :topic" + i);
                params.put("topic" + i, topicNames.get(i));
            }
        }
        if (condition.hasTechStacks()) {
            List<String> techStackNames = condition.techStacks().stream()
                    .map(TechStack::getName)
                    .distinct()
                    .toList();
            for (int i = 0; i < techStackNames.size(); i++) {
                idSetQueries.add("SELECT ats.article_id FROM article_tech_stack ats"
                        + " JOIN tech_stack ts ON ts.id = ats.tech_stack_id WHERE ts.name = :techStack" + i);
                params.put("techStack" + i, techStackNames.get(i));
            }
        }

        Query query = em.createNativeQuery(
                "SELECT COUNT(*) FROM (" + String.join(" INTERSECT ", idSetQueries) + ") matched");
        params.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).longValue();
    }

    private BooleanBuilder baseConditions(ArticleQueryCondition condition, Long projectId) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(projectIdEq(projectId));
        where.and(sectorEq(condition));
        where.and(containsAllTopics(condition));
        where.and(hasAllTechStacks(condition));
        return where;
    }

    private BooleanExpression projectIdEq(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return article.project.id.eq(projectId);
    }

    private BooleanExpression sectorEq(ArticleQueryCondition condition) {
        if (!condition.hasSector()) {
            return null;
        }
        return article.sector.eq(condition.sector());
    }

    private BooleanExpression containsAllTopics(ArticleQueryCondition condition) {
        if (!condition.hasTopics()) {
            return null;
        }
        BooleanExpression where = null;
        for (Topic topic : condition.topics()) {
            BooleanExpression expression = article.topics.contains(topic);
            where = where == null ? expression : where.and(expression);
        }
        return where;
    }

    private BooleanExpression hasAllTechStacks(ArticleQueryCondition condition) {
        if (!condition.hasTechStacks()) {
            return null;
        }
        BooleanExpression where = null;
        for (TechStack techStack : condition.techStacks()) {
            BooleanExpression expression = article.techStacks.any().techStack.name.eq(techStack.getName());
            where = where == null ? expression : where.and(expression);
        }
        return where;
    }

    private String formatSearchKeyword(SearchKeyword searchKeyword) {
        String replaced = searchKeyword.replaceSpecialCharacters(BLANK);
        return Arrays.stream(replaced.split(BLANK))
                .map(this::applyBooleanModeExpression)
                .collect(Collectors.joining(BLANK));
    }

    private String applyBooleanModeExpression(String keyword) {
        if (keyword.length() == 1) {
            return keyword + "*";
        }
        return "+" + keyword.toLowerCase() + "*";
    }

    private BooleanExpression cursorClause(ArticleQueryCondition condition) {
        if (!condition.hasCursor()) {
            return null;
        }
        ArticleCursor cursor = condition.cursor();

        if (condition.sortType() == ArticleSortType.CLICKS) {
            int sortValue = cursor.getSortValueAsInt();
            return article.clicks.lt(sortValue)
                    .or(article.clicks.eq(sortValue).and(article.id.lt(cursor.getLastId())));
        }

        LocalDateTime sortValue = cursor.getSortValueAsLocalDateTime();
        return article.createdAt.lt(sortValue)
                .or(article.createdAt.eq(sortValue).and(article.id.lt(cursor.getLastId())));
    }

    private OrderSpecifier<?>[] toOrderBy(ArticleSortType sortType) {
        if (sortType == ArticleSortType.CLICKS) {
            return new OrderSpecifier<?>[]{article.clicks.desc(), article.id.desc()};
        }
        return new OrderSpecifier<?>[]{article.createdAt.desc(), article.id.desc()};
    }
}
