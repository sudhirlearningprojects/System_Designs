package org.sudhir512kj.instagram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories
@EnableCaching
@EnableAsync
@EnableKafka
@EnableTransactionManagement
public class InstagramApplication {
    public static void main(String[] args) {
        SpringApplication.run(InstagramApplication.class, args);
    }
}