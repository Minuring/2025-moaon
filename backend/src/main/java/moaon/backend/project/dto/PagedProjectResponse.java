package moaon.backend.project.dto;

import java.util.List;
import moaon.backend.project.ProjectCursor;
import moaon.backend.project.domain.Project;

public record PagedProjectResponse(
        List<ProjectSummaryResponse> contents,
        int totalCount,
        boolean hasNext,
        String nextCursor
) {

    public static PagedProjectResponse from(
            List<Project> projectsToReturn,
            Long totalCount,
            boolean hasNext,
            ProjectCursor<?> nextCursor
    ) {
        return new PagedProjectResponse(
                ProjectSummaryResponse.from(projectsToReturn),
                totalCount.intValue(),
                hasNext,
                nextCursor == null ? null : nextCursor.getNextCursor()
        );
    }
}
