package org.sudhir512kj.distributeddb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Profile("distributeddb")
public class DistributedDBApplication {
    public static void main(String[] args) {
        SpringApplication.run(DistributedDBApplication.class, args);
    }
}
