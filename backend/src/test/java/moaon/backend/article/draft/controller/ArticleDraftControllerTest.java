package moaon.backend.article.draft.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.net.URL;
import java.util.List;
import moaon.backend.api.BaseApiTest;
import moaon.backend.article.api.crawl.dto.FinderCrawlResult;
import moaon.backend.article.api.crawl.service.client.AiSummarization;
import moaon.backend.article.api.crawl.service.client.AiSummarizer;
import moaon.backend.article.api.crawl.service.client.ContentFinder;
import moaon.backend.article.api.crawl.service.client.ContentFinders;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.draft.domain.ArticleDraft;
import moaon.backend.article.draft.dto.ArticleDraftAnalyzeResponse;
import moaon.backend.article.draft.dto.ArticleDraftCreateResponse;
import moaon.backend.article.draft.repository.ArticleDraftRepository;
import moaon.backend.fixture.Fixture;
import moaon.backend.fixture.RepositoryHelper;
import moaon.backend.member.domain.Member;
import moaon.backend.member.service.JwtTokenService;
import moaon.backend.member.service.MemberService;
import moaon.backend.techStack.domain.TechStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(RepositoryHelper.class)
class ArticleDraftControllerTest extends BaseApiTest {

    @Autowired
    private RepositoryHelper repositoryHelper;

    @Autowired
    private ArticleDraftRepository articleDraftRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private ContentFinders contentFinders;

    @MockitoBean
    private AiSummarizer aiSummarizer;

    private String token;
    private Member member;

    @BeforeEach
    void setUpMember() {
        member = repositoryHelper.save(Fixture.anyMember());
        token = jwtTokenService.createToken(member.getId());
    }

    @DisplayName("POST /articles/drafts: 크롤링 후 초안을 생성한다")
    @Test
    void create() {
        // given
        Mockito.when(memberService.getUserByToken(token)).thenReturn(member);

        ContentFinder finder = Mockito.mock(ContentFinder.class);
        Mockito.when(contentFinders.getFinder(Mockito.any(URL.class))).thenReturn(finder);
        Mockito.when(finder.crawl(Mockito.any(URL.class)))
                .thenReturn(new FinderCrawlResult("크롤링된 제목", "크롤링된 본문입니다."));

        // when
        ArticleDraftCreateResponse response = RestAssured.given(documentationSpecification).log().all()
                .contentType(ContentType.JSON)
                .cookie("token", token)
                .queryParam("url", "https://example.tistory.com/1")
                .filter(document())
                .when().post("/articles/drafts")
                .then().log().all()
                .statusCode(201)
                .extract().as(ArticleDraftCreateResponse.class);

        // then
        assertThat(response.title()).isEqualTo("크롤링된 제목");
        assertThat(articleDraftRepository.findById(response.draftId())).isPresent();
    }

    @DisplayName("POST /articles/drafts/{draftId}/analyze: LLM 분석 결과를 초안에 반영한다")
    @Test
    void analyze() {
        // given
        Mockito.when(memberService.getUserByToken(token)).thenReturn(member);

        ArticleDraft draft = new ArticleDraft(member, "https://example.tistory.com/1", "제목", "크롤링된 본문");
        ArticleDraft savedDraft = articleDraftRepository.save(draft);

        repositoryHelper.save(new TechStack("java"));

        Mockito.when(aiSummarizer.summarize(Mockito.eq("크롤링된 본문"), Mockito.eq(member)))
                .thenReturn(new AiSummarization(
                        "LLM이 만든 요약입니다.",
                        Sector.BE,
                        List.of(Topic.TECHNOLOGY_ADOPTION),
                        List.of("java")
                ));

        // when
        ArticleDraftAnalyzeResponse response = RestAssured.given(documentationSpecification).log().all()
                .contentType(ContentType.JSON)
                .cookie("token", token)
                .pathParam("draftId", savedDraft.getId())
                .filter(document())
                .when().post("/articles/drafts/{draftId}/analyze")
                .then().log().all()
                .statusCode(200)
                .extract().as(ArticleDraftAnalyzeResponse.class);

        // then
        assertThat(response.summary()).isEqualTo("LLM이 만든 요약입니다.");
        assertThat(response.sector()).isEqualTo("be");
        assertThat(response.techstacks()).isEqualTo("java");
    }

    @DisplayName("GET /articles/drafts/quota: 오늘 남은 분석 가능 횟수를 조회한다")
    @Test
    void getQuota() {
        // given
        Mockito.when(memberService.getUserByToken(token)).thenReturn(member);

        // when & then
        RestAssured.given(documentationSpecification).log().all()
                .cookie("token", token)
                .filter(document())
                .when().get("/articles/drafts/quota")
                .then().log().all()
                .statusCode(200)
                .body("remainingCount", org.hamcrest.Matchers.equalTo(20));
    }
}
