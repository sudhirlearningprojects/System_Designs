package org.sudhir512kj.uber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Profile("uber")
public class UberApplication {
    public static void main(String[] args) {
        SpringApplication.run(UberApplication.class, args);
    }
}
