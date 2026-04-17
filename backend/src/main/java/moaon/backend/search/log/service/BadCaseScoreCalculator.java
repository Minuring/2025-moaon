package moaon.backend.search.log.service;

import java.util.ArrayList;
import java.util.List;
import moaon.backend.search.log.domain.SearchLogCapture;
import org.springframework.stereotype.Component;

@Component
public class BadCaseScoreCalculator {

    private static final int SLOW_QUERY_THRESHOLD_MS = 300;

    public BadCaseScore calculate(SearchLogCapture capture) {
        int score = 0;
        List<String> flags = new ArrayList<>();

        if (capture.resultCount() == 0) {
            score += 20;
            flags.add("no_result");
        }

        if (isContentOnlyHeavy(capture)) {
            score += 30;
            flags.add("content_only_heavy");
        }

        if (capture.queryTimeMs() > SLOW_QUERY_THRESHOLD_MS) {
            score += 10;
            flags.add("slow_query");
        }

        return new BadCaseScore(score, flags);
    }

    private boolean isContentOnlyHeavy(SearchLogCapture capture) {
        if (capture.hits().isEmpty()) return false;
        long top5Count = Math.min(5, capture.hits().size());
        long contentOnlyCount = capture.hits().stream()
                .limit(5)
                .filter(h -> h.isContentOnly())
                .count();
        return (double) contentOnlyCount / top5Count >= 0.6;
    }

    public record BadCaseScore(int score, List<String> flags) {}
}
