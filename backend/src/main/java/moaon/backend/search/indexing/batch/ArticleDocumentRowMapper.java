package moaon.backend.search.indexing.batch;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.search.query.ArticleDocument;
import org.springframework.jdbc.core.RowMapper;

public class ArticleDocumentRowMapper implements RowMapper<ArticleDocument> {

    @Override
    public ArticleDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ArticleDocument(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("summary"),
                Objects.requireNonNullElse(rs.getString("content"), ""),
                Sector.valueOf(rs.getString("sector")),
                parseCsv(rs.getString("topics"), Topic::valueOf),
                parseCsv(rs.getString("tech_stacks"), s -> s),
                rs.getInt("clicks"),
                rs.getTimestamp("created_at").toLocalDateTime().truncatedTo(ChronoUnit.MILLIS),
                rs.getLong("project_id"),
                rs.getString("project_title"),
                rs.getString("article_url")
        );
    }

    private <T> Set<T> parseCsv(String csv, Function<String, T> mapper) {
        if (csv == null || csv.isBlank()) {
            return new HashSet<>();
        }
        return Arrays.stream(csv.split(",")).map(mapper).collect(java.util.stream.Collectors.toSet());
    }
}
