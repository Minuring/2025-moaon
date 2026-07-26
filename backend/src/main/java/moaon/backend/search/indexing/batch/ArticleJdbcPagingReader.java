package moaon.backend.search.indexing.batch;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import javax.sql.DataSource;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.search.query.ArticleDocument;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class ArticleJdbcPagingReader implements ItemReader<ArticleDocument>, ItemStream {

    private static final String LAST_ID_KEY = "lastId";

    private static final String BASE_QUERY = """
            SELECT a.id, a.title, a.summary, ac.content, a.article_url, a.clicks, a.created_at, a.sector,
                   p.id AS project_id, p.title AS project_title
            FROM article a
            INNER JOIN project p ON p.id = a.project_id
            LEFT JOIN article_content ac ON ac.id = a.id
            WHERE a.id > :lastId
            ORDER BY a.id ASC
            LIMIT :pageSize
            """;

    private static final String TECH_STACKS_QUERY = """
            SELECT ats.article_id, ts.name
            FROM article_tech_stack ats
            INNER JOIN tech_stack ts ON ts.id = ats.tech_stack_id
            WHERE ats.article_id IN (:ids)
            """;

    private static final String TOPICS_QUERY = """
            SELECT article_id, topics
            FROM article_topics
            WHERE article_id IN (:ids)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final int pageSize;

    private long lastId = 0L;
    private final Queue<ArticleDocument> buffer = new LinkedList<>();
    private boolean exhausted = false;

    public ArticleJdbcPagingReader(DataSource dataSource, int pageSize) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
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
        List<ArticleRow> rows = jdbcTemplate.query(BASE_QUERY,
                Map.of("lastId", lastId, "pageSize", pageSize),
                this::mapBaseRow);

        if (rows.isEmpty()) {
            exhausted = true;
            return;
        }

        lastId = rows.get(rows.size() - 1).id();
        List<Long> ids = rows.stream().map(ArticleRow::id).toList();

        Map<Long, Set<String>> techStacksMap = fetchTechStacks(ids);
        Map<Long, Set<Topic>> topicsMap = fetchTopics(ids);

        for (ArticleRow row : rows) {
            buffer.add(new ArticleDocument(
                    row.id(),
                    row.title(),
                    row.summary(),
                    row.content(),
                    Sector.valueOf(row.sector()),
                    topicsMap.getOrDefault(row.id(), new HashSet<>()),
                    techStacksMap.getOrDefault(row.id(), new HashSet<>()),
                    row.clicks(),
                    row.createdAt(),
                    row.projectId(),
                    row.projectTitle(),
                    row.articleUrl()
            ));
        }
    }

    private Map<Long, Set<String>> fetchTechStacks(List<Long> ids) {
        Map<Long, Set<String>> result = new HashMap<>();
        jdbcTemplate.query(TECH_STACKS_QUERY, Map.of("ids", ids), rs -> {
            long articleId = rs.getLong("article_id");
            result.computeIfAbsent(articleId, k -> new HashSet<>()).add(rs.getString("name"));
        });
        return result;
    }

    private Map<Long, Set<Topic>> fetchTopics(List<Long> ids) {
        Map<Long, Set<Topic>> result = new HashMap<>();
        jdbcTemplate.query(TOPICS_QUERY, Map.of("ids", ids), rs -> {
            long articleId = rs.getLong("article_id");
            result.computeIfAbsent(articleId, k -> new HashSet<>()).add(Topic.valueOf(rs.getString("topics")));
        });
        return result;
    }

    private ArticleRow mapBaseRow(ResultSet rs, int rowNum) throws SQLException {
        return new ArticleRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("summary"),
                Objects.requireNonNullElse(rs.getString("content"), ""),
                rs.getString("sector"),
                rs.getInt("clicks"),
                rs.getTimestamp("created_at").toLocalDateTime().truncatedTo(ChronoUnit.MILLIS),
                rs.getLong("project_id"),
                rs.getString("project_title"),
                rs.getString("article_url")
        );
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

    private record ArticleRow(
            long id,
            String title,
            String summary,
            String content,
            String sector,
            int clicks,
            LocalDateTime createdAt,
            long projectId,
            String projectTitle,
            String articleUrl
    ) {}
}
