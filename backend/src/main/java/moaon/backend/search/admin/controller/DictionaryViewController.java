package moaon.backend.search.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/dictionaries")
public class DictionaryViewController {

    @GetMapping("/synonyms")
    public String synonyms() {
        return "admin/dictionary/synonym/list";
    }

    @GetMapping("/nori")
    public String nori() {
        return "admin/dictionary/nori/list";
    }
}
