# Amazon DynamoDB - Deep Dive (Part 2: Operations)

## Table of Contents
1. [CRUD Operations](#crud-operations)
2. [Query Operations](#query-operations)
3. [Scan Operations](#scan-operations)
4. [Secondary Indexes](#secondary-indexes)
5. [Batch Operations](#batch-operations)
6. [Conditional Operations](#conditional-operations)

---

## CRUD Operations

### Create (PutItem)

```java
@Service
public class UserService {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    public void createUser(User user) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(user.getUserId()).build());
        item.put("name", AttributeValue.builder().s(user.getName()).build());
        item.put("email", AttributeValue.builder().s(user.getEmail()).build());
        item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());
        
        PutItemRequest request = PutItemRequest.builder()
            .tableName("Users")
            .item(item)
            .build();
        
        dynamoDbClient.putItem(request);
    }
}
```

### Read (GetItem)

```java
public User getUser(String userId) {
    GetItemRequest request = GetItemRequest.builder()
        .tableName("Users")
        .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
        .consistentRead(true)
        .build();
    
    GetItemResponse response = dynamoDbClient.getItem(request);
    
    if (!response.hasItem()) {
        throw new UserNotFoundException(userId);
    }
    
    return mapToUser(response.item());
}
```

### Update (UpdateItem)

```java
public void updateUserEmail(String userId, String newEmail) {
    UpdateItemRequest request = UpdateItemRequest.builder()
        .tableName("Users")
        .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
        .updateExpression("SET email = :email, updatedAt = :updatedAt")
        .expressionAttributeValues(Map.of(
            ":email", AttributeValue.builder().s(newEmail).build(),
            ":updatedAt", AttributeValue.builder().s(Instant.now().toString()).build()
        ))
        .returnValues(ReturnValue.ALL_NEW)
        .build();
    
    dynamoDbClient.updateItem(request);
}

// Atomic counter increment
public void incrementLoginCount(String userId) {
    UpdateItemRequest request = UpdateItemRequest.builder()
        .tableName("Users")
        .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
        .updateExpression("ADD loginCount :inc")
        .expressionAttributeValues(Map.of(
            ":inc", AttributeValue.builder().n("1").build()
        ))
        .build();
    
    dynamoDbClient.updateItem(request);
}
```

### Delete (DeleteItem)

```java
public void deleteUser(String userId) {
    DeleteItemRequest request = DeleteItemRequest.builder()
        .tableName("Users")
        .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
        .build();
    
    dynamoDbClient.deleteItem(request);
}
```

---

## Query Operations

### Basic Query

```java
// Query orders for a customer
public List<Order> getCustomerOrders(String customerId) {
    QueryRequest request = QueryRequest.builder()
        .tableName("Orders")
        .keyConditionExpression("customerId = :customerId")
        .expressionAttributeValues(Map.of(
            ":customerId", AttributeValue.builder().s(customerId).build()
        ))
        .build();
    
    QueryResponse response = dynamoDbClient.query(request);
    return response.items().stream()
        .map(this::mapToOrder)
        .collect(Collectors.toList());
}
```

### Query with Sort Key Condition

```java
// Get orders within date range
public List<Order> getOrdersByDateRange(String customerId, String startDate, String endDate) {
    QueryRequest request = QueryRequest.builder()
        .tableName("Orders")
        .keyConditionExpression("customerId = :customerId AND orderDate BETWEEN :start AND :end")
        .expressionAttributeValues(Map.of(
            ":customerId", AttributeValue.builder().s(customerId).build(),
            ":start", AttributeValue.builder().s(startDate).build(),
            ":end", AttributeValue.builder().s(endDate).build()
        ))
        .build();
    
    return dynamoDbClient.query(request).items().stream()
        .map(this::mapToOrder)
        .collect(Collectors.toList());
}
```

### Query with Filter Expression

```java
// Query with additional filtering
public List<Order> getHighValueOrders(String customerId, double minAmount) {
    QueryRequest request = QueryRequest.builder()
        .tableName("Orders")
        .keyConditionExpression("customerId = :customerId")
        .filterExpression("totalAmount > :minAmount")
        .expressionAttributeValues(Map.of(
            ":customerId", AttributeValue.builder().s(customerId).build(),
            ":minAmount", AttributeValue.builder().n(String.valueOf(minAmount)).build()
        ))
        .build();
    
    return dynamoDbClient.query(request).items().stream()
        .map(this::mapToOrder)
        .collect(Collectors.toList());
}
```

### Pagination

```java
public PaginatedResult<Order> getOrdersPaginated(String customerId, String lastEvaluatedKey, int limit) {
    QueryRequest.Builder requestBuilder = QueryRequest.builder()
        .tableName("Orders")
        .keyConditionExpression("customerId = :customerId")
        .expressionAttributeValues(Map.of(
            ":customerId", AttributeValue.builder().s(customerId).build()
        ))
        .limit(limit);
    
    // Add pagination token if provided
    if (lastEvaluatedKey != null) {
        requestBuilder.exclusiveStartKey(Map.of(
            "customerId", AttributeValue.builder().s(customerId).build(),
            "orderDate", AttributeValue.builder().s(lastEvaluatedKey).build()
        ));
    }
    
    QueryResponse response = dynamoDbClient.query(requestBuilder.build());
    
    List<Order> orders = response.items().stream()
        .map(this::mapToOrder)
        .collect(Collectors.toList());
    
    String nextToken = response.hasLastEvaluatedKey() 
        ? response.lastEvaluatedKey().get("orderDate").s() 
        : null;
    
    return new PaginatedResult<>(orders, nextToken);
}
```

---

## Scan Operations

### Basic Scan

```java
// Scan entire table (expensive!)
public List<User> getAllUsers() {
    ScanRequest request = ScanRequest.builder()
        .tableName("Users")
        .build();
    
    ScanResponse response = dynamoDbClient.scan(request);
    return response.items().stream()
        .map(this::mapToUser)
        .collect(Collectors.toList());
}
```

### Scan with Filter

```java
// Scan with filter expression
public List<User> getActiveUsers() {
    ScanRequest request = ScanRequest.builder()
        .tableName("Users")
        .filterExpression("accountStatus = :status")
        .expressionAttributeValues(Map.of(
            ":status", AttributeValue.builder().s("ACTIVE").build()
        ))
        .build();
    
    return dynamoDbClient.scan(request).items().stream()
        .map(this::mapToUser)
        .collect(Collectors.toList());
}
```

### Parallel Scan

```java
// Parallel scan for large tables
public List<User> parallelScan(int totalSegments) {
    List<CompletableFuture<List<User>>> futures = new ArrayList<>();
    
    for (int segment = 0; segment < totalSegments; segment++) {
        final int currentSegment = segment;
        
        CompletableFuture<List<User>> future = CompletableFuture.supplyAsync(() -> {
            ScanRequest request = ScanRequest.builder()
                .tableName("Users")
                .segment(currentSegment)
                .totalSegments(totalSegments)
                .build();
            
            return dynamoDbClient.scan(request).items().stream()
                .map(this::mapToUser)
                .collect(Collectors.toList());
        });
        
        futures.add(future);
    }
    
    return futures.stream()
        .map(CompletableFuture::join)
        .flatMap(List::stream)
        .collect(Collectors.toList());
}
```

---

## Secondary Indexes

### Global Secondary Index (GSI)

```java
// Create table with GSI
CreateTableRequest request = CreateTableRequest.builder()
    .tableName("Orders")
    .keySchema(
        KeySchemaElement.builder().attributeName("orderId").keyType(KeyType.HASH).build()
    )
    .attributeDefinitions(
        AttributeDefinition.builder().attributeName("orderId").attributeType(ScalarAttributeType.S).build(),
        AttributeDefinition.builder().attributeName("customerId").attributeType(ScalarAttributeType.S).build(),
        AttributeDefinition.builder().attributeName("orderDate").attributeType(ScalarAttributeType.S).build()
    )
    .globalSecondaryIndexes(
        GlobalSecondaryIndex.builder()
            .indexName("CustomerIdIndex")
            .keySchema(
                KeySchemaElement.builder().attributeName("customerId").keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName("orderDate").keyType(KeyType.RANGE).build()
            )
            .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
            .provisionedThroughput(ProvisionedThroughput.builder()
                .readCapacityUnits(5L)
                .writeCapacityUnits(5L)
                .build())
            .build()
    )
    .billingMode(BillingMode.PROVISIONED)
    .provisionedThroughput(ProvisionedThroughput.builder()
        .readCapacityUnits(5L)
        .writeCapacityUnits(5L)
        .build())
    .build();

// Query GSI
public List<Order> queryByCustomerId(String customerId) {
    QueryRequest request = QueryRequest.builder()
        .tableName("Orders")
        .indexName("CustomerIdIndex")
        .keyConditionExpression("customerId = :customerId")
        .expressionAttributeValues(Map.of(
            ":customerId", AttributeValue.builder().s(customerId).build()
        ))
        .build();
    
    return dynamoDbClient.query(request).items().stream()
        .map(this::mapToOrder)
        .collect(Collectors.toList());
}
```

### Local Secondary Index (LSI)

```java
// Create table with LSI
CreateTableRequest request = CreateTableRequest.builder()
    .tableName("GameScores")
    .keySchema(
        KeySchemaElement.builder().attributeName("userId").keyType(KeyType.HASH).build(),
        KeySchemaElement.builder().attributeName("gameId").keyType(KeyType.RANGE).build()
    )
    .attributeDefinitions(
        AttributeDefinition.builder().attributeName("userId").attributeType(ScalarAttributeType.S).build(),
        AttributeDefinition.builder().attributeName("gameId").attributeType(ScalarAttributeType.S).build(),
        AttributeDefinition.builder().attributeName("score").attributeType(ScalarAttributeType.N).build()
    )
    .localSecondaryIndexes(
        LocalSecondaryIndex.builder()
            .indexName("ScoreIndex")
            .keySchema(
                KeySchemaElement.builder().attributeName("userId").keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName("score").keyType(KeyType.RANGE).build()
            )
            .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
            .build()
    )
    .billingMode(BillingMode.PAY_PER_REQUEST)
    .build();

// Query LSI
public List<GameScore> getTopScores(String userId, int limit) {
    QueryRequest request = QueryRequest.builder()
        .tableName("GameScores")
        .indexName("ScoreIndex")
        .keyConditionExpression("userId = :userId")
        .expressionAttributeValues(Map.of(
            ":userId", AttributeValue.builder().s(userId).build()
        ))
        .scanIndexForward(false)  // Descending order
        .limit(limit)
        .build();
    
    return dynamoDbClient.query(request).items().stream()
        .map(this::mapToGameScore)
        .collect(Collectors.toList());
}
```

### Sparse Index

```java
// Only items with the indexed attribute appear in the index
// Example: Index only premium users
CreateTableRequest request = CreateTableRequest.builder()
    .tableName("Users")
    .globalSecondaryIndexes(
        GlobalSecondaryIndex.builder()
            .indexName("PremiumUsersIndex")
            .keySchema(
                KeySchemaElement.builder().attributeName("premiumStatus").keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName("subscriptionDate").keyType(KeyType.RANGE).build()
            )
            .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
            .build()
    )
    .build();

// Only users with premiumStatus attribute will be in the index
```

---

## Batch Operations

### BatchGetItem

```java
public List<User> getUsersBatch(List<String> userIds) {
    Map<String, KeysAndAttributes> requestItems = new HashMap<>();
    
    List<Map<String, AttributeValue>> keys = userIds.stream()
        .map(id -> Map.of("userId", AttributeValue.builder().s(id).build()))
        .collect(Collectors.toList());
    
    requestItems.put("Users", KeysAndAttributes.builder()
        .keys(keys)
        .consistentRead(true)
        .build());
    
    BatchGetItemRequest request = BatchGetItemRequest.builder()
        .requestItems(requestItems)
        .build();
    
    BatchGetItemResponse response = dynamoDbClient.batchGetItem(request);
    
    return response.responses().get("Users").stream()
        .map(this::mapToUser)
        .collect(Collectors.toList());
}
```

### BatchWriteItem

```java
public void batchCreateUsers(List<User> users) {
    List<WriteRequest> writeRequests = users.stream()
        .map(user -> {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("userId", AttributeValue.builder().s(user.getUserId()).build());
            item.put("name", AttributeValue.builder().s(user.getName()).build());
            item.put("email", AttributeValue.builder().s(user.getEmail()).build());
            
            return WriteRequest.builder()
                .putRequest(PutRequest.builder().item(item).build())
                .build();
        })
        .collect(Collectors.toList());
    
    // DynamoDB limits batch to 25 items
    List<List<WriteRequest>> batches = Lists.partition(writeRequests, 25);
    
    for (List<WriteRequest> batch : batches) {
        BatchWriteItemRequest request = BatchWriteItemRequest.builder()
            .requestItems(Map.of("Users", batch))
            .build();
        
        dynamoDbClient.batchWriteItem(request);
    }
}
```

---

## Conditional Operations

### Conditional Put

```java
// Put item only if it doesn't exist
public void createUserIfNotExists(User user) {
    Map<String, AttributeValue> item = mapToItem(user);
    
    PutItemRequest request = PutItemRequest.builder()
        .tableName("Users")
        .item(item)
        .conditionExpression("attribute_not_exists(userId)")
        .build();
    
    try {
        dynamoDbClient.putItem(request);
    } catch (ConditionalCheckFailedException e) {
        throw new UserAlreadyExistsException(user.getUserId());
    }
}
```

### Conditional Update

```java
// Update only if version matches (optimistic locking)
public void updateUserWithVersion(String userId, String newEmail, int expectedVersion) {
    UpdateItemRequest request = UpdateItemRequest.builder()
        .tableName("Users")
        .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
        .updateExpression("SET email = :email, version = :newVersion")
        .conditionExpression("version = :expectedVersion")
        .expressionAttributeValues(Map.of(
            ":email", AttributeValue.builder().s(newEmail).build(),
            ":expectedVersion", AttributeValue.builder().n(String.valueOf(expectedVersion)).build(),
            ":newVersion", AttributeValue.builder().n(String.valueOf(expectedVersion + 1)).build()
        ))
        .build();
    
    try {
        dynamoDbClient.updateItem(request);
    } catch (ConditionalCheckFailedException e) {
        throw new OptimisticLockException("Version mismatch");
    }
}
```

### Conditional Delete

```java
// Delete only if status is INACTIVE
public void deleteInactiveUser(String userId) {
    DeleteItemRequest request = DeleteItemRequest.builder()
        .tableName("Users")
        .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
        .conditionExpression("accountStatus = :status")
        .expressionAttributeValues(Map.of(
            ":status", AttributeValue.builder().s("INACTIVE").build()
        ))
        .build();
    
    try {
        dynamoDbClient.deleteItem(request);
    } catch (ConditionalCheckFailedException e) {
        throw new InvalidOperationException("User is not inactive");
    }
}
```

---

## Expression Attribute Names

```java
// Use when attribute name is a reserved word
public User getUserWithReservedAttributes(String userId) {
    GetItemRequest request = GetItemRequest.builder()
        .tableName("Users")
        .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
        .projectionExpression("#n, #s, email")  // 'name' and 'status' are reserved
        .expressionAttributeNames(Map.of(
            "#n", "name",
            "#s", "status"
        ))
        .build();
    
    return mapToUser(dynamoDbClient.getItem(request).item());
}
```

---

## Best Practices

### 1. Use Query Instead of Scan

```java
// GOOD: Query with partition key
QueryRequest query = QueryRequest.builder()
    .tableName("Orders")
    .keyConditionExpression("customerId = :id")
    .build();

// BAD: Scan entire table
ScanRequest scan = ScanRequest.builder()
    .tableName("Orders")
    .filterExpression("customerId = :id")
    .build();
```

### 2. Use Projection Expressions

```java
// Fetch only required attributes
GetItemRequest request = GetItemRequest.builder()
    .tableName("Users")
    .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
    .projectionExpression("userId, name, email")  // Don't fetch all attributes
    .build();
```

### 3. Use Batch Operations

```java
// GOOD: Batch get (up to 100 items)
BatchGetItemRequest batchRequest = BatchGetItemRequest.builder()
    .requestItems(...)
    .build();

// BAD: Multiple individual gets
for (String userId : userIds) {
    GetItemRequest request = GetItemRequest.builder()
        .tableName("Users")
        .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
        .build();
    dynamoDbClient.getItem(request);
}
```

### 4. Handle Pagination

```java
// Always handle pagination for large result sets
Map<String, AttributeValue> lastKey = null;
List<Order> allOrders = new ArrayList<>();

do {
    QueryRequest.Builder builder = QueryRequest.builder()
        .tableName("Orders")
        .keyConditionExpression("customerId = :id")
        .expressionAttributeValues(Map.of(":id", AttributeValue.builder().s(customerId).build()));
    
    if (lastKey != null) {
        builder.exclusiveStartKey(lastKey);
    }
    
    QueryResponse response = dynamoDbClient.query(builder.build());
    allOrders.addAll(response.items().stream().map(this::mapToOrder).collect(Collectors.toList()));
    lastKey = response.lastEvaluatedKey();
    
} while (lastKey != null && !lastKey.isEmpty());
```

### 5. Use Exponential Backoff for Retries

```java
@Service
public class DynamoDBRetryService {
    
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 100;
    
    public <T> T executeWithRetry(Supplier<T> operation) {
        int attempt = 0;
        
        while (true) {
            try {
                return operation.get();
            } catch (ProvisionedThroughputExceededException e) {
                if (++attempt >= MAX_RETRIES) {
                    throw e;
                }
                
                long delay = BASE_DELAY_MS * (long) Math.pow(2, attempt);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
    }
}
```

---

## Next Steps

Continue to:
- **Part 3**: Advanced Features (Streams, Global Tables, Transactions)
- **Part 4**: Integration with AWS Services
- **Part 5**: Enterprise Use Cases and Patterns
