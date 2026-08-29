package moaon.backend.article.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum Topic {

    // 공통
    TECHNOLOGY_ADOPTION("adoption"),
    TROUBLESHOOTING("trouble"),
    PERFORMANCE_OPTIMIZATION("performance"),
    TESTING("testing"),
    CODE_QUALITY("code"),
    ETC("etc"),
    SECURITY("security"), //BE+infra 공통
    UI_UX_IMPROVEMENT("uiux"), //FE+iOS+android 공통
    BUILD("build"), //iOS+android 공통
    ARCHITECTURE_DESIGN("architecture"), //BE+android 공통
    NATIVE("native"), //android+iOS 공통
    SDK("sdk"), //android+iOS 공통

    // FE,
    STATE_MANAGEMENT("state"),
    BUNDLING("bundling"),

    // BE,
    API_DESIGN("api"),
    DATABASE("db"),
    DEPLOYMENT_AND_OPERATION("deployment"),

    // android,

    // infra
    CI_CD("cicd"),
    MONITORING_AND_LOGGING("monitoring"),
    NETWORK("network"),

    // 비개발
    TEAM_CULTURE("culture"),
    RETROSPECTIVE("retrospective"),
    PLANNING("planning"),
    DESIGN("design");

    // todo ios, android 추가

    private final String name;

    private boolean matchesName(String name) {
        return this.name.equalsIgnoreCase(name);
    }

    public static Topic of(String name) {
        return Arrays.stream(Topic.values())
                .filter(topic -> topic.matchesName(name))
                .findAny()
                .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));
    }
}
