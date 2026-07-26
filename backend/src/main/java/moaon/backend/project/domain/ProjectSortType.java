package moaon.backend.project.domain;

import java.util.Arrays;
import java.util.function.Function;
import moaon.backend.global.util.Parsers;
import moaon.backend.project.ProjectCursor;

public enum ProjectSortType {

    CREATED_AT("createdAt",
            cursor -> Parsers.toCursor(cursor, Parsers::parseLocalDateTime),
            project -> new ProjectCursor<>(project.getCreatedAt(), project.getId())
    ),

    VIEWS("views",
            cursor -> Parsers.toCursor(cursor, Parsers::parseInt),
            project -> new ProjectCursor<>(project.getViews(), project.getId())
    ),

    LOVES("loves",
            cursor -> Parsers.toCursor(cursor, Parsers::parseInt),
            project -> new ProjectCursor<>(project.getLoveCount(), project.getId())
    ),

    ARTICLE_COUNT("articleCount",
            cursor -> Parsers.toCursor(cursor, Parsers::parseInt),
            project -> new ProjectCursor<>(project.getArticles().size(), project.getId())
    );

    private final String sortType;
    private final Function<String, ProjectCursor<?>> cursorFactory;
    private final Function<Project, ProjectCursor<?>> projectToCursorFactory;

    ProjectSortType(
            String sortType,
            Function<String, ProjectCursor<?>> cursorFactory,
            Function<Project, ProjectCursor<?>> projectToCursorFactory
    ) {
        this.sortType = sortType;
        this.cursorFactory = cursorFactory;
        this.projectToCursorFactory = projectToCursorFactory;
    }

    public static ProjectSortType from(String sortType) {
        return Arrays.stream(ProjectSortType.values())
                .filter(sortBy -> sortBy.sortType.equals(sortType))
                .findAny()
                .orElse(CREATED_AT);
    }

    public ProjectCursor<?> toCursor(String cursor) {
        return cursorFactory.apply(cursor);
    }

    public ProjectCursor<?> toCursor(Project project) {
        return projectToCursorFactory.apply(project);
    }
}
