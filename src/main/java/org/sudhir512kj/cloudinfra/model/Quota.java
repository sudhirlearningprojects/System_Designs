package org.sudhir512kj.cloudinfra.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "quotas")
public class Quota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String projectId;
    
    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;
    
    private Integer maxCount;
    private Integer currentCount;
    
    public enum ResourceType {
        VM, STORAGE, DATABASE, VPC, LOAD_BALANCER
    }
}
