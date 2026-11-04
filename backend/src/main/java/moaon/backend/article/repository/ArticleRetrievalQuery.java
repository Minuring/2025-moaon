package moaon.backend.article.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
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
import java.util.stream.Collectors;

import static moaon.backend.article.domain.QArticle.article;
import static moaon.backend.article.domain.QArticleContent.articleContent;
import static moaon.backend.project.domain.QProject.project;

@RequiredArgsConstructor
class ArticleRetrievalQuery {

    private static final double MINIMUM_MATCH_SCORE = 0.0;
    private static final int FETCH_EXTRA_FOR_HAS_NEXT = 1;
    private static final String BLANK = " ";

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
                .join(article.project, project).fetchJoin()
                .where(
                        baseConditions(condition, projectId),
                        cursorClause(condition, null)
                )
                .orderBy(toOrderBy(condition.sortType(), null))
                .limit(condition.limit() + FETCH_EXTRA_FOR_HAS_NEXT)
                .fetch();
    }

    private List<Article> fetchWithScore(ArticleQueryCondition condition, Long projectId, SearchKeyword search) {
        NumberExpression<Double> score = matchScore(search);

        List<Tuple> tuples = jpaQueryFactory
                .select(article, score)
                .from(article)
                .join(articleContent).on(articleContent.id.eq(article.id))
                .where(
                        baseConditions(condition, projectId),
                        cursorClause(condition, search)
                )
                .orderBy(toOrderBy(condition.sortType(), search))
                .limit(condition.limit() + FETCH_EXTRA_FOR_HAS_NEXT)
                .fetch();

        return tuples.stream()
                .map(tuple -> {
                    Article found = tuple.get(article);
                    found.setScore(tuple.get(score));
                    return found;
                })
                .toList();
    }

    public long count(ArticleQueryCondition condition, Long projectId) {
        if (condition.search() == null && hasFilter(condition, projectId)) {
            return countByIntersection(condition, projectId);
        }

        JPAQuery<Long> query = jpaQueryFactory
                .select(article.count())
                .from(article);

        if (condition.search() != null) {
            query.join(articleContent).on(articleContent.id.eq(article.id));
        }

        Long count = query
                .where(baseConditions(condition, projectId))
                .fetchOne();
        return count == null ? 0 : count;
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
        if (condition.search() != null) {
            where.and(matchScore(condition.search()).gt(MINIMUM_MATCH_SCORE));
        }
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

    private NumberExpression<Double> matchScore(SearchKeyword search) {
        return Expressions.numberTemplate(
                Double.class,
                ArticleFullTextSearchHQLFunction.EXPRESSION_TEMPLATE,
                formatSearchKeyword(search)
        );
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

    private BooleanExpression cursorClause(ArticleQueryCondition condition, @Nullable SearchKeyword search) {
        if (!condition.hasCursor()) {
            return null;
        }
        ArticleCursor cursor = condition.cursor();
        ArticleSortType sortType = condition.sortType();

        if (sortType == ArticleSortType.CLICKS) {
            int sortValue = cursor.getSortValueAsInt();
            return article.clicks.lt(sortValue)
                    .or(article.clicks.eq(sortValue).and(article.id.lt(cursor.getLastId())));
        }

        if (sortType == ArticleSortType.RELEVANCE && search != null) {
            double sortValue = cursor.getSortValueAsDouble();
            NumberExpression<Double> score = matchScore(search);
            return score.lt(sortValue)
                    .or(score.eq(sortValue).and(article.id.lt(cursor.getLastId())));
        }

        LocalDateTime sortValue = cursor.getSortValueAsLocalDateTime();
        return article.createdAt.lt(sortValue)
                .or(article.createdAt.eq(sortValue).and(article.id.lt(cursor.getLastId())));
    }

    private OrderSpecifier<?>[] toOrderBy(ArticleSortType sortType, @Nullable SearchKeyword search) {
        if (sortType == ArticleSortType.RELEVANCE && search != null) {
            NumberExpression<Double> score = matchScore(search);
            return new OrderSpecifier<?>[]{score.desc(), article.id.desc()};
        }
        if (sortType == ArticleSortType.CLICKS) {
            return new OrderSpecifier<?>[]{article.clicks.desc(), article.id.desc()};
        }
        return new OrderSpecifier<?>[]{article.createdAt.desc(), article.id.desc()};
    }
}
