package org.sudhir512kj.cloudinfra.dto;

import lombok.Data;
import org.sudhir512kj.cloudinfra.model.StorageBucket;

@Data
public class CreateStorageRequest {
    private String bucketName;
    private String region;
    private StorageBucket.StorageClass storageClass;
    private StorageBucket.AccessLevel accessLevel;
    private Boolean versioningEnabled;
    private Boolean encryptionEnabled;
}
