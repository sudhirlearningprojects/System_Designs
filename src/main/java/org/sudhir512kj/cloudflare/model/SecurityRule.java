package org.sudhir512kj.cloudflare.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;
    
    @Column(name = "rule_type", nullable = false)
    private String ruleType; // 'waf', 'rate_limit', 'firewall'
    
    @Column(nullable = false)
    private String pattern;
    
    @Column(nullable = false)
    private String action; // 'block', 'challenge', 'allow'
    
    @Builder.Default
    private Integer priority = 0;
    
    @Builder.Default
    private Boolean enabled = true;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}