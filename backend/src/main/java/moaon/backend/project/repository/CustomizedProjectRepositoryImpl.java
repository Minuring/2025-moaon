package moaon.backend.project.repository;

import lombok.RequiredArgsConstructor;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.project.ProjectDao;
import moaon.backend.project.domain.Category;
import moaon.backend.project.domain.Project;
import moaon.backend.project.domain.ProjectCategory;
import moaon.backend.project.domain.Projects;
import moaon.backend.project.dto.ProjectQueryCondition;
import moaon.backend.techStack.domain.ProjectTechStack;
import moaon.backend.techStack.domain.TechStack;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class CustomizedProjectRepositoryImpl implements CustomizedProjectRepository {

    private final ProjectDao projectDao;

    @Override
    public Projects findWithSearchConditions(ProjectQueryCondition condition) {
        int limit = condition.limit();
        SearchKeyword search = condition.search();

        FilteringIds filteringIds = FilteringIds.init();
        filteringIds = applyTechStacks(filteringIds, condition.techStacks());
        filteringIds = applyCategories(filteringIds, condition.categories());
        filteringIds = applySearch(filteringIds, search);

        if (filteringIds.hasEmptyResult()) {
            return Projects.empty(limit);
        }

        List<Project> projects = projectDao.findProjects(condition, filteringIds.getIds());
        return new Projects(projects, calculateTotalCount(filteringIds), limit);
    }

    @Override
    public List<ProjectCategory> findProjectCategoriesByProjectId(Long id) {
        return projectDao.findProjectCategoriesByProjectId(id);
    }

    @Override
    public List<ProjectTechStack> findProjectTechStacksByProjectId(Long id) {
        return projectDao.findProjectTechStacksByProjectId(id);
    }

    @Override
    public Project findProjectWithMemberJoin(Long id) {
        return projectDao.findProjectById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private FilteringIds applyTechStacks(FilteringIds filteringIds, List<TechStack> techStacks) {
        if (filteringIds.hasEmptyResult() || CollectionUtils.isEmpty(techStacks)) {
            return filteringIds;
        }

        List<String> techStackNames = techStacks.stream().map(TechStack::getName).toList();
        Set<Long> projectIdsByTechStacks = projectDao.findProjectIdsByTechStacks(filteringIds, techStackNames);
        return filteringIds.addFilterResult(projectIdsByTechStacks);
    }

    private FilteringIds applyCategories(FilteringIds filteringIds, List<Category> categories) {
        if (filteringIds.hasEmptyResult() || CollectionUtils.isEmpty(categories)) {
            return filteringIds;
        }

        List<String> categoryNames = categories.stream().map(Category::getName).toList();
        Set<Long> projectIdsByCategories = projectDao.findProjectIdsByCategories(filteringIds, categoryNames);
        return filteringIds.addFilterResult(projectIdsByCategories);
    }

    private FilteringIds applySearch(FilteringIds filteringIds, SearchKeyword keyword) {
        if (filteringIds.hasEmptyResult() || keyword == null) {
            return filteringIds;
        }

        Set<Long> projectIdsBySearchKeyword = projectDao.findProjectIdsBySearchKeyword(filteringIds, keyword);
        return filteringIds.addFilterResult(projectIdsBySearchKeyword);
    }

    private long calculateTotalCount(FilteringIds filteringIds) {
        if (filteringIds.isEmpty()) {
            return projectDao.count();
        }

        return filteringIds.size();
    }
}
