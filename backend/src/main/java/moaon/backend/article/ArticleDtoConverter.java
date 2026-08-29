package moaon.backend.article;

import lombok.RequiredArgsConstructor;
import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.dto.ArticleCreateParams;
import moaon.backend.article.dto.ArticleCreateRequest;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.dto.ArticleSearchRequest;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.techStack.TechStackResolver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleDtoConverter {

    private final TechStackResolver techStackResolver;

    public ArticleQueryCondition convert(ArticleSearchRequest r) {
        return new ArticleQueryCondition(
                !r.hasSearch() ? null : new SearchKeyword(r.getSearch()),
                !r.hasSector() ? null : Sector.of(r.getSector()),
                !r.hasTopics() ? null : r.getTopics().stream().map(Topic::of).toList(),
                !r.hasTechStacks() ? null : techStackResolver.resolve(r.getTechStacks()),
                !r.hasSort() ? null : ArticleSortType.from(r.getSort()),
                r.getLimit(),
                !r.hasCursor() ? null : new ArticleCursor(r.getCursor())
        );
    }

    public ArticleCreateParams convert(ArticleCreateRequest r) {
        return new ArticleCreateParams(
                r.projectId(),
                r.title(),
                r.summary(),
                techStackResolver.resolve(r.techStacks()),
                r.draftId(),
                Sector.of(r.sector()),
                r.topics().stream().map(Topic::of).toList()
        );
    }
}
