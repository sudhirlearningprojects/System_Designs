package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.uber.model.Driver;
import org.sudhir512kj.uber.model.Ride;
import org.sudhir512kj.uber.repository.DriverRepository;
import org.sudhir512kj.uber.repository.RideRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class RatingService {
    private static final Logger log = LoggerFactory.getLogger(RatingService.class);
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    
    public RatingService(RideRepository rideRepository, DriverRepository driverRepository) {
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional
    public void rateRide(UUID rideId, int rating, String feedback) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        if (ride.getDriverId() != null) {
            Driver driver = driverRepository.findById(ride.getDriverId())
                .orElseThrow(() -> new RuntimeException("Driver not found"));
            
            BigDecimal currentRating = driver.getRating() != null ? driver.getRating() : BigDecimal.ZERO;
            int totalRides = driver.getTotalRides();
            
            BigDecimal newRating = currentRating.multiply(BigDecimal.valueOf(totalRides))
                .add(BigDecimal.valueOf(rating))
                .divide(BigDecimal.valueOf(totalRides + 1), 2, RoundingMode.HALF_UP);
            
            driver.setRating(newRating);
            driverRepository.save(driver);
            
            log.info("Driver {} rated {} for ride {}", driver.getUserId(), rating, rideId);
        }
    }
}
