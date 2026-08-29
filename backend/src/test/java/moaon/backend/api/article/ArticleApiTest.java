package moaon.backend.api.article;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
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
import moaon.backend.fixture.FakeArticleSearchResult;
import moaon.backend.fixture.Fixtures;
import moaon.backend.fixture.RepositoryHelper;
import moaon.backend.global.config.QueryDslConfig;
import moaon.backend.member.Member;
import moaon.backend.member.service.JwtTokenService;
import moaon.backend.project.domain.Project;
import moaon.backend.search.ElasticSearchService;
import moaon.backend.techStack.domain.TechStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Import({RepositoryHelper.class, QueryDslConfig.class})
public class ArticleApiTest extends BaseApiTest {

    @Autowired
    protected RepositoryHelper repositoryHelper;

    @Autowired
    private ArticleDraftRepository articleDraftRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private ElasticSearchService elasticSearchService;

    private String token;

    private Member member;

    @BeforeEach
    void cookieSetUp() {
        member = repositoryHelper.save(Fixtures.anyMember());
        token = jwtTokenService.createToken(member.getId());
    }

    @DisplayName("POST /articles: 아티클 저장 API")
    @Test
    void save() {
        // given
        Project savedProject = repositoryHelper.save(
                Fixtures.projectBuilder()
                        .author(member)
                        .build()
        );
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
        TechStack filteredTechStack = repositoryHelper.saveAnyTechStack();
        TechStack unfilteredTechStack = repositoryHelper.saveAnyTechStack();
        String filteredSearch = "moa";
        String unfilteredSearch = "momo";

        Project project = repositoryHelper.saveAnyProject();

        // sector 비대상
        repositoryHelper.save(
                Fixtures.articleBuilder()
                        .sector(unfilteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(4)
                        .topics(filteredTopic)
                        .build(),
                filteredSearch
        );
        // techstack 비대상
        repositoryHelper.save(
                Fixtures.articleBuilder()
                        .techStacks(List.of(unfilteredTechStack))
                        .sector(filteredSector)
                        .project(project)
                        .clicks(4)
                        .topics(filteredTopic)
                        .build(),
                filteredSearch
        );
        // title 비대상
        repositoryHelper.save(
                Fixtures.articleBuilder()
                        .title(unfilteredSearch)
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(1)
                        .topics(filteredTopic)
                        .build()
        );
        // summary 비대상
        repositoryHelper.save(
                Fixtures.articleBuilder()
                        .summary(unfilteredSearch)
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(1)
                        .topics(filteredTopic)
                        .build()
        );
        // content 비대상
        repositoryHelper.save(
                Fixtures.articleBuilder()
                        .sector(filteredSector)
                        .techStacks(List.of(unfilteredTechStack))
                        .project(project)
                        .clicks(4)
                        .topics(filteredTopic)
                        .build(),
                unfilteredSearch
        );
        // topic 비대상
        repositoryHelper.save(
                Fixtures.articleBuilder()
                        .topics(unfilteredTopic)
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(4)
                        .build(),
                filteredSearch
        );
        // filter 전부 만족, clicks 1 (요청의 limit가 2이므로 제외되어야 함)
        Article articleClickRankThird = repositoryHelper.save(
                Fixtures.articleBuilder()
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(1)
                        .topics(filteredTopic)
                        .build(),
                filteredSearch
        );
        // filter 전부 만족, clicks 2
        Article articleClickRankSecond = repositoryHelper.save(
                Fixtures.articleBuilder()
                        .title(filteredSearch)
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(2)
                        .topics(filteredTopic)
                        .build()
        );
        // filter 전부 만족, clicks 3
        Article articleClickRankFirst = repositoryHelper.save(
                Fixtures.articleBuilder()
                        .sector(filteredSector)
                        .techStacks(List.of(filteredTechStack))
                        .project(project)
                        .clicks(3)
                        .topics(filteredTopic)
                        .build(),
                filteredSearch
        );

        Mockito.when(elasticSearchService.search(Mockito.any()))
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
        Article article = repositoryHelper.saveAnyArticle();

        // when 첫 클릭 - 기본 응답 + 클릭수 증가 + 쿠키 설정
        ValidatableResponse firstResponse = RestAssured.given().log().all()
                .pathParam("id", article.getId())
                .when().post("/articles/{id}/clicks")
                .then().log().all()
                .statusCode(200);

        String cookieName = "clicked_articles_" + article.getId();
        String cookie = firstResponse.extract().cookie(cookieName);
        // then 클릭수 및 쿠키 검증을 위해 서비스에서 직접 조회
        Article firstResult = repositoryHelper.getArticleById(article.getId());
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
        Article secondResult = repositoryHelper.getArticleById(article.getId());

        // then 클릭수 미증가 검증
        assertThat(secondResult.getClicks()).isEqualTo(1);
    }
}
