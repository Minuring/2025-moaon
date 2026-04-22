package moaon.backend.search.log.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.log.SearchLogRepository;
import moaon.backend.search.log.domain.SearchLogCapture;
import moaon.backend.search.log.domain.SearchLogDocument;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchLogService {

    private final SearchLogRepository searchLogRepository;

    @Async
    public void saveAsync(SearchLogCapture logCapture) {
        try {
            List<SearchLogDocument.SearchedDoc> results = logCapture.hits().stream()
                    .map(hit -> new SearchLogDocument.SearchedDoc(
                            hit.rank(), hit.docId(), hit.title(), hit.score(), hit.matchedFields()))
                    .toList();
            SearchLogDocument doc = new SearchLogDocument(
                    logCapture.query(),
                    logCapture.resultCount(),
                    logCapture.queryTimeMs(),
                    logCapture.hasCursor(),
                    logCapture.synonymMatchCount(),
                    logCapture.fieldMatchStats(),
                    results
            );
            searchLogRepository.save(doc);
        } catch (Exception e) {
            log.error("검색 로그 ES 저장 실패: query={}", logCapture.query(), e);
        }
    }
}
