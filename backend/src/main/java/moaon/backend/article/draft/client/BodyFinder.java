package moaon.backend.article.draft.client;

import java.io.IOException;
import java.net.URL;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class BodyFinder extends ContentFinder {

    private final long requestTimeoutMillis;

    public BodyFinder(long requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    @Override
    public FinderCrawlResult crawl(URL url) {
        try {
            Response response = Jsoup.connect(url.toString())
                    .timeout((int) requestTimeoutMillis)
                    .ignoreHttpErrors(true)
                    .execute();
            validateLink(response.statusCode());

            Document doc = response.parse();
            String title = doc.title();

            String content = doc.body().text();

            return new FinderCrawlResult(title, content);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.ARTICLE_CRAWL_FAILED, e);
        }
    }

    @Override
    public boolean canHandle(URL url) {
        return true;
    }

    private void validateLink(int statusCode) {
        if (statusCode >= 400) {
            throw new CustomException(ErrorCode.ARTICLE_NOT_FOUND);
        }
    }
}
