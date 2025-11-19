package org.sudhir512kj.ticketbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.ticketbooking.model.BookingFoodItem;

import java.util.List;

@Repository
public interface BookingFoodItemRepository extends JpaRepository<BookingFoodItem, Long> {
    
    List<BookingFoodItem> findByBookingId(Long bookingId);
}