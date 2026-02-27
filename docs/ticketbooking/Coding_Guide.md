# Ticket Booking - Complete Coding Guide

## System Design Overview

**Problem**: Book tickets without overselling (like BookMyShow)

**Core Challenge**: Handle concurrent bookings, prevent double-booking

**Requirements**:
1. Search events
2. Hold tickets (10 min timeout)
3. Confirm booking with payment
4. Zero overselling

## SOLID Principles

- **SRP**: Separate inventory, booking, payment
- **OCP**: Add new ticket types without modifying
- **DIP**: Depend on PaymentGateway interface

## Design Patterns

1. **State Pattern**: Booking states (PENDING → CONFIRMED → CANCELLED)
2. **Strategy Pattern**: Payment strategies
3. **Repository Pattern**: Data access

## Complete Implementation

```java
import java.util.*;
import java.time.*;

enum BookingStatus { PENDING, CONFIRMED, CANCELLED, EXPIRED }
enum TicketType { REGULAR, VIP, PREMIUM }

class Event {
    String id, name;
    LocalDateTime dateTime;
    Map<TicketType, Integer> inventory;
    
    Event(String id, String name, LocalDateTime dt, Map<TicketType, Integer> inv) {
        this.id = id;
        this.name = name;
        this.dateTime = dt;
        this.inventory = new HashMap<>(inv);
    }
    
    synchronized boolean reserveTickets(TicketType type, int count) {
        int available = inventory.getOrDefault(type, 0);
        if (available >= count) {
            inventory.put(type, available - count);
            return true;
        }
        return false;
    }
    
    synchronized void releaseTickets(TicketType type, int count) {
        inventory.put(type, inventory.getOrDefault(type, 0) + count);
    }
}

class Booking {
    String id;
    String userId;
    Event event;
    TicketType ticketType;
    int quantity;
    double amount;
    BookingStatus status;
    LocalDateTime createdAt;
    LocalDateTime expiresAt;
    
    Booking(String userId, Event event, TicketType type, int qty, double amount) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.userId = userId;
        this.event = event;
        this.ticketType = type;
        this.quantity = qty;
        this.amount = amount;
        this.status = BookingStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusMinutes(10);
    }
    
    boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}

interface PaymentGateway {
    boolean processPayment(String userId, double amount);
}

class MockPaymentGateway implements PaymentGateway {
    public boolean processPayment(String userId, double amount) {
        System.out.println("Processing payment: $" + amount);
        return true; // Simulate success
    }
}

class TicketBookingService {
    private Map<String, Event> events = new HashMap<>();
    private Map<String, Booking> bookings = new HashMap<>();
    private PaymentGateway paymentGateway;
    private static final Map<TicketType, Double> PRICES = Map.of(
        TicketType.REGULAR, 50.0,
        TicketType.VIP, 100.0,
        TicketType.PREMIUM, 150.0
    );
    
    TicketBookingService(PaymentGateway gateway) {
        this.paymentGateway = gateway;
    }
    
    public void addEvent(Event event) {
        events.put(event.id, event);
        System.out.println("Added event: " + event.name);
    }
    
    public Booking createBooking(String userId, String eventId, TicketType type, int qty) {
        Event event = events.get(eventId);
        if (event == null) {
            System.out.println("Event not found");
            return null;
        }
        
        // Reserve tickets
        if (!event.reserveTickets(type, qty)) {
            System.out.println("Not enough tickets available");
            return null;
        }
        
        double amount = PRICES.get(type) * qty;
        Booking booking = new Booking(userId, event, type, qty, amount);
        bookings.put(booking.id, booking);
        
        System.out.println("\n=== Booking Created ===");
        System.out.println("Booking ID: " + booking.id);
        System.out.println("Event: " + event.name);
        System.out.println("Tickets: " + qty + " x " + type);
        System.out.println("Amount: $" + amount);
        System.out.println("Expires at: " + booking.expiresAt);
        
        return booking;
    }
    
    public boolean confirmBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            System.out.println("Booking not found");
            return false;
        }
        
        if (booking.isExpired()) {
            cancelBooking(bookingId);
            System.out.println("Booking expired");
            return false;
        }
        
        // Process payment
        if (paymentGateway.processPayment(booking.userId, booking.amount)) {
            booking.status = BookingStatus.CONFIRMED;
            System.out.println("\n=== Booking Confirmed ===");
            System.out.println("Booking ID: " + booking.id);
            System.out.println("Status: CONFIRMED");
            return true;
        }
        
        cancelBooking(bookingId);
        return false;
    }
    
    public void cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking != null && booking.status == BookingStatus.PENDING) {
            booking.status = BookingStatus.CANCELLED;
            booking.event.releaseTickets(booking.ticketType, booking.quantity);
            System.out.println("Booking cancelled, tickets released");
        }
    }
    
    public void showInventory(String eventId) {
        Event event = events.get(eventId);
        if (event != null) {
            System.out.println("\n=== Inventory for " + event.name + " ===");
            event.inventory.forEach((type, count) -> 
                System.out.println(type + ": " + count + " available"));
        }
    }
}

public class TicketBookingDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Ticket Booking System ===\n");
        
        TicketBookingService service = new TicketBookingService(new MockPaymentGateway());
        
        // Create event
        Event concert = new Event("E1", "Rock Concert", 
            LocalDateTime.now().plusDays(7),
            Map.of(
                TicketType.REGULAR, 100,
                TicketType.VIP, 50,
                TicketType.PREMIUM, 20
            ));
        service.addEvent(concert);
        service.showInventory("E1");
        
        // User 1 books tickets
        Booking b1 = service.createBooking("user1", "E1", TicketType.VIP, 2);
        service.showInventory("E1");
        
        // User 2 books tickets
        Booking b2 = service.createBooking("user2", "E1", TicketType.VIP, 3);
        service.showInventory("E1");
        
        // Confirm booking 1
        service.confirmBooking(b1.id);
        
        // Cancel booking 2
        service.cancelBooking(b2.id);
        service.showInventory("E1");
        
        // Try to overbook
        System.out.println("\n=== Attempting to book 50 VIP tickets ===");
        service.createBooking("user3", "E1", TicketType.VIP, 50);
    }
}
```

## Key Concepts

**Concurrency Control**:
- Synchronized methods on inventory
- Pessimistic locking in DB
- Redis distributed locks

**Timeout Handling**:
- 10-minute hold
- Background job to expire bookings
- Release tickets automatically

**Zero Overselling**:
- Atomic reserve operation
- Check-then-act in transaction
- Redis DECR for inventory

## Interview Questions

**Q: Prevent overselling?**
A: Synchronized methods, DB row locks, Redis atomic operations

**Q: Handle 100K concurrent requests?**
A: Redis for inventory, queue for bookings, horizontal scaling

**Q: Timeout implementation?**
A: Scheduled job checks expiresAt, releases tickets

**Q: Flash sales (1M requests/sec)?**
A: Queue system, rate limiting, CDN for static content

Run: https://www.jdoodle.com/online-java-compiler
