package moaon.backend.search.admin.service;

import moaon.backend.search.query.log.SearchHitLog;
import moaon.backend.search.query.log.SearchLogCapture;
import moaon.backend.search.admin.service.BadCaseScoreCalculator.BadCaseScore;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class BadCaseScoreCalculatorTest {

    private final BadCaseScoreCalculator calculator = new BadCaseScoreCalculator();

    @Test
    void no_result이면_20점() {
        SearchLogCapture capture = new SearchLogCapture("spring", 0, 100, List.of());
        BadCaseScore result = calculator.calculate(capture);
        assertThat(result.score()).isEqualTo(20);
        assertThat(result.flags()).contains("no_result");
    }

    @Test
    void top5_중_content_only_비율_60퍼센트_이상이면_30점() {
        List<SearchHitLog> hits = List.of(
                contentOnly(1), contentOnly(2), contentOnly(3), titleHit(4), titleHit(5)
        );
        SearchLogCapture capture = new SearchLogCapture("spring", 5, 50, hits);
        BadCaseScore result = calculator.calculate(capture);
        assertThat(result.score()).isEqualTo(30);
        assertThat(result.flags()).contains("content_only_heavy");
    }

    @Test
    void slow_query_300ms_초과이면_10점() {
        SearchLogCapture capture = new SearchLogCapture("spring", 5, 301, List.of());
        BadCaseScore result = calculator.calculate(capture);
        assertThat(result.score()).isEqualTo(10);
        assertThat(result.flags()).contains("slow_query");
    }

    @Test
    void 규칙_복합_합산() {
        List<SearchHitLog> hits = List.of(
                contentOnly(1), contentOnly(2), contentOnly(3), contentOnly(4), titleHit(5)
        );
        SearchLogCapture capture = new SearchLogCapture("spring", 5, 500, hits);
        BadCaseScore result = calculator.calculate(capture);
        assertThat(result.score()).isEqualTo(40);
        assertThat(result.flags()).containsExactlyInAnyOrder("content_only_heavy", "slow_query");
    }

    @Test
    void 정상_케이스_0점() {
        List<SearchHitLog> hits = List.of(titleHit(1), titleHit(2));
        SearchLogCapture capture = new SearchLogCapture("spring", 2, 50, hits);
        BadCaseScore result = calculator.calculate(capture);
        assertThat(result.score()).isEqualTo(0);
        assertThat(result.flags()).isEmpty();
    }

    private SearchHitLog contentOnly(int rank) {
        return SearchHitLog.of(rank, (long) rank, "제목" + rank, 3.0f, Map.of("title", List.of(), "summary", List.of()));
    }

    private SearchHitLog titleHit(int rank) {
        return SearchHitLog.of(rank, (long) rank, "제목" + rank, 9.0f, Map.of("title", List.of("<mark>spring</mark>")));
    }
}
