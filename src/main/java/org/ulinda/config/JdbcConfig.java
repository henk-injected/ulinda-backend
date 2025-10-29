package org.ulinda.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

@Configuration
@EnableJdbcAuditing
public class JdbcConfig {
    // This enables @CreatedDate and @LastModifiedDate
}
