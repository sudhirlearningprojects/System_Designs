package org.sudhir512kj.whatsapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "whatsapp")
public class WhatsAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhatsAppApplication.class, args);
    }
}