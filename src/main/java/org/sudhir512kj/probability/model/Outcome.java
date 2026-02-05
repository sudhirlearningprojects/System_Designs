package org.sudhir512kj.probability.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outcomes", indexes = {
    @Index(name = "idx_market", columnList = "market_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outcome {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID outcomeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal outstandingShares = BigDecimal.ZERO;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
