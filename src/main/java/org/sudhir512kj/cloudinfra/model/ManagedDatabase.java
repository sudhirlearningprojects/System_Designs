package org.sudhir512kj.cloudinfra.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "managed_databases")
public class ManagedDatabase {
    @Id
    private String id;
    private String name;
    private String region;
    private String accountId;
    
    @Enumerated(EnumType.STRING)
    private DBEngine engine;
    
    private String engineVersion;
    private String instanceClass;
    private Integer storageGb;
    private String endpoint;
    private Integer port;
    private String masterUsername;
    private Boolean multiAz;
    private Boolean backupEnabled;
    private Integer backupRetentionDays;
    
    public enum DBEngine {
        POSTGRES, MYSQL, MONGODB, REDIS, CASSANDRA
    }
}
