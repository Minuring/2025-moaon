package moaon.backend.search.log.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.AnalyzeResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.search.log.domain.SearchHitLog;
import moaon.backend.search.log.domain.SearchLogCapture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchLogCaptureAssemblerTest {

    @Mock
    ElasticsearchClient elasticsearchClient;

    @InjectMocks
    SearchLogCaptureAssembler assembler;

    @BeforeEach
    void stubDefaultAnalyzeFailure() throws Exception {
        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.analyze(any(java.util.function.Function.class)))
                .thenThrow(new java.io.IOException("default stub"));
    }

    @Test
    void 검색어가_있으면_캡처를_반환한다() {
        ArticleQueryCondition condition = conditionWithQuery("spring");
        List<SearchHitLog> hits = List.of(
                titleHit(1), titleHit(2), contentHit(3)
        );

        Optional<SearchLogCapture> result = assembler.assemble(hits, 10, condition, 80);

        assertThat(result).isPresent();
        assertThat(result.get().query()).isEqualTo("spring");
        assertThat(result.get().fieldMatchStats().titleCount()).isEqualTo(2);
        assertThat(result.get().fieldMatchStats().contentCount()).isEqualTo(1);
        assertThat(result.get().fieldMatchStats().hitCount()).isEqualTo(3);
    }

    @Test
    void 검색어가_null이면_빈_Optional을_반환한다() {
        ArticleQueryCondition condition = conditionWithQuery(null);

        Optional<SearchLogCapture> result = assembler.assemble(List.of(), 0, condition, 0);

        assertThat(result).isEmpty();
    }

    @Test
    void 검색어가_공백이면_빈_Optional을_반환한다() {
        ArticleQueryCondition condition = conditionWithQuery("   ");

        Optional<SearchLogCapture> result = assembler.assemble(List.of(), 0, condition, 0);

        assertThat(result).isEmpty();
    }

    @Test
    void 커서가_있으면_hasCursor가_true다() {
        ArticleQueryCondition condition = conditionWithQuery("spring");
        when(condition.cursor()).thenReturn(mock(moaon.backend.article.domain.ArticleCursor.class));

        Optional<SearchLogCapture> result = assembler.assemble(List.of(), 0, condition, 0);

        assertThat(result).isPresent();
        assertThat(result.get().hasCursor()).isTrue();
    }

    @Test
    void 커서가_없으면_hasCursor가_false다() {
        ArticleQueryCondition condition = conditionWithQuery("spring");
        when(condition.cursor()).thenReturn(null);

        Optional<SearchLogCapture> result = assembler.assemble(List.of(), 0, condition, 0);

        assertThat(result).isPresent();
        assertThat(result.get().hasCursor()).isFalse();
    }

    @Test
    void ES_분석_실패시_synonymMatchCount는_null이다() throws Exception {
        ArticleQueryCondition condition = conditionWithQuery("spring");
        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.analyze(any(java.util.function.Function.class)))
                .thenThrow(new java.io.IOException("ES 연결 실패"));

        Optional<SearchLogCapture> result = assembler.assemble(List.of(), 0, condition, 0);

        assertThat(result).isPresent();
        assertThat(result.get().synonymMatchCount()).isNull();
    }

    @Test
    void 동의어가_없으면_synonymMatchCount가_0이다() throws Exception {
        ArticleQueryCondition condition = conditionWithQuery("java");
        AnalyzeResponse response = analyzeResponse(List.of("java"));

        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.analyze(any(java.util.function.Function.class))).thenReturn(response);

        Optional<SearchLogCapture> result = assembler.assemble(List.of(titleHit(1)), 1, condition, 0);

        assertThat(result).isPresent();
        assertThat(result.get().synonymMatchCount()).isEqualTo(0);
    }

    @Test
    void 동의어_토큰이_스니펫에_있으면_synonymMatchCount를_카운트한다() throws Exception {
        ArticleQueryCondition condition = conditionWithQuery("java");
        AnalyzeResponse baseResponse = analyzeResponse(List.of("java"));
        AnalyzeResponse expandedResponse = analyzeResponse(List.of("java", "자바"));

        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.analyze(any(java.util.function.Function.class))).thenReturn(baseResponse, expandedResponse);

        SearchHitLog synonymHit = SearchHitLog.of(1, 1L, "자바 튜토리얼", 9.0f,
                Map.of("title", List.of("<mark>자바</mark> 튜토리얼")));
        SearchHitLog noSynonymHit = SearchHitLog.of(2, 2L, "Java Guide", 8.0f,
                Map.of("title", List.of("<mark>java</mark> guide")));

        Optional<SearchLogCapture> result = assembler.assemble(
                List.of(synonymHit, noSynonymHit), 2, condition, 0);

        assertThat(result).isPresent();
        assertThat(result.get().synonymMatchCount()).isEqualTo(1);
    }

    @Test
    void 동의어와_베이스_토큰이_모두_있으면_카운트하지_않는다() throws Exception {
        ArticleQueryCondition condition = conditionWithQuery("java");
        AnalyzeResponse baseResponse = analyzeResponse(List.of("java"));
        AnalyzeResponse expandedResponse = analyzeResponse(List.of("java", "자바"));

        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.analyze(any(java.util.function.Function.class))).thenReturn(baseResponse, expandedResponse);

        SearchHitLog bothHit = SearchHitLog.of(1, 1L, "Java와 자바", 9.0f,
                Map.of("title", List.of("<mark>자바</mark>와 <mark>java</mark>")));

        Optional<SearchLogCapture> result = assembler.assemble(List.of(bothHit), 1, condition, 0);

        assertThat(result).isPresent();
        assertThat(result.get().synonymMatchCount()).isEqualTo(0);
    }

    private ArticleQueryCondition conditionWithQuery(String query) {
        ArticleQueryCondition condition = mock(ArticleQueryCondition.class);
        SearchKeyword keyword = query != null ? mock(SearchKeyword.class) : null;
        when(condition.search()).thenReturn(keyword);
        if (keyword != null) {
            when(keyword.value()).thenReturn(query);
        }
        return condition;
    }

    private AnalyzeResponse analyzeResponse(List<String> tokens) {
        List<AnalyzeToken> analyzeTokens = tokens.stream()
                .map(t -> AnalyzeToken.of(at -> at
                        .token(t)
                        .startOffset(0)
                        .endOffset(t.length())
                        .position(0)
                        .type("<ALPHANUM>")))
                .toList();
        return AnalyzeResponse.of(r -> r.tokens(analyzeTokens));
    }

    private SearchHitLog titleHit(int rank) {
        return SearchHitLog.of(rank, (long) rank, "제목" + rank, 9.0f,
                Map.of("title", List.of("<mark>spring</mark>")));
    }

    private SearchHitLog contentHit(int rank) {
        return SearchHitLog.of(rank, (long) rank, "제목" + rank, 3.0f,
                Map.of("title", List.of(), "summary", List.of()));
    }
}
