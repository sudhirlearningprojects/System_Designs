package org.sudhir512kj.dropbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class DropboxApplication {
    public static void main(String[] args) {
        SpringApplication.run(DropboxApplication.class, args);
    }
}