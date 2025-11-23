package org.sudhir512kj.cloudinfra.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "projects")
public class Project {
    @Id
    private String projectId;
    private String name;
    private String ownerUserId;
    private LocalDateTime createdAt;
    
    @Column(columnDefinition = "TEXT")
    private String tags;
}
