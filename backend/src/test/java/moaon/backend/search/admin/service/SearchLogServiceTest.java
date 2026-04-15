package moaon.backend.search.admin.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import moaon.backend.search.admin.domain.SearchCase;
import moaon.backend.search.admin.domain.SearchCaseResult;
import moaon.backend.search.query.log.SearchHitLog;
import moaon.backend.search.query.log.SearchLogCapture;
import moaon.backend.search.admin.repository.SearchCaseRepository;
import moaon.backend.search.admin.repository.SearchCaseResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchLogServiceTest {

    @Mock
    SearchCaseRepository searchCaseRepository;

    @Mock
    SearchCaseResultRepository searchCaseResultRepository;

    @Mock
    BadCaseScoreCalculator badCaseScoreCalculator;

    @InjectMocks
    SearchLogService searchLogService;

    @Test
    void save_whenQueryPresent_savesSearchCaseAndResults() {
        // Given
        given(badCaseScoreCalculator.calculate(any()))
                .willReturn(new BadCaseScoreCalculator.BadCaseScore(20, List.of("no_result")));
        given(searchCaseRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        SearchLogCapture capture = new SearchLogCapture(
                "spring boot", 0, 150,
                List.of(new SearchHitLog(1, 42L, "Spring Boot Guide", 1.2f,
                        List.of("title"), Map.of("title", List.of("Spring <mark>Boot</mark>"))))
        );

        // When
        searchLogService.save(capture);

        // Then
        verify(searchCaseRepository).save(any(SearchCase.class));
        verify(searchCaseResultRepository).saveAll(argThat((List<SearchCaseResult> list) -> list.size() == 1));
    }

    @Test
    void save_whenQueryNull_skips() {
        searchLogService.save(new SearchLogCapture(null, 0, 100, List.of()));
        verifyNoInteractions(searchCaseRepository, searchCaseResultRepository);
    }

    @Test
    void save_whenQueryBlank_skips() {
        searchLogService.save(new SearchLogCapture("   ", 0, 100, List.of()));
        verifyNoInteractions(searchCaseRepository, searchCaseResultRepository);
    }
}
