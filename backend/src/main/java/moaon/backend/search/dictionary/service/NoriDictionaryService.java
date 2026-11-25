package moaon.backend.search.dictionary.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import moaon.backend.search.dictionary.domain.NoriDictionaryEntry;
import moaon.backend.search.dictionary.dto.BulkConflict;
import moaon.backend.search.dictionary.dto.NoriEntry;
import moaon.backend.search.dictionary.repository.NoriDictionaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoriDictionaryService {

    private final NoriDictionaryRepository noriDictionaryRepository;

    public List<NoriEntry> findAll() {
        return noriDictionaryRepository.findAll().stream()
                .map(NoriEntry::from)
                .toList();
    }

    @Transactional
    public NoriEntry add(String surface, List<String> segments) {
        validateSurface(surface);
        checkDuplicate(surface, null);
        NoriDictionaryEntry entry = noriDictionaryRepository.save(new NoriDictionaryEntry(surface, segments));
        return NoriEntry.from(entry);
    }

    /**
     * dictionary.txt와 같은 형식(한 줄에 "표면어" 또는 "표면어 분해1 분해2")을 한 번에 추가한다.
     * 기존 항목이나 같은 입력의 다른 줄과 겹치는 줄이 하나라도 있으면 아무것도 추가하지 않는다.
     */
    @Transactional
    public List<NoriEntry> addAll(String text) {
        List<NoriDictionaryEntry> entries = parseLines(text);
        List<BulkConflict> conflicts = findConflicts(entries);
        if (!conflicts.isEmpty()) {
            throw new BulkConflictException(conflicts);
        }
        return noriDictionaryRepository.saveAll(entries).stream()
                .map(NoriEntry::from)
                .toList();
    }

    @Transactional
    public NoriEntry update(Long id, String surface, List<String> segments) {
        validateSurface(surface);
        checkDuplicate(surface, id);
        NoriDictionaryEntry entry = noriDictionaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 nori id: " + id));
        entry.update(surface, segments);
        return NoriEntry.from(entry);
    }

    @Transactional
    public void delete(Long id) {
        if (!noriDictionaryRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 nori id: " + id);
        }
        noriDictionaryRepository.deleteById(id);
    }

    private List<NoriDictionaryEntry> parseLines(String text) {
        List<NoriDictionaryEntry> entries = text.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> line.split("\\s+"))
                .map(tokens -> new NoriDictionaryEntry(tokens[0], Arrays.asList(tokens).subList(1, tokens.length)))
                .toList();
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("추가할 항목이 없습니다.");
        }
        return entries;
    }

    private List<BulkConflict> findConflicts(List<NoriDictionaryEntry> newEntries) {
        Map<String, String> existingLines = new HashMap<>();
        noriDictionaryRepository.findAll()
                .forEach(e -> existingLines.put(normalize(e.getSurface()), e.toFileLine()));

        Map<String, String> inputLines = new HashMap<>();
        List<BulkConflict> conflicts = new ArrayList<>();
        for (NoriDictionaryEntry entry : newEntries) {
            String line = entry.toFileLine();
            String key = normalize(entry.getSurface());
            if (existingLines.containsKey(key)) {
                conflicts.add(new BulkConflict(line, entry.getSurface(), existingLines.get(key)));
            } else if (inputLines.containsKey(key)) {
                conflicts.add(new BulkConflict(line, entry.getSurface(), inputLines.get(key)));
            }
            inputLines.putIfAbsent(key, line);
        }
        return conflicts;
    }

    private void checkDuplicate(String surface, Long excludeId) {
        String normalizedNew = normalize(surface);
        noriDictionaryRepository.findAll().stream()
                .filter(e -> excludeId == null || !e.getId().equals(excludeId))
                .filter(e -> e.getSurface().equalsIgnoreCase(surface)
                        || normalize(e.getSurface()).equals(normalizedNew))
                .findFirst()
                .ifPresent(e -> { throw new DuplicateNoriEntryException(NoriEntry.from(e)); });
    }

    private static String normalize(String surface) {
        return surface.replaceAll("\\s+", "").toLowerCase();
    }

    private void validateSurface(String surface) {
        if (surface == null || surface.isBlank()) {
            throw new IllegalArgumentException("surface는 비어있으면 안 됩니다.");
        }
    }
}
