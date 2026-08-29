package moaon.backend.article.dto;

import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Sector;
import moaon.backend.global.domain.SearchKeyword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleQueryConditionTest {

    @DisplayName("관련도순 정렬인데 검색어가 없으면 CREATED_AT 정렬 기준을 선택한다.")
    @Test
    void from_relevance_with_no_searchKeyword() {
        ArticleQueryCondition condition = new ArticleQueryCondition(
                null,
                Sector.BE,
                List.of(),
                List.of(),
                ArticleSortType.RELEVANCE,
                10,
                null);

        assertThat(condition.sortType()).isEqualTo(ArticleSortType.CREATED_AT);
    }

    @DisplayName("관련도순 정렬이면서 검색어가 있으면 RELEVANCE 정렬 기준을 선택한다.")
    @Test
    void from_relevance_with_searchKeyword() {
        ArticleQueryCondition condition = new ArticleQueryCondition(
                new SearchKeyword("검색어"),
                Sector.BE,
                List.of(),
                List.of(),
                ArticleSortType.RELEVANCE,
                10,
                null);
        assertThat(condition.sortType()).isEqualTo(ArticleSortType.RELEVANCE);
    }
}
