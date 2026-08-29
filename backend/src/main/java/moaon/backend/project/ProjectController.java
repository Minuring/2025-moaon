package moaon.backend.project;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import moaon.backend.article.ArticleService;
import moaon.backend.global.CountCooldownCookieManager;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.project.domain.ProjectSortType;
import moaon.backend.project.dto.*;
import moaon.backend.techStack.TechStackResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private static final String VIEW_COOKIE_NAME_PREFIX = "viewed_projects_";
    private static final String VIEW_COOKIE_PATH = "/projects";

    private final CountCooldownCookieManager cookieManager;
    private final ProjectService projectService;
    private final ArticleService articleService;
    private final TechStackResolver techStackResolver;
    private final CategoryResolver categoryResolver;

    @PostMapping
    public ResponseEntity<ProjectCreateResponse> saveProject(
            @CookieValue(value = "token", required = false) String token,
            @RequestBody @Valid ProjectCreateRequest projectCreateRequest
    ) {
        Long savedId = projectService.save(token, projectCreateRequest);
        ProjectCreateResponse response = ProjectCreateResponse.from(savedId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailResponse> getProjectById(
            @PathVariable("id") long id,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (cookieManager.isCountIncreasable(VIEW_COOKIE_NAME_PREFIX, id, request)) {
            ProjectDetailResponse projectDetailResponse = projectService.increaseViewsCount(id);
            cookieManager.markAsCounted(VIEW_COOKIE_NAME_PREFIX, VIEW_COOKIE_PATH, id, response);
            return ResponseEntity.ok(projectDetailResponse);
        }

        ProjectDetailResponse projectDetailResponse = projectService.getById(id);

        return ResponseEntity.ok(projectDetailResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects(
            @CookieValue(value = "token", required = false) String token
    ) {
        return ResponseEntity.ok(projectService.getMyProjects(token));
    }

    @GetMapping
    public ResponseEntity<PagedProjectResponse> getPagedProjects(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "categories", required = false) List<String> categories,
            @RequestParam(value = "techStacks", required = false) List<String> techStacks,
            @RequestParam(value = "sort", required = false) String sortType,
            @RequestParam(value = "limit") @Validated @Max(100) int limit,
            @RequestParam(value = "cursor", required = false) String cursor
    ) {
        ProjectSortType projectSortType = ProjectSortType.from(sortType);
        ProjectQueryCondition projectQueryCondition = ProjectQueryCondition.builder()
                .search(search == null ? null : new SearchKeyword(search))
                .categories(categories == null ? List.of() : categoryResolver.resolve(categories))
                .techStacks(techStacks == null ? List.of() : techStackResolver.resolve(techStacks))
                .projectSortType(projectSortType)
                .limit(limit)
                .cursor(projectSortType.toCursor(cursor))
                .build();

        return ResponseEntity.ok(projectService.getPagedProjects(projectQueryCondition));
    }

    @GetMapping("/{id}/articles")
    public ResponseEntity<ProjectArticleResponse> getArticlesByProjectId(
            @PathVariable("id") long id,
            @RequestParam(value = "sector", required = false) String sector,
            @RequestParam(value = "search", required = false) String search
    ) {
        ProjectArticleQueryCondition condition = new ProjectArticleQueryCondition(sector, search);
        ProjectArticleResponse projectArticleResponse = articleService.getByProjectId(id, condition);
        return ResponseEntity.ok(projectArticleResponse);
    }
}
