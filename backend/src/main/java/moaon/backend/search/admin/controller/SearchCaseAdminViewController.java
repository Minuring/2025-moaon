package moaon.backend.search.admin.controller;

import lombok.RequiredArgsConstructor;
import moaon.backend.search.admin.domain.ReviewStatus;
import moaon.backend.search.admin.dto.SearchCaseDetail;
import moaon.backend.search.admin.service.SearchCaseAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/search-cases")
@RequiredArgsConstructor
public class SearchCaseAdminViewController {

    private final SearchCaseAdminService searchCaseAdminService;

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        SearchCaseDetail detail = searchCaseAdminService.detail(id);
        model.addAttribute("case", detail);
        model.addAttribute("reviewStatuses", ReviewStatus.values());
        return "admin/search-case/detail";
    }
}
