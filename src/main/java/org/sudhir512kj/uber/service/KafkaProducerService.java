package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.model.Location;
import java.util.UUID;

@Service
public class KafkaProducerService {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    private static final String LOCATION_TOPIC = "uber.location.updates";
    private static final String RIDE_EVENTS_TOPIC = "uber.ride.events";
    
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishLocationUpdate(UUID driverId, Location location) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            log.warn("Invalid location for driver: {}", driverId);
            return;
        }
        String message = String.format("{\"driverId\":\"%s\",\"lat\":%.6f,\"lng\":%.6f,\"timestamp\":%d}",
            driverId, location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
        
        kafkaTemplate.send(LOCATION_TOPIC, driverId.toString(), message);
        log.debug("Published location update for driver: {}", driverId);
    }

    public void publishRideEvent(UUID rideId, String eventType, String payload) {
        String message = String.format("{\"rideId\":\"%s\",\"event\":\"%s\",\"data\":%s,\"timestamp\":%d}",
            rideId, eventType, payload, System.currentTimeMillis());
        
        kafkaTemplate.send(RIDE_EVENTS_TOPIC, rideId.toString(), message);
        log.info("Published ride event: {} for ride: {}", eventType, rideId);
    }
}
