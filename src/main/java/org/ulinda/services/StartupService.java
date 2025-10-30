package org.ulinda.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ulinda.entities.SecuritySettings;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class StartupService {

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private SecuritySettingsService securitySettingsService;

    private AtomicBoolean isNew =  new AtomicBoolean(false);

    private boolean tableExists(String tableName) {
        String sql = """
            SELECT COUNT(*) 
            FROM information_schema.tables 
            WHERE table_schema = 'public' 
            AND table_name = ?
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName.toLowerCase());
        return count != null && count > 0;
    }

    private void createUsersTable() {
        String createSql = """
            CREATE TABLE users (
                id UUID DEFAULT gen_random_uuid() PRIMARY KEY NOT NULL,
                username TEXT NOT NULL,
                password TEXT NOT NULL,
                name TEXT,
                surname TEXT,
                can_create_models BOOLEAN NOT NULL,
                is_admin_user BOOLEAN NOT NULL,
                must_change_password BOOLEAN NOT NULL,
                can_generate_tokens BOOLEAN NOT NULL,
                max_token_count INTEGER NOT NULL,
                account_disabled BOOLEAN NOT NULL DEFAULT false,
                failed_login_attempts INTEGER DEFAULT 0,
                last_failed_login_attempt TIMESTAMP WITH TIME ZONE,
                account_locked_until TIMESTAMP WITH TIME ZONE
            )
            """;
        jdbcTemplate.execute(createSql);

        String createIndexSql = """
            CREATE UNIQUE INDEX idx_users_username ON users (username)
        """;
        jdbcTemplate.execute(createIndexSql);
    }

    private void createModelsTable() {
        String createSql = """
            CREATE TABLE models (
                        id UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
                        owner_id UUID,
                        name TEXT NOT NULL,
                        description TEXT,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                    );
            ALTER TABLE models
            ADD CONSTRAINT fk_models_owner_id
            FOREIGN KEY (owner_id) REFERENCES users(id);
        """;
        jdbcTemplate.execute(createSql);
    }

    private void createFieldsTable() {
        String createSql = """
            CREATE TABLE fields (
                        id UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
                        model_id UUID NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        type TEXT NOT NULL,
                        is_required BOOLEAN NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                    );
        """;
        jdbcTemplate.execute(createSql);
        String index = " create index idx_fields_model_id on fields (model_id );";
        jdbcTemplate.execute(index);
    }

    private void createErrorLogTable() {
        String createSql = """
            CREATE TABLE error_logs (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       error_identifier UUID NOT NULL,
                                       timestamp TIMESTAMPTZ NOT NULL,
                                       message TEXT,
                                       stack_trace TEXT,
                                       error_code TEXT
                                   );
        """;
        jdbcTemplate.execute(createSql);
    }

    private void createCurrentUserTokensTable() {
        String createSql = """
            CREATE TABLE current_user_tokens (
                id UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
                user_id UUID NOT NULL,
                token_hash TEXT NOT NULL,
                token_prefix TEXT NOT NULL,
                token_name TEXT,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                token_expiry_date_time TIMESTAMP WITH TIME ZONE,

                -- Foreign key constraint for referential integrity
                CONSTRAINT fk_current_user_tokens_id
                    FOREIGN KEY (user_id) REFERENCES users(id)
            );
        """;
        jdbcTemplate.execute(createSql);

        String createUserIdIndexSql = """
            CREATE INDEX idx_current_user_tokens_user_id ON current_user_tokens (user_id)
        """;
        jdbcTemplate.execute(createUserIdIndexSql);

        String createPrefixIndexSql = """
            CREATE INDEX idx_current_user_tokens_prefix ON current_user_tokens (token_prefix)
        """;
        jdbcTemplate.execute(createPrefixIndexSql);
    }

    private void createSessionsTable() {
        String createSql = """
            CREATE TABLE sessions (
                id UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                last_accessed TIMESTAMP WITH TIME ZONE NOT NULL,
                user_id UUID NOT NULL,
                ip_addr TEXT,

                -- Foreign key constraint for referential integrity
                CONSTRAINT fk_sessions_user_id
                    FOREIGN KEY (user_id) REFERENCES users(id)
            );
        """;
        jdbcTemplate.execute(createSql);

        String createIndexSql = """
            CREATE INDEX idx_sessions_user_id ON sessions (user_id)
        """;
        jdbcTemplate.execute(createIndexSql);
    }

    private void createSecuritySettingsTable() {
        String createSql = """
            CREATE TABLE security_settings (
                id INT PRIMARY KEY,
                session_timeout_minutes INTEGER NOT NULL,
                minimum_password_length INTEGER NOT NULL,
                password_requires_uppercase_letters  BOOLEAN NOT NULL,
                password_requires_uppercase_letters_minimum_count INTEGER NOT NULL,
                password_requires_lowercase_letters  BOOLEAN NOT NULL,
                password_requires_lowercase_letters_minimum_count INTEGER NOT NULL,
                password_requires_numbers BOOLEAN NOT NULL,
                password_requires_numbers_minimum_count INTEGER NOT NULL,
                password_requires_special_characters  BOOLEAN NOT NULL,
                password_requires_special_characters_minimum_count INTEGER NOT NULL,
                password_allowed_special_characters  TEXT NOT NULL,
                block_common_passwords BOOLEAN NOT NULL,
                block_dictionary_words BOOLEAN NOT NULL,
                prevent_username_in_password BOOLEAN NOT NULL,
                password_expiration BOOLEAN NOT NULL,
                password_expiration_days INTEGER NOT NULL,
                remember_previous_passwords BOOLEAN NOT NULL,
                remember_last_passwords_count INTEGER NOT NULL,
                maximum_login_attempts INTEGER NOT NULL,
                after_max_attempts_lockout_time_minutes INTEGER NOT NULL,
                show_password_strength_meter BOOLEAN NOT NULL,
                show_password_requirments_on_form BOOLEAN NOT NULL,
                allow_show_password_toggle BOOLEAN NOT NULL
            );
        """;
        jdbcTemplate.execute(createSql);
    }

    private void deleteTables() {
        deleteModelLinksTables();

        deleteRecordsTables();

        // Then delete the fixed schema tables
        String deleteSql = """
            DROP TABLE IF EXISTS fields;
            DROP TABLE IF EXISTS user_model_permissions;
            DROP TABLE IF EXISTS model_links;
            DROP TABLE IF EXISTS models;
            DROP TABLE IF EXISTS user_roles;
            DROP TABLE IF EXISTS current_user_tokens;
            DROP TABLE IF EXISTS sessions;
            DROP TABLE IF EXISTS security_settings;
            DROP TABLE IF EXISTS users;
            DROP TABLE IF EXISTS error_logs;
        """;
        jdbcTemplate.execute(deleteSql);
    }

    private void deleteRecordsTables() {
        // Get all table names starting with 'records_'
        String query = """
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public' 
        AND tablename LIKE 'records_%'
        """;

        List<String> tableNames = jdbcTemplate.queryForList(query, String.class);

        // Drop each table
        for (String tableName : tableNames) {
            log.info("Dropping table: " + tableName);
            String dropSql = "DROP TABLE IF EXISTS " + tableName;
            jdbcTemplate.execute(dropSql);
        }
    }

    private void deleteModelLinksTables() {
        // Get all table names starting with 'records_'
        String query = """
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public' 
        AND tablename LIKE 'model_links_%'
        """;

        List<String> tableNames = jdbcTemplate.queryForList(query, String.class);

        // Drop each table
        for (String tableName : tableNames) {
            log.info("Dropping table: " + tableName);
            String dropSql = "DROP TABLE IF EXISTS " + tableName;
            jdbcTemplate.execute(dropSql);
        }
    }

    private void createTableReferences() {
        String createSql = "ALTER TABLE fields ADD CONSTRAINT fk_fields_model_id FOREIGN KEY (model_id) REFERENCES models(id)";
        jdbcTemplate.execute(createSql);
    }

    private void createModelPermissionsTable() {
        String createSql = """
            CREATE TABLE user_model_permissions (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        user_id UUID NOT NULL,
                        model_id UUID NOT NULL,
                        permission TEXT NOT NULL,
                
                        -- Foreign key constraints for referential integrity
                        CONSTRAINT fk_user_model_permissions_user_id
                            FOREIGN KEY (user_id) REFERENCES users(id),
                
                        CONSTRAINT fk_user_model_permissions_model_id
                            FOREIGN KEY (model_id) REFERENCES models(id),
                
                        -- Prevent duplicate permissions for same user-model combination
                        CONSTRAINT uk_user_model_permission
                            UNIQUE (user_id, model_id, permission)
                    );
        """;
        jdbcTemplate.execute(createSql);
    }

    private void createModelLinksTable() {
        String createSql = """
            -- Create the model_links table
            CREATE TABLE model_links (
                id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                model_1_id UUID NOT NULL,
                model_2_id UUID NOT NULL,
                model1_can_have_unlimited_model2s BOOLEAN NOT NULL,
                model2_can_have_unlimited_model1s BOOLEAN NOT NULL,
                model1_can_have_so_many_model2s_count BIGINT,
                model2_can_have_so_many_model1s_count BIGINT,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
            
                -- Foreign key constraints for referential integrity
                CONSTRAINT fk_model_links_model_1_id FOREIGN KEY (model_1_id) REFERENCES models(id),
                CONSTRAINT fk_model_links_model_2_id FOREIGN KEY (model_2_id) REFERENCES models(id)
            );
        """;
        jdbcTemplate.execute(createSql);
    }

    @Transactional
    public void runStartup() {
        //deleteTables();
        log.info("Checking if users table exists");
        if (!tableExists("users")) {
            isNew.set(true);
            createUsersTable();
            log.info("Users table created successfully");
            log.info("Creating admin user");
            userService.createAdminUser();
            log.info("Admin user created successfully");
            createModelsTable();
            log.info("Models table created successfully");
            createFieldsTable();
            log.info("Fields table created successfully");
            createTableReferences();
            log.info("Table references created successfully");
            createModelPermissionsTable();
            log.info("Models permissions table created successfully");
            createModelLinksTable();
            log.info("Created model links table");
            createErrorLogTable();
            log.info("Errors table created successfully");
            createCurrentUserTokensTable();
            log.info("Current user tokens table created successfully");
            createSessionsTable();
            log.info("Sessions table created successfully");
            createSecuritySettingsTable();
            log.info("Security settings table created successfully");
            log.info("Insert security settings");
            securitySettingsService.saveNewSecuritySettings();
        } else {
            log.info("Users table already exists");
        }
    }

    public void loadDemoData() {
        if (isNew.get()) {
            demoDataService.loadDemoData();
        }
        //log.info("Loaded Demo Data");
    }
}
