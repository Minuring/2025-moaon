package moaon.backend.article.api.crawl.service;

import java.net.URL;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.api.crawl.dto.ArticleCrawlResult;
import moaon.backend.article.api.crawl.dto.FinderCrawlResult;
import moaon.backend.article.api.crawl.service.client.AiSummarizer;
import moaon.backend.article.api.crawl.service.client.ContentFinder;
import moaon.backend.article.api.crawl.service.client.ContentFinders;
import moaon.backend.article.domain.ArticleContent;
import moaon.backend.article.repository.db.ArticleContentRepository;
import moaon.backend.global.parser.URLParser;
import moaon.backend.member.domain.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCrawlService {

    private static final ContentFinders FINDER = new ContentFinders();

    private final ArticleContentRepository repository;
    private final AiSummarizer summarizer;

    public ArticleCrawlResult crawl(String url, Member member) {
        URL parsedUrl = URLParser.parse(url);
        ContentFinder finder = FINDER.getFinder(parsedUrl);

        FinderCrawlResult crawlResult = finder.crawl(parsedUrl);
        String summary = summarizer.summarize(crawlResult.content(), member);

        if (summary.isBlank()) {
            return ArticleCrawlResult.withoutSummary(crawlResult);
        }
        return ArticleCrawlResult.success(crawlResult.title(), summary, crawlResult.content());
    }

    @Transactional
    public void saveTemporary(String url, ArticleCrawlResult result) {
        Optional<ArticleContent> content = repository.findByUrl(url);

        if (content.isEmpty()) {
            repository.save(new ArticleContent(url, result.content()));
            return;
        }

        content.get().update(result.content());
    }
}
