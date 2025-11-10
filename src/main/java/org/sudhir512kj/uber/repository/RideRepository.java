package org.sudhir512kj.uber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.uber.model.Ride;
import java.util.List;
import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    List<Ride> findByRiderId(UUID riderId);
    List<Ride> findByDriverId(UUID driverId);
    List<Ride> findByDriverIdAndStatus(UUID driverId, Ride.RideStatus status);
}
