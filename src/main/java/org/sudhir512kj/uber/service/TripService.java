package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.model.Location;
import org.sudhir512kj.uber.model.Ride;
import org.sudhir512kj.uber.repository.RideRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TripService {
    private static final Logger log = LoggerFactory.getLogger(TripService.class);
    private final RideRepository rideRepository;
    private final H3GeoService h3GeoService;
    
    public TripService(RideRepository rideRepository, H3GeoService h3GeoService) {
        this.rideRepository = rideRepository;
        this.h3GeoService = h3GeoService;
    }
    
    public List<Ride> getActiveRides(UUID userId) {
        return rideRepository.findByRiderId(userId).stream()
            .filter(r -> r.getStatus() == Ride.RideStatus.STARTED || 
                        r.getStatus() == Ride.RideStatus.ACCEPTED)
            .collect(Collectors.toList());
    }
    
    public List<Ride> getCompletedRides(UUID userId, int limit) {
        return rideRepository.findByRiderId(userId).stream()
            .filter(r -> r.getStatus() == Ride.RideStatus.COMPLETED)
            .sorted((r1, r2) -> r2.getCompletedAt().compareTo(r1.getCompletedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    public int calculateRemainingDistance(Location current, Location destination) {
        return (int) Math.ceil(current.distanceTo(destination));
    }
    
    public int calculateRemainingTime(Location current, Location destination) {
        double distanceKm = current.distanceTo(destination);
        return (int) Math.ceil((distanceKm / 30.0) * 60); // 30 km/h avg
    }
}
