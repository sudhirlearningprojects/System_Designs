package org.sudhir512kj.probability.dto;

import lombok.Data;
import org.sudhir512kj.probability.model.MarketType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateMarketRequest {
    private String question;
    private String description;
    private MarketType marketType;
    private List<String> outcomes;
    private LocalDateTime endDate;
    private BigDecimal liquidityParameter;
    private String resolutionSource;
    private String category;
}
