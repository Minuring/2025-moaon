package moaon.backend.search.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import moaon.backend.search.admin.domain.ReviewStatus;
import moaon.backend.search.admin.domain.SearchCase;
import moaon.backend.search.admin.domain.SearchCaseResult;
import moaon.backend.search.admin.dto.ReviewRequest;
import moaon.backend.search.admin.dto.SearchCaseDetail;
import moaon.backend.search.admin.dto.SearchCaseSummary;
import moaon.backend.search.admin.repository.SearchCaseRepository;
import moaon.backend.search.admin.repository.SearchCaseResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SearchCaseAdminServiceTest {

    @Mock
    SearchCaseRepository searchCaseRepository;

    @Mock
    SearchCaseResultRepository searchCaseResultRepository;

    @InjectMocks
    SearchCaseAdminService searchCaseAdminService;

    @Test
    void list_returnsPageSortedByBadCaseScore() {
        SearchCase sc = createSearchCase("query1", 20, List.of("no_result"));
        Page<SearchCase> page = new PageImpl<>(List.of(sc));
        given(searchCaseRepository.findAll(any(Pageable.class))).willReturn(page);

        Page<SearchCaseSummary> result = searchCaseAdminService.list(0, 20, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).query()).isEqualTo("query1");
    }

    @Test
    void detail_returnsSearchCaseWithResults() {
        SearchCase sc = createSearchCase("query1", 20, List.of("no_result"));
        SearchCaseResult result1 = new SearchCaseResult(sc.getId(), 1, 42L, "Spring Boot", 1.2f,
            List.of("title"), Map.of("title", List.of("Spring <mark>Boot</mark>")));
        given(searchCaseRepository.findById(sc.getId())).willReturn(Optional.of(sc));
        given(searchCaseResultRepository.findByCaseIdOrderByRankAsc(sc.getId()))
            .willReturn(List.of(result1));

        SearchCaseDetail detail = searchCaseAdminService.detail(sc.getId());

        assertThat(detail.query()).isEqualTo("query1");
        assertThat(detail.hits()).hasSize(1);
        assertThat(detail.hits().get(0).rank()).isEqualTo(1);
    }

    @Test
    void review_updatesStatusAndMemo() {
        SearchCase sc = createSearchCase("query1", 0, List.of());
        given(searchCaseRepository.findById(sc.getId())).willReturn(Optional.of(sc));

        searchCaseAdminService.review(sc.getId(), new ReviewRequest(ReviewStatus.BAD, "확인됨"));

        assertThat(sc.getStatus()).isEqualTo(ReviewStatus.BAD);
        assertThat(sc.getMemo()).isEqualTo("확인됨");
    }

    private SearchCase createSearchCase(String query, int badCaseScore, List<String> flags) {
        return new SearchCase(query, 0, 100, badCaseScore, flags);
    }
}
