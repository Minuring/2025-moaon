package moaon.backend.search.dictionary.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moaon.backend.search.dictionary.model.NoriEntry;
import moaon.backend.search.dictionary.repository.NoriFileRepository;
import moaon.backend.search.indexing.ReindexFlagService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoriDictionaryService {

    private final NoriFileRepository noriFileRepository;
    private final ReindexFlagService reindexFlagService;

    public List<NoriEntry> findAll() {
        return noriFileRepository.findAll();
    }

    public NoriEntry add(String surface, List<String> segments) {
        validateSurface(surface);
        List<NoriEntry> all = noriFileRepository.findAll();
        int newId = all.size() + 1;
        String raw = buildRaw(surface, segments);
        NoriEntry entry = new NoriEntry(newId, surface, segments, raw);
        all.add(entry);
        noriFileRepository.saveAll(all);
        reindexFlagService.markDirty();
        return entry;
    }

    public NoriEntry update(int id, String surface, List<String> segments) {
        validateSurface(surface);
        List<NoriEntry> all = noriFileRepository.findAll();
        if (id < 1 || id > all.size()) {
            throw new IllegalArgumentException("존재하지 않는 nori id: " + id);
        }
        String raw = buildRaw(surface, segments);
        NoriEntry updated = new NoriEntry(id, surface, segments, raw);
        all.set(id - 1, updated);
        noriFileRepository.saveAll(all);
        reindexFlagService.markDirty();
        return updated;
    }

    public void delete(int id) {
        List<NoriEntry> all = noriFileRepository.findAll();
        if (id < 1 || id > all.size()) {
            throw new IllegalArgumentException("존재하지 않는 nori id: " + id);
        }
        all.remove(id - 1);
        List<NoriEntry> renumbered = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            NoriEntry e = all.get(i);
            renumbered.add(new NoriEntry(i + 1, e.getSurface(), e.getSegments(), e.getRaw()));
        }
        noriFileRepository.saveAll(renumbered);
        reindexFlagService.markDirty();
    }

    private String buildRaw(String surface, List<String> segments) {
        return segments.isEmpty() ? surface : surface + " " + String.join(" ", segments);
    }

    private void validateSurface(String surface) {
        if (surface == null || surface.isBlank()) {
            throw new IllegalArgumentException("surface는 비어있으면 안 됩니다.");
        }
    }
}
