package moaon.backend.search.dictionary;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moaon.backend.search.dictionary.dto.BulkConflict;
import moaon.backend.search.dictionary.dto.BulkRequest;
import moaon.backend.search.dictionary.dto.ReloadResponse;
import moaon.backend.search.dictionary.dto.SynonymRequest;
import moaon.backend.search.dictionary.dto.SynonymEntry;
import moaon.backend.search.dictionary.service.BulkConflictException;
import moaon.backend.search.dictionary.service.DuplicateSynonymEntryException;
import moaon.backend.search.dictionary.service.SynonymDictionaryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/dictionaries/synonyms")
@RequiredArgsConstructor
public class SynonymApiController {

    private final SynonymDictionaryService synonymDictionaryService;

    @GetMapping
    public List<SynonymEntry> list() {
        return synonymDictionaryService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SynonymEntry create(@RequestBody @Valid SynonymRequest request) {
        return synonymDictionaryService.add(request.getTerms());
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SynonymEntry> createAll(@RequestBody @Valid BulkRequest request) {
        return synonymDictionaryService.addAll(request.getText());
    }

    @PutMapping("/{id}")
    public SynonymEntry update(@PathVariable Long id, @RequestBody @Valid SynonymRequest request) {
        return synonymDictionaryService.update(id, request.getTerms());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        synonymDictionaryService.delete(id);
    }

    @PostMapping("/reload")
    public ReloadResponse reload() {
        return synonymDictionaryService.reloadSynonyms();
    }

    @ExceptionHandler(DuplicateSynonymEntryException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public DuplicateConflictResponse handleDuplicate(DuplicateSynonymEntryException e) {
        return new DuplicateConflictResponse(e.getConflictEntry(), e.getDuplicateTerm());
    }

    @ExceptionHandler(BulkConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public List<BulkConflict> handleBulkConflict(BulkConflictException e) {
        return e.getConflicts();
    }

    record DuplicateConflictResponse(SynonymEntry conflictEntry, String duplicateTerm) {}
}
