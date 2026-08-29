package moaon.backend.project.repository;

import moaon.backend.fixture.DataAccessLayerTest;
import moaon.backend.fixture.Fixtures;
import moaon.backend.fixture.RepositoryHelper;
import moaon.backend.member.Member;
import moaon.backend.project.domain.Category;
import moaon.backend.project.domain.Project;
import moaon.backend.project.domain.ProjectSortType;
import moaon.backend.project.domain.Projects;
import moaon.backend.project.dto.ProjectQueryCondition;
import moaon.backend.techStack.domain.TechStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataAccessLayerTest
class ProjectRepositoryTest {

    @Autowired
    private RepositoryHelper repositoryHelper;

    @Autowired
    private ProjectRepository projectRepository;

    @DisplayName("조건 없이 모든 프로젝트를 조회한다.")
    @Test
    void findWithSearchConditions() {
        // given
        repositoryHelper.saveAnyProject();
        repositoryHelper.saveAnyProject();
        repositoryHelper.saveAnyProject();

        //when
        ProjectQueryCondition condition = ProjectQueryCondition.builder().limit(20).build();
        Projects projects = projectRepository.findWithSearchConditions(condition);

        //then
        assertThat(projects.getCount()).isEqualTo(3);
    }

    @DisplayName("카테고리 필터를 이용해 프로젝트를 조회한다.")
    @Test
    void findWithCategoryFilter() {
        // given
        Category category1 = Fixtures.anyProjectCategory();
        Category category2 = Fixtures.anyProjectCategory();
        Category category3 = Fixtures.anyProjectCategory();
        Category category4 = Fixtures.anyProjectCategory();
        Category category5 = Fixtures.anyProjectCategory();

        Project other1 = repositoryHelper.save(Fixtures.projectBuilder().categories(category1, category2).build());
        Project other2 = repositoryHelper.save(Fixtures.projectBuilder().categories(category2, category3).build());
        Project expectedProject = repositoryHelper.save(Fixtures.projectBuilder()
                .categories(category1, category3, category4, category5)
                .build()
        );

        // when
        ProjectQueryCondition queryCondition = ProjectQueryCondition.builder()
                .categories(List.of(
                        category1,
                        category3,
                        category4,
                        category5))
                .build();
        Projects projects = projectRepository.findWithSearchConditions(queryCondition);

        // then
        assertThat(projects.getProjects()).containsOnlyOnce(expectedProject);
    }

    @DisplayName("기술스택 필터를 이용해 프로젝트를 조회한다.")
    @Test
    void findWithTechStackFilter() {
        // given
        TechStack techStack1 = Fixtures.anyTechStack();
        TechStack techStack2 = Fixtures.anyTechStack();
        TechStack techStack3 = Fixtures.anyTechStack();
        TechStack techStack4 = Fixtures.anyTechStack();
        TechStack techStack5 = Fixtures.anyTechStack();

        Project other1 = repositoryHelper.save(Fixtures.projectBuilder().techStacks(techStack1, techStack2).build());
        Project other2 = repositoryHelper.save(Fixtures.projectBuilder().techStacks(techStack3, techStack5).build());
        Project expectedProject = repositoryHelper.save(Fixtures.projectBuilder()
                .techStacks(techStack1, techStack2, techStack3, techStack4)
                .build()
        );

        // when
        ProjectQueryCondition queryCondition = ProjectQueryCondition.builder()
                .techStacks(List.of(
                        techStack1,
                        techStack2,
                        techStack3,
                        techStack4))
                .build();
        Projects projects = projectRepository.findWithSearchConditions(queryCondition);

        // then
        assertThat(projects.getProjects()).containsOnlyOnce(expectedProject);
    }

    @DisplayName("카테고리 기술스택 필터를 모두 이용하여 조회한다.")
    @Test
    void findWithTechStackFilterAndCategory() {
        // given
        Category category1 = Fixtures.anyProjectCategory();
        Category category2 = Fixtures.anyProjectCategory();
        Category category3 = Fixtures.anyProjectCategory();

        TechStack techStack1 = Fixtures.anyTechStack();
        TechStack techStack2 = Fixtures.anyTechStack();

        Project notContainsCategory3 = repositoryHelper.save(
                Fixtures.projectBuilder()
                        .categories(category1, category2)
                        .techStacks(techStack1, techStack2)
                        .build()
        );
        Project notContainsTechStack2 = repositoryHelper.save(
                Fixtures.projectBuilder()
                        .categories(category1, category2, category3)
                        .techStacks(techStack1)
                        .build()
        );
        Project expectedProject = repositoryHelper.save(
                Fixtures.projectBuilder()
                        .categories(category1, category2, category3)
                        .techStacks(techStack1, techStack2)
                        .build()
        );

        // when
        ProjectQueryCondition queryCondition = ProjectQueryCondition.builder()
                .categories(List.of(category1, category2, category3))
                .techStacks(List.of(techStack1, techStack2))
                .build();
        Projects actual = projectRepository.findWithSearchConditions(queryCondition);

        // then
        assertThat(actual.getProjects()).containsOnlyOnce(expectedProject);
    }

    @DisplayName("프로젝트를 조회순을 기준으로 정렬한다.")
    @Test
    void toOrderByViews() {
        // given
        Project three = repositoryHelper.save(Fixtures.projectBuilder().views(3).build());
        Project two = repositoryHelper.save(Fixtures.projectBuilder().views(2).build());
        Project one = repositoryHelper.save(Fixtures.projectBuilder().views(1).build());

        // when
        ProjectQueryCondition queryCondition = ProjectQueryCondition.builder()
                .projectSortType(ProjectSortType.VIEWS)
                .build();

        Projects projects = projectRepository.findWithSearchConditions(queryCondition);

        // then
        assertThat(projects.getProjects()).containsSequence(three, two, one);
    }

    @DisplayName("프로젝트의 아티클 갯수를 기준으로 정렬한다.")
    @Test
    void toOrderByArticleCount() {
        // given
        Project three = repositoryHelper.saveAnyProject();
        repositoryHelper.save(Fixtures.articleBuilder().project(three).build());
        repositoryHelper.save(Fixtures.articleBuilder().project(three).build());
        repositoryHelper.save(Fixtures.articleBuilder().project(three).build());

        Project two = repositoryHelper.saveAnyProject();
        repositoryHelper.save(Fixtures.articleBuilder().project(two).build());
        repositoryHelper.save(Fixtures.articleBuilder().project(two).build());

        Project one = repositoryHelper.saveAnyProject();
        repositoryHelper.save(Fixtures.articleBuilder().project(one).build());

        // when
        ProjectQueryCondition queryCondition = ProjectQueryCondition.builder()
                .projectSortType(ProjectSortType.ARTICLE_COUNT)
                .build();

        Projects projects = projectRepository.findWithSearchConditions(queryCondition);

        // then
        assertThat(projects.getProjects()).containsSequence(three, two, one);
    }

    @DisplayName("프로젝트를 생성일자 기준으로 정렬한다.")
    @Test
    void toOrderByCreatedAt() {
        // given
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime tomorrow = today.plusDays(1);
        LocalDateTime yesterday = today.minusDays(1);
        Project tomorrowProject = repositoryHelper.save(Fixtures.projectBuilder()
                .createdAt(tomorrow)
                .build()
        );
        Project todayProject = repositoryHelper.save(Fixtures.projectBuilder()
                .createdAt(today)
                .build()
        );
        Project yesterdayProject = repositoryHelper.save(Fixtures.projectBuilder()
                .createdAt(yesterday)
                .build()
        );

        // when
        ProjectQueryCondition queryCondition = ProjectQueryCondition.builder()
                .projectSortType(ProjectSortType.CREATED_AT)
                .build();
        Projects projects = projectRepository.findWithSearchConditions(queryCondition);

        // then
        assertThat(projects.getProjects()).containsSequence(tomorrowProject, todayProject, yesterdayProject);
    }

    @DisplayName("프로젝트를 좋아요 순으로 정렬한다.")
    @Test
    void toOrderByLove() {
        // given
        Member author1 = repositoryHelper.saveAnyMember();
        Member author2 = repositoryHelper.saveAnyMember();
        Member author3 = repositoryHelper.saveAnyMember();

        Project three = repositoryHelper.saveAnyProject();
        three.addLovedMember(author1);
        three.addLovedMember(author2);
        three.addLovedMember(author3);

        Project two = repositoryHelper.saveAnyProject();
        two.addLovedMember(author1);
        two.addLovedMember(author2);

        Project one = repositoryHelper.saveAnyProject();
        one.addLovedMember(author1);

        // when
        ProjectQueryCondition queryCondition = ProjectQueryCondition.builder()
                .projectSortType(ProjectSortType.LOVES)
                .build();

        Projects projects = projectRepository.findWithSearchConditions(queryCondition);

        // then
        assertThat(projects.getProjects()).containsSequence(three, two, one);
    }
}
