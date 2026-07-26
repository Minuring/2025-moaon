package moaon.backend.api.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.restassured.RestAssured;
import moaon.backend.api.BaseApiTest;
import moaon.backend.fixture.Fixture;
import moaon.backend.fixture.RepositoryHelper;
import moaon.backend.global.config.QueryDslConfig;
import moaon.backend.member.Member;
import moaon.backend.member.dto.LoginStatusResponse;
import moaon.backend.member.service.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({RepositoryHelper.class, QueryDslConfig.class})
public class MemberApiTest extends BaseApiTest {

    @Autowired
    private RepositoryHelper helper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @DisplayName("GET /auth/me: 로그인 상태 확인 API")
    @Test
    void loginCheck() {
        // given
        Member member = Fixture.anyMember();
        Member savedMember = helper.save(member);

        String token = jwtTokenService.createToken(member.getId());

        // when - then
        LoginStatusResponse statusResponse = RestAssured.given().log().all()
                .cookie("token", token)
                .when().get("/auth/me")
                .then().log().all()
                .statusCode(200)
                .extract().as(LoginStatusResponse.class);

        assertAll(
                () -> assertThat(statusResponse.isLoggedIn()).isTrue(),
                () -> assertThat(statusResponse.id()).isEqualTo(savedMember.getId()),
                () -> assertThat(statusResponse.name()).isEqualTo(savedMember.getName()),
                () -> assertThat(statusResponse.email()).isEqualTo(savedMember.getEmail())
        );
    }
}
