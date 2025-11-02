package org.sudhir512kj.parkinglot.dto;

import org.sudhir512kj.parkinglot.model.VehicleType;

public class EntryRequest {
    private String licensePlate;
    private VehicleType vehicleType;
    
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    
    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }
}