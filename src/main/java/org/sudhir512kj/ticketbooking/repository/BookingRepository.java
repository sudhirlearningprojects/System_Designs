package org.sudhir512kj.ticketbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.ticketbooking.model.Booking;
import org.sudhir512kj.ticketbooking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.holdExpiresAt < :currentTime")
    List<Booking> findExpiredHolds(@Param("status") BookingStatus status, @Param("currentTime") LocalDateTime currentTime);
}