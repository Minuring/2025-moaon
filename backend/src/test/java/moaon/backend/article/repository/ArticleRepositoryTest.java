package moaon.backend.article.repository;

import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.dto.ArticleDto;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.fixture.DataAccessLayerTest;
import moaon.backend.fixture.Fixtures;
import moaon.backend.fixture.RepositoryHelper;
import moaon.backend.project.domain.Project;
import moaon.backend.techStack.domain.TechStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 검색어가 없는 필터/정렬/페이징만 검증한다. 검색어(fulltext) 케이스는 테스트 환경에
 * FULLTEXT 인덱스가 없어 실행 시 MySQL 컨테이너 구성이 추가로 필요하므로 생략한다.
 */
@DataAccessLayerTest
class ArticleRepositoryTest {

    @Autowired
    private RepositoryHelper repositoryHelper;

    @Autowired
    private ArticleRepository articleRepository;

    @Test
    @DisplayName("sector로 필터링한다")
    void search_filtersBySector() {
        // given
        Article backend = repositoryHelper.save(Fixtures.articleBuilder()
                .sector(Sector.BE)
                .build());

        Article frontend = repositoryHelper.save(Fixtures.articleBuilder()
                .sector(Sector.FE)
                .build());

        // when
        ArticleQueryCondition queryCondition = ArticleQueryCondition.builder()
                .sector(Sector.BE)
                .build();

        ArticleSearchResult result = articleRepository.search(queryCondition);

        // then
        assertThat(result.articles())
                .extracting(ArticleDto::id)
                .containsExactly(backend.getId());
    }

    @Test
    @DisplayName("모든 techStack을 포함하는 아티클만 조회한다")
    void search_filtersByAllTechStacks() {
        // given
        TechStack ts1 = Fixtures.anyTechStack();
        TechStack ts2 = Fixtures.anyTechStack();

        Article articleWithBoth = repositoryHelper.saveArticle(b -> b.techStacks(ts1, ts2));
        Article articleWithOnlyOne = repositoryHelper.saveArticle(b -> b.techStacks(ts1));

        // when
        ArticleQueryCondition condition = ArticleQueryCondition.builder()
                .techStacks(List.of(ts1, ts2))
                .build();
        ArticleSearchResult result = articleRepository.search(condition);

        // then
        assertThat(result.articles())
                .extracting(ArticleDto::id)
                .containsExactly(articleWithBoth.getId());
    }

    @Test
    @DisplayName("projectId로 필터링한다 (프로젝트 상세 목록 폴백)")
    void search_filtersByProjectId() {
        // given
        Project targetProject = repositoryHelper.saveAnyProject();
        Project otherProject = repositoryHelper.saveAnyProject();

        Article targetArticle = repositoryHelper.save(Fixtures.articleBuilder().project(targetProject).build());
        Article otherArticle = repositoryHelper.save(Fixtures.articleBuilder().project(otherProject).build());

        // when
        ArticleSearchResult result = articleRepository.search(new ArticleQueryCondition(), targetProject.getId());

        // then
        assertThat(result.articles())
                .extracting(ArticleDto::id)
                .containsExactly(targetArticle.getId());
    }

    @Test
    @DisplayName("limit보다 결과가 많으면 hasNext와 다음 커서를 계산한다")
    void search_calculatesHasNextAndCursor() {
        // given
        repositoryHelper.saveAnyArticle();
        repositoryHelper.saveAnyArticle();
        repositoryHelper.saveAnyArticle();

        // when
        ArticleQueryCondition condition = ArticleQueryCondition.builder()
                .limit(2)
                .build();
        ArticleSearchResult result = articleRepository.search(condition);

        // then
        assertAll(
                () -> assertThat(result.articles()).hasSize(2),
                () -> assertThat(result.hasNext()).isTrue(),
                () -> assertThat(result.nextCursor()).isNotNull()
        );
    }

    @Test
    @DisplayName("정렬값이 같으면 id 내림차순으로 이어서 페이지를 조회한다")
    void search_paginatesTiedSortValueByIdDesc() {
        // given
        Article first = repositoryHelper.save(Fixtures.articleBuilder().clicks(0).build());
        Article second = repositoryHelper.save(Fixtures.articleBuilder().clicks(0).build());
        Article third = repositoryHelper.save(Fixtures.articleBuilder().clicks(0).build());

        // when
        ArticleSearchResult firstPage = articleRepository.search(ArticleQueryCondition.builder()
                .sortType(ArticleSortType.CLICKS)
                .limit(2)
                .build());
        ArticleSearchResult secondPage = articleRepository.search(ArticleQueryCondition.builder()
                .sortType(ArticleSortType.CLICKS)
                .limit(2)
                .cursor(firstPage.nextCursor())
                .build());

        // then
        assertAll(
                () -> assertThat(firstPage.articles())
                        .extracting(ArticleDto::id)
                        .containsExactly(third.getId(), second.getId()),
                () -> assertThat(secondPage.articles())
                        .extracting(ArticleDto::id)
                        .containsExactly(first.getId())
        );
    }
}
