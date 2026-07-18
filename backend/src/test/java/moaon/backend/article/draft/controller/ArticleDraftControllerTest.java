package moaon.backend.article.draft.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.net.URL;
import moaon.backend.api.BaseApiTest;
import moaon.backend.article.api.crawl.dto.FinderCrawlResult;
import moaon.backend.article.api.crawl.service.client.ContentFinder;
import moaon.backend.article.api.crawl.service.client.ContentFinders;
import moaon.backend.article.draft.dto.ArticleDraftCreateResponse;
import moaon.backend.article.draft.repository.ArticleDraftRepository;
import moaon.backend.fixture.Fixture;
import moaon.backend.fixture.RepositoryHelper;
import moaon.backend.member.domain.Member;
import moaon.backend.member.service.JwtTokenService;
import moaon.backend.member.service.MemberService;
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
}
