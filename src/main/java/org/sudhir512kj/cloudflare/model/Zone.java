package org.sudhir512kj.cloudflare.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "zones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Zone {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String domain;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "plan_type", nullable = false)
    private String planType;
    
    @Column(nullable = false)
    @Builder.Default
    private String status = "active";
    
    @ElementCollection
    @CollectionTable(name = "zone_nameservers", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "nameserver")
    private List<String> nameservers;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DNSRecord> dnsRecords;
    
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SecurityRule> securityRules;
}