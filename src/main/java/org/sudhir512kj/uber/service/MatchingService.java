package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.dto.RideRequest;
import org.sudhir512kj.uber.model.*;
import org.sudhir512kj.uber.repository.DriverRepository;
import java.util.*;

@Service
public class MatchingService {
    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);
    private final GeoLocationService geoLocationService;
    private final DriverRepository driverRepository;
    private final NotificationService notificationService;
    
    public MatchingService(GeoLocationService geoLocationService,
                          DriverRepository driverRepository,
                          NotificationService notificationService) {
        this.geoLocationService = geoLocationService;
        this.driverRepository = driverRepository;
        this.notificationService = notificationService;
    }

    public Driver findBestDriver(RideRequest request) {
        Location pickup = request.getPickupLocation();
        Vehicle.VehicleType vehicleType = request.getVehicleType();
        
        // Use gRPC for internal service call (production-grade)
        List<UUID> nearbyDriverIds = geoLocationService.findNearbyDrivers(pickup, 5.0, 20);
        
        if (nearbyDriverIds.isEmpty()) {
            nearbyDriverIds = geoLocationService.findNearbyDrivers(pickup, 10.0, 20);
        }
        
        if (nearbyDriverIds.isEmpty()) return null;
        
        List<Driver> availableDrivers = driverRepository.findAllById(nearbyDriverIds);
        
        for (Driver driver : availableDrivers) {
            if (isDriverEligible(driver, vehicleType)) {
                notificationService.sendRideRequest(driver.getUserId(), request);
                return driver;
            }
        }
        
        return null;
    }

    private boolean isDriverEligible(Driver driver, Vehicle.VehicleType vehicleType) {
        return driver.getStatus() == Driver.DriverStatus.ONLINE &&
               driver.getIsVerified() &&
               driver.getVehicle() != null &&
               driver.getVehicle().getVehicleType() == vehicleType;
    }
}
