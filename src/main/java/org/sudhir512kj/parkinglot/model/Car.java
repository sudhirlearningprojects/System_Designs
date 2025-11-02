package org.sudhir512kj.parkinglot.model;

public class Car extends Vehicle {
    public Car(String licensePlate) {
        super(licensePlate, VehicleType.CAR);
    }
    public int getSize() { return 1; }
}