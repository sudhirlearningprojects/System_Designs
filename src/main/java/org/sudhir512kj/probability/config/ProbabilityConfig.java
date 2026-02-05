package org.sudhir512kj.probability.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "org.sudhir512kj.probability.repository")
public class ProbabilityConfig {
}
