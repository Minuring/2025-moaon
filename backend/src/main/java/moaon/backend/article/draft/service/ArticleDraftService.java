package moaon.backend.article.draft.service;

import java.net.URL;
import lombok.RequiredArgsConstructor;
import moaon.backend.article.api.crawl.dto.FinderCrawlResult;
import moaon.backend.article.api.crawl.service.client.ContentFinder;
import moaon.backend.article.api.crawl.service.client.ContentFinders;
import moaon.backend.article.draft.domain.ArticleDraft;
import moaon.backend.article.draft.repository.ArticleDraftRepository;
import moaon.backend.global.parser.URLParser;
import moaon.backend.member.domain.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleDraftService {

    private final ArticleDraftRepository articleDraftRepository;
    private final ContentFinders contentFinders;

    @Transactional
    public ArticleDraft create(String url, Member member) {
        URL parsedUrl = URLParser.parse(url);
        ContentFinder finder = contentFinders.getFinder(parsedUrl);
        FinderCrawlResult crawlResult = finder.crawl(parsedUrl);

        ArticleDraft draft = new ArticleDraft(member, url, crawlResult.title(), crawlResult.content());
        return articleDraftRepository.save(draft);
    }
}
