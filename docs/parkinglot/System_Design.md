# Parking Lot Management System Design

## Understanding Parking Lot Systems

### What is a Parking Lot Management System?
A parking lot management system automates the process of vehicle entry, parking spot allocation, fee calculation, and payment processing. The system must handle concurrent vehicle entries/exits while preventing double-booking of parking spots.

### Key Challenges in Parking Systems
1. **Concurrency Control**: Multiple vehicles entering simultaneously
2. **Real-time Availability**: Instant spot availability updates
3. **Atomic Operations**: Prevent double-booking of spots
4. **Payment Processing**: Handle various payment methods reliably
5. **Scalability**: Support multiple parking lots and thousands of spots

### Parking System Fundamentals

#### Concurrency Problems in Parking

##### The Double-Booking Problem
```
Scenario: Two cars arrive simultaneously at different gates
Gate 1: Checks availability → Finds spot A1 available → Assigns spot A1
Gate 2: Checks availability → Finds spot A1 available → Assigns spot A1
Result: Two cars assigned to same spot!
```

##### Solution: Atomic Operations
```java
public class ParkingSpot {
    private final AtomicBoolean isOccupied = new AtomicBoolean(false);
    
    public boolean tryOccupy() {
        // Atomic compare-and-swap operation
        return isOccupied.compareAndSet(false, true);
    }
    
    public void release() {
        isOccupied.set(false);
    }
}
```

#### Vehicle Type Hierarchy
```java
abstract class Vehicle {
    protected String licensePlate;
    protected VehicleType type;
    
    public abstract boolean canFitInSpot(ParkingSpot spot);
}

class Car extends Vehicle {
    public boolean canFitInSpot(ParkingSpot spot) {
        return spot.getType() == SpotType.COMPACT || 
               spot.getType() == SpotType.REGULAR ||
               spot.getType() == SpotType.LARGE;
    }
}

class Truck extends Vehicle {
    public boolean canFitInSpot(ParkingSpot spot) {
        return spot.getType() == SpotType.LARGE;
    }
}
```

#### Spot Allocation Strategies

##### First Available Strategy
```java
public ParkingSpot findFirstAvailable(VehicleType vehicleType) {
    for (ParkingFloor floor : floors) {
        for (ParkingSpot spot : floor.getSpots()) {
            if (!spot.isOccupied() && spot.canFit(vehicleType)) {
                if (spot.tryOccupy()) {
                    return spot;
                }
            }
        }
    }
    return null; // No available spots
}
```

##### Optimized Strategy (Prefer Lower Floors)
```java
public ParkingSpot findOptimalSpot(VehicleType vehicleType) {
    // Sort floors by preference (ground floor first)
    List<ParkingFloor> sortedFloors = floors.stream()
        .sorted(Comparator.comparing(ParkingFloor::getFloorNumber))
        .collect(Collectors.toList());
    
    for (ParkingFloor floor : sortedFloors) {
        ParkingSpot spot = floor.findAvailableSpot(vehicleType);
        if (spot != null) {
            return spot;
        }
    }
    return null;
}
```

### Caching Strategy for Performance

#### Why Redis for Parking Systems?
1. **Sub-millisecond Latency**: Critical for real-time availability
2. **Atomic Operations**: INCR/DECR for available spot counts
3. **Data Structures**: Sets for available spot IDs
4. **Pub/Sub**: Real-time updates to display boards

#### Cache Implementation
```java
@Service
public class ParkingCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void updateAvailability(String floorId, VehicleType type, int change) {
        String key = String.format("availability:%s:%s", floorId, type);
        redisTemplate.opsForValue().increment(key, change);
        
        // Publish update to display boards
        redisTemplate.convertAndSend("parking-updates", 
            new AvailabilityUpdate(floorId, type, getAvailableCount(floorId, type)));
    }
    
    public int getAvailableCount(String floorId, VehicleType type) {
        String key = String.format("availability:%s:%s", floorId, type);
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        return count != null ? count : 0;
    }
}
```

## Overview
A highly available, fault-tolerant parking lot management system designed for multi-story parking facilities with multiple entry/exit gates.

## High-Level Design (HLD)

### System Architecture
- **Microservices Architecture**: Modular design for high availability
- **API Gateway**: Single entry point with load balancing
- **Redis Cache**: Real-time spot availability (O(1) lookups)
- **PostgreSQL**: Transactional data (tickets, payments)
- **Circuit Breaker**: Fault tolerance for payment processing

### Core Services
1. **Parking Service**: Spot allocation and availability management
2. **Ticket Service**: Ticket generation and lifecycle management  
3. **Payment Service**: Fee calculation and payment processing

### High Availability Features
- **Atomic Operations**: Thread-safe spot assignment using AtomicBoolean
- **Cache-First Strategy**: Redis for sub-second response times
- **Graceful Degradation**: System continues operating during component failures
- **Load Balancing**: Multiple service instances across availability zones

### Payment Processing in Parking Systems

#### Fee Calculation Strategies

##### Time-based Pricing
```java
public class ParkingFeeCalculator {
    private static final BigDecimal HOURLY_RATE = new BigDecimal("5.00");
    private static final BigDecimal DAILY_MAX = new BigDecimal("25.00");
    
    public BigDecimal calculateFee(LocalDateTime entryTime, LocalDateTime exitTime) {
        Duration parkingDuration = Duration.between(entryTime, exitTime);
        long hours = parkingDuration.toHours();
        
        // Round up partial hours
        if (parkingDuration.toMinutes() % 60 > 0) {
            hours++;
        }
        
        BigDecimal totalFee = HOURLY_RATE.multiply(BigDecimal.valueOf(hours));
        
        // Apply daily maximum
        return totalFee.min(DAILY_MAX);
    }
}
```

##### Dynamic Pricing (Peak Hours)
```java
public class DynamicPricingCalculator {
    public BigDecimal calculateFee(LocalDateTime entryTime, LocalDateTime exitTime, 
                                 double occupancyRate) {
        BigDecimal baseFee = calculateBaseFee(entryTime, exitTime);
        
        // Surge pricing during peak hours or high occupancy
        if (isPeakHour(entryTime) || occupancyRate > 0.8) {
            return baseFee.multiply(new BigDecimal("1.5")); // 50% surge
        }
        
        return baseFee;
    }
    
    private boolean isPeakHour(LocalDateTime time) {
        int hour = time.getHour();
        return (hour >= 8 && hour <= 10) || (hour >= 17 && hour <= 19);
    }
}
```

#### Circuit Breaker for Payment Processing
```java
@Component
public class PaymentCircuitBreaker {
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private volatile CircuitState state = CircuitState.CLOSED;
    
    private static final int FAILURE_THRESHOLD = 5;
    private static final long TIMEOUT_DURATION = 60000; // 1 minute
    
    public PaymentResult processPayment(PaymentRequest request) {
        if (state == CircuitState.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime.get() > TIMEOUT_DURATION) {
                state = CircuitState.HALF_OPEN;
            } else {
                return PaymentResult.failure("Payment service unavailable");
            }
        }
        
        try {
            PaymentResult result = paymentService.processPayment(request);
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }
    
    private void onSuccess() {
        failureCount.set(0);
        state = CircuitState.CLOSED;
    }
    
    private void onFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        if (failures >= FAILURE_THRESHOLD) {
            state = CircuitState.OPEN;
        }
    }
}
```

## Low-Level Design (LLD)

### Thread Safety in Parking Operations

#### Synchronized Spot Assignment
```java
@Service
public class ParkingService {
    private final ConcurrentHashMap<String, ParkingFloor> floors = new ConcurrentHashMap<>();
    
    public synchronized ParkingTicket assignSpot(Vehicle vehicle) {
        // Find available spot
        ParkingSpot spot = findAvailableSpot(vehicle.getType());
        
        if (spot == null) {
            throw new NoAvailableSpotException("Parking lot is full");
        }
        
        // Atomic spot occupation
        if (!spot.tryOccupy()) {
            // Spot was taken by another thread, try again
            return assignSpot(vehicle);
        }
        
        // Create ticket
        ParkingTicket ticket = new ParkingTicket();
        ticket.setVehicle(vehicle);
        ticket.setSpot(spot);
        ticket.setEntryTime(LocalDateTime.now());
        
        // Update cache
        updateAvailabilityCache(spot.getFloor(), vehicle.getType(), -1);
        
        return ticketRepository.save(ticket);
    }
}
```

#### Database Transaction Management
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public PaymentResult processExit(String ticketId, PaymentMethod paymentMethod) {
    // Find ticket
    ParkingTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new TicketNotFoundException("Invalid ticket"));
    
    if (ticket.getStatus() != TicketStatus.ACTIVE) {
        throw new InvalidTicketException("Ticket already processed");
    }
    
    // Calculate fee
    BigDecimal fee = feeCalculator.calculateFee(
        ticket.getEntryTime(), LocalDateTime.now());
    
    // Process payment
    PaymentResult paymentResult = paymentService.processPayment(
        new PaymentRequest(fee, paymentMethod));
    
    if (paymentResult.isSuccess()) {
        // Update ticket status
        ticket.setStatus(TicketStatus.PAID);
        ticket.setExitTime(LocalDateTime.now());
        ticket.setFee(fee);
        
        // Release spot
        ParkingSpot spot = ticket.getSpot();
        spot.release();
        
        // Update cache
        updateAvailabilityCache(spot.getFloor(), 
            ticket.getVehicle().getType(), 1);
        
        ticketRepository.save(ticket);
    }
    
    return paymentResult;
}
```

### Key Classes
- **Vehicle Hierarchy**: Abstract Vehicle → Car, Truck, Motorcycle
- **ParkingSpot Hierarchy**: Abstract ParkingSpot → CompactSpot, RegularSpot, LargeSpot
- **Ticket Entity**: JPA entity for persistence
- **Strategy Pattern**: PaymentMethod interface for different payment types

### Concurrency Control
- **AtomicBoolean**: Prevents double-booking of spots
- **ConcurrentHashMap**: Thread-safe floor management
- **Database Transactions**: ACID compliance for payments

### Performance Optimizations
- **Redis Caching**: Available spots cached by vehicle type
- **Batch Updates**: Cache updates minimize database calls
- **Index Strategy**: Database indexes on frequently queried fields

## Scalability
- **Horizontal Scaling**: Stateless services with load balancers
- **Database Sharding**: Partition by parking lot ID for large deployments
- **Cache Partitioning**: Redis cluster for high-throughput scenarios

## Reliability Features
- **Circuit Breaker**: Payment service fault tolerance
- **Retry Logic**: Exponential backoff for transient failures
- **Health Checks**: Proactive monitoring and alerting
- **Audit Logging**: Complete transaction history

## API Endpoints
- `POST /api/parking/entry` - Vehicle entry with spot assignment
- `POST /api/parking/exit` - Vehicle exit with payment processing
- `GET /api/parking/availability/{floor}` - Real-time availability display

## Technology Stack
- **Backend**: Spring Boot 3.2, Java 17
- **Database**: PostgreSQL (ACID transactions)
- **Cache**: Redis (real-time data)
- **Messaging**: Kafka (async processing)
- **Monitoring**: Micrometer, Prometheus

## Deployment
- **Containerization**: Docker containers
- **Orchestration**: Kubernetes with auto-scaling
- **Load Balancing**: NGINX/AWS ALB
- **Database**: PostgreSQL with read replicas

This design ensures 99.99% uptime with sub-second response times for critical operations while maintaining data consistency and fault tolerance.