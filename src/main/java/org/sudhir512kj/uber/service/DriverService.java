package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.uber.model.Driver;
import org.sudhir512kj.uber.model.Ride;
import org.sudhir512kj.uber.model.Vehicle;
import org.sudhir512kj.uber.repository.DriverRepository;
import org.sudhir512kj.uber.repository.RideRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DriverService {
    private static final Logger log = LoggerFactory.getLogger(DriverService.class);
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    
    public DriverService(DriverRepository driverRepository, RideRepository rideRepository) {
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
    }

    @Transactional
    public Driver registerDriver(String name, String phoneNumber, String licenseNumber, Vehicle.VehicleType vehicleType) {
        Driver driver = new Driver();
        driver.setName(name);
        driver.setPhoneNumber(phoneNumber);
        driver.setLicenseNumber(licenseNumber);
        driver.setUserType(org.sudhir512kj.uber.model.User.UserType.DRIVER);
        driver.setStatus(Driver.DriverStatus.OFFLINE);
        
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleType(vehicleType);
        driver.setVehicle(vehicle);
        
        driver = driverRepository.save(driver);
        log.info("Driver registered: {}", driver.getUserId());
        return driver;
    }

    @Transactional
    public void updateStatus(UUID driverId, Driver.DriverStatus status) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found"));
        driver.setStatus(status);
        driverRepository.save(driver);
        log.info("Driver {} status updated to {}", driverId, status);
    }

    public BigDecimal getEarnings(UUID driverId) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found"));
        return driver.getTotalEarnings();
    }

    public List<Ride> getRideRequests(UUID driverId) {
        return rideRepository.findByDriverIdAndStatus(driverId, Ride.RideStatus.REQUESTED);
    }

    public Driver getDriverById(UUID driverId) {
        return driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found"));
    }
    
    @Transactional
    public void updateEarnings(UUID driverId, BigDecimal amount) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found"));
        
        BigDecimal currentEarnings = driver.getTotalEarnings() != null ? 
            driver.getTotalEarnings() : BigDecimal.ZERO;
        driver.setTotalEarnings(currentEarnings.add(amount));
        driver.setTotalRides(driver.getTotalRides() + 1);
        
        driverRepository.save(driver);
        log.info("Driver {} earnings updated: +${}", driverId, amount);
    }
    
    public List<Driver> getOnlineDrivers() {
        return driverRepository.findByStatus(Driver.DriverStatus.ONLINE);
    }
    
    @Transactional
    public void goOnline(UUID driverId) {
        updateStatus(driverId, Driver.DriverStatus.ONLINE);
    }
    
    @Transactional
    public void goOffline(UUID driverId) {
        updateStatus(driverId, Driver.DriverStatus.OFFLINE);
    }
}
