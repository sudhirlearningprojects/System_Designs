package org.sudhir512kj.parkinglot.model;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ParkingSpot {
    protected String id;
    protected int floor;
    protected SpotType type;
    protected AtomicBoolean isFree = new AtomicBoolean(true);
    protected Vehicle parkedVehicle;
    
    public ParkingSpot(String id, int floor, SpotType type) {
        this.id = id;
        this.floor = floor;
        this.type = type;
    }
    
    public boolean assignVehicle(Vehicle vehicle) {
        if (canFitVehicle(vehicle) && isFree.compareAndSet(true, false)) {
            this.parkedVehicle = vehicle;
            return true;
        }
        return false;
    }
    
    public void removeVehicle() {
        this.parkedVehicle = null;
        this.isFree.set(true);
    }
    
    public abstract boolean canFitVehicle(Vehicle vehicle);
    
    public String getId() { return id; }
    public int getFloor() { return floor; }
    public SpotType getType() { return type; }
    public boolean isFree() { return isFree.get(); }
}

class CompactSpot extends ParkingSpot {
    public CompactSpot(String id, int floor) {
        super(id, floor, SpotType.COMPACT);
    }
    
    public boolean canFitVehicle(Vehicle vehicle) {
        return vehicle.getType() == VehicleType.MOTORCYCLE;
    }
}

class RegularSpot extends ParkingSpot {
    public RegularSpot(String id, int floor) {
        super(id, floor, SpotType.REGULAR);
    }
    
    public boolean canFitVehicle(Vehicle vehicle) {
        return vehicle.getType() != VehicleType.TRUCK;
    }
}

class LargeSpot extends ParkingSpot {
    public LargeSpot(String id, int floor) {
        super(id, floor, SpotType.LARGE);
    }
    
    public boolean canFitVehicle(Vehicle vehicle) {
        return true;
    }
}