package moaon.backend.article.draft.client;

import java.net.URL;

public abstract class ContentFinder {

    public abstract FinderCrawlResult crawl(URL url);

    public abstract boolean canHandle(URL url);
}
