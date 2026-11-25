package moaon.backend.search.dictionary.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ReloadSearchAnalyzersResponse;
import co.elastic.clients.elasticsearch.synonyms.SynonymRule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.ElasticsearchClientConfig;
import moaon.backend.search.dictionary.domain.SynonymDictionaryEntry;
import moaon.backend.search.dictionary.dto.BulkConflict;
import moaon.backend.search.dictionary.dto.ReloadResponse;
import moaon.backend.search.dictionary.dto.SynonymEntry;
import moaon.backend.search.dictionary.repository.SynonymDictionaryRepository;
import moaon.backend.search.query.ArticleDocument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class SynonymDictionaryService {

    private static final String ARTICLE_INDEX =
            ArticleDocument.class.getAnnotation(Document.class).aliases()[0].alias();
    private static final String SYNONYM_SET_ID = "moaon_synonyms";

    private final SynonymDictionaryRepository synonymDictionaryRepository;
    private final ElasticsearchClient elasticsearchClient;

    public SynonymDictionaryService(
            SynonymDictionaryRepository synonymDictionaryRepository,
            @Qualifier(ElasticsearchClientConfig.BULK_CLIENT) ElasticsearchClient elasticsearchClient
    ) {
        this.synonymDictionaryRepository = synonymDictionaryRepository;
        this.elasticsearchClient = elasticsearchClient;
    }

    public List<SynonymEntry> findAll() {
        return synonymDictionaryRepository.findAll().stream()
                .map(SynonymEntry::from)
                .toList();
    }

    @Transactional
    public SynonymEntry add(List<String> terms) {
        validateTerms(terms);
        checkDuplicate(terms, null);
        SynonymDictionaryEntry entry = synonymDictionaryRepository.save(new SynonymDictionaryEntry(terms));
        return SynonymEntry.from(entry);
    }

    /**
     * 한 줄에 쉼표로 구분한 동의어 묶음 하나("아키텍처, 아키텍쳐, architecture")를 한 번에 추가한다.
     * 기존 항목이나 같은 입력의 다른 줄과 term이 겹치는 줄이 하나라도 있으면 아무것도 추가하지 않는다.
     */
    @Transactional
    public List<SynonymEntry> addAll(String text) {
        List<SynonymDictionaryEntry> entries = parseLines(text);
        List<BulkConflict> conflicts = findConflicts(entries);
        if (!conflicts.isEmpty()) {
            throw new BulkConflictException(conflicts);
        }
        return synonymDictionaryRepository.saveAll(entries).stream()
                .map(SynonymEntry::from)
                .toList();
    }

    @Transactional
    public SynonymEntry update(Long id, List<String> terms) {
        validateTerms(terms);
        checkDuplicate(terms, id);
        SynonymDictionaryEntry entry = synonymDictionaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 synonym id: " + id));
        entry.update(terms);
        return SynonymEntry.from(entry);
    }

    @Transactional
    public void delete(Long id) {
        if (!synonymDictionaryRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 synonym id: " + id);
        }
        synonymDictionaryRepository.deleteById(id);
    }

    public ReloadResponse reloadSynonyms() {
        try {
            List<SynonymRule> rules = synonymDictionaryRepository.findAll().stream()
                    .map(entry -> SynonymRule.of(r -> r.synonyms(entry.getRawExpression())))
                    .toList();

            elasticsearchClient.synonyms().putSynonym(r -> r
                    .id(SYNONYM_SET_ID)
                    .synonymsSet(rules)
            );
            log.info("Synonym set '{}' 업데이트 완료 ({} 개)", SYNONYM_SET_ID, rules.size());

            ReloadSearchAnalyzersResponse response =
                    elasticsearchClient.indices().reloadSearchAnalyzers(r -> r.index(ARTICLE_INDEX));

            String detail = response.reloadDetails().stream()
                    .map(d -> d.index() + " → " + d.reloadedAnalyzers())
                    .collect(Collectors.joining(", "));

            log.info("Synonym reload 성공: {}", detail);
            return new ReloadResponse(true, "ES synonym reload 완료: " + detail);
        } catch (Exception e) {
            log.error("Synonym reload 실패", e);
            return new ReloadResponse(false, "synonym reload 실패: " + e.getMessage());
        }
    }

    private List<SynonymDictionaryEntry> parseLines(String text) {
        List<SynonymDictionaryEntry> entries = text.lines()
                .map(line -> Arrays.stream(line.split(","))
                        .map(String::trim)
                        .filter(term -> !term.isEmpty())
                        .toList())
                .filter(terms -> !terms.isEmpty())
                .map(SynonymDictionaryEntry::new)
                .toList();
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("추가할 항목이 없습니다.");
        }
        return entries;
    }

    private List<BulkConflict> findConflicts(List<SynonymDictionaryEntry> newEntries) {
        Map<String, String> existingRaws = new HashMap<>();
        synonymDictionaryRepository.findAll()
                .forEach(e -> e.terms().forEach(term -> existingRaws.put(normalize(term), e.getRawExpression())));

        Map<String, String> inputRaws = new HashMap<>();
        List<BulkConflict> conflicts = new ArrayList<>();
        for (SynonymDictionaryEntry entry : newEntries) {
            String raw = entry.getRawExpression();
            entry.terms().stream()
                    .map(term -> findConflict(raw, term, existingRaws, inputRaws))
                    .flatMap(Optional::stream)
                    .findFirst()
                    .ifPresent(conflicts::add);
            entry.terms().forEach(term -> inputRaws.putIfAbsent(normalize(term), raw));
        }
        return conflicts;
    }

    private Optional<BulkConflict> findConflict(
            String raw,
            String term,
            Map<String, String> existingRaws,
            Map<String, String> inputRaws
    ) {
        String key = normalize(term);
        if (existingRaws.containsKey(key)) {
            return Optional.of(new BulkConflict(raw, term, existingRaws.get(key)));
        }
        if (inputRaws.containsKey(key)) {
            return Optional.of(new BulkConflict(raw, term, inputRaws.get(key)));
        }
        return Optional.empty();
    }

    private static String normalize(String term) {
        return term.trim().toLowerCase();
    }

    private void checkDuplicate(List<String> newTerms, Long excludeId) {
        List<String> normalizedNew = newTerms.stream().map(t -> t.trim().toLowerCase()).toList();
        synonymDictionaryRepository.findAll().stream()
                .filter(e -> excludeId == null || !e.getId().equals(excludeId))
                .forEach(existing -> {
                    SynonymEntry existingEntry = SynonymEntry.from(existing);
                    existing.terms().stream()
                            .map(t -> t.trim().toLowerCase())
                            .filter(normalizedNew::contains)
                            .findFirst()
                            .ifPresent(dup -> { throw new DuplicateSynonymEntryException(existingEntry, dup); });
                });
    }

    private void validateTerms(List<String> terms) {
        long validCount = terms == null ? 0 : terms.stream().filter(t -> !t.isBlank()).count();
        if (validCount < 1) {
            throw new IllegalArgumentException("유효한 term이 없습니다.");
        }
    }
}
