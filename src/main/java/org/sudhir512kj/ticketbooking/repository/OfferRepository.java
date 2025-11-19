package org.sudhir512kj.ticketbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.ticketbooking.model.Offer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    
    Optional<Offer> findByOfferCodeAndIsActiveTrue(String offerCode);
    
    @Query("SELECT o FROM Offer o WHERE o.isActive = true AND o.validFrom <= :now AND o.validUntil >= :now")
    List<Offer> findActiveOffers(@Param("now") LocalDateTime now);
    
    @Query("SELECT o FROM Offer o WHERE o.isActive = true AND o.validFrom <= :now AND o.validUntil >= :now " +
           "AND (o.applicableCities IS NULL OR o.applicableCities LIKE %:city%)")
    List<Offer> findActiveOffersByCity(@Param("now") LocalDateTime now, @Param("city") String city);
    
    @Query("SELECT o FROM Offer o WHERE o.isActive = true AND o.validFrom <= :now AND o.validUntil >= :now " +
           "AND (o.applicableCategories IS NULL OR o.applicableCategories LIKE %:category%)")
    List<Offer> findActiveOffersByCategory(@Param("now") LocalDateTime now, @Param("category") String category);
}