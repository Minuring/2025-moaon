package moaon.backend.search.admin.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class SearchCaseTest {

    @Test
    void 생성_시_status는_NEW이다() {
        SearchCase sc = new SearchCase("spring", 15, 42, 25, List.of("no_result"));
        assertThat(sc.getStatus()).isEqualTo(ReviewStatus.NEW);
        assertThat(sc.getMemo()).isNull();
    }

    @Test
    void review_호출_시_status와_memo가_변경된다() {
        SearchCase sc = new SearchCase("spring", 15, 42, 25, List.of());
        sc.review(ReviewStatus.BAD, "본문 오염");
        assertThat(sc.getStatus()).isEqualTo(ReviewStatus.BAD);
        assertThat(sc.getMemo()).isEqualTo("본문 오염");
    }

    @Test
    void review_시_memo는_null_허용() {
        SearchCase sc = new SearchCase("spring", 15, 42, 25, List.of());
        sc.review(ReviewStatus.SKIP, null);
        assertThat(sc.getStatus()).isEqualTo(ReviewStatus.SKIP);
        assertThat(sc.getMemo()).isNull();
    }
}
