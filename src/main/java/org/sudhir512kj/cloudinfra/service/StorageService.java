package org.sudhir512kj.cloudinfra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.sudhir512kj.cloudinfra.dto.CreateStorageRequest;
import org.sudhir512kj.cloudinfra.model.ResourceState;
import org.sudhir512kj.cloudinfra.model.StorageBucket;
import org.sudhir512kj.cloudinfra.repository.StorageBucketRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
    private final StorageBucketRepository bucketRepository;
    private final String storageBasePath = "/tmp/cloud-storage";
    
    @Transactional
    public StorageBucket createBucket(String accountId, CreateStorageRequest request) {
        if (bucketRepository.existsByBucketName(request.getBucketName())) {
            throw new RuntimeException("Bucket name already exists");
        }
        
        StorageBucket bucket = new StorageBucket();
        bucket.setId("bucket-" + UUID.randomUUID().toString().substring(0, 8));
        bucket.setName(request.getBucketName());
        bucket.setBucketName(request.getBucketName());
        bucket.setRegion(request.getRegion());
        bucket.setAccountId(accountId);
        bucket.setState(ResourceState.RUNNING);
        bucket.setStorageClass(request.getStorageClass());
        bucket.setAccessLevel(request.getAccessLevel());
        bucket.setVersioningEnabled(request.getVersioningEnabled());
        bucket.setEncryptionEnabled(request.getEncryptionEnabled());
        bucket.setSizeBytes(0L);
        bucket.setObjectCount(0L);
        
        try {
            Path bucketPath = Paths.get(storageBasePath, bucket.getBucketName());
            Files.createDirectories(bucketPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create bucket directory", e);
        }
        
        return bucketRepository.save(bucket);
    }
    
    @Transactional
    public void uploadObject(String bucketName, String objectKey, MultipartFile file) {
        StorageBucket bucket = bucketRepository.findByBucketName(bucketName)
            .orElseThrow(() -> new RuntimeException("Bucket not found"));
        
        try {
            Path objectPath = Paths.get(storageBasePath, bucketName, objectKey);
            Files.createDirectories(objectPath.getParent());
            Files.write(objectPath, file.getBytes());
            
            bucket.setSizeBytes(bucket.getSizeBytes() + file.getSize());
            bucket.setObjectCount(bucket.getObjectCount() + 1);
            bucketRepository.save(bucket);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload object", e);
        }
    }
    
    public byte[] downloadObject(String bucketName, String objectKey) {
        bucketRepository.findByBucketName(bucketName)
            .orElseThrow(() -> new RuntimeException("Bucket not found"));
        
        try {
            Path objectPath = Paths.get(storageBasePath, bucketName, objectKey);
            return Files.readAllBytes(objectPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download object", e);
        }
    }
    
    public List<StorageBucket> listBuckets(String accountId) {
        return bucketRepository.findByAccountId(accountId);
    }
}
