# Uber Clone - Complete Coding Guide

## System Design Overview

**Problem**: Match riders with nearby drivers in real-time

**Core Features**:
1. Find nearby drivers (geo-location)
2. Match rider with best driver
3. Track ride status
4. Calculate fare with surge pricing

## SOLID Principles

- **SRP**: Separate classes for matching, pricing, location
- **OCP**: Add new matching algorithms without modifying existing
- **Strategy**: Different pricing strategies (base, surge, dynamic)

## Design Patterns

1. **Strategy Pattern**: Pricing strategies
2. **Observer Pattern**: Ride status updates
3. **Factory Pattern**: Create rides

## Complete Implementation

```java
import java.util.*;

enum RideStatus { REQUESTED, MATCHED, STARTED, COMPLETED, CANCELLED }
enum VehicleType { UBER_X, UBER_XL, UBER_BLACK }

class Location {
    double lat, lon;
    
    Location(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }
    
    double distanceTo(Location other) {
        double latDiff = this.lat - other.lat;
        double lonDiff = this.lon - other.lon;
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111; // km
    }
}

class Driver {
    String id, name;
    Location location;
    VehicleType vehicleType;
    boolean available;
    double rating;
    
    Driver(String id, String name, Location loc, VehicleType type, double rating) {
        this.id = id;
        this.name = name;
        this.location = loc;
        this.vehicleType = type;
        this.available = true;
        this.rating = rating;
    }
}

class Rider {
    String id, name;
    Location location;
    
    Rider(String id, String name, Location loc) {
        this.id = id;
        this.name = name;
        this.location = loc;
    }
}

class Ride {
    String id;
    Rider rider;
    Driver driver;
    Location pickup, dropoff;
    RideStatus status;
    double fare;
    
    Ride(Rider rider, Location pickup, Location dropoff) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.rider = rider;
        this.pickup = pickup;
        this.dropoff = dropoff;
        this.status = RideStatus.REQUESTED;
    }
}

interface PricingStrategy {
    double calculateFare(double distance, VehicleType type);
}

class BasePricing implements PricingStrategy {
    private static final Map<VehicleType, Double> BASE_RATES = Map.of(
        VehicleType.UBER_X, 1.5,
        VehicleType.UBER_XL, 2.5,
        VehicleType.UBER_BLACK, 4.0
    );
    
    public double calculateFare(double distance, VehicleType type) {
        return 5.0 + distance * BASE_RATES.get(type);
    }
}

class SurgePricing implements PricingStrategy {
    private double surgeMultiplier;
    private PricingStrategy baseStrategy;
    
    SurgePricing(double multiplier) {
        this.surgeMultiplier = multiplier;
        this.baseStrategy = new BasePricing();
    }
    
    public double calculateFare(double distance, VehicleType type) {
        return baseStrategy.calculateFare(distance, type) * surgeMultiplier;
    }
}

class DriverMatcher {
    public Driver findBestDriver(List<Driver> drivers, Location pickup, VehicleType type) {
        return drivers.stream()
            .filter(d -> d.available && d.vehicleType == type)
            .min(Comparator.comparingDouble(d -> {
                double distance = d.location.distanceTo(pickup);
                return distance - d.rating; // Prefer closer + higher rated
            }))
            .orElse(null);
    }
}

class UberService {
    private List<Driver> drivers = new ArrayList<>();
    private List<Ride> rides = new ArrayList<>();
    private DriverMatcher matcher = new DriverMatcher();
    private PricingStrategy pricingStrategy;
    
    UberService(PricingStrategy pricing) {
        this.pricingStrategy = pricing;
    }
    
    public void registerDriver(Driver driver) {
        drivers.add(driver);
        System.out.println("Registered driver: " + driver.name);
    }
    
    public Ride requestRide(Rider rider, Location pickup, Location dropoff, VehicleType type) {
        System.out.println("\n=== Ride Request ===");
        System.out.println("Rider: " + rider.name);
        System.out.println("Pickup: (" + pickup.lat + ", " + pickup.lon + ")");
        
        Driver driver = matcher.findBestDriver(drivers, pickup, type);
        if (driver == null) {
            System.out.println("No drivers available");
            return null;
        }
        
        Ride ride = new Ride(rider, pickup, dropoff);
        ride.driver = driver;
        ride.status = RideStatus.MATCHED;
        driver.available = false;
        
        double distance = pickup.distanceTo(dropoff);
        ride.fare = pricingStrategy.calculateFare(distance, type);
        
        rides.add(ride);
        
        System.out.println("Matched with: " + driver.name);
        System.out.println("Distance to pickup: " + String.format("%.2f", driver.location.distanceTo(pickup)) + " km");
        System.out.println("Trip distance: " + String.format("%.2f", distance) + " km");
        System.out.println("Fare: $" + String.format("%.2f", ride.fare));
        
        return ride;
    }
    
    public void completeRide(String rideId) {
        Ride ride = rides.stream().filter(r -> r.id.equals(rideId)).findFirst().orElse(null);
        if (ride != null) {
            ride.status = RideStatus.COMPLETED;
            ride.driver.available = true;
            System.out.println("\nRide completed! Fare: $" + String.format("%.2f", ride.fare));
        }
    }
}

public class UberDemo {
    public static void main(String[] args) {
        System.out.println("=== Uber Ride-Hailing System ===");
        
        // Normal pricing
        UberService uber = new UberService(new BasePricing());
        
        // Register drivers
        uber.registerDriver(new Driver("D1", "John", new Location(37.7749, -122.4194), VehicleType.UBER_X, 4.8));
        uber.registerDriver(new Driver("D2", "Sarah", new Location(37.7849, -122.4094), VehicleType.UBER_XL, 4.9));
        uber.registerDriver(new Driver("D3", "Mike", new Location(37.7649, -122.4294), VehicleType.UBER_BLACK, 4.7));
        
        // Request rides
        Rider rider1 = new Rider("R1", "Alice", new Location(37.7750, -122.4195));
        Ride ride1 = uber.requestRide(rider1, 
            new Location(37.7750, -122.4195), 
            new Location(37.8044, -122.2712), 
            VehicleType.UBER_X);
        
        uber.completeRide(ride1.id);
        
        // Surge pricing
        System.out.println("\n=== Surge Pricing (2x) ===");
        UberService uberSurge = new UberService(new SurgePricing(2.0));
        uberSurge.registerDriver(new Driver("D4", "Tom", new Location(37.7749, -122.4194), VehicleType.UBER_X, 4.8));
        
        Rider rider2 = new Rider("R2", "Bob", new Location(37.7750, -122.4195));
        uberSurge.requestRide(rider2, 
            new Location(37.7750, -122.4195), 
            new Location(37.8044, -122.2712), 
            VehicleType.UBER_X);
    }
}
```

## Key Concepts

**Geo-Location**:
- Use Geohash or QuadTree for spatial indexing
- Redis GEOADD/GEORADIUS for nearby drivers

**Matching Algorithm**:
- Distance + Rating + ETA
- Prevent starvation (round-robin)

**Surge Pricing**:
- demand/supply ratio
- Real-time calculation

## Interview Questions

**Q: Find drivers within 5km?**
A: Geohash (prefix matching) or Redis GEORADIUS

**Q: Handle millions of drivers?**
A: Geo-sharding, partition by city/region

**Q: Prevent driver starvation?**
A: Weighted matching, time-based priority

**Q: Real-time location updates?**
A: WebSocket, update every 5 seconds

Run: https://www.jdoodle.com/online-java-compiler
