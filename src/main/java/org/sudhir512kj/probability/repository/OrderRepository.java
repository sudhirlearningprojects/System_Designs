package org.sudhir512kj.probability.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.probability.model.Order;
import org.sudhir512kj.probability.model.OrderSide;
import org.sudhir512kj.probability.model.OrderStatus;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByMarketIdAndOutcomeIdAndSideAndStatus(
        UUID marketId, UUID outcomeId, OrderSide side, OrderStatus status
    );
}
