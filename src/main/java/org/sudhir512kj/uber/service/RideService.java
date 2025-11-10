package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.uber.dto.RideRequest;
import org.sudhir512kj.uber.model.*;
import org.sudhir512kj.uber.repository.RideRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    
    public RideService(RideRepository rideRepository,
                      MatchingService matchingService,
                      PricingService pricingService,
                      NotificationService notificationService,
                      KafkaProducerService kafkaProducerService,
                      ElasticsearchService elasticsearchService) {
        this.rideRepository = rideRepository;
        this.matchingService = matchingService;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
        this.kafkaProducerService = kafkaProducerService;
        this.elasticsearchService = elasticsearchService;
    }

    @Transactional
    public Ride requestRide(RideRequest request) {
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
        log.info("Driver {} declined ride {}", driverId, rideId);
        // In production: find another driver
    }
}
