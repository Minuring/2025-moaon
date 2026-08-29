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
import lombok.RequiredArgsConstructor;
import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.techStack.domain.TechStack;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static moaon.backend.article.domain.QArticle.article;
import static moaon.backend.article.domain.QArticleContent.articleContent;
import static moaon.backend.project.domain.QProject.project;

/**
 * ES 검색이 실패했을 때만 사용되는 DB 폴백 검색. FilteringIds로 필터별 쿼리를 쪼개는 튜닝은 하지 않고,
 * 동적 조건을 담은 단일 쿼리로 최대한 단순하게 구현한다.
 */
@RequiredArgsConstructor
class ArticleRetrievalQuery {

    private static final double MINIMUM_MATCH_SCORE = 0.0;
    private static final int FETCH_EXTRA_FOR_HAS_NEXT = 1;
    private static final String BLANK = " ";

    private final JPAQueryFactory jpaQueryFactory;

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
            return new OrderSpecifier<?>[]{score.desc(), article.id.asc()};
        }
        if (sortType == ArticleSortType.CLICKS) {
            return new OrderSpecifier<?>[]{article.clicks.desc(), article.id.asc()};
        }
        return new OrderSpecifier<?>[]{article.createdAt.desc(), article.id.asc()};
    }
}
