package moaon.backend.article;

import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.dto.ArticleSearchRequest;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.techStack.TechStackResolver;
import moaon.backend.techStack.domain.TechStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ArticleDtoConverterTest {

    private final TechStackResolver techStackResolver = Mockito.mock(TechStackResolver.class);
    private final ArticleDtoConverter converter = new ArticleDtoConverter(techStackResolver);

    @DisplayName("Request DTO를 Query Condition으로 변환한다.")
    @Test
    void articleSearchRequestToArticleQueryCondition() {
        ArticleSearchRequest request = new ArticleSearchRequest();
        request.setSearch("검색어");
        request.setSector("be");
        request.setTopics(List.of("db"));
        request.setTechStacks(List.of("mysql"));
        request.setSort("relevance");
        request.setLimit(10);
        request.setCursor("2025-09-25T04:35:00.764_35867");
        when(techStackResolver.resolve(List.of("mysql"))).thenReturn(List.of(new TechStack(1L, "mysql")));

        // when
        ArticleQueryCondition queryCondition = converter.convert(request);

        // then
        assertThat(queryCondition).isEqualTo(new ArticleQueryCondition(
                new SearchKeyword("검색어"),
                Sector.BE,
                List.of(Topic.DATABASE),
                List.of(new TechStack(1L, "mysql")),
                ArticleSortType.RELEVANCE,
                10,
                new ArticleCursor("2025-09-25T04:35:00.764_35867")
        ));
    }
}