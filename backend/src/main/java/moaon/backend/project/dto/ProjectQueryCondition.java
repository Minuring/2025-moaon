package moaon.backend.project.dto;

import lombok.*;
import lombok.experimental.Accessors;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.project.ProjectCursor;
import moaon.backend.project.domain.Category;
import moaon.backend.project.domain.ProjectSortType;
import moaon.backend.techStack.domain.TechStack;

import java.util.List;


@Builder
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@EqualsAndHashCode
@ToString
public final class ProjectQueryCondition {

    private SearchKeyword search;
    private List<Category> categories;
    private List<TechStack> techStacks;
    private ProjectSortType projectSortType;
    @Builder.Default
    private int limit = 20;
    private ProjectCursor<?> cursor;

    public ProjectQueryCondition() {
        this.limit = 20;
    }
}
