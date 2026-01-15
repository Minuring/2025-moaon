package moaon.backend.article.api.crawl.dto;

public record ArticleCrawlResult(
        String title,
        String summary,
        String content,
        boolean isSucceed
) {

    public static ArticleCrawlResult success(String title, String summary, String content) {
        return new ArticleCrawlResult(title, enforceLength(summary, 0, 255), content, true);
    }

    public static ArticleCrawlResult withoutSummary(FinderCrawlResult result) {
        return new ArticleCrawlResult(result.title(), "", result.content(), false);
    }

    private static String enforceLength(String text, int min, int max) {
        if (text == null) {
            return "";
        }
        text = text.strip();
        if (text.length() <= max) {
            return text;
        }

        int lastSentenceEnd = text.lastIndexOf(".", max);
        if (lastSentenceEnd > min) {
            return text.substring(0, lastSentenceEnd + 1);
        }

        return text.substring(0, max);
    }
}
