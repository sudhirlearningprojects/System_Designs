package org.sudhir512kj.cloudflare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Profile("cloudflare")
public class CloudflareApplication {
    
    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "cloudflare");
        SpringApplication.run(CloudflareApplication.class, args);
    }
}