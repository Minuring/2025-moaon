package moaon.backend.fixture;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.util.List;

public class DatabaseCleanupExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<String> tableNames = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
                String.class
        );

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        tableNames.forEach(tableName -> jdbcTemplate.execute("TRUNCATE TABLE " + tableName));
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
