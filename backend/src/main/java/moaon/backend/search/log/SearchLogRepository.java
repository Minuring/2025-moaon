package moaon.backend.search.log;

import moaon.backend.search.log.domain.SearchLogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SearchLogRepository extends ElasticsearchRepository<SearchLogDocument, String> {
}
