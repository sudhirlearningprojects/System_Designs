package org.sudhir512kj.ticketbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.ticketbooking.model.FoodBeverage;
import org.sudhir512kj.ticketbooking.model.FoodCategory;

import java.util.List;

@Repository
public interface FoodBeverageRepository extends JpaRepository<FoodBeverage, Long> {
    
    List<FoodBeverage> findByVenueIdAndIsAvailableTrue(Long venueId);
    
    List<FoodBeverage> findByVenueIdAndCategoryAndIsAvailableTrue(Long venueId, FoodCategory category);
    
    List<FoodBeverage> findByVenueIdAndIsVegetarianAndIsAvailableTrue(Long venueId, Boolean isVegetarian);
}