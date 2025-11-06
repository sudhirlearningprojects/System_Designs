package org.sudhir512kj.ticketbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.ticketbooking.model.Event;

import java.time.LocalDateTime;

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
}