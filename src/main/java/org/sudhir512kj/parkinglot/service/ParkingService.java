package org.sudhir512kj.parkinglot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.parkinglot.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParkingService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private final Map<Integer, List<ParkingSpot>> floorSpots = new ConcurrentHashMap<>();
    
    public ParkingSpot findAvailableSpot(VehicleType vehicleType) {
        String cacheKey = "available_spots_" + vehicleType;
        
        // Check cache first
        List<String> cachedSpots = (List<String>) redisTemplate.opsForValue().get(cacheKey);
        
        // Find spot using strategy pattern
        for (List<ParkingSpot> spots : floorSpots.values()) {
            for (ParkingSpot spot : spots) {
                if (spot.isFree() && spot.canFitVehicle(createVehicle("temp", vehicleType))) {
                    return spot;
                }
            }
        }
        return null;
    }
    
    public boolean parkVehicle(Vehicle vehicle, ParkingSpot spot) {
        if (spot.assignVehicle(vehicle)) {
            updateCache();
            return true;
        }
        return false;
    }
    
    public void unparkVehicle(ParkingSpot spot) {
        spot.removeVehicle();
        updateCache();
    }
    
    public Map<SpotType, Integer> getAvailabilityByFloor(int floor) {
        Map<SpotType, Integer> availability = new HashMap<>();
        List<ParkingSpot> spots = floorSpots.get(floor);
        
        if (spots != null) {
            for (SpotType type : SpotType.values()) {
                long count = spots.stream()
                    .filter(spot -> spot.getType() == type && spot.isFree())
                    .count();
                availability.put(type, (int) count);
            }
        }
        return availability;
    }
    
    private void updateCache() {
        // Update Redis cache with current availability
        for (VehicleType type : VehicleType.values()) {
            List<String> availableSpots = new ArrayList<>();
            for (List<ParkingSpot> spots : floorSpots.values()) {
                spots.stream()
                    .filter(spot -> spot.isFree() && spot.canFitVehicle(createVehicle("temp", type)))
                    .forEach(spot -> availableSpots.add(spot.getId()));
            }
            redisTemplate.opsForValue().set("available_spots_" + type, availableSpots);
        }
    }
    
    private Vehicle createVehicle(String plate, VehicleType type) {
        switch (type) {
            case CAR: return new Car(plate);
            case TRUCK: return new Truck(plate);
            case MOTORCYCLE: return new Motorcycle(plate);
            default: throw new IllegalArgumentException("Unknown vehicle type");
        }
    }
    
    public void addFloor(int floorNumber, List<ParkingSpot> spots) {
        floorSpots.put(floorNumber, spots);
        updateCache();
    }
}