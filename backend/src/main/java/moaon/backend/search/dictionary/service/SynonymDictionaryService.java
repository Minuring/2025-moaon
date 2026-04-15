package moaon.backend.search.dictionary.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moaon.backend.search.dictionary.model.SynonymEntry;
import moaon.backend.search.dictionary.repository.SynonymFileRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SynonymDictionaryService {

    private final SynonymFileRepository synonymFileRepository;

    public List<SynonymEntry> findAll() {
        return synonymFileRepository.findAll();
    }

    public SynonymEntry add(List<String> terms) {
        validateTerms(terms);
        List<SynonymEntry> all = synonymFileRepository.findAll();
        int newId = all.size() + 1;
        SynonymEntry entry = new SynonymEntry(newId, terms, String.join(", ", terms));
        all.add(entry);
        synonymFileRepository.saveAll(all);
        return entry;
    }

    public SynonymEntry update(int id, List<String> terms) {
        validateTerms(terms);
        List<SynonymEntry> all = synonymFileRepository.findAll();
        if (id < 1 || id > all.size()) {
            throw new IllegalArgumentException("존재하지 않는 synonym id: " + id);
        }
        SynonymEntry updated = new SynonymEntry(id, terms, String.join(", ", terms));
        all.set(id - 1, updated);
        synonymFileRepository.saveAll(all);
        return updated;
    }

    public void delete(int id) {
        List<SynonymEntry> all = synonymFileRepository.findAll();
        if (id < 1 || id > all.size()) {
            throw new IllegalArgumentException("존재하지 않는 synonym id: " + id);
        }
        all.remove(id - 1);
        List<SynonymEntry> renumbered = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            SynonymEntry e = all.get(i);
            renumbered.add(new SynonymEntry(i + 1, e.getTerms(), e.getRaw()));
        }
        synonymFileRepository.saveAll(renumbered);
    }

    private void validateTerms(List<String> terms) {
        long validCount = terms == null ? 0 : terms.stream().filter(t -> !t.isBlank()).count();
        if (validCount < 1) {
            throw new IllegalArgumentException("유효한 term이 없습니다.");
        }
    }
}
