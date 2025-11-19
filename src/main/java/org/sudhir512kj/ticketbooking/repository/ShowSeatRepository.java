package org.sudhir512kj.ticketbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.ticketbooking.model.ShowSeat;
import org.sudhir512kj.ticketbooking.model.SeatStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {
    
    List<ShowSeat> findByShowId(Long showId);
    
    List<ShowSeat> findByShowIdAndStatus(Long showId, SeatStatus status);
    
    @Query("SELECT ss FROM ShowSeat ss WHERE ss.status = 'HELD' AND ss.holdExpiresAt < :now")
    List<ShowSeat> findExpiredHolds(@Param("now") LocalDateTime now);
    
    long countByShowIdAndStatus(Long showId, SeatStatus status);
}