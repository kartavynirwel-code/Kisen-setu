package com.kisansetu;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the full context against a real MySQL container. Passing means:
 *  1. Flyway applied V1-V3 cleanly, and
 *  2. Hibernate (ddl-auto=validate) found every entity column in the migrated schema.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class SchemaMigrationTest {

    @Autowired
    DataSource dataSource;

    @Test
    void migrationsApplyAndEntitiesMatchSchema() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name IN ('app_user','farmer','land_record','kyc_record',"
                        + "'claim_batch','claim','validation_error','audit_log')",
                Integer.class);

        assertThat(tables).isEqualTo(8);
    }
}
