package org.sudhir512kj.uber.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.uber.model.Driver;
import org.sudhir512kj.uber.model.Location;
import org.sudhir512kj.uber.model.Ride;
import org.sudhir512kj.uber.model.Vehicle;
import org.sudhir512kj.uber.service.AuthService;
import org.sudhir512kj.uber.service.DriverService;
import org.sudhir512kj.uber.service.GeoLocationService;
import org.sudhir512kj.uber.service.RideService;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {
    private final GeoLocationService geoLocationService;
    private final RideService rideService;
    private final DriverService driverService;
    private final AuthService authService;
    
    public DriverController(GeoLocationService geoLocationService, RideService rideService, 
                           DriverService driverService, AuthService authService) {
        this.geoLocationService = geoLocationService;
        this.rideService = rideService;
        this.driverService = driverService;
        this.authService = authService;
    }

    @PostMapping("/{driverId}/location")
    public ResponseEntity<Void> updateLocation(@PathVariable UUID driverId, @RequestBody Location location) {
        geoLocationService.updateDriverLocation(driverId, location);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rides/{rideId}/accept")
    public ResponseEntity<Ride> acceptRide(@PathVariable UUID rideId, @RequestParam UUID driverId) {
        Ride ride = rideService.acceptRide(rideId, driverId);
        return ResponseEntity.ok(ride);
    }

    @PutMapping("/rides/{rideId}/start")
    public ResponseEntity<Ride> startRide(@PathVariable UUID rideId) {
        Ride ride = rideService.startRide(rideId);
        return ResponseEntity.ok(ride);
    }

    @PutMapping("/rides/{rideId}/complete")
    public ResponseEntity<Ride> completeRide(@PathVariable UUID rideId, @RequestParam BigDecimal actualFare) {
        Ride ride = rideService.completeRide(rideId, actualFare);
        return ResponseEntity.ok(ride);
    }
    
    @PostMapping("/register")
    public ResponseEntity<Driver> register(@RequestBody DriverRegistration registration) {
        Driver driver = driverService.registerDriver(
            registration.name, 
            registration.phoneNumber, 
            registration.licenseNumber, 
            Vehicle.VehicleType.valueOf(registration.vehicleType)
        );
        return ResponseEntity.ok(driver);
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.phoneNumber, request.password);
        return ResponseEntity.ok(new LoginResponse(token));
    }
    
    @PutMapping("/{driverId}/status")
    public ResponseEntity<String> updateStatus(@PathVariable UUID driverId, @RequestParam String status) {
        driverService.updateStatus(driverId, Driver.DriverStatus.valueOf(status));
        return ResponseEntity.ok("Status updated to: " + status);
    }
    
    @GetMapping("/{driverId}/earnings")
    public ResponseEntity<EarningsResponse> getEarnings(@PathVariable UUID driverId) {
        BigDecimal earnings = driverService.getEarnings(driverId);
        Driver driver = driverService.getDriverById(driverId);
        return ResponseEntity.ok(new EarningsResponse(earnings.doubleValue(), driver.getTotalRides()));
    }
    
    @GetMapping("/{driverId}/ride-requests")
    public ResponseEntity<List<Ride>> getRideRequests(@PathVariable UUID driverId) {
        List<Ride> rides = driverService.getRideRequests(driverId);
        return ResponseEntity.ok(rides);
    }
    
    @PostMapping("/rides/{rideId}/decline")
    public ResponseEntity<String> declineRide(@PathVariable UUID rideId, @RequestParam UUID driverId) {
        rideService.declineRide(rideId, driverId);
        return ResponseEntity.ok("Ride declined");
    }
    
    static class DriverRegistration {
        public String name;
        public String phoneNumber;
        public String licenseNumber;
        public String vehicleType;
    }
    
    static class LoginRequest {
        public String phoneNumber;
        public String password;
    }

    static class LoginResponse {
        public String token;
        
        public LoginResponse(String token) {
            this.token = token;
        }
    }
    
    static class EarningsResponse {
        public double totalEarnings;
        public int totalRides;
        
        public EarningsResponse(double totalEarnings, int totalRides) {
            this.totalEarnings = totalEarnings;
            this.totalRides = totalRides;
        }
    }
}
