package org.sudhir512kj.cloudinfra.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "resource_metrics", indexes = {
    @Index(name = "idx_resource_time", columnList = "resourceId,timestamp")
})
public class ResourceMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String resourceId;
    private LocalDateTime timestamp;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Double networkIn;
    private Double networkOut;
}
