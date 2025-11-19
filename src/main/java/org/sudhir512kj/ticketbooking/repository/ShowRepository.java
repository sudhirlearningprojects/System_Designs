package org.sudhir512kj.ticketbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.ticketbooking.model.Show;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    
    List<Show> findByEventId(Long eventId);
    
    @Query("SELECT s FROM Show s WHERE s.event.id = :eventId AND DATE(s.showDate) = :date")
    List<Show> findByEventIdAndDate(@Param("eventId") Long eventId, @Param("date") LocalDate date);
    
    @Query("SELECT s FROM Show s WHERE s.event.id = :eventId AND s.venue.city = :city")
    List<Show> findByEventIdAndCity(@Param("eventId") Long eventId, @Param("city") String city);
    
    @Query("SELECT s FROM Show s WHERE s.event.id = :eventId AND DATE(s.showDate) = :date AND s.venue.city = :city")
    List<Show> findByEventIdAndDateAndCity(@Param("eventId") Long eventId, @Param("date") LocalDate date, @Param("city") String city);
    
    @Query("SELECT s FROM Show s WHERE s.venue.city = :city AND s.showDate >= :fromDate")
    List<Show> findByVenueCityAndShowDateAfter(@Param("city") String city, @Param("fromDate") LocalDateTime fromDate);
    
    List<Show> findByVenueId(Long venueId);
}