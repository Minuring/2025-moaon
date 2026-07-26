package moaon.backend.article.draft.service;

import java.net.URL;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moaon.backend.article.draft.client.FinderCrawlResult;
import moaon.backend.article.draft.client.AiSummarization;
import moaon.backend.article.draft.client.AiSummarizer;
import moaon.backend.article.draft.client.ContentFinder;
import moaon.backend.article.draft.client.ContentFinders;
import moaon.backend.article.draft.ArticleDraft;
import moaon.backend.article.draft.ArticleDraftRepository;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.global.util.Parsers;
import moaon.backend.member.Member;
import moaon.backend.member.service.MemberService;
import moaon.backend.techStack.domain.TechStack;
import moaon.backend.techStack.TechStackResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleDraftService {

    private final ArticleDraftRepository articleDraftRepository;
    private final ContentFinders contentFinders;
    private final AiSummarizer aiSummarizer;
    private final TechStackResolver techStackResolver;
    private final MemberService memberService;

    @Transactional
    public ArticleDraft create(String url, Member member) {
        URL parsedUrl = Parsers.parseURL(url);
        ContentFinder finder = contentFinders.getFinder(parsedUrl);
        FinderCrawlResult crawlResult = finder.crawl(parsedUrl);

        ArticleDraft draft = new ArticleDraft(member, url, crawlResult.title(), crawlResult.content());
        return articleDraftRepository.save(draft);
    }

    @Transactional
    public ArticleDraft analyze(Long draftId, Member member) {
        ArticleDraft draft = getOwnedDraft(draftId, member);

        AiSummarization summarization = aiSummarizer.summarize(draft.getCrawledContent(), member);
        if (summarization.isBlank()) {
            return draft;
        }

        memberService.increaseCrawlCount(member.getId());
        List<TechStack> resolvedTechStacks = techStackResolver.resolve(summarization.techStacks());
        draft.applyAnalysis(
                summarization.summary(),
                summarization.sector(),
                summarization.topics(),
                resolvedTechStacks.stream().map(TechStack::getName).toList()
        );
        return draft;
    }

    private ArticleDraft getOwnedDraft(Long draftId, Member member) {
        ArticleDraft draft = articleDraftRepository.findById(draftId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_DRAFT_NOT_FOUND));
        if (!draft.isOwnedBy(member)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_MEMBER);
        }
        return draft;
    }
}
