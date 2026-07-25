package moaon.backend.article.draft.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.draft.repository.ArticleDraftRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleDraftCleaner {

    private final ArticleDraftRepository articleDraftRepository;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void deleteExpiredDrafts() {
        int deleted = articleDraftRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(1));
        log.info("만료된 아티클 초안 {}건 삭제", deleted);
    }
}
