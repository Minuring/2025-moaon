package moaon.backend.project.dto;

import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.global.domain.SearchKeyword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ProjectArticleQueryConditionTest {

    @DisplayName("toArticleCondition()은 프로젝트 상세 페이지 기본 정책을 반영한다.")
    @Test
    void toArticleCondition_setsDefaultPolicy() {
        // given
        SearchKeyword search = new SearchKeyword("검색어");
        ProjectArticleQueryCondition condition = new ProjectArticleQueryCondition(Sector.BE, search);

        // when
        ArticleQueryCondition articleCondition = condition.toArticleCondition();

        // then
        assertAll(
                () -> assertThat(articleCondition.sector()).isEqualTo(Sector.BE),
                () -> assertThat(articleCondition.search()).isEqualTo(search),
                () -> assertThat(articleCondition.sortType()).isEqualTo(ArticleSortType.CREATED_AT),
                () -> assertThat(articleCondition.limit()).isEqualTo(999),
                () -> assertThat(articleCondition.cursor()).isNull(),
                () -> assertThat(articleCondition.techStacks()).isEmpty(),
                () -> assertThat(articleCondition.topics()).isEmpty()
        );
    }

    @DisplayName("생성자에서 문자열을 받아 Sector, SearchKeyword로 변환한다.")
    @Test
    void from_convertsStringToTypes() {
        // when
        ProjectArticleQueryCondition condition = new ProjectArticleQueryCondition("be", "버저닝");

        // then
        assertAll(
                () -> assertThat(condition.sector()).isEqualTo(Sector.BE),
                () -> assertThat(condition.search().value()).isEqualTo("버저닝")
        );
    }
}
