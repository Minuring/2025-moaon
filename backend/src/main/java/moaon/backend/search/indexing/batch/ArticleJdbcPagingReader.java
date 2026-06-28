package moaon.backend.search.indexing.batch;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.sql.DataSource;
import moaon.backend.search.query.ArticleDocument;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class ArticleJdbcPagingReader implements ItemReader<ArticleDocument>, ItemStream {

    private static final String LAST_ID_KEY = "lastId";

    private static final String QUERY = """
            SELECT a.id, a.title, a.summary, acs.content, a.article_url, a.clicks, a.created_at, a.sector,
                   p.id AS project_id, p.title AS project_title,
                   GROUP_CONCAT(DISTINCT ts.name ORDER BY ts.name SEPARATOR ',') AS tech_stacks,
                   GROUP_CONCAT(DISTINCT at2.topics ORDER BY at2.topics SEPARATOR ',') AS topics
            FROM article a
            INNER JOIN project p ON p.id = a.project_id
            LEFT JOIN article_content_separated acs ON acs.id = a.id
            LEFT JOIN article_tech_stack ats ON ats.article_id = a.id
            LEFT JOIN tech_stack ts ON ts.id = ats.tech_stacks_id
            LEFT JOIN article_topics at2 ON at2.article_id = a.id
            WHERE a.id > :lastId
            GROUP BY a.id, a.title, a.summary, acs.content, a.article_url, a.clicks, a.created_at, a.sector, p.id, p.title
            ORDER BY a.id ASC
            LIMIT :pageSize
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<ArticleDocument> rowMapper;
    private final int pageSize;

    private long lastId = 0L;
    private final Queue<ArticleDocument> buffer = new LinkedList<>();
    private boolean exhausted = false;

    public ArticleJdbcPagingReader(DataSource dataSource, RowMapper<ArticleDocument> rowMapper, int pageSize) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.rowMapper = rowMapper;
        this.pageSize = pageSize;
    }

    @Override
    public ArticleDocument read() {
        if (buffer.isEmpty() && !exhausted) {
            fetchNextPage();
        }
        return buffer.poll();
    }

    private void fetchNextPage() {
        List<ArticleDocument> page = jdbcTemplate.query(QUERY,
                Map.of("lastId", lastId, "pageSize", pageSize),
                rowMapper);
        if (page.isEmpty()) {
            exhausted = true;
            return;
        }
        lastId = page.get(page.size() - 1).getId();
        buffer.addAll(page);
    }

    @Override
    public void open(ExecutionContext executionContext) {
        if (executionContext.containsKey(LAST_ID_KEY)) {
            lastId = executionContext.getLong(LAST_ID_KEY);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong(LAST_ID_KEY, lastId);
    }

    @Override
    public void close() {
        buffer.clear();
    }
}
