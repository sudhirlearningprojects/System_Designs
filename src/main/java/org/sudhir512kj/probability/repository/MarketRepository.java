package org.sudhir512kj.probability.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.probability.model.Market;
import org.sudhir512kj.probability.model.MarketStatus;
import java.util.UUID;

public interface MarketRepository extends JpaRepository<Market, UUID> {
    Page<Market> findByStatus(MarketStatus status, Pageable pageable);
}
