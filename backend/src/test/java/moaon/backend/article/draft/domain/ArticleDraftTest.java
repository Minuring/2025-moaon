package moaon.backend.article.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.fixture.Fixture;
import moaon.backend.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArticleDraftTest {

    @DisplayName("applyAnalysis: 255자 이하 요약은 그대로 저장한다")
    @Test
    void applyAnalysis_keepsShortSummary() {
        ArticleDraft draft = new ArticleDraft(Fixture.anyMember(), "https://example.com", "title", "content");

        draft.applyAnalysis("짧은 요약입니다.", Sector.BE, List.of(Topic.TECHNOLOGY_ADOPTION), List.of());

        assertThat(draft.getAnalyzedSummary()).isEqualTo("짧은 요약입니다.");
    }

    @DisplayName("applyAnalysis: 255자를 초과하는 요약은 컬럼 길이에 맞게 잘라서 저장한다")
    @Test
    void applyAnalysis_truncatesLongSummary() {
        ArticleDraft draft = new ArticleDraft(Fixture.anyMember(), "https://example.com", "title", "content");
        String longSummary = "가".repeat(300);

        draft.applyAnalysis(longSummary, Sector.BE, List.of(Topic.TECHNOLOGY_ADOPTION), List.of());

        assertThat(draft.getAnalyzedSummary().length()).isLessThanOrEqualTo(255);
    }
}
