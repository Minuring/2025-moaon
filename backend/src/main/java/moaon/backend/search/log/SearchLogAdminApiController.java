package moaon.backend.search.log;

import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import moaon.backend.search.log.dto.SearchLogDetail;
import moaon.backend.search.log.dto.SearchLogSummary;
import moaon.backend.search.log.service.SearchLogAdminService;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/search-logs")
@RequiredArgsConstructor
@Validated
public class SearchLogAdminApiController {

    private final SearchLogAdminService searchLogAdminService;

    @GetMapping
    public Page<SearchLogSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        return searchLogAdminService.list(page, size);
    }

    @GetMapping("/{id}")
    public SearchLogDetail detail(@PathVariable String id) {
        return searchLogAdminService.detail(id);
    }
}
