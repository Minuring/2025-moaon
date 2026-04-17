package moaon.backend.search.log.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.search.log.domain.SearchLogDocument;
import moaon.backend.search.log.dto.SearchLogDetail;
import moaon.backend.search.log.dto.SearchLogSummary;
import moaon.backend.search.log.SearchLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SearchLogAdminServiceTest {

    @Mock
    SearchLogRepository searchLogRepository;

    @InjectMocks
    SearchLogAdminService searchLogAdminService;

    @Test
    void list_ES에서_페이지를_조회해_SearchLogSummary로_반환한다() {
        SearchLogDocument doc = new SearchLogDocument("spring", 5, 120, 20,
                List.of("no_result"), List.of());
        given(searchLogRepository.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(doc)));

        Page<SearchLogSummary> result = searchLogAdminService.list(0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).query()).isEqualTo("spring");
        assertThat(result.getContent().get(0).badCaseScore()).isEqualTo(20);
    }

    @Test
    void detail_존재하는_id면_hits를_포함한_SearchLogDetail을_반환한다() {
        SearchLogDocument.SearchedDoc hit = new SearchLogDocument.SearchedDoc(1, 42L, "제목", 1.5f, List.of("title"));
        SearchLogDocument doc = new SearchLogDocument("spring", 1, 100, 0, List.of(), List.of(hit));
        given(searchLogRepository.findById(doc.getId())).willReturn(Optional.of(doc));

        SearchLogDetail detail = searchLogAdminService.detail(doc.getId());

        assertThat(detail.query()).isEqualTo("spring");
        assertThat(detail.hits()).hasSize(1);
        assertThat(detail.hits().get(0).rank()).isEqualTo(1);
    }

    @Test
    void detail_없는_id면_CustomException을_던진다() {
        given(searchLogRepository.findById("nonexistent")).willReturn(Optional.empty());

        assertThatThrownBy(() -> searchLogAdminService.detail("nonexistent"))
                .isInstanceOf(CustomException.class);
    }
}
