package org.sudhir512kj.digitalpayment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "org.sudhir512kj.digitalpayment.repository")
@EnableTransactionManagement
public class DatabaseConfig {
    // JPA configuration is handled by Spring Boot auto-configuration
    // This class enables transaction management and JPA repositories
}