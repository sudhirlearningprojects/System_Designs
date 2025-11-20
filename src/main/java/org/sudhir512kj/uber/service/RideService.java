package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.uber.dto.RideRequest;
import org.sudhir512kj.uber.model.*;
import org.sudhir512kj.uber.repository.RideRepository;
import org.springframework.data.redis.core.RedisTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class RideService {
    private static final Logger log = LoggerFactory.getLogger(RideService.class);
    private final RideRepository rideRepository;
    private final MatchingService matchingService;
    private final PricingService pricingService;
    private final NotificationService notificationService;
    private final KafkaProducerService kafkaProducerService;
    private final ElasticsearchService elasticsearchService;
    private final RedisTemplate<String, String> redisTemplate;
    private final SurgePricingService surgePricingService;
    
    public RideService(RideRepository rideRepository,
                      MatchingService matchingService,
                      PricingService pricingService,
                      NotificationService notificationService,
                      KafkaProducerService kafkaProducerService,
                      ElasticsearchService elasticsearchService,
                      RedisTemplate<String, String> redisTemplate,
                      SurgePricingService surgePricingService) {
        this.rideRepository = rideRepository;
        this.matchingService = matchingService;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
        this.kafkaProducerService = kafkaProducerService;
        this.elasticsearchService = elasticsearchService;
        this.redisTemplate = redisTemplate;
        this.surgePricingService = surgePricingService;
    }

    @Transactional
    public Ride requestRide(RideRequest request) {
        // Increment demand for surge pricing
        surgePricingService.incrementDemand(request.getPickupLocation());
        
        BigDecimal estimatedFare = pricingService.calculateFare(
            request.getPickupLocation(),
            request.getDropoffLocation(),
            request.getVehicleType()
        );

        Ride ride = new Ride();
        ride.setRiderId(request.getRiderId());
        ride.setVehicleType(request.getVehicleType());
        ride.setStatus(Ride.RideStatus.REQUESTED);
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDropoffLocation(request.getDropoffLocation());
        ride.setEstimatedFare(estimatedFare);
        ride.setRequestedAt(LocalDateTime.now());

        ride = rideRepository.save(ride);
        log.info("Ride requested: {}", ride.getRideId());
        
        // Publish ride event to Kafka
        kafkaProducerService.publishRideEvent(ride.getRideId(), "RIDE_REQUESTED", 
            String.format("{\"riderId\":\"%s\",\"fare\":%.2f}", request.getRiderId(), estimatedFare));

        Driver matchedDriver = matchingService.findBestDriver(request);
        
        if (matchedDriver != null) {
            ride.setDriverId(matchedDriver.getUserId());
            ride.setStatus(Ride.RideStatus.ACCEPTED);
            ride.setAcceptedAt(LocalDateTime.now());
            ride = rideRepository.save(ride);
            
            // Publish acceptance event
            kafkaProducerService.publishRideEvent(ride.getRideId(), "RIDE_ACCEPTED", 
                String.format("{\"driverId\":\"%s\"}", matchedDriver.getUserId()));
            
            notificationService.notifyRider(request.getRiderId(), "Driver found!", ride);
        }

        return ride;
    }

    @Transactional
    public Ride acceptRide(UUID rideId, UUID driverId) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));

        ride.setDriverId(driverId);
        ride.setStatus(Ride.RideStatus.ACCEPTED);
        ride.setAcceptedAt(LocalDateTime.now());

        return rideRepository.save(ride);
    }

    @Transactional
    public Ride startRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));

        ride.setStatus(Ride.RideStatus.STARTED);
        ride.setStartedAt(LocalDateTime.now());

        return rideRepository.save(ride);
    }

    @Transactional
    public Ride completeRide(UUID rideId, BigDecimal actualFare) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));

        ride.setStatus(Ride.RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        ride.setActualFare(actualFare);
        
        // Decrement demand after ride completion
        surgePricingService.decrementDemand(ride.getPickupLocation());
        
        ride = rideRepository.save(ride);
        
        // Publish completion event to Kafka
        kafkaProducerService.publishRideEvent(rideId, "RIDE_COMPLETED", 
            String.format("{\"fare\":%.2f,\"duration\":%d}", actualFare, ride.getDurationMinutes()));
        
        // Index to Elasticsearch for analytics
        elasticsearchService.indexRideForAnalytics(rideId, "COMPLETED", 
            actualFare.doubleValue(), ride.getDurationMinutes() != null ? ride.getDurationMinutes() : 0);

        return ride;
    }

    @Transactional
    public Ride cancelRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));

        ride.setStatus(Ride.RideStatus.CANCELLED);
        ride.setCancelledAt(LocalDateTime.now());

        return rideRepository.save(ride);
    }

    public Ride getRide(UUID rideId) {
        return rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));
    }

    public java.util.List<Ride> getRideHistory(UUID riderId) {
        return rideRepository.findByRiderId(riderId);
    }

    @Transactional
    public void declineRide(UUID rideId, UUID driverId) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        if (ride.getStatus() == Ride.RideStatus.REQUESTED) {
            Driver nextDriver = matchingService.findBestDriver(
                new RideRequest(ride.getRiderId(), ride.getPickupLocation(), 
                    ride.getDropoffLocation(), ride.getVehicleType())
            );
            
            if (nextDriver != null) {
                ride.setDriverId(nextDriver.getUserId());
                ride.setStatus(Ride.RideStatus.ACCEPTED);
                ride.setAcceptedAt(LocalDateTime.now());
                rideRepository.save(ride);
                notificationService.notifyRider(ride.getRiderId(), "New driver assigned", ride);
            }
        }
    }
    
    public Location getDriverLocation(UUID driverId) {
        Map<Object, Object> driverData = redisTemplate.opsForHash()
            .entries("driver:" + driverId);
        
        if (driverData.isEmpty()) return null;
        
        Location location = new Location();
        location.setLatitude(Double.parseDouble((String) driverData.get("lat")));
        location.setLongitude(Double.parseDouble((String) driverData.get("lng")));
        return location;
    }
    
    public int calculateETA(Location from, Location to) {
        double distanceKm = from.distanceTo(to);
        return (int) Math.ceil((distanceKm / 30.0) * 60); // 30 km/h avg speed
    }
}
