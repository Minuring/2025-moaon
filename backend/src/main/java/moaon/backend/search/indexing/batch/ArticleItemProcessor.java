package moaon.backend.search.indexing.batch;

import moaon.backend.article.domain.Article;
import moaon.backend.search.query.ArticleDocument;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;

public class ArticleItemProcessor implements ItemProcessor<Article, IndexQuery> {

    @Override
    public IndexQuery process(Article article) {
        ArticleDocument doc = new ArticleDocument(article);
        return new IndexQueryBuilder().withObject(doc).build();
    }
}
