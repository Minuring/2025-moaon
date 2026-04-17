package moaon.backend.search.log.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.log.*;
import moaon.backend.search.log.domain.SearchHitLog;
import moaon.backend.search.log.domain.SearchLogCapture;
import moaon.backend.search.log.domain.SearchLogDocument;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchLogService {

    private final SearchLogRepository searchLogRepository;
    private final BadCaseScoreCalculator badCaseScoreCalculator;

    @Async
    public void saveAsync(SearchLogCapture logCapture) {
        if (logCapture == null || logCapture.query() == null || logCapture.query().isBlank()) {
            return;
        }
        try {
            BadCaseScoreCalculator.BadCaseScore badCaseScore = badCaseScoreCalculator.calculate(logCapture);
            List<SearchHitLog> hits = logCapture.hits() != null ? logCapture.hits() : List.of();
            List<SearchLogDocument.SearchedDoc> results = hits.stream()
                    .map(hit -> new SearchLogDocument.SearchedDoc(
                            hit.rank(), hit.docId(), hit.title(), hit.score(), hit.matchedFields()))
                    .toList();
            SearchLogDocument doc = new SearchLogDocument(
                    logCapture.query(),
                    logCapture.resultCount(),
                    logCapture.queryTimeMs(),
                    badCaseScore.score(),
                    badCaseScore.flags(),
                    results
            );
            searchLogRepository.save(doc);
        } catch (Exception e) {
            log.error("검색 로그 ES 저장 실패: query={}", logCapture.query(), e);
        }
    }
}
