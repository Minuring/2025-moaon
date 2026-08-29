package moaon.backend.project.service;


import moaon.backend.fixture.RepositoryHelper;
import moaon.backend.fixture.ServiceLayerTest;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.project.ProjectService;
import moaon.backend.project.domain.Project;
import moaon.backend.project.dto.PagedProjectResponse;
import moaon.backend.project.dto.ProjectDetailResponse;
import moaon.backend.project.dto.ProjectQueryCondition;
import moaon.backend.project.dto.ProjectSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@ServiceLayerTest
class ProjectServiceTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private RepositoryHelper repositoryHelper;

    @DisplayName("특정 프로젝트를 조회한다.")
    @Test
    void getById() {
        // given
        Project project = repositoryHelper.saveAnyProject();

        // when
        ProjectDetailResponse response = projectService.getById(project.getId());

        // then
        assertAll(
                () -> assertThat(response.id()).isEqualTo(project.getId()),
                () -> assertThat(response.title()).isEqualTo(project.getTitle()),
                () -> assertThat(response.summary()).isEqualTo(project.getSummary())
        );
    }

    @DisplayName("ID에 해당하는 프로젝트가 존재하지 않는다면 예외가 발생한다.")
    @Test
    void getById_notFound() {
        assertThatThrownBy(() -> projectService.getById(999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PROJECT_NOT_FOUND.getMessage());
    }

    @DisplayName("주어진 조건으로 프로젝트들을 조회한다")
    @Test
    void getPagedProjects() {
        // given
        Project p1 = repositoryHelper.saveAnyProject();
        Project p2 = repositoryHelper.saveAnyProject();

        // when
        ProjectQueryCondition condition = new ProjectQueryCondition();
        PagedProjectResponse response = projectService.getPagedProjects(condition);

        // then
        assertAll(
                () -> assertThat(response.contents()).extracting(ProjectSummaryResponse::id).contains(p1.getId(), p2.getId()),
                () -> assertThat(response.hasNext()).isFalse(),
                () -> assertThat(response.nextCursor()).isNull(),
                () -> assertThat(response.totalCount()).isEqualTo(2L)
        );
    }
}
