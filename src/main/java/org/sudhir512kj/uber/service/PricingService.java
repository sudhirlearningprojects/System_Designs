package org.sudhir512kj.uber.service;

import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.model.Location;
import org.sudhir512kj.uber.model.Vehicle;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PricingService {
    private static final BigDecimal BASE_FARE = new BigDecimal("2.50");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("1.20");
    private static final BigDecimal PER_MINUTE_RATE = new BigDecimal("0.30");
    private static final BigDecimal MINIMUM_FARE = new BigDecimal("5.00");
    
    private final SurgePricingService surgePricingService;
    
    public PricingService(SurgePricingService surgePricingService) {
        this.surgePricingService = surgePricingService;
    }

    public BigDecimal calculateFare(Location pickup, Location dropoff, Vehicle.VehicleType vehicleType) {
        double distanceKm = pickup.distanceTo(dropoff);
        int estimatedMinutes = (int) ((distanceKm / 30) * 60);

        BigDecimal distanceCost = PER_KM_RATE.multiply(BigDecimal.valueOf(distanceKm));
        BigDecimal timeCost = PER_MINUTE_RATE.multiply(BigDecimal.valueOf(estimatedMinutes));
        BigDecimal vehicleMultiplier = getVehicleMultiplier(vehicleType);
        BigDecimal surgeMultiplier = surgePricingService.calculateSurgeMultiplier(pickup);

        BigDecimal totalFare = BASE_FARE.add(distanceCost).add(timeCost)
            .multiply(vehicleMultiplier).multiply(surgeMultiplier);
        return totalFare.max(MINIMUM_FARE).setScale(2, RoundingMode.HALF_UP);
    }

    public double calculateDistance(Location pickup, Location dropoff) {
        return pickup.distanceTo(dropoff);
    }

    private BigDecimal getVehicleMultiplier(Vehicle.VehicleType vehicleType) {
        return switch (vehicleType) {
            case UBERX -> BigDecimal.ONE;
            case XL -> new BigDecimal("1.5");
            case BLACK -> new BigDecimal("2.0");
            case POOL -> new BigDecimal("0.7");
        };
    }
}
