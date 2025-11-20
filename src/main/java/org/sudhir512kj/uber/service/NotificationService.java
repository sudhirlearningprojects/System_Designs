package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.dto.RideRequest;
import org.sudhir512kj.uber.model.Ride;
import org.sudhir512kj.uber.notification.CCGPersistor;
import org.sudhir512kj.uber.notification.PushMessage;
import java.util.Map;
import java.util.UUID;

/**
 * Notification Service using Uber's CCG (Consumer Communication Gateway) architecture
 * 
 * Components:
 * - Persistor: Stores messages in Push Inbox
 * - Scheduler: Processes priority queues (HIGH/MEDIUM/LOW)
 * - Push Delivery: Sends to FCM/APNS
 */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final CCGPersistor persistor;
    
    public NotificationService(CCGPersistor persistor) {
        this.persistor = persistor;
    }
    
    public void sendRideRequest(UUID driverId, RideRequest request) {
        PushMessage message = PushMessage.builder()
            .userId(driverId)
            .title("New Ride Request")
            .body("Pickup: " + request.getPickupLocation())
            .priority(PushMessage.Priority.HIGH)
            .data(Map.of(
                "ride_id", request.getRiderId().toString(),
                "vehicle_type", request.getVehicleType().toString(),
                "pickup_lat", request.getPickupLocation().getLatitude().toString(),
                "pickup_lng", request.getPickupLocation().getLongitude().toString()
            ))
            .build();
        
        persistor.persist(message);
        log.info("Queued ride request notification for driver: {}", driverId);
    }

    public void notifyRider(UUID riderId, String messageText, Ride ride) {
        PushMessage message = PushMessage.builder()
            .userId(riderId)
            .title("Ride Update")
            .body(messageText)
            .priority(PushMessage.Priority.MEDIUM)
            .data(Map.of(
                "ride_id", ride.getRideId().toString(),
                "status", ride.getStatus().toString()
            ))
            .build();
        
        persistor.persist(message);
        log.info("Queued ride update notification for rider: {}", riderId);
    }
}
