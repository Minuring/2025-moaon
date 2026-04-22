package moaon.backend.search.log.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import moaon.backend.search.log.SearchLogRepository;
import moaon.backend.search.log.domain.FieldMatchStats;
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

    @InjectMocks
    SearchLogService searchLogService;

    @Test
    void saveAsync_캡처를_ES_문서로_변환해_저장한다() {
        SearchLogCapture capture = new SearchLogCapture("spring boot", 0, 150,
                List.of(new SearchHitLog(1, 42L, "Spring Boot Guide", 1.2f,
                        List.of("title"), Map.of())),
                new FieldMatchStats(1, 0, 0, 1), false, 0);

        searchLogService.saveAsync(capture);

        verify(searchLogRepository).save(argThat(doc ->
                doc.getQuery().equals("spring boot") &&
                doc.getSearchedDocs().size() == 1 &&
                doc.getSearchedDocs().get(0).getRank() == 1
        ));
    }
}
