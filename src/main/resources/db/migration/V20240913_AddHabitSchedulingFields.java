package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Adds scheduling-related fields to habit and habit_log tables.
 */
public class V20240913_AddHabitSchedulingFields extends BaseJavaMigration {
    
    @Override
    public void migrate(Context context) throws Exception {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(
            new SingleConnectionDataSource(context.getConnection(), true)
        );
        
        // Add columns to habit table
        jdbcTemplate.execute("""
            ALTER TABLE habit 
            ADD COLUMN IF NOT EXISTS grace_period_minutes INT DEFAULT 15,
            ADD COLUMN IF NOT EXISTS allow_multiple_daily BOOLEAN DEFAULT FALSE,
            ADD COLUMN IF NOT EXISTS last_scheduled TIMESTAMP,
            ADD COLUMN IF NOT EXISTS next_scheduled TIMESTAMP,
            ADD COLUMN IF NOT EXISTS time_zone VARCHAR(50) DEFAULT 'UTC',
            ADD COLUMN IF NOT EXISTS missed_count INT DEFAULT 0
        """);
        
        // Add columns to habit_log table
        jdbcTemplate.execute("""
            ALTER TABLE habit_log
            ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP,
            ADD COLUMN IF NOT EXISTS missed_at TIMESTAMP,
            ADD COLUMN IF NOT EXISTS missed BOOLEAN DEFAULT FALSE,
            ADD COLUMN IF NOT EXISTS notif_sent BOOLEAN DEFAULT FALSE,
            ADD COLUMN IF NOT EXISTS grace_period_used BOOLEAN DEFAULT FALSE,
            ADD COLUMN IF NOT EXISTS time_zone VARCHAR(50) DEFAULT 'UTC',
            ADD COLUMN IF NOT EXISTS completed_in_grace_period BOOLEAN DEFAULT FALSE,
            ADD COLUMN IF NOT EXISTS rescheduled BOOLEAN DEFAULT FALSE,
            ADD COLUMN IF NOT EXISTS reschedule_reason TEXT
        """);
        
        // Add index for better performance on common queries
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_habit_log_scheduled_completed 
            ON habit_log (scheduled_date_time, completed, missed)
        """);
    }
}
