package moaon.backend.search.dictionary.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ReloadSearchAnalyzersResponse;
import co.elastic.clients.elasticsearch.synonyms.SynonymRule;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.dictionary.domain.SynonymDictionaryEntry;
import moaon.backend.search.dictionary.dto.ReloadResponse;
import moaon.backend.search.dictionary.dto.SynonymEntry;
import moaon.backend.search.dictionary.repository.SynonymDictionaryRepository;
import moaon.backend.search.query.ArticleDocument;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class SynonymDictionaryService {

    private static final String ARTICLE_INDEX =
            ArticleDocument.class.getAnnotation(Document.class).aliases()[0].alias();
    private static final String SYNONYM_SET_ID = "moaon_synonyms";

    private final SynonymDictionaryRepository synonymDictionaryRepository;
    private final ElasticsearchClient elasticsearchClient;

    public List<SynonymEntry> findAll() {
        return synonymDictionaryRepository.findAll().stream()
                .map(SynonymEntry::from)
                .toList();
    }

    @Transactional
    public SynonymEntry add(List<String> terms) {
        validateTerms(terms);
        SynonymDictionaryEntry entry = synonymDictionaryRepository.save(new SynonymDictionaryEntry(terms));
        return SynonymEntry.from(entry);
    }

    @Transactional
    public SynonymEntry update(Long id, List<String> terms) {
        validateTerms(terms);
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

    private void validateTerms(List<String> terms) {
        long validCount = terms == null ? 0 : terms.stream().filter(t -> !t.isBlank()).count();
        if (validCount < 1) {
            throw new IllegalArgumentException("유효한 term이 없습니다.");
        }
    }
}
