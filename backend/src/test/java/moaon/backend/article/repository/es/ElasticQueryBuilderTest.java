package moaon.backend.search.query;

import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.techStack.domain.TechStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ElasticQueryBuilderTest {

    @DisplayName("Article ID 목록으로 Filter 쿼리 안에 Terms 쿼리를 만든다.")
    @Test
    void withIds_addsTermsFilter() {
        NativeQuery query = new ElasticQueryBuilder()
                .withIds(List.of(1L, 2L, 3L))
                .build();

        // then
        assertThat(query.getQuery().toString()).containsSubsequence("filter", "terms", "id", "[1,2,3]");
    }

    @DisplayName("검색어를 포함하면 multi_match 쿼리에 추가된다.")
    @Test
    void withTextSearch_addsMustQuery() {
        NativeQuery query = new ElasticQueryBuilder()
                .withTextSearch(new SearchKeyword("버저닝"))
                .build();

        assertThat(query.getQuery().toString())
                .containsSubsequence("multi_match", "query", "버저닝");
    }

    @DisplayName("검색어가 비어있으면 multi_match 쿼리를 추가하지 않는다.")
    @Test
    void withEmptyText_throwsException() {
        NativeQuery query = new ElasticQueryBuilder()
                .withTextSearch(null)
                .build();

        assertThat(query.getQuery().toString()).doesNotContain("multi_match", "query");
    }

    @DisplayName("Sector 필터를 추가하면 Filter 쿼리 안에 Term 쿼리를 만든다.")
    @Test
    void withSector_addsFilter() {
        NativeQuery query = new ElasticQueryBuilder()
                .withSector(Sector.FE)
                .build();

        assertThat(query.getQuery().toString())
                .containsSubsequence("filter", "term", "sector", "FE");
    }

    @DisplayName("Topics 필터를 AND조건으로 추가하면 Filter 조건 안에 Term쿼리로 각 토픽을 추가한다.")
    @Test
    void withTopicsAndMatch_addsFilter() {
        NativeQuery query = new ElasticQueryBuilder()
                .withTopicsAndMatch(List.of(Topic.DATABASE, Topic.API_DESIGN))
                .build();

        assertThat(query.getQuery().toString())
                .containsSubsequence("filter", "term", "topics", "DATABASE", "term", "topics", "API_DESIGN");
    }

    @DisplayName("Topics 필터를 OR조건으로 추가하면 Filter - Should 조건 안에 Term쿼리로 각 토픽을 추가한다.")
    @Test
    void withTopicsORMatch_addsFilter() {
        NativeQuery query = new ElasticQueryBuilder()
                .withTopicsOrMatch(List.of(Topic.DATABASE, Topic.API_DESIGN))
                .build();

        assertThat(query.getQuery().toString())
                .containsSubsequence("filter", "should", "term", "topics", "DATABASE", "term", "topics", "API_DESIGN");
    }

    @DisplayName("Pagination을 설정한다.")
    @Test
    void withPagination_and_Sort() {
        NativeQuery query = new ElasticQueryBuilder()
                .withPagination(20, new ArticleCursor(100, 10L), ArticleSortType.CLICKS)
                .withSort(ArticleSortType.CLICKS)
                .build();

        Pageable pageable = query.getPageable();
        Sort sort = query.getSort();

        assertAll(
                () -> assertThat(pageable.getPageSize()).isEqualTo(20),
                () -> assertThat(sort.getOrderFor("clicks")).isNotNull(),
                () -> assertThat(query.getSearchAfter()).contains(10L) // ES 쿼리 내부적으로는 ID만 있어도 된다고 함.
        );
    }

    @DisplayName("정렬 타입이 RELEVANCE일 경우 score를 추적하고 _score 정렬이 포함된다.")
    @Test
    void relevance_sort_enablesTrackScores() {
        NativeQuery query = new ElasticQueryBuilder()
                .withSort(ArticleSortType.RELEVANCE)
                .build();

        assertAll(
                () -> assertThat(query.getSort().getOrderFor("_score")).isNotNull(),
                () -> assertThat(query.getTrackScores()).isTrue()
        );
    }

    @DisplayName("ArticleQueryCondition으로 전체 조건을 조합할 수 있다.")
    @Test
    void withQueryCondition_combinesAll() {
        NativeQuery query = new ElasticQueryBuilder()
                .withQueryCondition(new ArticleQueryCondition(
                        new SearchKeyword("테스트"),            // search
                        Sector.BE,                                  // sector
                        List.of(Topic.DATABASE),                    // topics
                        List.of(new TechStack("mysql")),      // techStacks
                        ArticleSortType.CLICKS,                     // sort
                        10,                                         // limit
                        new ArticleCursor(10.0, 5L)  // cursor
                ))
                .build();

        String queryString = query.getQuery().toString();
        assertAll(
                () -> assertThat(queryString).containsSubsequence("filter", "term", "sector", "BE"),
                () -> assertThat(queryString).contains("filter", "term", "topics", "DATABASE"),
                () -> assertThat(queryString).contains("filter", "term", "techStacks", "mysql"),
                () -> assertThat(query.getSort().getOrderFor("clicks")).isNotNull(),
                () -> assertThat(query.getSearchAfter()).contains(10.0, 5L)
        );
    }
}
