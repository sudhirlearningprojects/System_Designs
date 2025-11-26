# Amazon DynamoDB - Deep Dive (Part 3: Advanced Features)

## Table of Contents
1. [DynamoDB Streams](#dynamodb-streams)
2. [Global Tables](#global-tables)
3. [Transactions](#transactions)
4. [Time To Live (TTL)](#time-to-live-ttl)
5. [Point-in-Time Recovery](#point-in-time-recovery)
6. [DynamoDB Accelerator (DAX)](#dynamodb-accelerator-dax)

---

## DynamoDB Streams

### Overview

DynamoDB Streams captures time-ordered sequence of item-level modifications in a table.

**Use Cases:**
- Real-time analytics
- Data replication
- Event-driven architectures
- Audit logging
- Materialized views

### Enable Streams

```java
UpdateTableRequest request = UpdateTableRequest.builder()
    .tableName("Orders")
    .streamSpecification(StreamSpecification.builder()
        .streamEnabled(true)
        .streamViewType(StreamViewType.NEW_AND_OLD_IMAGES)
        .build())
    .build();

dynamoDbClient.updateTable(request);
```

### Stream View Types

| Type | Description |
|------|-------------|
| KEYS_ONLY | Only key attributes |
| NEW_IMAGE | Entire item after modification |
| OLD_IMAGE | Entire item before modification |
| NEW_AND_OLD_IMAGES | Both before and after |

### Process Streams with Lambda

```java
@Component
public class OrderStreamProcessor implements RequestHandler<DynamodbEvent, String> {
    
    @Override
    public String handleRequest(DynamodbEvent event, Context context) {
        for (DynamodbStreamRecord record : event.getRecords()) {
            String eventName = record.getEventName();
            
            switch (eventName) {
                case "INSERT":
                    handleInsert(record.getDynamodb().getNewImage());
                    break;
                case "MODIFY":
                    handleModify(
                        record.getDynamodb().getOldImage(),
                        record.getDynamodb().getNewImage()
                    );
                    break;
                case "REMOVE":
                    handleRemove(record.getDynamodb().getOldImage());
                    break;
            }
        }
        return "Processed " + event.getRecords().size() + " records";
    }
    
    private void handleInsert(Map<String, AttributeValue> newImage) {
        String orderId = newImage.get("orderId").s();
        String customerId = newImage.get("customerId").s();
        
        // Send order confirmation email
        emailService.sendOrderConfirmation(customerId, orderId);
        
        // Update analytics
        analyticsService.recordNewOrder(orderId);
    }
    
    private void handleModify(Map<String, AttributeValue> oldImage, 
                              Map<String, AttributeValue> newImage) {
        String oldStatus = oldImage.get("status").s();
        String newStatus = newImage.get("status").s();
        
        if (!oldStatus.equals(newStatus)) {
            // Status changed - send notification
            notificationService.sendStatusUpdate(
                newImage.get("customerId").s(),
                newImage.get("orderId").s(),
                newStatus
            );
        }
    }
    
    private void handleRemove(Map<String, AttributeValue> oldImage) {
        // Archive deleted order
        archiveService.archiveOrder(oldImage);
    }
}
```

### Process Streams with Kinesis

```java
@Service
public class StreamToKinesisProcessor {
    
    @Autowired
    private KinesisClient kinesisClient;
    
    public void processStreamRecord(DynamodbStreamRecord record) {
        // Convert DynamoDB record to Kinesis record
        String data = convertToJson(record);
        
        PutRecordRequest request = PutRecordRequest.builder()
            .streamName("order-events")
            .data(SdkBytes.fromUtf8String(data))
            .partitionKey(record.getDynamodb().getKeys().get("orderId").s())
            .build();
        
        kinesisClient.putRecord(request);
    }
}
```

### Cross-Region Replication with Streams

```java
@Component
public class CrossRegionReplicator implements RequestHandler<DynamodbEvent, String> {
    
    @Autowired
    private DynamoDbClient targetRegionClient;
    
    @Override
    public String handleRequest(DynamodbEvent event, Context context) {
        for (DynamodbStreamRecord record : event.getRecords()) {
            if ("INSERT".equals(record.getEventName()) || "MODIFY".equals(record.getEventName())) {
                // Replicate to target region
                PutItemRequest request = PutItemRequest.builder()
                    .tableName("Orders")
                    .item(record.getDynamodb().getNewImage())
                    .build();
                
                targetRegionClient.putItem(request);
            } else if ("REMOVE".equals(record.getEventName())) {
                // Delete from target region
                DeleteItemRequest request = DeleteItemRequest.builder()
                    .tableName("Orders")
                    .key(record.getDynamodb().getKeys())
                    .build();
                
                targetRegionClient.deleteItem(request);
            }
        }
        return "Replicated " + event.getRecords().size() + " records";
    }
}
```

---

## Global Tables

### Overview

Multi-region, multi-active database with automatic replication.

**Features:**
- Multi-region writes
- Automatic conflict resolution
- 99.999% availability SLA
- Sub-second replication latency

### Create Global Table

```java
CreateGlobalTableRequest request = CreateGlobalTableRequest.builder()
    .globalTableName("Users")
    .replicationGroup(
        Replica.builder().regionName("us-east-1").build(),
        Replica.builder().regionName("eu-west-1").build(),
        Replica.builder().regionName("ap-southeast-1").build()
    )
    .build();

dynamoDbClient.createGlobalTable(request);
```

### Add Region to Global Table

```java
UpdateTableRequest request = UpdateTableRequest.builder()
    .tableName("Users")
    .replicaUpdates(
        ReplicationGroupUpdate.builder()
            .create(CreateReplicationGroupMemberAction.builder()
                .regionName("us-west-2")
                .build())
            .build()
    )
    .build();

dynamoDbClient.updateTable(request);
```

### Conflict Resolution

DynamoDB uses **last-writer-wins** strategy based on timestamp.

```java
// Example: Two regions update same item simultaneously
// Region 1 (10:00:01): Update email to "user1@example.com"
// Region 2 (10:00:02): Update email to "user2@example.com"
// Result: "user2@example.com" wins (later timestamp)
```

### Global Table Best Practices

```java
@Service
public class GlobalTableService {
    
    // Use version numbers for conflict detection
    public void updateWithVersion(String userId, String email, int version) {
        UpdateItemRequest request = UpdateItemRequest.builder()
            .tableName("Users")
            .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
            .updateExpression("SET email = :email, version = :newVersion, updatedAt = :timestamp")
            .conditionExpression("version = :currentVersion")
            .expressionAttributeValues(Map.of(
                ":email", AttributeValue.builder().s(email).build(),
                ":currentVersion", AttributeValue.builder().n(String.valueOf(version)).build(),
                ":newVersion", AttributeValue.builder().n(String.valueOf(version + 1)).build(),
                ":timestamp", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build()
            ))
            .build();
        
        try {
            dynamoDbClient.updateItem(request);
        } catch (ConditionalCheckFailedException e) {
            // Version conflict - handle appropriately
            throw new ConcurrentModificationException("Item was modified by another region");
        }
    }
}
```

---

## Transactions

### Overview

ACID transactions across multiple items and tables.

**Limits:**
- Up to 100 items per transaction
- 4 MB total transaction size
- Items can be in different tables

### TransactWriteItems

```java
@Service
public class TransactionService {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    // Transfer money between accounts
    public void transferMoney(String fromAccount, String toAccount, BigDecimal amount) {
        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
            .transactItems(
                // Debit from source account
                TransactWriteItem.builder()
                    .update(Update.builder()
                        .tableName("Accounts")
                        .key(Map.of("accountId", AttributeValue.builder().s(fromAccount).build()))
                        .updateExpression("SET balance = balance - :amount")
                        .conditionExpression("balance >= :amount")
                        .expressionAttributeValues(Map.of(
                            ":amount", AttributeValue.builder().n(amount.toString()).build()
                        ))
                        .build())
                    .build(),
                
                // Credit to destination account
                TransactWriteItem.builder()
                    .update(Update.builder()
                        .tableName("Accounts")
                        .key(Map.of("accountId", AttributeValue.builder().s(toAccount).build()))
                        .updateExpression("SET balance = balance + :amount")
                        .expressionAttributeValues(Map.of(
                            ":amount", AttributeValue.builder().n(amount.toString()).build()
                        ))
                        .build())
                    .build(),
                
                // Record transaction
                TransactWriteItem.builder()
                    .put(Put.builder()
                        .tableName("Transactions")
                        .item(Map.of(
                            "transactionId", AttributeValue.builder().s(UUID.randomUUID().toString()).build(),
                            "fromAccount", AttributeValue.builder().s(fromAccount).build(),
                            "toAccount", AttributeValue.builder().s(toAccount).build(),
                            "amount", AttributeValue.builder().n(amount.toString()).build(),
                            "timestamp", AttributeValue.builder().s(Instant.now().toString()).build()
                        ))
                        .build())
                    .build()
            )
            .build();
        
        try {
            dynamoDbClient.transactWriteItems(request);
        } catch (TransactionCanceledException e) {
            // Transaction failed - all operations rolled back
            throw new InsufficientFundsException("Transfer failed: " + e.getMessage());
        }
    }
}
```

### TransactGetItems

```java
// Read multiple items atomically
public OrderDetails getOrderDetails(String orderId) {
    TransactGetItemsRequest request = TransactGetItemsRequest.builder()
        .transactItems(
            // Get order
            TransactGetItem.builder()
                .get(Get.builder()
                    .tableName("Orders")
                    .key(Map.of("orderId", AttributeValue.builder().s(orderId).build()))
                    .build())
                .build(),
            
            // Get customer
            TransactGetItem.builder()
                .get(Get.builder()
                    .tableName("Customers")
                    .key(Map.of("customerId", AttributeValue.builder().s("CUST-123").build()))
                    .build())
                .build(),
            
            // Get payment
            TransactGetItem.builder()
                .get(Get.builder()
                    .tableName("Payments")
                    .key(Map.of("orderId", AttributeValue.builder().s(orderId).build()))
                    .build())
                .build()
        )
        .build();
    
    TransactGetItemsResponse response = dynamoDbClient.transactGetItems(request);
    
    return OrderDetails.builder()
        .order(mapToOrder(response.responses().get(0).item()))
        .customer(mapToCustomer(response.responses().get(1).item()))
        .payment(mapToPayment(response.responses().get(2).item()))
        .build();
}
```

### Idempotent Transactions

```java
// Use client request token for idempotency
public void createOrderTransaction(Order order, String idempotencyToken) {
    TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
        .clientRequestToken(idempotencyToken)  // Ensures idempotency
        .transactItems(
            TransactWriteItem.builder()
                .put(Put.builder()
                    .tableName("Orders")
                    .item(mapToItem(order))
                    .conditionExpression("attribute_not_exists(orderId)")
                    .build())
                .build()
        )
        .build();
    
    dynamoDbClient.transactWriteItems(request);
}
```

---

## Time To Live (TTL)

### Overview

Automatically delete expired items at no cost.

**Use Cases:**
- Session management
- Temporary data
- Event logs
- Cache expiration

### Enable TTL

```java
UpdateTimeToLiveRequest request = UpdateTimeToLiveRequest.builder()
    .tableName("Sessions")
    .timeToLiveSpecification(TimeToLiveSpecification.builder()
        .enabled(true)
        .attributeName("expirationTime")
        .build())
    .build();

dynamoDbClient.updateTimeToLive(request);
```

### Create Item with TTL

```java
@Service
public class SessionService {
    
    public void createSession(String sessionId, String userId, int ttlMinutes) {
        long expirationTime = Instant.now().plusSeconds(ttlMinutes * 60).getEpochSecond();
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("sessionId", AttributeValue.builder().s(sessionId).build());
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());
        item.put("expirationTime", AttributeValue.builder().n(String.valueOf(expirationTime)).build());
        
        PutItemRequest request = PutItemRequest.builder()
            .tableName("Sessions")
            .item(item)
            .build();
        
        dynamoDbClient.putItem(request);
    }
}
```

### TTL with Streams

```java
// Process expired items before deletion
@Component
public class TTLStreamProcessor implements RequestHandler<DynamodbEvent, String> {
    
    @Override
    public String handleRequest(DynamodbEvent event, Context context) {
        for (DynamodbStreamRecord record : event.getRecords()) {
            if ("REMOVE".equals(record.getEventName())) {
                Map<String, AttributeValue> oldImage = record.getDynamodb().getOldImage();
                
                // Check if deletion was due to TTL
                if (record.getUserIdentity() != null && 
                    "dynamodb.amazonaws.com".equals(record.getUserIdentity().getPrincipalId())) {
                    
                    // Archive expired session
                    archiveExpiredSession(oldImage);
                }
            }
        }
        return "Processed";
    }
}
```

---

## Point-in-Time Recovery

### Overview

Restore table to any point in time within last 35 days.

### Enable PITR

```java
UpdateContinuousBackupsRequest request = UpdateContinuousBackupsRequest.builder()
    .tableName("Orders")
    .pointInTimeRecoverySpecification(PointInTimeRecoverySpecification.builder()
        .pointInTimeRecoveryEnabled(true)
        .build())
    .build();

dynamoDbClient.updateContinuousBackups(request);
```

### Restore Table

```java
RestoreTableToPointInTimeRequest request = RestoreTableToPointInTimeRequest.builder()
    .sourceTableName("Orders")
    .targetTableName("Orders-Restored")
    .restoreDateTime(Instant.parse("2024-01-15T10:00:00Z"))
    .build();

dynamoDbClient.restoreTableToPointInTime(request);
```

### On-Demand Backup

```java
// Create backup
CreateBackupRequest backupRequest = CreateBackupRequest.builder()
    .tableName("Orders")
    .backupName("Orders-Backup-2024-01-15")
    .build();

CreateBackupResponse backupResponse = dynamoDbClient.createBackup(backupRequest);

// Restore from backup
RestoreTableFromBackupRequest restoreRequest = RestoreTableFromBackupRequest.builder()
    .targetTableName("Orders-Restored")
    .backupArn(backupResponse.backupDetails().backupArn())
    .build();

dynamoDbClient.restoreTableFromBackup(restoreRequest);
```

---

## DynamoDB Accelerator (DAX)

### Overview

In-memory cache for DynamoDB with microsecond latency.

**Features:**
- Microsecond read latency
- Write-through cache
- Compatible with DynamoDB API
- Automatic cache invalidation

### Setup DAX Client

```java
@Configuration
public class DAXConfig {
    
    @Bean
    public AmazonDaxClient daxClient() {
        ClientConfig config = new ClientConfig()
            .withEndpoints("mycluster.dax-clusters.us-east-1.amazonaws.com:8111")
            .withRequestTimeout(60000)
            .withConnectionTimeout(10000);
        
        return AmazonDaxClientBuilder.standard()
            .withClientConfiguration(config)
            .build();
    }
}

@Service
public class ProductService {
    
    @Autowired
    private AmazonDaxClient daxClient;
    
    public Product getProduct(String productId) {
        // DAX automatically caches the result
        GetItemRequest request = new GetItemRequest()
            .withTableName("Products")
            .withKey(Map.of("productId", new AttributeValue(productId)));
        
        GetItemResult result = daxClient.getItem(request);
        return mapToProduct(result.getItem());
    }
}
```

### DAX vs ElastiCache

| Feature | DAX | ElastiCache |
|---------|-----|-------------|
| API | DynamoDB API | Redis/Memcached API |
| Integration | Seamless | Manual |
| Write-through | Yes | No |
| Consistency | Eventual | Manual management |
| Use Case | DynamoDB acceleration | General caching |

### When to Use DAX

✅ **Use DAX when:**
- Need microsecond read latency
- Read-heavy workloads
- Eventually consistent reads acceptable
- Want seamless DynamoDB integration

❌ **Don't use DAX when:**
- Need strongly consistent reads
- Write-heavy workloads
- Cost-sensitive (DAX is expensive)
- Complex caching logic required

---

## Best Practices

### 1. Use Streams for Event-Driven Architecture

```java
// Decouple services using streams
Orders Table → Stream → Lambda → SNS → Multiple Subscribers
```

### 2. Enable PITR for Critical Tables

```java
// Always enable for production tables
UpdateContinuousBackupsRequest request = UpdateContinuousBackupsRequest.builder()
    .tableName("CriticalData")
    .pointInTimeRecoverySpecification(PointInTimeRecoverySpecification.builder()
        .pointInTimeRecoveryEnabled(true)
        .build())
    .build();
```

### 3. Use Transactions for Multi-Item Operations

```java
// Ensure atomicity for related operations
TransactWriteItems for order + payment + inventory
```

### 4. Implement TTL for Temporary Data

```java
// Reduce storage costs
Sessions, logs, temporary data → TTL enabled
```

### 5. Use Global Tables for Multi-Region

```java
// High availability and low latency globally
Global Tables with 3+ regions
```

---

## Next Steps

Continue to:
- **Part 4**: Integration with AWS Services
- **Part 5**: Enterprise Use Cases and Patterns
