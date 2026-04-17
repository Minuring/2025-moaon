package moaon.backend.search.log;

import lombok.RequiredArgsConstructor;
import moaon.backend.search.log.dto.SearchLogDetail;
import moaon.backend.search.log.service.SearchLogAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/search-logs")
@RequiredArgsConstructor
public class SearchLogAdminViewController {

    private final SearchLogAdminService searchLogAdminService;

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        SearchLogDetail detail = searchLogAdminService.detail(id);
        model.addAttribute("log", detail);
        return "admin/search-log/detail";
    }
}
