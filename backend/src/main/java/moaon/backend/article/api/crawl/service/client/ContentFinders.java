package moaon.backend.article.api.crawl.service.client;

import java.net.URL;
import java.util.List;
import moaon.backend.global.util.EnvLoader;
import org.springframework.stereotype.Component;

@Component
public class ContentFinders {

    private static final List<ContentFinder> FINDERS = List.of(
            new TistoryContentFinder(1000L),
            new NotionContentFinder(
                    500L, 1000L, // 노션은 p99가 높음.
                    EnvLoader.getEnv("NOTION_USER_ID"),
                    EnvLoader.getEnv("NOTION_TOKEN_V2")
            ),
            new VelogContentFinder(100L, 900L),
            new BodyFinder(1000L)
    );

    public ContentFinder getFinder(URL url) {
        return FINDERS.stream()
                .filter(contentFinder -> contentFinder.canHandle(url))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("[ERROR] 지원하지 않는 URL 입니다."));
    }
}
