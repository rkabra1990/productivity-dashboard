package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V20231010_AddCompletionTimestampToTask extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        
        // Add the new column if it doesn't exist
        jdbcTemplate.execute("ALTER TABLE task ADD COLUMN IF NOT EXISTS completion_timestamp DATETIME");
    }
}
