-- Uber Database Schema

CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    name VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    rating DECIMAL(3,2),
    total_rides INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS drivers (
    user_id UUID PRIMARY KEY REFERENCES users(user_id),
    license_number VARCHAR(50) UNIQUE NOT NULL,
    vehicle_id UUID,
    status VARCHAR(20) DEFAULT 'OFFLINE',
    is_verified BOOLEAN DEFAULT FALSE,
    total_earnings DECIMAL(10,2) DEFAULT 0.00,
    last_location_update TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id UUID PRIMARY KEY,
    vehicle_type VARCHAR(20) NOT NULL,
    license_plate VARCHAR(20) UNIQUE NOT NULL,
    model VARCHAR(100),
    color VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS rides (
    ride_id UUID PRIMARY KEY,
    rider_id UUID NOT NULL REFERENCES users(user_id),
    driver_id UUID REFERENCES users(user_id),
    vehicle_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    estimated_fare DECIMAL(10,2),
    actual_fare DECIMAL(10,2),
    distance_km DOUBLE PRECISION,
    duration_minutes INTEGER,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP
);

CREATE INDEX idx_rides_rider ON rides(rider_id);
CREATE INDEX idx_rides_driver ON rides(driver_id);
CREATE INDEX idx_rides_status ON rides(status);
