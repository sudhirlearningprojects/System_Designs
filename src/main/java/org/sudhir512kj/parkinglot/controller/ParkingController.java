package org.sudhir512kj.parkinglot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.parkinglot.dto.EntryRequest;
import org.sudhir512kj.parkinglot.dto.ExitRequest;
import org.sudhir512kj.parkinglot.model.*;
import org.sudhir512kj.parkinglot.service.*;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {
    
    @Autowired
    private ParkingService parkingService;
    
    @Autowired
    private TicketService ticketService;
    
    @Autowired
    private PaymentService paymentService;
    
    @PostMapping("/entry")
    public ResponseEntity<?> vehicleEntry(@RequestBody EntryRequest request) {
        try {
            ParkingSpot spot = parkingService.findAvailableSpot(request.getVehicleType());
            if (spot == null) {
                return ResponseEntity.badRequest().body("No available spots");
            }
            
            Vehicle vehicle = createVehicle(request.getLicensePlate(), request.getVehicleType());
            if (parkingService.parkVehicle(vehicle, spot)) {
                Ticket ticket = ticketService.generateTicket(request.getLicensePlate(), spot.getId());
                return ResponseEntity.ok(ticket);
            }
            
            return ResponseEntity.badRequest().body("Failed to park vehicle");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("System error");
        }
    }
    
    @PostMapping("/exit")
    public ResponseEntity<?> vehicleExit(@RequestBody ExitRequest request) {
        try {
            Ticket ticket = ticketService.getTicket(request.getTicketId());
            if (ticket == null) {
                return ResponseEntity.badRequest().body("Invalid ticket");
            }
            
            double fee = paymentService.calculateFee(ticket);
            boolean paymentSuccess = paymentService.processPayment(
                request.getTicketId(), fee, "CREDIT_CARD");
            
            if (paymentSuccess) {
                // Find and free the spot
                // In real implementation, we'd have a spot lookup service
                return ResponseEntity.ok("Payment successful. Fee: $" + fee);
            }
            
            return ResponseEntity.badRequest().body("Payment failed");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("System error");
        }
    }
    
    @GetMapping("/availability/{floor}")
    public ResponseEntity<?> getAvailability(@PathVariable int floor) {
        return ResponseEntity.ok(parkingService.getAvailabilityByFloor(floor));
    }
    
    private Vehicle createVehicle(String plate, VehicleType type) {
        switch (type) {
            case CAR: return new Car(plate);
            case TRUCK: return new Truck(plate);
            case MOTORCYCLE: return new Motorcycle(plate);
            default: throw new IllegalArgumentException("Unknown vehicle type");
        }
    }
}