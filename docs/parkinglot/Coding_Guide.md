# Parking Lot - Complete Coding Guide

## System Design Overview

**Problem**: Design a parking lot system with multiple floors and spot types

**Core Requirements**:
1. Park vehicle in appropriate spot
2. Calculate parking fee based on duration
3. Track real-time availability
4. Handle multiple vehicle types

## SOLID Principles Applied

### 1. Single Responsibility Principle
- `ParkingSpot`: Manages spot state only
- `Vehicle`: Represents vehicle data only
- `PricingStrategy`: Calculates fees only

### 2. Open/Closed Principle
- Add new vehicle types by extending `Vehicle` class
- Add new pricing strategies without modifying existing code

### 3. Liskov Substitution Principle
- Any `Vehicle` subclass (Car, Motorcycle, Truck) can replace base class

### 4. Dependency Inversion Principle
- `ParkingLot` depends on `PricingStrategy` interface, not concrete implementation

## Design Patterns Used

### 1. Factory Pattern
Create vehicles dynamically based on type

### 2. Strategy Pattern
Different pricing strategies (hourly, flat, dynamic)

### 3. Singleton Pattern
Single instance of ParkingLot

## Complete Single-File Implementation

```java
import java.util.*;
import java.time.*;

enum VehicleType { MOTORCYCLE, CAR, TRUCK }
enum SpotType { COMPACT, REGULAR, LARGE }
enum SpotStatus { AVAILABLE, OCCUPIED }

// ============================================
// DOMAIN MODELS
// ============================================

abstract class Vehicle {
    String licensePlate;
    VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    public abstract boolean canFitInSpot(SpotType spotType);
}

class Motorcycle extends Vehicle {
    public Motorcycle(String plate) {
        super(plate, VehicleType.MOTORCYCLE);
    }

    @Override
    public boolean canFitInSpot(SpotType spotType) {
        return true; // Fits in any spot
    }
}

class Car extends Vehicle {
    public Car(String plate) {
        super(plate, VehicleType.CAR);
    }

    @Override
    public boolean canFitInSpot(SpotType spotType) {
        return spotType != SpotType.COMPACT;
    }
}

class Truck extends Vehicle {
    public Truck(String plate) {
        super(plate, VehicleType.TRUCK);
    }

    @Override
    public boolean canFitInSpot(SpotType spotType) {
        return spotType == SpotType.LARGE;
    }
}

class ParkingSpot {
    String id;
    SpotType type;
    SpotStatus status;
    Vehicle vehicle;

    public ParkingSpot(String id, SpotType type) {
        this.id = id;
        this.type = type;
        this.status = SpotStatus.AVAILABLE;
    }

    public synchronized boolean park(Vehicle vehicle) {
        if (status == SpotStatus.OCCUPIED || !vehicle.canFitInSpot(type)) {
            return false;
        }
        this.vehicle = vehicle;
        this.status = SpotStatus.OCCUPIED;
        return true;
    }

    public synchronized void vacate() {
        this.vehicle = null;
        this.status = SpotStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return status == SpotStatus.AVAILABLE;
    }
}

class ParkingTicket {
    String ticketId;
    String licensePlate;
    String spotId;
    LocalDateTime entryTime;
    LocalDateTime exitTime;
    double fee;

    public ParkingTicket(String licensePlate, String spotId) {
        this.ticketId = UUID.randomUUID().toString().substring(0, 8);
        this.licensePlate = licensePlate;
        this.spotId = spotId;
        this.entryTime = LocalDateTime.now();
    }
}

// ============================================
// STRATEGY PATTERN - Pricing
// ============================================

interface PricingStrategy {
    double calculateFee(Duration duration, VehicleType type);
}

class HourlyPricing implements PricingStrategy {
    private static final Map<VehicleType, Double> RATES = Map.of(
        VehicleType.MOTORCYCLE, 2.0,
        VehicleType.CAR, 5.0,
        VehicleType.TRUCK, 10.0
    );

    @Override
    public double calculateFee(Duration duration, VehicleType type) {
        long hours = Math.max(1, duration.toHours());
        return hours * RATES.get(type);
    }
}

// ============================================
// FACTORY PATTERN
// ============================================

class VehicleFactory {
    public static Vehicle createVehicle(VehicleType type, String plate) {
        return switch (type) {
            case MOTORCYCLE -> new Motorcycle(plate);
            case CAR -> new Car(plate);
            case TRUCK -> new Truck(plate);
        };
    }
}

// ============================================
// SINGLETON PATTERN - Parking Lot
// ============================================

class ParkingLot {
    private static ParkingLot instance;
    private final List<ParkingSpot> spots;
    private final Map<String, ParkingTicket> activeTickets;
    private final PricingStrategy pricingStrategy;

    private ParkingLot(int compact, int regular, int large) {
        this.spots = new ArrayList<>();
        this.activeTickets = new HashMap<>();
        this.pricingStrategy = new HourlyPricing();

        for (int i = 1; i <= compact; i++) spots.add(new ParkingSpot("C" + i, SpotType.COMPACT));
        for (int i = 1; i <= regular; i++) spots.add(new ParkingSpot("R" + i, SpotType.REGULAR));
        for (int i = 1; i <= large; i++) spots.add(new ParkingSpot("L" + i, SpotType.LARGE));
    }

    public static synchronized ParkingLot getInstance(int compact, int regular, int large) {
        if (instance == null) instance = new ParkingLot(compact, regular, large);
        return instance;
    }

    public ParkingTicket parkVehicle(Vehicle vehicle) {
        ParkingSpot spot = spots.stream()
            .filter(ParkingSpot::isAvailable)
            .filter(s -> vehicle.canFitInSpot(s.type))
            .findFirst()
            .orElse(null);

        if (spot == null) {
            System.out.println("No spot available for " + vehicle.type);
            return null;
        }

        if (spot.park(vehicle)) {
            ParkingTicket ticket = new ParkingTicket(vehicle.licensePlate, spot.id);
            activeTickets.put(ticket.ticketId, ticket);
            System.out.println("Parked " + vehicle.type + " at " + spot.id + " | Ticket: " + ticket.ticketId);
            return ticket;
        }
        return null;
    }

    public double exitVehicle(String ticketId) {
        ParkingTicket ticket = activeTickets.get(ticketId);
        if (ticket == null) throw new IllegalArgumentException("Invalid ticket");

        ticket.exitTime = LocalDateTime.now();
        Duration duration = Duration.between(ticket.entryTime, ticket.exitTime);

        ParkingSpot spot = spots.stream()
            .filter(s -> s.id.equals(ticket.spotId))
            .findFirst().orElseThrow();

        ticket.fee = pricingStrategy.calculateFee(duration, spot.vehicle.type);
        spot.vacate();
        activeTickets.remove(ticketId);

        System.out.println("Exit from " + ticket.spotId + " | Duration: " + duration.toMinutes() + "m | Fee: $" + ticket.fee);
        return ticket.fee;
    }

    public Map<SpotType, Long> getAvailability() {
        Map<SpotType, Long> availability = new HashMap<>();
        for (SpotType type : SpotType.values()) {
            long count = spots.stream().filter(s -> s.type == type && s.isAvailable()).count();
            availability.put(type, count);
        }
        return availability;
    }
}

// ============================================
// MAIN DEMO
// ============================================

public class ParkingLotDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Parking Lot System ===\n");

        ParkingLot lot = ParkingLot.getInstance(5, 10, 3);
        System.out.println("Initial: " + lot.getAvailability());

        Vehicle m1 = VehicleFactory.createVehicle(VehicleType.MOTORCYCLE, "M-123");
        Vehicle c1 = VehicleFactory.createVehicle(VehicleType.CAR, "C-456");
        Vehicle t1 = VehicleFactory.createVehicle(VehicleType.TRUCK, "T-789");

        ParkingTicket t1Ticket = lot.parkVehicle(m1);
        ParkingTicket t2Ticket = lot.parkVehicle(c1);
        ParkingTicket t3Ticket = lot.parkVehicle(t1);

        System.out.println("\nAfter parking: " + lot.getAvailability());

        Thread.sleep(2000);

        lot.exitVehicle(t1Ticket.ticketId);
        lot.exitVehicle(t2Ticket.ticketId);

        System.out.println("\nFinal: " + lot.getAvailability());
    }
}
```

## How to Run

**Online**: Copy to https://www.jdoodle.com/online-java-compiler

**Expected Output**:
```
=== Parking Lot System ===

Initial: {COMPACT=5, REGULAR=10, LARGE=3}
Parked MOTORCYCLE at C1 | Ticket: a1b2c3d4
Parked CAR at R1 | Ticket: e5f6g7h8
Parked TRUCK at L1 | Ticket: i9j0k1l2

After parking: {COMPACT=4, REGULAR=9, LARGE=2}
Exit from C1 | Duration: 0m | Fee: $2.0
Exit from R1 | Duration: 0m | Fee: $5.0

Final: {COMPACT=5, REGULAR=10, LARGE=2}
```

## Interview Questions

**Q: How to prevent double-booking?**
A: Use `synchronized` methods on park/vacate operations

**Q: How to handle payment failures?**
A: Hold spot for 5 minutes, release if payment not completed

**Q: How to scale to 10,000 spots?**
A: Database sharding by floor, Redis for availability cache

**Q: How to find nearest spot?**
A: Add coordinates, sort by distance from entrance
