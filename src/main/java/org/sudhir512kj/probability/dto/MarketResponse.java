package org.sudhir512kj.probability.dto;

import lombok.Builder;
import lombok.Data;
import org.sudhir512kj.probability.model.MarketStatus;
import org.sudhir512kj.probability.model.MarketType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MarketResponse {
    private UUID marketId;
    private String question;
    private String description;
    private MarketType marketType;
    private MarketStatus status;
    private LocalDateTime endDate;
    private BigDecimal totalVolume;
    private LocalDateTime createdAt;
}
