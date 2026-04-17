package moaon.backend.search.dictionary;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moaon.backend.search.dictionary.dto.NoriRequest;
import moaon.backend.search.dictionary.dto.ReloadResponse;
import moaon.backend.search.dictionary.dto.NoriEntry;
import moaon.backend.search.dictionary.service.NoriDictionaryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/dictionaries/nori")
@RequiredArgsConstructor
public class NoriApiController {

    private final NoriDictionaryService noriDictionaryService;

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
    public NoriEntry update(@PathVariable Long id, @RequestBody @Valid NoriRequest request) {
        return noriDictionaryService.update(id, request.getSurface(), request.getSegments());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        noriDictionaryService.delete(id);
    }

    @PostMapping("/reload")
    public ReloadResponse reload() {
        return noriDictionaryService.reloadIndex();
    }
}
