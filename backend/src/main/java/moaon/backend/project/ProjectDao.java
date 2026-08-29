package moaon.backend.project;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.project.domain.Project;
import moaon.backend.project.domain.ProjectCategory;
import moaon.backend.project.domain.ProjectSortType;
import moaon.backend.project.dto.ProjectQueryCondition;
import moaon.backend.project.repository.FilteringIds;
import moaon.backend.project.repository.ProjectFullTextSearchHQLFunction;
import moaon.backend.techStack.domain.ProjectTechStack;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static moaon.backend.member.QMember.member;
import static moaon.backend.project.domain.QCategory.category;
import static moaon.backend.project.domain.QProject.project;
import static moaon.backend.project.domain.QProjectCategory.projectCategory;
import static moaon.backend.techStack.domain.QProjectTechStack.projectTechStack;
import static moaon.backend.techStack.domain.QTechStack.techStack;

@Repository
@RequiredArgsConstructor
public class ProjectDao {

    private static final String BLANK = " ";

    private final JPAQueryFactory jpaQueryFactory;

    public Optional<Project> findProjectById(Long id) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(project)
                .leftJoin(project.author, member).fetchJoin()
                .where(project.id.eq(id))
                .fetchOne());
    }

    public List<ProjectCategory> findProjectCategoriesByProjectId(Long id) {
        return jpaQueryFactory
                .selectFrom(projectCategory)
                .leftJoin(projectCategory.category, category).fetchJoin()
                .where(projectCategory.project.id.eq(id))
                .fetch();
    }

    public List<ProjectTechStack> findProjectTechStacksByProjectId(Long id) {
        return jpaQueryFactory
                .selectFrom(projectTechStack)
                .leftJoin(projectTechStack.techStack, techStack).fetchJoin()
                .where(projectTechStack.project.id.eq(id))
                .fetch();
    }

    public List<Project> findProjects(ProjectQueryCondition condition, Set<Long> projectIdsByFilter) {
        ProjectCursor<?> cursor = condition.cursor();
        ProjectSortType sortBy = condition.projectSortType();
        int limit = condition.limit();

        int fetchExtraForHasNext = 1;
        return jpaQueryFactory.selectFrom(project)
                .where(
                        idsInCondition(projectIdsByFilter),
                        applyCursor(cursor, sortBy)
                )
                .orderBy(toOrderBy(sortBy))
                .limit(limit + fetchExtraForHasNext)
                .fetch();
    }

    public Set<Long> findProjectIdsByTechStacks(FilteringIds filteringIds, List<String> techStacks) {
        if (CollectionUtils.isEmpty(techStacks)) {
            return new HashSet<>();
        }

        return new HashSet<>(jpaQueryFactory.select(projectTechStack.project.id)
                .from(projectTechStack)
                .where(
                        projectTechStack.techStack.name.in(techStacks),
                        projectIdInFilteringIds(filteringIds, projectTechStack.project.id)
                )
                .groupBy(projectTechStack.project.id)
                .having(projectTechStack.techStack.name.count().eq((long) techStacks.size()))
                .fetch());
    }

    public Set<Long> findProjectIdsByCategories(FilteringIds filteringIds, List<String> categories) {
        if (CollectionUtils.isEmpty(categories)) {
            return new HashSet<>();
        }

        return new HashSet<>(jpaQueryFactory.select(projectCategory.project.id)
                .from(projectCategory)
                .where(
                        projectCategory.category.name.in(categories),
                        projectIdInFilteringIds(filteringIds, projectCategory.project.id)
                )
                .groupBy(projectCategory.project.id)
                .having(projectCategory.category.name.count().eq((long) categories.size()))
                .fetch());
    }

    public Set<Long> findProjectIdsBySearchKeyword(FilteringIds filteringIds, SearchKeyword searchKeyword) {
        if (searchKeyword == null) {
            return new HashSet<>();
        }

        return new HashSet<>(jpaQueryFactory.select(project.id)
                .from(project)
                .where(
                        satisfiesMatchScore(searchKeyword),
                        projectIdInFilteringIds(filteringIds, project.id)
                )
                .fetch());
    }

    public long count() {
        return Optional.ofNullable(jpaQueryFactory.select(Wildcard.count)
                .from(project)
                .fetchOne()).orElse(0L);
    }

    private BooleanExpression projectIdInFilteringIds(
            FilteringIds filteringIds,
            SimpleExpression<Long> projectIdExpression
    ) {
        if (filteringIds.isHasResult()) {
            return projectIdExpression.in(filteringIds.getIds());
        }

        return null;
    }


    private BooleanExpression idsInCondition(Set<Long> projectIdsByFilter) {
        if (CollectionUtils.isEmpty(projectIdsByFilter)) {
            return null;
        }

        return project.id.in(projectIdsByFilter);
    }

    private BooleanExpression satisfiesMatchScore(SearchKeyword searchKeyword) {
        if (searchKeyword == null) {
            return null;
        }
        double minimumMatchScore = 0.0;
        return Expressions.numberTemplate(
                        Double.class,
                        ProjectFullTextSearchHQLFunction.EXPRESSION_TEMPLATE,
                        formatSearchKeyword(searchKeyword)
                )
                .gt(minimumMatchScore);
    }

    private String formatSearchKeyword(SearchKeyword searchKeyword) {
        String search = searchKeyword.replaceSpecialCharacters(BLANK);
        return Arrays.stream(search.split(BLANK))
                .map(this::applyBooleanModeExpression)
                .collect(Collectors.joining(BLANK));
    }

    private String applyBooleanModeExpression(String keyword) {
        if (keyword.length() == 1) {
            return keyword + "*";
        }
        return "+" + keyword.toLowerCase() + "*";
    }

    private BooleanExpression applyCursor(ProjectCursor<?> cursor, ProjectSortType sortType) {
        if (cursor == null) {
            return null;
        }

        if (sortType == ProjectSortType.CREATED_AT) {
            LocalDateTime sortValue = (LocalDateTime) cursor.getSortValue();
            return project.createdAt.lt(sortValue)
                    .or(project.createdAt.eq(sortValue).and(project.id.lt(cursor.getLastId())));
        }

        if (sortType == ProjectSortType.VIEWS) {
            Integer sortValue = (Integer) cursor.getSortValue();
            return project.views.lt(sortValue)
                    .or(project.views.eq(sortValue).and(project.id.lt(cursor.getLastId())));
        }

        if (sortType == ProjectSortType.ARTICLE_COUNT) {
            Integer sortValue = (Integer) cursor.getSortValue();
            return project.articles.size().lt(sortValue)
                    .or(project.articles.size().eq(sortValue).and(project.id.lt(cursor.getLastId())));
        }

        Integer sortValue = (Integer) cursor.getSortValue();
        return project.lovedMembers.size().lt(sortValue)
                .or(project.lovedMembers.size().eq(sortValue).and(project.id.lt(cursor.getLastId())));
    }

    private OrderSpecifier<?>[] toOrderBy(ProjectSortType sortBy) {
        if (sortBy == ProjectSortType.CREATED_AT) {
            return new OrderSpecifier<?>[]{project.createdAt.desc(), project.id.desc()};
        }

        if (sortBy == ProjectSortType.VIEWS) {
            return new OrderSpecifier<?>[]{project.views.desc(), project.id.desc()};
        }

        if (sortBy == ProjectSortType.ARTICLE_COUNT) {
            return new OrderSpecifier<?>[]{project.articles.size().desc(), project.id.desc()};
        }

        return new OrderSpecifier<?>[]{project.lovedMembers.size().desc(), project.id.desc()};
    }
}
