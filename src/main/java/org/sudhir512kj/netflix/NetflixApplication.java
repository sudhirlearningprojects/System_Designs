package org.sudhir512kj.netflix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class NetflixApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetflixApplication.class, args);
    }
}