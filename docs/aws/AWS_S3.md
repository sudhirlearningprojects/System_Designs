# AWS S3 (Simple Storage Service) - Complete Guide

## Table of Contents
1. [Introduction to S3](#introduction)
2. [S3 Storage Classes](#storage-classes)
3. [Core Concepts](#core-concepts)
4. [Java/Spring Boot Integration](#java-integration)
5. [IAM Policies & Roles](#iam-policies)
6. [Practical Examples](#practical-examples)
7. [Best Practices](#best-practices)

---

## 1. Introduction to S3 {#introduction}

Amazon S3 is an object storage service offering industry-leading scalability, data availability, security, and performance.

### Key Features
- **Durability**: 99.999999999% (11 9's) durability
- **Availability**: 99.99% availability SLA
- **Scalability**: Unlimited storage capacity
- **Performance**: 3,500 PUT/COPY/POST/DELETE and 5,500 GET/HEAD requests per second per prefix
- **Security**: Encryption at rest and in transit, IAM policies, bucket policies, ACLs

### Use Cases
- Static website hosting
- Data lakes and big data analytics
- Backup and disaster recovery
- Application data storage
- Media hosting (images, videos)
- Software delivery

---

## 2. S3 Storage Classes {#storage-classes}

### S3 Standard
- **Use Case**: Frequently accessed data
- **Durability**: 11 9's
- **Availability**: 99.99%
- **Min Storage Duration**: None
- **Retrieval Fee**: None
- **Cost**: $0.023/GB (first 50 TB)

### S3 Intelligent-Tiering
- **Use Case**: Unknown or changing access patterns
- **Features**: Automatically moves objects between tiers
- **Tiers**:
  - Frequent Access (automatic)
  - Infrequent Access (30 days)
  - Archive Instant Access (90 days)
  - Archive Access (90-270 days, optional)
  - Deep Archive Access (180-730 days, optional)
- **Cost**: $0.023/GB + $0.0025 per 1,000 objects monitoring fee

### S3 Standard-IA (Infrequent Access)
- **Use Case**: Long-lived, infrequently accessed data
- **Availability**: 99.9%
- **Min Storage Duration**: 30 days
- **Min Object Size**: 128 KB
- **Retrieval Fee**: $0.01/GB
- **Cost**: $0.0125/GB

### S3 One Zone-IA
- **Use Case**: Recreatable infrequently accessed data
- **Availability**: 99.5% (single AZ)
- **Min Storage Duration**: 30 days
- **Retrieval Fee**: $0.01/GB
- **Cost**: $0.01/GB

### S3 Glacier Instant Retrieval
- **Use Case**: Archive data with instant retrieval
- **Retrieval Time**: Milliseconds
- **Min Storage Duration**: 90 days
- **Cost**: $0.004/GB

### S3 Glacier Flexible Retrieval
- **Use Case**: Archive data with retrieval in minutes to hours
- **Retrieval Options**:
  - Expedited: 1-5 minutes ($0.03/GB)
  - Standard: 3-5 hours ($0.01/GB)
  - Bulk: 5-12 hours ($0.0025/GB)
- **Min Storage Duration**: 90 days
- **Cost**: $0.0036/GB

### S3 Glacier Deep Archive
- **Use Case**: Long-term archive, rarely accessed
- **Retrieval Time**: 12-48 hours
- **Min Storage Duration**: 180 days
- **Cost**: $0.00099/GB (cheapest)

### Comparison Table

| Storage Class | Availability | Min Duration | Retrieval Time | Cost/GB |
|--------------|--------------|--------------|----------------|---------|
| Standard | 99.99% | None | Instant | $0.023 |
| Intelligent-Tiering | 99.9% | None | Instant | $0.023 |
| Standard-IA | 99.9% | 30 days | Instant | $0.0125 |
| One Zone-IA | 99.5% | 30 days | Instant | $0.01 |
| Glacier Instant | 99.9% | 90 days | Instant | $0.004 |
| Glacier Flexible | 99.99% | 90 days | Minutes-Hours | $0.0036 |
| Glacier Deep Archive | 99.99% | 180 days | 12-48 hours | $0.00099 |

---

## 3. Core Concepts {#core-concepts}

### Buckets
- Container for objects
- Globally unique name
- Region-specific
- Max 100 buckets per account (soft limit)

### Objects
- Files stored in S3
- Max size: 5 TB
- Key: Full path (e.g., `folder/subfolder/file.txt`)
- Value: Content of the object
- Metadata: Key-value pairs
- Version ID: If versioning enabled

### Versioning
- Protects against accidental deletion
- Stores all versions of an object
- Once enabled, can only be suspended (not disabled)

### Encryption
- **SSE-S3**: Server-side encryption with S3-managed keys (AES-256)
- **SSE-KMS**: Server-side encryption with KMS keys
- **SSE-C**: Server-side encryption with customer-provided keys
- **Client-Side**: Encrypt before uploading

### Multipart Upload
- Required for objects > 5 GB
- Recommended for objects > 100 MB
- Upload parts in parallel
- Resume failed uploads

---

## 4. Java/Spring Boot Integration {#java-integration}

### Maven Dependencies

```xml
<dependencies>
    <!-- AWS SDK v2 -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <version>2.20.26</version>
    </dependency>
    
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### Configuration (application.yml)

```yaml
aws:
  s3:
    bucket-name: my-application-bucket
    region: us-east-1
  credentials:
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
```

### S3 Configuration Class

```java
@Configuration
public class S3Config {
    
    @Value("${aws.s3.region}")
    private String region;
    
    @Value("${aws.credentials.access-key}")
    private String accessKey;
    
    @Value("${aws.credentials.secret-key}")
    private String secretKey;
    
    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
    
    // For async operations
    @Bean
    public S3AsyncClient s3AsyncClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        return S3AsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
```

### S3 Service Implementation

```java
@Service
@Slf4j
public class S3Service {
    
    private final S3Client s3Client;
    private final String bucketName;
    
    public S3Service(S3Client s3Client, @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }
    
    // Upload file
    public String uploadFile(String key, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();
        
        s3Client.putObject(request, RequestBody.fromBytes(content));
        return getFileUrl(key);
    }
    
    // Upload with metadata
    public String uploadFileWithMetadata(String key, byte[] content, Map<String, String> metadata) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .metadata(metadata)
                .build();
        
        s3Client.putObject(request, RequestBody.fromBytes(content));
        return getFileUrl(key);
    }
    
    // Download file
    public byte[] downloadFile(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asByteArray();
    }
    
    // Delete file
    public void deleteFile(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        
        s3Client.deleteObject(request);
    }
    
    // List files
    public List<String> listFiles(String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();
        
        ListObjectsV2Response response = s3Client.listObjectsV2(request);
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }
    
    // Generate presigned URL (valid for 1 hour)
    public String generatePresignedUrl(String key, Duration duration) {
        S3Presigner presigner = S3Presigner.create();
        
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();
        
        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }
    
    // Multipart upload for large files
    public String uploadLargeFile(String key, File file) throws IOException {
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        
        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();
        
        List<CompletedPart> completedParts = new ArrayList<>();
        long partSize = 5 * 1024 * 1024; // 5 MB
        long fileSize = file.length();
        int partNumber = 1;
        
        try (FileInputStream fis = new FileInputStream(file)) {
            long position = 0;
            while (position < fileSize) {
                long currentPartSize = Math.min(partSize, fileSize - position);
                byte[] buffer = new byte[(int) currentPartSize];
                fis.read(buffer);
                
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build();
                
                UploadPartResponse uploadPartResponse = s3Client.uploadPart(
                        uploadPartRequest, 
                        RequestBody.fromBytes(buffer)
                );
                
                completedParts.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(uploadPartResponse.eTag())
                        .build());
                
                position += currentPartSize;
                partNumber++;
            }
        }
        
        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder()
                        .parts(completedParts)
                        .build())
                .build();
        
        s3Client.completeMultipartUpload(completeRequest);
        return getFileUrl(key);
    }
    
    private String getFileUrl(String key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
    }
}
```

### REST Controller

```java
@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    
    private final S3Service s3Service;
    
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folder") String folder) throws IOException {
        
        String key = folder + "/" + file.getOriginalFilename();
        String url = s3Service.uploadFile(key, file.getBytes(), file.getContentType());
        return ResponseEntity.ok(url);
    }
    
    @GetMapping("/download/{folder}/{filename}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String folder,
            @PathVariable String filename) {
        
        String key = folder + "/" + filename;
        byte[] content = s3Service.downloadFile(key);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content);
    }
    
    @DeleteMapping("/{folder}/{filename}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String folder,
            @PathVariable String filename) {
        
        String key = folder + "/" + filename;
        s3Service.deleteFile(key);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/presigned-url/{folder}/{filename}")
    public ResponseEntity<String> getPresignedUrl(
            @PathVariable String folder,
            @PathVariable String filename) {
        
        String key = folder + "/" + filename;
        String url = s3Service.generatePresignedUrl(key, Duration.ofHours(1));
        return ResponseEntity.ok(url);
    }
}
```

---

## 5. IAM Policies & Roles {#iam-policies}

### S3 Bucket Policy (Resource-Based)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::my-bucket/*"
    }
  ]
}
```

### IAM Policy for Full S3 Access

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:GetBucketLocation"
      ],
      "Resource": "arn:aws:s3:::my-bucket"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::my-bucket/*"
    }
  ]
}
```

### IAM Policy for Read-Only Access

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:GetBucketLocation"
      ],
      "Resource": "arn:aws:s3:::my-bucket"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::my-bucket/*"
    }
  ]
}
```

### IAM Policy for Specific Folder Access

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::my-bucket",
      "Condition": {
        "StringLike": {
          "s3:prefix": "user123/*"
        }
      }
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::my-bucket/user123/*"
    }
  ]
}
```

### IAM Role for Lambda Function

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::my-bucket/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

### Trust Policy for Lambda Role

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

### Creating IAM Role via AWS CLI

```bash
# Create role
aws iam create-role \
  --role-name S3LambdaRole \
  --assume-role-policy-document file://trust-policy.json

# Attach policy
aws iam put-role-policy \
  --role-name S3LambdaRole \
  --policy-name S3AccessPolicy \
  --policy-document file://s3-policy.json

# Or attach managed policy
aws iam attach-role-policy \
  --role-name S3LambdaRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess
```

### IAM Policy for Cross-Account Access

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::123456789012:root"
      },
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::my-bucket/*"
    }
  ]
}
```

---

## 6. Practical Examples {#practical-examples}

### Example 1: Create S3 Bucket via CLI

```bash
# Create bucket
aws s3 mb s3://my-unique-bucket-name --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket my-unique-bucket-name \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket my-unique-bucket-name \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# Set lifecycle policy
aws s3api put-bucket-lifecycle-configuration \
  --bucket my-unique-bucket-name \
  --lifecycle-configuration file://lifecycle.json
```

### Lifecycle Policy (lifecycle.json)

```json
{
  "Rules": [
    {
      "Id": "MoveToIA",
      "Status": "Enabled",
      "Transitions": [
        {
          "Days": 30,
          "StorageClass": "STANDARD_IA"
        },
        {
          "Days": 90,
          "StorageClass": "GLACIER"
        }
      ],
      "Expiration": {
        "Days": 365
      }
    }
  ]
}
```

### Example 2: Spring Boot File Upload Service

```java
@Service
public class DocumentService {
    
    private final S3Service s3Service;
    
    public DocumentUploadResponse uploadDocument(MultipartFile file, String userId) {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Generate unique key
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String key = String.format("documents/%s/%s_%s", userId, timestamp, file.getOriginalFilename());
        
        // Upload to S3
        String url = s3Service.uploadFile(key, file.getBytes(), file.getContentType());
        
        // Save metadata to database
        Document document = new Document();
        document.setUserId(userId);
        document.setFileName(file.getOriginalFilename());
        document.setS3Key(key);
        document.setS3Url(url);
        document.setFileSize(file.getSize());
        document.setContentType(file.getContentType());
        documentRepository.save(document);
        
        return new DocumentUploadResponse(document.getId(), url);
    }
}
```

### Example 3: S3 Event Notification Configuration

```bash
# Configure S3 to trigger Lambda on object creation
aws s3api put-bucket-notification-configuration \
  --bucket my-bucket \
  --notification-configuration '{
    "LambdaFunctionConfigurations": [{
      "LambdaFunctionArn": "arn:aws:lambda:us-east-1:123456789012:function:ProcessS3Upload",
      "Events": ["s3:ObjectCreated:*"],
      "Filter": {
        "Key": {
          "FilterRules": [{
            "Name": "prefix",
            "Value": "uploads/"
          }]
        }
      }
    }]
  }'
```

---

## 7. Best Practices {#best-practices}

### Security
1. **Enable encryption** at rest (SSE-S3 or SSE-KMS)
2. **Use IAM roles** instead of access keys when possible
3. **Enable versioning** for critical data
4. **Block public access** unless explicitly needed
5. **Use presigned URLs** for temporary access
6. **Enable MFA Delete** for critical buckets
7. **Use VPC endpoints** for private access

### Performance
1. **Use CloudFront** for global content delivery
2. **Enable Transfer Acceleration** for long-distance uploads
3. **Use multipart upload** for files > 100 MB
4. **Parallelize requests** using multiple prefixes
5. **Use byte-range fetches** for large downloads
6. **Cache frequently accessed objects**

### Cost Optimization
1. **Use appropriate storage class** based on access patterns
2. **Enable Intelligent-Tiering** for unknown patterns
3. **Set lifecycle policies** to transition/expire objects
4. **Delete incomplete multipart uploads**
5. **Use S3 Storage Lens** for visibility
6. **Compress objects** before uploading

### Reliability
1. **Enable versioning** for data protection
2. **Use Cross-Region Replication** for disaster recovery
3. **Implement retry logic** with exponential backoff
4. **Monitor with CloudWatch** metrics and alarms
5. **Use S3 Inventory** for audit and compliance

### Naming Conventions
```
bucket-name: company-environment-purpose (e.g., acme-prod-documents)
object-key: category/subcategory/identifier/filename
  Example: documents/invoices/2024/01/invoice-12345.pdf
```

---

## Additional Resources

- [AWS S3 Documentation](https://docs.aws.amazon.com/s3/)
- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [S3 Pricing Calculator](https://calculator.aws/#/addService/S3)
- [S3 Best Practices](https://docs.aws.amazon.com/AmazonS3/latest/userguide/best-practices.html)

