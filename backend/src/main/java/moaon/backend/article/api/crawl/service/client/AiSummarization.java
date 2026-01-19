package moaon.backend.article.api.crawl.service.client;

import java.util.List;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;

public record AiSummarization(
        String summary,
        Sector sector,
        List<Topic> topics,
        List<String> techStacks
) {

    public boolean isBlank() {
        return summary == null  || summary.trim().isEmpty();
    }

    public static AiSummarization nothing() {
        return new AiSummarization(null, null, null, null);
    }
}
