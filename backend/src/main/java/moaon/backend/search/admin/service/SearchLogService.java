package moaon.backend.search.admin.service;

import java.util.List;
import moaon.backend.search.admin.domain.SearchCase;
import moaon.backend.search.admin.domain.SearchCaseResult;
import moaon.backend.search.query.log.SearchHitLog;
import moaon.backend.search.query.log.SearchLogCapture;
import moaon.backend.search.admin.repository.SearchCaseRepository;
import moaon.backend.search.admin.repository.SearchCaseResultRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SearchLogService {

    private final SearchCaseRepository searchCaseRepository;
    private final SearchCaseResultRepository searchCaseResultRepository;
    private final BadCaseScoreCalculator badCaseScoreCalculator;

    public SearchLogService(SearchCaseRepository searchCaseRepository,
            SearchCaseResultRepository searchCaseResultRepository,
            BadCaseScoreCalculator badCaseScoreCalculator) {
        this.searchCaseRepository = searchCaseRepository;
        this.searchCaseResultRepository = searchCaseResultRepository;
        this.badCaseScoreCalculator = badCaseScoreCalculator;
    }

    @Async
    public void saveAsync(SearchLogCapture logCapture) {
        save(logCapture);
    }

    void save(SearchLogCapture logCapture) {
        if (logCapture == null || logCapture.query() == null || logCapture.query().isBlank()) {
            return;
        }

        BadCaseScoreCalculator.BadCaseScore badCaseScore = badCaseScoreCalculator.calculate(logCapture);

        SearchCase searchCase = new SearchCase(
                logCapture.query(),
                logCapture.resultCount(),
                logCapture.queryTimeMs(),
                badCaseScore.score(),
                badCaseScore.flags()
        );
        searchCaseRepository.save(searchCase);

        List<SearchHitLog> hits = logCapture.hits() != null ? logCapture.hits() : List.of();
        List<SearchCaseResult> results = hits.stream()
                .map(hit -> new SearchCaseResult(
                        searchCase.getId(),
                        hit.rank(),
                        hit.docId(),
                        hit.title(),
                        hit.score(),
                        hit.matchedFields(),
                        hit.snippets()
                ))
                .toList();
        if (!results.isEmpty()) {
            searchCaseResultRepository.saveAll(results);
        }
    }
}
