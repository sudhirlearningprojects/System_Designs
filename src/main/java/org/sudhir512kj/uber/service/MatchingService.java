package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.dto.RideRequest;
import org.sudhir512kj.uber.model.*;
import org.sudhir512kj.uber.repository.DriverRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * DISCO-inspired Matching Service
 * Implements Uber's dispatch optimization algorithm for efficient driver-rider matching
 * 
 * Algorithm:
 * 1. Spatial Filtering: H3-based geo search (100m → 5km → 10km)
 * 2. Temporal Filtering: Remove unavailable drivers
 * 3. Scoring: Multi-factor weighted scoring
 * 4. Notification: Send to top driver with 30s timeout
 * 5. Fallback: Try next driver if declined/timeout
 */
@Service
public class MatchingService {
    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);
    private final H3GeoService h3GeoService;
    private final DriverRepository driverRepository;
    private final NotificationService notificationService;
    private final RedisTemplate<String, String> redisTemplate;
    
    // Scoring weights (ML-optimized in production)
    private static final double WEIGHT_DISTANCE = 0.5;
    private static final double WEIGHT_ACCEPTANCE_RATE = 0.2;
    private static final double WEIGHT_RATING = 0.2;
    private static final double WEIGHT_ETA = 0.1;
    
    // Matching parameters
    private static final int MAX_DRIVER_ATTEMPTS = 3;
    private static final long DRIVER_RESPONSE_TIMEOUT_SEC = 30;
    private static final long DRIVER_COOLDOWN_SEC = 300; // 5 minutes
    
    public MatchingService(H3GeoService h3GeoService,
                          DriverRepository driverRepository,
                          NotificationService notificationService,
                          RedisTemplate<String, String> redisTemplate) {
        this.h3GeoService = h3GeoService;
        this.driverRepository = driverRepository;
        this.notificationService = notificationService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Find the best driver using DISCO algorithm
     * Returns null if no suitable driver found after MAX_DRIVER_ATTEMPTS
     */
    public Driver findBestDriver(RideRequest request) {
        Location pickup = request.getPickupLocation();
        Vehicle.VehicleType vehicleType = request.getVehicleType();
        
        log.info("Starting driver matching for ride request: riderId={}, vehicleType={}", 
                 request.getRiderId(), vehicleType);
        
        // Step 1: Spatial Filtering (H3-based hierarchical search)
        List<UUID> nearbyDriverIds = findNearbyDriversHierarchical(pickup);
        
        if (nearbyDriverIds.isEmpty()) {
            log.warn("No drivers found near pickup location");
            return null;
        }
        
        log.info("Found {} nearby drivers", nearbyDriverIds.size());
        
        // Step 2: Temporal Filtering + Scoring
        List<DriverScore> scoredDrivers = scoreAndFilterDrivers(nearbyDriverIds, pickup, vehicleType);
        
        if (scoredDrivers.isEmpty()) {
            log.warn("No eligible drivers after filtering");
            return null;
        }
        
        // Step 3: Try top drivers sequentially
        for (int attempt = 0; attempt < Math.min(MAX_DRIVER_ATTEMPTS, scoredDrivers.size()); attempt++) {
            DriverScore topDriver = scoredDrivers.get(attempt);
            log.info("Attempting driver {} (score: {:.2f})", topDriver.driver.getUserId(), topDriver.score);
            
            // Send notification
            notificationService.sendRideRequest(
                topDriver.driver.getUserId(), 
                request
            );
            
            // In production: Wait for driver response via WebSocket/callback
            // For now, assume first driver accepts
            if (attempt == 0) {
                log.info("Driver {} accepted the ride", topDriver.driver.getUserId());
                return topDriver.driver;
            } else {
                // Add to cooldown (prevent immediate re-matching)
                addDriverToCooldown(topDriver.driver.getUserId());
                log.info("Driver {} declined/timeout, trying next", topDriver.driver.getUserId());
            }
        }
        
        log.warn("All {} driver attempts failed", MAX_DRIVER_ATTEMPTS);
        return null;
    }

    /**
     * Hierarchical search: 100m → 5km → 10km
     */
    private List<UUID> findNearbyDriversHierarchical(Location pickup) {
        // Level 1: Search within 100m (resolution 9)
        List<UUID> drivers = h3GeoService.findNearbyDrivers(pickup, 0.1, 20);
        if (!drivers.isEmpty()) {
            log.debug("Found {} drivers within 100m", drivers.size());
            return drivers;
        }
        
        // Level 2: Expand to 5km (resolution 7)
        drivers = h3GeoService.findNearbyDrivers(pickup, 5.0, 20);
        if (!drivers.isEmpty()) {
            log.debug("Found {} drivers within 5km", drivers.size());
            return drivers;
        }
        
        // Level 3: Expand to 10km (last resort)
        drivers = h3GeoService.findNearbyDrivers(pickup, 10.0, 20);
        log.debug("Found {} drivers within 10km", drivers.size());
        return drivers;
    }

    /**
     * Score and filter drivers based on multiple factors
     */
    private List<DriverScore> scoreAndFilterDrivers(List<UUID> driverIds, 
                                                     Location pickup, 
                                                     Vehicle.VehicleType vehicleType) {
        List<Driver> drivers = driverRepository.findAllById(driverIds);
        
        return drivers.stream()
            .filter(driver -> isDriverEligible(driver, vehicleType))
            .map(driver -> new DriverScore(driver, calculateScore(driver, pickup)))
            .sorted(Comparator.comparingDouble(ds -> -ds.score)) // Descending order
            .collect(Collectors.toList());
    }

    /**
     * Calculate driver score using weighted multi-factor formula
     * 
     * score = w1 * (1/distance) + w2 * acceptance_rate + w3 * rating + w4 * (1/eta)
     */
    private double calculateScore(Driver driver, Location pickup) {
        // Factor 1: Distance (closer is better)
        double distance = pickup.distanceTo(driver.getCurrentLocation());
        double distanceScore = 1.0 / (1.0 + distance); // Normalize to [0, 1]
        
        // Factor 2: Acceptance rate (higher is better)
        double acceptanceRate = getDriverAcceptanceRate(driver.getUserId());
        
        // Factor 3: Driver rating (higher is better)
        double rating = driver.getRating() != null ? 
            driver.getRating().doubleValue() / 5.0 : 0.8; // Normalize to [0, 1]
        
        // Factor 4: ETA (faster is better)
        double eta = estimateETA(driver.getCurrentLocation(), pickup);
        double etaScore = 1.0 / (1.0 + eta / 10.0); // Normalize
        
        double totalScore = WEIGHT_DISTANCE * distanceScore +
                           WEIGHT_ACCEPTANCE_RATE * acceptanceRate +
                           WEIGHT_RATING * rating +
                           WEIGHT_ETA * etaScore;
        
        log.debug("Driver {} score: {:.2f} (dist={:.2f}km, accept={:.2f}, rating={:.2f}, eta={:.0f}min)",
                 driver.getUserId(), totalScore, distance, acceptanceRate, rating, eta);
        
        return totalScore;
    }

    /**
     * Check if driver is eligible for matching
     */
    private boolean isDriverEligible(Driver driver, Vehicle.VehicleType vehicleType) {
        // Basic checks
        if (driver.getStatus() != Driver.DriverStatus.ONLINE) return false;
        if (!driver.getIsVerified()) return false;
        if (driver.getVehicle() == null) return false;
        if (driver.getVehicle().getVehicleType() != vehicleType) return false;
        
        // Check if driver is in cooldown (recently declined)
        String cooldownKey = "driver:cooldown:" + driver.getUserId();
        Boolean inCooldown = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(inCooldown)) {
            log.debug("Driver {} is in cooldown", driver.getUserId());
            return false;
        }
        
        // Check last location update (must be < 30 seconds)
        String driverKey = "driver:" + driver.getUserId();
        String updatedAt = (String) redisTemplate.opsForHash().get(driverKey, "updated_at");
        if (updatedAt != null) {
            long lastUpdate = Long.parseLong(updatedAt);
            long ageSeconds = (System.currentTimeMillis() - lastUpdate) / 1000;
            if (ageSeconds > 30) {
                log.debug("Driver {} location is stale ({}s old)", driver.getUserId(), ageSeconds);
                return false;
            }
        }
        
        return true;
    }

    /**
     * Get driver's acceptance rate from Redis cache
     */
    private double getDriverAcceptanceRate(UUID driverId) {
        String key = "driver:stats:" + driverId;
        String acceptanceRate = (String) redisTemplate.opsForHash().get(key, "acceptance_rate");
        return acceptanceRate != null ? Double.parseDouble(acceptanceRate) : 0.8; // Default 80%
    }
    
    /**
     * Update driver acceptance rate after ride response
     */
    public void updateAcceptanceRate(UUID driverId, boolean accepted) {
        String key = "driver:stats:" + driverId;
        String totalStr = (String) redisTemplate.opsForHash().get(key, "total_requests");
        String acceptedStr = (String) redisTemplate.opsForHash().get(key, "accepted_requests");
        
        int total = totalStr != null ? Integer.parseInt(totalStr) : 0;
        int acceptedCount = acceptedStr != null ? Integer.parseInt(acceptedStr) : 0;
        
        total++;
        if (accepted) acceptedCount++;
        
        double rate = (double) acceptedCount / total;
        
        Map<String, String> stats = Map.of(
            "total_requests", String.valueOf(total),
            "accepted_requests", String.valueOf(acceptedCount),
            "acceptance_rate", String.format("%.2f", rate),
            "updated_at", String.valueOf(System.currentTimeMillis())
        );
        
        redisTemplate.opsForHash().putAll(key, stats);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    /**
     * Estimate ETA in minutes (simplified)
     * In production: Use ML model with traffic data
     */
    private double estimateETA(Location from, Location to) {
        double distanceKm = from.distanceTo(to);
        double avgSpeedKmh = 30.0; // Average city speed
        return (distanceKm / avgSpeedKmh) * 60.0; // Convert to minutes
    }

    /**
     * Add driver to cooldown period after declining
     */
    private void addDriverToCooldown(UUID driverId) {
        String key = "driver:cooldown:" + driverId;
        redisTemplate.opsForValue().set(key, "1", DRIVER_COOLDOWN_SEC, TimeUnit.SECONDS);
    }

    /**
     * Get nearby drivers for display (without matching)
     */
    public List<Driver> getNearbyDriversForMap(Location location, int limit) {
        List<UUID> driverIds = h3GeoService.findNearbyDrivers(location, 5.0, limit);
        return driverRepository.findAllById(driverIds);
    }
    
    /**
     * Helper class to store driver with score
     */
    private static class DriverScore {
        final Driver driver;
        final double score;
        
        DriverScore(Driver driver, double score) {
            this.driver = driver;
            this.score = score;
        }
    }
}
