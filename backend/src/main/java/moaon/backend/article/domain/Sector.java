package moaon.backend.article.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;

import java.util.Arrays;
import java.util.List;

import static moaon.backend.article.domain.Topic.*;

@RequiredArgsConstructor
@Getter
public enum Sector {

    FE("fe", List.of(
            TECHNOLOGY_ADOPTION,
            TROUBLESHOOTING,
            PERFORMANCE_OPTIMIZATION,
            TESTING,
            CODE_QUALITY,
            STATE_MANAGEMENT,
            UI_UX_IMPROVEMENT,
            BUNDLING,
            ETC
    )),
    BE("be", List.of(
            TECHNOLOGY_ADOPTION,
            TROUBLESHOOTING,
            PERFORMANCE_OPTIMIZATION,
            TESTING,
            CODE_QUALITY,
            SECURITY,
            ARCHITECTURE_DESIGN,
            API_DESIGN,
            DATABASE,
            DEPLOYMENT_AND_OPERATION,
            ETC
    )),
    ANDROID("android", List.of(
            BUILD,
            NATIVE,
            SDK,
            TECHNOLOGY_ADOPTION,
            TROUBLESHOOTING,
            PERFORMANCE_OPTIMIZATION,
            TESTING,
            CODE_QUALITY,
            UI_UX_IMPROVEMENT,
            ARCHITECTURE_DESIGN,
            ETC
    )),
    IOS("ios", List.of(
            BUILD,
            NATIVE,
            SDK,
            TECHNOLOGY_ADOPTION,
            TROUBLESHOOTING,
            PERFORMANCE_OPTIMIZATION,
            TESTING,
            CODE_QUALITY,
            UI_UX_IMPROVEMENT,
            ARCHITECTURE_DESIGN,
            ETC
    )),
    INFRA("infra", List.of(
            TECHNOLOGY_ADOPTION,
            TROUBLESHOOTING,
            PERFORMANCE_OPTIMIZATION,
            SECURITY,
            CI_CD,
            MONITORING_AND_LOGGING,
            NETWORK,
            ETC
    )),
    NON_TECH("nonTech", List.of(
            TEAM_CULTURE,
            RETROSPECTIVE,
            PLANNING,
            DESIGN,
            ETC
    ));

    private final String name;
    private final List<Topic> topics;

    private boolean matchesName(String name) {
        return this.name.equalsIgnoreCase(name);
    }

    public static Sector of(String name) {
        return Arrays.stream(Sector.values())
                .filter(s -> s.matchesName(name))
                .findAny()
                .orElseThrow(() -> new CustomException(ErrorCode.SECTOR_NOT_FOUND));
    }
}
