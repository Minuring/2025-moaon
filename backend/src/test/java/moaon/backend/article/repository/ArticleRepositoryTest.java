package moaon.backend.article.repository;

import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.dto.ArticleDto;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.fixture.*;
import moaon.backend.global.config.QueryDslConfig;
import moaon.backend.project.domain.Project;
import moaon.backend.techStack.TechStackRepository;
import moaon.backend.techStack.domain.TechStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 검색어가 없는 필터/정렬/페이징만 검증한다. 검색어(fulltext) 케이스는 테스트 환경에
 * FULLTEXT 인덱스가 없어 실행 시 MySQL 컨테이너 구성이 추가로 필요하므로 생략한다.
 */
@SpringBootTest
@Import({RepositoryHelper.class, QueryDslConfig.class})
@Transactional
@DisplayName("ArticleSearchDao DB 폴백 검색 테스트")
class ArticleRepositoryTest {

    @Autowired
    private RepositoryHelper repositoryHelper;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private TechStackRepository techStackRepository;

    /**
     * RepositoryHelper.save(Article)은 매번 article.getProject()도 함께 저장한다.
     * 같은 프로젝트를 여러 article에서 재사용하면 한 트랜잭션 안에서 동일 영속 엔티티를
     * 반복 merge하게 되어 충돌이 나므로, 이미 저장된 프로젝트를 참조하는 article은
     * 이 메서드로 techStack만 저장하고 article만 저장한다.
     */
    private Article saveArticleUnderExistingProject(Article article) {
        techStackRepository.saveAll(article.getTechStacks());
        return articleRepository.save(article);
    }

    @Test
    @DisplayName("sector로 필터링한다")
    void search_filtersBySector() {
        Project project = repositoryHelper.save(new ProjectFixtureBuilder().build());
        Article backend = saveArticleUnderExistingProject(
                new ArticleFixtureBuilder().project(project).sector(Sector.BE).build()
        );
        saveArticleUnderExistingProject(
                new ArticleFixtureBuilder().project(project).sector(Sector.FE).build()
        );

        ArticleQueryCondition condition = new ArticleQueryConditionBuilder()
                .sector(Sector.BE)
                .sortBy(ArticleSortType.CREATED_AT)
                .build();

        ArticleSearchResult result = articleRepository.search(condition, null);

        assertThat(result.articles())
                .extracting(ArticleDto::id)
                .containsExactly(backend.getId());
    }

    @Test
    @DisplayName("모든 techStack을 포함하는 아티클만 조회한다")
    void search_filtersByAllTechStacks() {
        TechStack react = repositoryHelper.save(Fixture.anyTechStack());
        TechStack spring = repositoryHelper.save(Fixture.anyTechStack());
        Project project = repositoryHelper.save(new ProjectFixtureBuilder().build());

        Article both = saveArticleUnderExistingProject(
                new ArticleFixtureBuilder().project(project).techStacks(react, spring).build()
        );
        saveArticleUnderExistingProject(
                new ArticleFixtureBuilder().project(project).techStacks(react).build()
        );

        ArticleQueryCondition condition = new ArticleQueryConditionBuilder()
                .techStackNames(react.getName(), spring.getName())
                .sortBy(ArticleSortType.CREATED_AT)
                .build();

        ArticleSearchResult result = articleRepository.search(condition, null);

        assertThat(result.articles())
                .extracting(ArticleDto::id)
                .containsExactly(both.getId());
    }

    @Test
    @DisplayName("projectId로 필터링한다 (프로젝트 상세 목록 폴백)")
    void search_filtersByProjectId() {
        Project targetProject = repositoryHelper.save(new ProjectFixtureBuilder().build());
        Project otherProject = repositoryHelper.save(new ProjectFixtureBuilder().build());

        Article inTargetProject = saveArticleUnderExistingProject(
                new ArticleFixtureBuilder().project(targetProject).build()
        );
        saveArticleUnderExistingProject(
                new ArticleFixtureBuilder().project(otherProject).build()
        );

        ArticleQueryCondition condition = new ArticleQueryConditionBuilder()
                .sortBy(ArticleSortType.CREATED_AT)
                .limit(999)
                .build();

        ArticleSearchResult result = articleRepository.search(condition, targetProject.getId());

        assertThat(result.articles())
                .extracting(ArticleDto::id)
                .containsExactly(inTargetProject.getId());
    }

    @Test
    @DisplayName("limit보다 결과가 많으면 hasNext와 다음 커서를 계산한다")
    void search_calculatesHasNextAndCursor() {
        Project project = repositoryHelper.save(new ProjectFixtureBuilder().build());
        saveArticleUnderExistingProject(new ArticleFixtureBuilder().project(project).build());
        saveArticleUnderExistingProject(new ArticleFixtureBuilder().project(project).build());
        saveArticleUnderExistingProject(new ArticleFixtureBuilder().project(project).build());

        ArticleQueryCondition condition = new ArticleQueryConditionBuilder()
                .sortBy(ArticleSortType.CREATED_AT)
                .limit(2)
                .build();

        ArticleSearchResult result = articleRepository.search(condition, project.getId());

        assertThat(result.articles()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isNotNull();
    }
}
