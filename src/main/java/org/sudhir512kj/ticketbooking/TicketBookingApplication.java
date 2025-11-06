package org.sudhir512kj.ticketbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class TicketBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketBookingApplication.class, args);
    }
}