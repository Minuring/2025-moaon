package moaon.backend.search.indexing.batch;

import moaon.backend.search.query.ArticleDocument;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;

public class ArticleItemProcessor implements ItemProcessor<ArticleDocument, IndexQuery> {

    @Override
    public IndexQuery process(ArticleDocument doc) {
        return new IndexQueryBuilder().withObject(doc).build();
    }
}
