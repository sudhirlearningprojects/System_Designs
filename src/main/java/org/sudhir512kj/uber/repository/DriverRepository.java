package org.sudhir512kj.uber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.uber.model.Driver;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {
}
