package org.sudhir512kj.cloudinfra.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "hosts")
public class Host {
    @Id
    private String hostId;
    private String region;
    private String zone;
    
    private Integer totalCpu;
    private Integer availableCpu;
    private Integer totalMemoryGb;
    private Integer availableMemoryGb;
    private Integer totalDiskGb;
    private Integer availableDiskGb;
    
    @Enumerated(EnumType.STRING)
    private HostStatus status;
    
    private LocalDateTime lastHeartbeat;
    
    public enum HostStatus {
        ACTIVE, QUARANTINE, MAINTENANCE, OFFLINE
    }
}
