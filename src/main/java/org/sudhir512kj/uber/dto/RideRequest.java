package org.sudhir512kj.uber.dto;

import org.sudhir512kj.uber.model.Location;
import org.sudhir512kj.uber.model.Vehicle;
import java.util.UUID;

public class RideRequest {
    private UUID riderId;
    private Location pickupLocation;
    private Location dropoffLocation;
    private Vehicle.VehicleType vehicleType;
    
    public RideRequest() {}
    
    public RideRequest(UUID riderId, Location pickupLocation, Location dropoffLocation, Vehicle.VehicleType vehicleType) {
        this.riderId = riderId;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.vehicleType = vehicleType;
    }

    public UUID getRiderId() { return riderId; }
    public void setRiderId(UUID riderId) { this.riderId = riderId; }
    
    public Location getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(Location pickupLocation) { this.pickupLocation = pickupLocation; }
    
    public Location getDropoffLocation() { return dropoffLocation; }
    public void setDropoffLocation(Location dropoffLocation) { this.dropoffLocation = dropoffLocation; }
    
    public Vehicle.VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(Vehicle.VehicleType vehicleType) { this.vehicleType = vehicleType; }
}
