package org.sudhir512kj.parkinglot.model;

public class Truck extends Vehicle {
    public Truck(String licensePlate) {
        super(licensePlate, VehicleType.TRUCK);
    }
    public int getSize() { return 2; }
}