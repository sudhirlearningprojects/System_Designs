# AWS S3 File Size Limits

## Overview
Amazon S3 (Simple Storage Service) supports storing objects with specific size constraints. Understanding these limits is crucial for designing scalable storage solutions.

---

## Maximum Object Size

**5 TB (5,120 GB)** - Maximum size for a single object in S3

---

## Upload Methods

### 1. Single PUT Operation
- **Maximum Size**: 5 GB
- **Use Case**: Files ≤ 5 GB
- **Method**: Simple single API call using `PutObject`
- **Pros**: Simple implementation, single request
- **Cons**: Limited to 5 GB, no resume capability

### 2. Multipart Upload
- **Maximum Size**: 5 TB
- **Required For**: Files > 5 GB (mandatory)
- **Recommended For**: Files > 100 MB
- **Part Size Range**: 5 MB to 5 GB per part
- **Maximum Parts**: 10,000 parts per upload
- **Pros**: 
  - Supports large files up to 5 TB
  - Resume capability on failure
  - Parallel uploads for better performance
  - Network efficiency
- **Cons**: More complex implementation

---

## Implementation Example

### Java with AWS SDK

```java
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

public class S3FileUploader {
    
    private final AmazonS3 s3Client;
    private final String bucketName;
    
    public void uploadFile(File file, String key) throws Exception {
        long fileSize = file.length();
        
        // For small files (< 100 MB), use simple PUT
        if (fileSize < 100 * 1024 * 1024) {
            s3Client.putObject(bucketName, key, file);
            return;
        }
        
        // For large files (≥ 100 MB), use multipart upload
        TransferManager tm = TransferManagerBuilder.standard()
            .withS3Client(s3Client)
            .withMinimumUploadPartSize(10L * 1024 * 1024) // 10 MB parts
            .withMultipartUploadThreshold(100L * 1024 * 1024) // 100 MB threshold
            .build();
        
        Upload upload = tm.upload(bucketName, key, file);
        upload.waitForCompletion();
        
        tm.shutdownNow();
    }
}
```

---

## Quick Reference Table

| Scenario | Size Limit | Method | API |
|----------|------------|--------|-----|
| Single PUT | 5 GB | Simple upload | `PutObject` |
| Multipart Upload | 5 TB | Chunked upload | `CreateMultipartUpload` |
| Recommended threshold | 100 MB | Switch to multipart | - |
| Minimum part size | 5 MB | Per part (except last) | - |
| Maximum part size | 5 GB | Per part | - |
| Maximum parts | 10,000 | Per object | - |

---

## Best Practices

### 1. **Choose the Right Method**
- Files < 100 MB → Single PUT
- Files ≥ 100 MB → Multipart Upload
- Files > 5 GB → Multipart Upload (mandatory)

### 2. **Optimize Part Size**
- Recommended: 10-100 MB per part
- Larger parts = fewer requests, faster for good networks
- Smaller parts = better retry granularity, better for unstable networks

### 3. **Handle Failures**
- Implement retry logic with exponential backoff
- Store multipart upload IDs for resume capability
- Clean up incomplete multipart uploads to avoid storage costs

### 4. **Performance Optimization**
- Use parallel uploads for multipart
- Enable transfer acceleration for cross-region uploads
- Use AWS Transfer Manager (handles complexity automatically)

### 5. **Cost Optimization**
- Set lifecycle policies to abort incomplete multipart uploads after 7 days
- Monitor and clean up orphaned parts

---

## Common Use Cases

### Dropbox/Cloud Storage Systems
- **Chunking Strategy**: Split files into 4-10 MB chunks
- **Parallel Upload**: Upload multiple chunks simultaneously
- **Resume Support**: Track uploaded chunks for resume capability
- **Deduplication**: Hash-based chunk deduplication before upload

### Video Streaming Platforms
- **Large Files**: Movies/videos often exceed 5 GB
- **Multipart Required**: Mandatory for HD/4K content
- **Progressive Upload**: Start processing while uploading

### Backup Systems
- **Large Backups**: Database dumps, VM images
- **Reliability**: Multipart provides better fault tolerance
- **Bandwidth**: Parallel uploads maximize throughput

---

## Error Handling

```java
public void uploadWithRetry(File file, String key, int maxRetries) {
    int attempt = 0;
    while (attempt < maxRetries) {
        try {
            uploadFile(file, key);
            return;
        } catch (AmazonS3Exception e) {
            attempt++;
            if (attempt >= maxRetries) {
                throw new RuntimeException("Upload failed after " + maxRetries + " attempts", e);
            }
            // Exponential backoff
            Thread.sleep((long) Math.pow(2, attempt) * 1000);
        }
    }
}
```

---

## Monitoring and Cleanup

### List Incomplete Multipart Uploads
```java
ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(bucketName);
MultipartUploadListing uploadListing = s3Client.listMultipartUploads(request);

for (MultipartUpload upload : uploadListing.getMultipartUploads()) {
    // Abort uploads older than 7 days
    if (upload.getInitiated().before(sevenDaysAgo)) {
        s3Client.abortMultipartUpload(
            new AbortMultipartUploadRequest(bucketName, upload.getKey(), upload.getUploadId())
        );
    }
}
```

---

## Additional Limits

| Resource | Limit |
|----------|-------|
| Bucket name length | 3-63 characters |
| Object key length | 1-1024 bytes |
| Buckets per account | 100 (soft limit, can be increased) |
| Objects per bucket | Unlimited |
| Request rate | 3,500 PUT/COPY/POST/DELETE, 5,500 GET/HEAD per prefix per second |

---

## References

- [AWS S3 Documentation](https://docs.aws.amazon.com/s3/)
- [Multipart Upload Overview](https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html)
- [AWS SDK for Java](https://aws.amazon.com/sdk-for-java/)
- [S3 Best Practices](https://docs.aws.amazon.com/AmazonS3/latest/userguide/optimizing-performance.html)

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Author**: System Designs Collection
