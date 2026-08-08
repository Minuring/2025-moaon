package moaon.backend.api.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

import java.util.List;

import moaon.backend.api.BaseApiTest;
import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.draft.ArticleDraft;
import moaon.backend.article.draft.ArticleDraftRepository;
import moaon.backend.article.dto.ArticleCreateRequest;
import moaon.backend.article.dto.ArticleDto;
import moaon.backend.article.dto.ArticleListResponse;
import moaon.backend.search.query.ArticleDocumentRepository;
import moaon.backend.fixture.ArticleFixtureBuilder;
import moaon.backend.fixture.FakeArticleSearchResult;
import moaon.backend.fixture.Fixture;
import moaon.backend.fixture.ProjectFixtureBuilder;
import moaon.backend.fixture.RepositoryHelper;
import moaon.backend.global.config.QueryDslConfig;
import moaon.backend.member.Member;
import moaon.backend.member.service.JwtTokenService;
import moaon.backend.member.service.MemberService;
import moaon.backend.project.domain.Project;
import moaon.backend.techStack.domain.TechStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import({RepositoryHelper.class, QueryDslConfig.class})
public class ArticleApiTest extends BaseApiTest {

    @Autowired
    protected RepositoryHelper repositoryHelper;

    @Autowired
    private ArticleDraftRepository articleDraftRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private ArticleDocumentRepository articleDocumentRepository;

    private String token;

    private Member member;

    @BeforeEach
    void cookieSetUp() {
        member = repositoryHelper.save(Fixture.anyMember());

        token = jwtTokenService.createToken(member.getId());
    }

    @DisplayName("POST /articles: 아티클 저장 API")
    @Test
    void save() {
        // given
        Project savedProject = repositoryHelper.save(
                new ProjectFixtureBuilder()
                        .author(member)
                        .build()
        );

        Mockito.when(memberService.getUserByToken(token)).thenReturn(member);

        repositoryHelper.save(new TechStack("react"));

        ArticleDraft draft = articleDraftRepository.save(
                new ArticleDraft(member, "https://tattered-drive-af3.notion.site/fork-ts-checker-webpack-plugin", "크롤링 제목", "크롤링 본문")
        );

        ArticleCreateRequest articleCreateRequest = ArticleCreateRequest.builder()
                .projectId(savedProject.getId())
                .title("fork-ts-checker-webpack-plugin")
                .summary("webpack-plugin 도입")
                .techStacks(List.of("react"))
                .draftId(draft.getId())
                .sector("fe")
                .topics(List.of("etc"))
                .build();

        // when
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .cookie("token", token)
                .body(List.of(articleCreateRequest))
                .when().post("/articles")
                .then().log().all()
                .statusCode(201);
    }

    @DisplayName("GET /articles : 페이징 방식의 아티클 조회 API")
    @Test
    void getPagedArticles() {
        // given
        Sector filteredSector = Sector.BE;
        Sector unfilteredSector = Sector.FE;
        Topic filteredTopic = Topic.DATABASE;
        Topic unfilteredTopic = Topic.API_DESIGN;
        TechStack filteredTechStack = Fixture.anyTechStack();
        TechStack unfilteredTechStack = Fixture.anyTechStack();
        String filteredSearch = "moa";
        String unfilteredSearch = "momo";

        Project project = repositoryHelper.save(
                new ProjectFixtureBuilder()
                        .build()
        );

        repositoryHelper.save(
                new ArticleFixtureBuilder()
                        .sector(unfilteredSector)
                        .content(filteredSearch)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(4)
                        .topics(filteredTopic)
                        .build()
        );
        repositoryHelper.save(
                new ArticleFixtureBuilder()
                        .techStacks(List.of(unfilteredTechStack))
                        .content(filteredSearch)
                        .sector(filteredSector)
                        .project(project)
                        .clicks(4)
                        .topics(filteredTopic)
                        .build()
        );
        repositoryHelper.save(
                new ArticleFixtureBuilder()
                        .title(unfilteredSearch)
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(1)
                        .topics(filteredTopic)
                        .build()
        );
        repositoryHelper.save(
                new ArticleFixtureBuilder()
                        .summary(unfilteredSearch)
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(1)
                        .topics(filteredTopic)
                        .build()
        );
        repositoryHelper.save(
                new ArticleFixtureBuilder()
                        .content(unfilteredSearch)
                        .sector(filteredSector)
                        .techStacks(List.of(unfilteredTechStack))
                        .project(project)
                        .clicks(4)
                        .topics(filteredTopic)
                        .build()
        );
        repositoryHelper.save(
                new ArticleFixtureBuilder()
                        .topics(unfilteredTopic)
                        .content(filteredSearch)
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(4)
                        .build()
        );
        Article articleClickRankThird = repositoryHelper.save(
                new ArticleFixtureBuilder()
                        .content(filteredSearch)
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(1)
                        .topics(filteredTopic)
                        .build()
        );
        Article articleClickRankSecond = repositoryHelper.save(
                new ArticleFixtureBuilder()
                        .title(filteredSearch)
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(2)
                        .topics(filteredTopic)
                        .build()
        );
        Article articleClickRankFirst = repositoryHelper.save(
                new ArticleFixtureBuilder()
                        .content(filteredSearch)
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(3)
                        .topics(filteredTopic)
                        .build()
        );

        Mockito.when(articleDocumentRepository.search(Mockito.any()))
                .thenReturn(FakeArticleSearchResult.create(
                        List.of(ArticleDto.from(articleClickRankFirst), ArticleDto.from(articleClickRankSecond)),
                        3, 2, ArticleSortType.CLICKS));

        // when
        ArticleListResponse actualResponse = RestAssured.given().log().all()
                .queryParams("sort", "clicks")
                .queryParams("search", filteredSearch)
                .queryParams("sector", filteredSector.getName())
                .queryParams("topics", List.of(filteredTopic.getName()))
                .queryParams("techStacks", List.of(filteredTechStack.getName()))
                .queryParams("limit", 2)
                .queryParams("cursor", "5_6")
                .when().get("/articles")
                .then().log().all()
                .statusCode(200)
                .extract().as(ArticleListResponse.class);

        // then
        assertThat(actualResponse.contents())
                .extracting(ArticleDto::id)
                .containsExactly(articleClickRankFirst.getId(), articleClickRankSecond.getId());
    }

    @DisplayName("POST /articles/{id}/clicks : 아티클 클릭수 증가 API")
    @Test
    void updateArticleClicks() {
        // given
        Article article = repositoryHelper.save(new ArticleFixtureBuilder().build());

        // when 첫 클릭 - 기본 응답 + 클릭수 증가 + 쿠키 설정
        ValidatableResponse firstResponse = RestAssured.given().log().all()
                .pathParam("id", article.getId())
                .when().post("/articles/{id}/clicks")
                .then().log().all()
                .statusCode(200);

        String cookieName = "clicked_articles_" + article.getId();
        String cookie = firstResponse.extract().cookie(cookieName);
        // then 클릭수 및 쿠키 검증을 위해 서비스에서 직접 조회
        Article firstResult = repositoryHelper.getById(article.getId());
        assertAll("아티클 클릭수 증가 및 쿠키 설정 검증",
                () -> assertThat(firstResult.getClicks()).isEqualTo(1),
                () -> assertThat(cookie).isNotNull()
        );

        // when 쿠키와 함께 재클릭 - 클릭수 미증가 확인
        RestAssured.given().log().all()
                .cookie(cookieName, cookie)
                .pathParam("id", article.getId())
                .when().post("/articles/{id}/clicks")
                .then().log().all()
                .statusCode(200);
        Article secondResult = repositoryHelper.getById(article.getId());

        // then 클릭수 미증가 검증
        assertThat(secondResult.getClicks()).isEqualTo(1);
    }
}
