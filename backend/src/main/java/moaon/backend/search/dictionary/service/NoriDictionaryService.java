package moaon.backend.search.dictionary.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.ElasticsearchClientConfig;
import moaon.backend.search.dictionary.domain.NoriDictionaryEntry;
import moaon.backend.search.dictionary.dto.ReloadResponse;
import moaon.backend.search.dictionary.dto.NoriEntry;
import moaon.backend.search.dictionary.repository.NoriDictionaryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class NoriDictionaryService {

    private static final String ARTICLE_INDEX = "articles";

    private final NoriDictionaryRepository noriDictionaryRepository;
    private final ElasticsearchClient elasticsearchClient;

    public NoriDictionaryService(
            NoriDictionaryRepository noriDictionaryRepository,
            @Qualifier(ElasticsearchClientConfig.BULK_CLIENT) ElasticsearchClient elasticsearchClient
    ) {
        this.noriDictionaryRepository = noriDictionaryRepository;
        this.elasticsearchClient = elasticsearchClient;
    }

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

    public ReloadResponse reloadIndex() {
        try {
            elasticsearchClient.indices().close(r -> r.index(ARTICLE_INDEX));
            log.info("ES 인덱스 close: {}", ARTICLE_INDEX);

            elasticsearchClient.indices().open(r -> r.index(ARTICLE_INDEX));
            log.info("ES 인덱스 open: {}", ARTICLE_INDEX);

            return new ReloadResponse(true, "Nori 사전 반영 완료 (인덱스 재시작됨)");
        } catch (Exception e) {
            log.error("Nori 인덱스 reload 실패", e);
            return new ReloadResponse(false, "Nori 인덱스 reload 실패: " + e.getMessage());
        }
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
