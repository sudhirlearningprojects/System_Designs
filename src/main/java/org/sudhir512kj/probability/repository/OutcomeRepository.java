package org.sudhir512kj.probability.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.probability.model.Outcome;
import java.util.UUID;

public interface OutcomeRepository extends JpaRepository<Outcome, UUID> {
}
