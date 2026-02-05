package org.sudhir512kj.probability.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "markets", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_end_date", columnList = "end_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Market {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID marketId;
    
    @Column(nullable = false, length = 500)
    private String question;
    
    @Column(length = 2000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketType marketType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketStatus status;
    
    @Column(nullable = false)
    private LocalDateTime endDate;
    
    private LocalDateTime resolutionDate;
    
    private UUID winningOutcomeId;
    
    @Column(nullable = false)
    private UUID creatorId;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal liquidityParameter;
    
    @Column(precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal totalVolume = BigDecimal.ZERO;
    
    private String category;
    
    private String resolutionSource;
    
    @OneToMany(mappedBy = "market", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Outcome> outcomes = new ArrayList<>();
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
