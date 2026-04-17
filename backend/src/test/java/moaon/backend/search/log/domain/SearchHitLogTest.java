package moaon.backend.search.log.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class SearchHitLogTest {

    @Test
    void title_highlight_있으면_matched_fields에_title_포함() {
        Map<String, List<String>> highlights = Map.of(
                "title", List.of("<mark>spring</mark> 배포"),
                "summary", List.of()
        );
        SearchHitLog hit = SearchHitLog.of(1, 42L, "Spring 배포", 9.5f, highlights);
        assertThat(hit.matchedFields()).contains("title");
        assertThat(hit.matchedFields()).doesNotContain("content");
    }

    @Test
    void summary_highlight_있으면_matched_fields에_summary_포함() {
        Map<String, List<String>> highlights = Map.of(
                "title", List.of(),
                "summary", List.of("본문에 <mark>spring</mark> 등장")
        );
        SearchHitLog hit = SearchHitLog.of(1, 42L, "글 제목", 7.2f, highlights);
        assertThat(hit.matchedFields()).contains("summary");
        assertThat(hit.matchedFields()).doesNotContain("content");
    }

    @Test
    void highlight_없으면_content_매칭으로_추론() {
        Map<String, List<String>> highlights = Map.of(
                "title", List.of(),
                "summary", List.of()
        );
        SearchHitLog hit = SearchHitLog.of(1, 42L, "글 제목", 3.1f, highlights);
        assertThat(hit.matchedFields()).containsExactly("content");
    }

    @Test
    void highlight_맵이_비어있으면_content_매칭으로_추론() {
        SearchHitLog hit = SearchHitLog.of(1, 42L, "글 제목", 3.1f, Map.of());
        assertThat(hit.matchedFields()).containsExactly("content");
    }

    @Test
    void snippets에는_비어있지_않은_highlight만_포함() {
        Map<String, List<String>> highlights = Map.of(
                "title", List.of("<mark>spring</mark>"),
                "summary", List.of()
        );
        SearchHitLog hit = SearchHitLog.of(1, 42L, "글 제목", 9.5f, highlights);
        assertThat(hit.snippets()).containsKey("title");
        assertThat(hit.snippets()).doesNotContainKey("summary");
    }
}
