package moaon.backend.search.admin.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import moaon.backend.search.admin.domain.ReviewStatus;
import moaon.backend.search.admin.dto.ReviewRequest;
import moaon.backend.search.admin.dto.SearchCaseDetail;
import moaon.backend.search.admin.dto.SearchCaseSummary;
import moaon.backend.search.admin.service.SearchCaseAdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/search-cases")
@RequiredArgsConstructor
@Validated
public class SearchCaseAdminApiController {

    private final SearchCaseAdminService searchCaseAdminService;

    // GET /admin/api/search-cases?page=0&size=20&status=NEW
    @GetMapping
    public Page<SearchCaseSummary> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") @Max(100) int size,
        @RequestParam(required = false) ReviewStatus status
    ) {
        return searchCaseAdminService.list(page, size, status);
    }

    // GET /admin/api/search-cases/{id}
    @GetMapping("/{id}")
    public SearchCaseDetail detail(@PathVariable String id) {
        return searchCaseAdminService.detail(id);
    }

    // PATCH /admin/api/search-cases/{id}/review
    @PatchMapping("/{id}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(@PathVariable String id, @RequestBody @Valid ReviewRequest request) {
        searchCaseAdminService.review(id, request);
    }
}
