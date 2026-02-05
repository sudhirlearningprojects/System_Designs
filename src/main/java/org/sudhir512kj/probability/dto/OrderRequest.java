package org.sudhir512kj.probability.dto;

import lombok.Data;
import org.sudhir512kj.probability.model.OrderSide;
import org.sudhir512kj.probability.model.OrderType;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderRequest {
    private UUID marketId;
    private UUID outcomeId;
    private OrderType orderType;
    private OrderSide side;
    private BigDecimal price;
    private BigDecimal quantity;
    private String idempotencyKey;
}
