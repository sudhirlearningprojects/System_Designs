package org.sudhir512kj.ticketbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.ticketbooking.model.BookingSeat;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    
    List<BookingSeat> findByBookingId(Long bookingId);
    
    List<BookingSeat> findByShowSeatId(Long showSeatId);
}