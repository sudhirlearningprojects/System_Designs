package org.sudhir512kj.cronjob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CronJobApplication {
    public static void main(String[] args) {
        SpringApplication.run(CronJobApplication.class, args);
    }
}
