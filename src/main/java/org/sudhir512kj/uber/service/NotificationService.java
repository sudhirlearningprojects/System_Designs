package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.dto.RideRequest;
import org.sudhir512kj.uber.model.Ride;
import java.util.UUID;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    public void sendRideRequest(UUID driverId, RideRequest request) {
        log.info("Sending ride request to driver: {}", driverId);
    }

    public void notifyRider(UUID riderId, String message, Ride ride) {
        log.info("Notifying rider {}: {}", riderId, message);
    }
}
