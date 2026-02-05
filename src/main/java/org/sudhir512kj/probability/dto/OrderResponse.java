package org.sudhir512kj.probability.dto;

import lombok.Builder;
import lombok.Data;
import org.sudhir512kj.probability.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID orderId;
    private OrderStatus status;
    private BigDecimal filledQuantity;
    private BigDecimal remainingQuantity;
    private LocalDateTime createdAt;
}
