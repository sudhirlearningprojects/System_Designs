package org.sudhir512kj.cloudinfra.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.sudhir512kj.cloudinfra.dto.CreateStorageRequest;
import org.sudhir512kj.cloudinfra.model.StorageBucket;
import org.sudhir512kj.cloudinfra.service.StorageService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {
    private final StorageService storageService;
    
    @PostMapping("/buckets")
    public ResponseEntity<StorageBucket> createBucket(
            @RequestHeader("X-Account-Id") String accountId,
            @RequestBody CreateStorageRequest request) {
        return ResponseEntity.ok(storageService.createBucket(accountId, request));
    }
    
    @GetMapping("/buckets")
    public ResponseEntity<List<StorageBucket>> listBuckets(
            @RequestHeader("X-Account-Id") String accountId) {
        return ResponseEntity.ok(storageService.listBuckets(accountId));
    }
    
    @PostMapping("/buckets/{bucketName}/objects/{objectKey}")
    public ResponseEntity<Void> uploadObject(
            @PathVariable String bucketName,
            @PathVariable String objectKey,
            @RequestParam("file") MultipartFile file) {
        storageService.uploadObject(bucketName, objectKey, file);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/buckets/{bucketName}/objects/{objectKey}")
    public ResponseEntity<byte[]> downloadObject(
            @PathVariable String bucketName,
            @PathVariable String objectKey) {
        byte[] data = storageService.downloadObject(bucketName, objectKey);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectKey + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(data);
    }
}
