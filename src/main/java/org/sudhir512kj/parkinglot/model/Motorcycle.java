package org.sudhir512kj.parkinglot.model;

public class Motorcycle extends Vehicle {
    public Motorcycle(String licensePlate) {
        super(licensePlate, VehicleType.MOTORCYCLE);
    }
    public int getSize() { return 1; }
}