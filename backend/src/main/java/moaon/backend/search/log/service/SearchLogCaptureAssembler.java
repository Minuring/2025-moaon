package moaon.backend.search.log.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.search.log.domain.FieldMatchStats;
import moaon.backend.search.log.domain.SearchHitLog;
import moaon.backend.search.log.domain.SearchLogCapture;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchLogCaptureAssembler {

    private static final String ARTICLES_INDEX = "articles";
    private static final String SEARCH_ANALYZER = "article_nori_search_analyzer";
    private static final String BASE_ANALYZER = "article_nori_analyzer";
    private static final Pattern MARK_PATTERN = Pattern.compile("<mark>(.*?)</mark>");

    private final ElasticsearchClient elasticsearchClient;

    public Optional<SearchLogCapture> assemble(List<SearchHitLog> hitLogs, long totalHits, ArticleQueryCondition condition, int queryTimeMs) {
        String query = condition.search() != null ? condition.search().value() : null;
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new SearchLogCapture(
                query,
                (int) Math.min(totalHits, Integer.MAX_VALUE),
                queryTimeMs,
                hitLogs,
                computeFieldMatchStats(hitLogs),
                condition.cursor() != null,
                computeSynonymMatchCount(query, hitLogs)
        ));
    }

    private FieldMatchStats computeFieldMatchStats(List<SearchHitLog> hitLogs) {
        int titleCount = 0, summaryCount = 0, contentCount = 0;
        for (SearchHitLog hit : hitLogs) {
            if (hit.matchedFields().contains("title")) titleCount++;
            if (hit.matchedFields().contains("summary")) summaryCount++;
            if (hit.hasNoTitleOrSummaryMatch()) contentCount++;
        }
        return new FieldMatchStats(titleCount, summaryCount, contentCount, hitLogs.size());
    }

    private Integer computeSynonymMatchCount(String query, List<SearchHitLog> hitLogs) {
        try {
            Set<String> baseTokens = analyze(query, BASE_ANALYZER);
            Set<String> expandedTokens = analyze(query, SEARCH_ANALYZER);
            Set<String> synonymTokens = expandedTokens.stream()
                    .filter(t -> !baseTokens.contains(t))
                    .collect(Collectors.toSet());

            if (synonymTokens.isEmpty()) {
                return 0;
            }

            return (int) hitLogs.stream()
                    .filter(hit -> isSynonymOnlyMatch(hit.snippets(), synonymTokens, baseTokens))
                    .count();
        } catch (Exception e) {
            log.warn("동의어 분석 실패: query={}", query, e);
            return null;
        }
    }

    private Set<String> analyze(String text, String analyzer) throws IOException {
        return elasticsearchClient.indices()
                .analyze(r -> r.index(ARTICLES_INDEX).analyzer(analyzer).text(text))
                .tokens().stream()
                .map(t -> t.token().toLowerCase())
                .collect(Collectors.toSet());
    }

    private boolean isSynonymOnlyMatch(Map<String, List<String>> snippets, Set<String> synonymTokens, Set<String> baseTokens) {
        List<String> allSnippets = snippets.values().stream().flatMap(Collection::stream).toList();
        boolean hasSynonymHighlight = allSnippets.stream().anyMatch(s -> containsToken(s, synonymTokens));
        if (!hasSynonymHighlight) return false;
        boolean hasBaseHighlight = allSnippets.stream().anyMatch(s -> containsToken(s, baseTokens));
        return !hasBaseHighlight;
    }

    private boolean containsToken(String snippet, Set<String> tokens) {
        Matcher matcher = MARK_PATTERN.matcher(snippet);
        while (matcher.find()) {
            if (tokens.contains(matcher.group(1).toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
