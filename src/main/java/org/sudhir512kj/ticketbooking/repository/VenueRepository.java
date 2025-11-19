package org.sudhir512kj.ticketbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.ticketbooking.model.Venue;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
    
    Page<Venue> findByCityContainingIgnoreCase(String city, Pageable pageable);
    
    Page<Venue> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    @Query("SELECT v FROM Venue v WHERE " +
           "(:city IS NULL OR LOWER(v.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:name IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:type IS NULL OR LOWER(v.type) = LOWER(:type))")
    Page<Venue> searchVenues(@Param("city") String city, 
                             @Param("name") String name, 
                             @Param("type") String type, 
                             Pageable pageable);
    
    @Query(value = "SELECT * FROM venues v WHERE " +
           "ST_DWithin(ST_MakePoint(v.longitude, v.latitude)::geography, " +
           "ST_MakePoint(:longitude, :latitude)::geography, :radiusMeters)",
           nativeQuery = true)
    List<Venue> findNearbyVenues(@Param("latitude") Double latitude, 
                                 @Param("longitude") Double longitude, 
                                 @Param("radiusMeters") Double radiusMeters);
}