package org.sudhir512kj.ticketbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.ticketbooking.model.Event;
import org.sudhir512kj.ticketbooking.model.EventCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    @Query("SELECT e FROM Event e WHERE " +
           "(:city IS NULL OR LOWER(e.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:genre IS NULL OR LOWER(e.genre) LIKE LOWER(CONCAT('%', :genre, '%'))) AND " +
           "(:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "e.eventDate >= :fromDate")
    Page<Event> searchEvents(@Param("city") String city,
                            @Param("genre") String genre,
                            @Param("name") String name,
                            @Param("fromDate") LocalDateTime fromDate,
                            Pageable pageable);
    
    @Query("SELECT DISTINCT e FROM Event e " +
           "LEFT JOIN Show s ON s.event.id = e.id " +
           "LEFT JOIN ShowSeat ss ON ss.show.id = s.id " +
           "WHERE e.isActive = true " +
           "AND (:city IS NULL OR LOWER(e.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (:genre IS NULL OR LOWER(e.genre) LIKE LOWER(CONCAT('%', :genre, '%'))) " +
           "AND (:language IS NULL OR LOWER(e.language) LIKE LOWER(CONCAT('%', :language, '%'))) " +
           "AND (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:fromDate IS NULL OR DATE(e.eventDate) >= :fromDate) " +
           "AND (:toDate IS NULL OR DATE(e.eventDate) <= :toDate) " +
           "AND (:minPrice IS NULL OR ss.finalPrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR ss.finalPrice <= :maxPrice)")
    Page<Event> searchEventsWithFilters(@Param("city") String city,
                                       @Param("category") EventCategory category,
                                       @Param("genre") String genre,
                                       @Param("language") String language,
                                       @Param("name") String name,
                                       @Param("fromDate") LocalDate fromDate,
                                       @Param("toDate") LocalDate toDate,
                                       @Param("minPrice") BigDecimal minPrice,
                                       @Param("maxPrice") BigDecimal maxPrice,
                                       Pageable pageable);
    
    List<Event> findByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc();
    
    @Query("SELECT e FROM Event e " +
           "LEFT JOIN Show s ON s.event.id = e.id " +
           "LEFT JOIN Booking b ON b.show.id = s.id " +
           "WHERE e.isActive = true " +
           "AND (:city IS NULL OR LOWER(e.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND b.createdAt >= :since " +
           "GROUP BY e.id " +
           "ORDER BY COUNT(b.id) DESC")
    List<Event> findTrendingEvents(@Param("city") String city, @Param("since") LocalDateTime since);
    
    @Query("SELECT e.category, COUNT(e) FROM Event e " +
           "WHERE e.isActive = true " +
           "AND (:city IS NULL OR LOWER(e.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "GROUP BY e.category")
    List<Object[]> findEventCountByCategory(@Param("city") String city);
}