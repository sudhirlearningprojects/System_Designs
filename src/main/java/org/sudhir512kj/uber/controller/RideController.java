package org.sudhir512kj.uber.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.uber.dto.RideRequest;
import org.sudhir512kj.uber.model.Ride;
import org.sudhir512kj.uber.service.PricingService;
import org.sudhir512kj.uber.service.RatingService;
import org.sudhir512kj.uber.service.RideService;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rides")
public class RideController {
    private final RideService rideService;
    private final PricingService pricingService;
    private final RatingService ratingService;
    
    public RideController(RideService rideService, PricingService pricingService, RatingService ratingService) {
        this.rideService = rideService;
        this.pricingService = pricingService;
        this.ratingService = ratingService;
    }

    @PostMapping("/request")
    public ResponseEntity<Ride> requestRide(@RequestBody RideRequest request) {
        Ride ride = rideService.requestRide(request);
        return ResponseEntity.ok(ride);
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<Ride> getRide(@PathVariable UUID rideId) {
        Ride ride = rideService.getRide(rideId);
        return ResponseEntity.ok(ride);
    }

    @PutMapping("/{rideId}/cancel")
    public ResponseEntity<Ride> cancelRide(@PathVariable UUID rideId) {
        Ride ride = rideService.cancelRide(rideId);
        return ResponseEntity.ok(ride);
    }
    
    @PostMapping("/estimate")
    public ResponseEntity<FareEstimate> estimateFare(@RequestBody RideRequest request) {
        BigDecimal fare = pricingService.calculateFare(
            request.getPickupLocation(), 
            request.getDropoffLocation(), 
            request.getVehicleType()
        );
        int estimatedMinutes = (int) (pricingService.calculateDistance(
            request.getPickupLocation(), 
            request.getDropoffLocation()
        ) / 0.5); // Assume 30 km/h average speed
        return ResponseEntity.ok(new FareEstimate(fare.doubleValue(), estimatedMinutes));
    }
    
    @GetMapping("/{rideId}/location")
    public ResponseEntity<LocationResponse> getRideLocation(@PathVariable UUID rideId) {
        Ride ride = rideService.getRide(rideId);
        return ResponseEntity.ok(new LocationResponse(
            ride.getPickupLocation().getLatitude(), 
            ride.getPickupLocation().getLongitude(), 
            5
        ));
    }
    
    @PostMapping("/{rideId}/rating")
    public ResponseEntity<String> rateRide(@PathVariable UUID rideId, @RequestBody RatingRequest rating) {
        ratingService.rateRide(rideId, rating.rating, rating.feedback);
        return ResponseEntity.ok("Rating submitted");
    }
    
    static class FareEstimate {
        public double estimatedFare;
        public int estimatedMinutes;
        
        public FareEstimate(double estimatedFare, int estimatedMinutes) {
            this.estimatedFare = estimatedFare;
            this.estimatedMinutes = estimatedMinutes;
        }
    }
    
    static class LocationResponse {
        public double latitude;
        public double longitude;
        public int etaMinutes;
        
        public LocationResponse(double latitude, double longitude, int etaMinutes) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.etaMinutes = etaMinutes;
        }
    }
    
    static class RatingRequest {
        public int rating;
        public String feedback;
    }
}
