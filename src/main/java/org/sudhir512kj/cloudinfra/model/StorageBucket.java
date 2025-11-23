package org.sudhir512kj.cloudinfra.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "storage_buckets")
public class StorageBucket {
    @Id
    private String id;
    private String name;
    private String bucketName;
    private String region;
    private String accountId;
    
    @Enumerated(EnumType.STRING)
    private ResourceState state;
    
    private Long sizeBytes;
    private Long objectCount;
    
    @Enumerated(EnumType.STRING)
    private StorageClass storageClass;
    
    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel;
    
    private Boolean versioningEnabled;
    private Boolean encryptionEnabled;
    
    public enum StorageClass {
        STANDARD, INFREQUENT_ACCESS, GLACIER, DEEP_ARCHIVE
    }
    
    public enum AccessLevel {
        PRIVATE, PUBLIC_READ, PUBLIC_READ_WRITE
    }
}
