package org.sudhir512kj.jobscheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduler_leases", indexes = {
    @Index(name = "idx_lease_expires", columnList = "lease_expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerLease {
    @Id
    @Column(name = "partition_key")
    private String partitionKey;
    
    @Column(name = "node_id", nullable = false)
    private String nodeId;
    
    @Column(name = "lease_expires_at", nullable = false)
    private LocalDateTime leaseExpiresAt;
    
    @Column(name = "heartbeat_at", nullable = false)
    private LocalDateTime heartbeatAt;
}