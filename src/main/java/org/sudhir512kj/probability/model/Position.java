package org.sudhir512kj.probability.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "positions", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "market_id", "outcome_id"}),
    indexes = {
        @Index(name = "idx_user", columnList = "user_id"),
        @Index(name = "idx_market", columnList = "market_id")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID positionId;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private UUID marketId;
    
    @Column(nullable = false)
    private UUID outcomeId;
    
    @Column(nullable = false, precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal shares = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal avgCost = BigDecimal.ZERO;
    
    @Column(precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
