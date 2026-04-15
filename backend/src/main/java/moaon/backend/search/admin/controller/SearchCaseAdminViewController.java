package moaon.backend.search.admin.controller;

import lombok.RequiredArgsConstructor;
import moaon.backend.search.admin.domain.ReviewStatus;
import moaon.backend.search.admin.dto.SearchCaseDetail;
import moaon.backend.search.admin.dto.SearchCaseSummary;
import moaon.backend.search.admin.service.SearchCaseAdminService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/search-cases")
@RequiredArgsConstructor
public class SearchCaseAdminViewController {
    private final SearchCaseAdminService searchCaseAdminService;

    @GetMapping
    public String list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) ReviewStatus statusFilter,
        Model model
    ) {
        Page<SearchCaseSummary> cases = searchCaseAdminService.list(page, size, statusFilter);
        model.addAttribute("cases", cases);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("currentPage", page);
        model.addAttribute("reviewStatuses", ReviewStatus.values());
        return "admin/search-case/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        SearchCaseDetail detail = searchCaseAdminService.detail(id);
        model.addAttribute("case", detail);
        model.addAttribute("reviewStatuses", ReviewStatus.values());
        return "admin/search-case/detail";
    }
}
