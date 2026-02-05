package org.sudhir512kj.probability.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.probability.model.Position;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
}
