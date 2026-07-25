package moaon.backend.article.draft.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import moaon.backend.article.draft.repository.ArticleDraftRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleDraftCleanerTest {

    @Mock
    private ArticleDraftRepository articleDraftRepository;

    @InjectMocks
    private ArticleDraftCleaner cleaner;

    @DisplayName("deleteExpiredDrafts: 기준 시각 이전 draft 삭제를 레포지토리에 위임한다")
    @Test
    void deleteExpiredDrafts() {
        // given
        when(articleDraftRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(3);

        // when
        cleaner.deleteExpiredDrafts();

        // then
        verify(articleDraftRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }
}
