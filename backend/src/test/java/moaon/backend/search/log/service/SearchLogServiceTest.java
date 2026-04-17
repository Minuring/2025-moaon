package moaon.backend.search.log.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;

import moaon.backend.search.log.SearchLogRepository;
import moaon.backend.search.log.domain.SearchHitLog;
import moaon.backend.search.log.domain.SearchLogCapture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchLogServiceTest {

    @Mock
    SearchLogRepository searchLogRepository;

    @Mock
    BadCaseScoreCalculator badCaseScoreCalculator;

    @InjectMocks
    SearchLogService searchLogService;

    @Test
    void saveAsync_쿼리가_있으면_ES에_문서를_저장한다() {
        given(badCaseScoreCalculator.calculate(any()))
                .willReturn(new BadCaseScoreCalculator.BadCaseScore(20, List.of("no_result")));
        SearchLogCapture capture = new SearchLogCapture("spring boot", 0, 150,
                List.of(new SearchHitLog(1, 42L, "Spring Boot Guide", 1.2f,
                        List.of("title"), Map.of())));

        searchLogService.saveAsync(capture);

        verify(searchLogRepository).save(argThat(doc ->
                doc.getQuery().equals("spring boot") &&
                doc.getBadCaseScore() == 20 &&
                doc.getSearchedDocs().size() == 1 &&
                doc.getSearchedDocs().get(0).getRank() == 1
        ));
    }

    @Test
    void saveAsync_쿼리가_null이면_저장하지_않는다() {
        searchLogService.saveAsync(new SearchLogCapture(null, 0, 100, List.of()));
        verifyNoInteractions(searchLogRepository, badCaseScoreCalculator);
    }

    @Test
    void saveAsync_쿼리가_공백이면_저장하지_않는다() {
        searchLogService.saveAsync(new SearchLogCapture("   ", 0, 100, List.of()));
        verifyNoInteractions(searchLogRepository, badCaseScoreCalculator);
    }
}
