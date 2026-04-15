package moaon.backend.search.admin.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import moaon.backend.search.dictionary.dto.NoriRequest;
import moaon.backend.search.dictionary.model.NoriEntry;
import moaon.backend.search.dictionary.service.NoriDictionaryService;
import moaon.backend.search.indexing.ReindexFlagService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dictionaries/nori")
@RequiredArgsConstructor
public class NoriApiController {

    private final NoriDictionaryService noriDictionaryService;
    private final ReindexFlagService reindexFlagService;

    @GetMapping
    public List<NoriEntry> list() {
        return noriDictionaryService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoriEntry create(@RequestBody @Valid NoriRequest request) {
        return noriDictionaryService.add(request.getSurface(), request.getSegments());
    }

    @PutMapping("/{id}")
    public NoriEntry update(@PathVariable int id, @RequestBody @Valid NoriRequest request) {
        return noriDictionaryService.update(id, request.getSurface(), request.getSegments());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        noriDictionaryService.delete(id);
    }

    @GetMapping("/reindex-status")
    public Map<String, Boolean> reindexStatus() {
        return Map.of("reindexRequired", reindexFlagService.isReindexRequired());
    }
}
