package moaon.backend.search.admin.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moaon.backend.search.dictionary.dto.ReloadResponse;
import moaon.backend.search.dictionary.dto.SynonymRequest;
import moaon.backend.search.dictionary.model.SynonymEntry;
import moaon.backend.search.dictionary.service.SynonymDictionaryService;
import moaon.backend.search.indexing.SynonymReloadService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/dictionaries/synonyms")
@RequiredArgsConstructor
public class SynonymApiController {

    private final SynonymDictionaryService synonymDictionaryService;
    private final SynonymReloadService synonymReloadService;

    @GetMapping
    public List<SynonymEntry> list() {
        return synonymDictionaryService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SynonymEntry create(@RequestBody @Valid SynonymRequest request) {
        return synonymDictionaryService.add(request.getTerms());
    }

    @PutMapping("/{id}")
    public SynonymEntry update(@PathVariable int id, @RequestBody @Valid SynonymRequest request) {
        return synonymDictionaryService.update(id, request.getTerms());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        synonymDictionaryService.delete(id);
    }

    @PostMapping("/reload")
    public ReloadResponse reload() {
        return synonymReloadService.reload();
    }
}
