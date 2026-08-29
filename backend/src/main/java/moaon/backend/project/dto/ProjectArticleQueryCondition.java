package moaon.backend.project.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.global.domain.SearchKeyword;

import java.util.List;

@Builder
@Accessors(fluent = true)
@Getter
@EqualsAndHashCode
@ToString
public final class ProjectArticleQueryCondition {

    private Sector sector;
    private SearchKeyword search;

    public ProjectArticleQueryCondition(Sector sector, SearchKeyword search) {
        this.sector = sector;
        this.search = search;
    }

    public ProjectArticleQueryCondition(String sector, String search) {
        this(sector == null ? null : Sector.of(sector), new SearchKeyword(search));
    }

    public ProjectArticleQueryCondition() {
        this((Sector) null, null);
    }

    public ArticleQueryCondition toArticleCondition() {
        return new ArticleQueryCondition(
                this.search,
                this.sector,
                List.of(),
                List.of(),
                ArticleSortType.CREATED_AT, // 프로젝트 상세 페이지 정렬옵션 정책 없으므로 기본값
                999, // 프로젝트 상세 페이지 무제한 페이징
                null
        );
    }
}
