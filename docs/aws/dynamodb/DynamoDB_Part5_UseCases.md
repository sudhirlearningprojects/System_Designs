# Amazon DynamoDB - Deep Dive (Part 5: Enterprise Use Cases)

## Table of Contents
1. [E-Commerce Platform](#e-commerce-platform)
2. [Gaming Leaderboards](#gaming-leaderboards)
3. [IoT Data Storage](#iot-data-storage)
4. [Session Management](#session-management)
5. [Social Media Feed](#social-media-feed)
6. [Real-Time Analytics](#real-time-analytics)
7. [Content Management](#content-management)
8. [Financial Services](#financial-services)

---

## E-Commerce Platform

### Table Design

```java
// Orders Table
PK: customerId (HASH)
SK: orderDate#orderId (RANGE)

// GSI: OrderStatusIndex
PK: status (HASH)
SK: orderDate (RANGE)

// GSI: ProductIndex
PK: productId (HASH)
SK: orderDate (RANGE)
```

### Implementation

```java
@Service
public class ECommerceService {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    // Create order
    public Order createOrder(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        String sortKey = Instant.now().toString() + "#" + orderId;
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("customerId", AttributeValue.builder().s(request.getCustomerId()).build());
        item.put("orderDate#orderId", AttributeValue.builder().s(sortKey).build());
        item.put("orderId", AttributeValue.builder().s(orderId).build());
        item.put("status", AttributeValue.builder().s("PENDING").build());
        item.put("totalAmount", AttributeValue.builder().n(request.getTotalAmount().toString()).build());
        item.put("items", AttributeValue.builder().l(
            request.getItems().stream()
                .map(this::itemToAttributeValue)
                .collect(Collectors.toList())
        ).build());
        
        PutItemRequest putRequest = PutItemRequest.builder()
            .tableName("Orders")
            .item(item)
            .build();
        
        dynamoDbClient.putItem(putRequest);
        return mapToOrder(item);
    }
    
    // Get customer orders
    public List<Order> getCustomerOrders(String customerId, int limit) {
        QueryRequest request = QueryRequest.builder()
            .tableName("Orders")
            .keyConditionExpression("customerId = :customerId")
            .expressionAttributeValues(Map.of(
                ":customerId", AttributeValue.builder().s(customerId).build()
            ))
            .scanIndexForward(false)  // Latest first
            .limit(limit)
            .build();
        
        return dynamoDbClient.query(request).items().stream()
            .map(this::mapToOrder)
            .collect(Collectors.toList());
    }
    
    // Get orders by status
    public List<Order> getOrdersByStatus(String status) {
        QueryRequest request = QueryRequest.builder()
            .tableName("Orders")
            .indexName("OrderStatusIndex")
            .keyConditionExpression("#status = :status")
            .expressionAttributeNames(Map.of("#status", "status"))
            .expressionAttributeValues(Map.of(
                ":status", AttributeValue.builder().s(status).build()
            ))
            .build();
        
        return dynamoDbClient.query(request).items().stream()
            .map(this::mapToOrder)
            .collect(Collectors.toList());
    }
    
    // Update order status with transaction
    public void updateOrderStatus(String orderId, String customerId, String newStatus) {
        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
            .transactItems(
                // Update order status
                TransactWriteItem.builder()
                    .update(Update.builder()
                        .tableName("Orders")
                        .key(Map.of(
                            "customerId", AttributeValue.builder().s(customerId).build(),
                            "orderDate#orderId", AttributeValue.builder().s(getOrderSortKey(orderId)).build()
                        ))
                        .updateExpression("SET #status = :status, updatedAt = :timestamp")
                        .expressionAttributeNames(Map.of("#status", "status"))
                        .expressionAttributeValues(Map.of(
                            ":status", AttributeValue.builder().s(newStatus).build(),
                            ":timestamp", AttributeValue.builder().s(Instant.now().toString()).build()
                        ))
                        .build())
                    .build(),
                
                // Log status change
                TransactWriteItem.builder()
                    .put(Put.builder()
                        .tableName("OrderHistory")
                        .item(Map.of(
                            "orderId", AttributeValue.builder().s(orderId).build(),
                            "timestamp", AttributeValue.builder().s(Instant.now().toString()).build(),
                            "status", AttributeValue.builder().s(newStatus).build()
                        ))
                        .build())
                    .build()
            )
            .build();
        
        dynamoDbClient.transactWriteItems(request);
    }
}
```

---

## Gaming Leaderboards

### Table Design

```java
// Leaderboard Table
PK: gameId (HASH)
SK: score#userId (RANGE)  // Composite sort key for ranking

// GSI: UserScoreIndex
PK: userId (HASH)
SK: gameId (RANGE)
```

### Implementation

```java
@Service
public class LeaderboardService {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    // Submit score
    public void submitScore(String gameId, String userId, int score, Map<String, String> metadata) {
        // Use score with leading zeros for proper sorting
        String sortKey = String.format("%010d", Integer.MAX_VALUE - score) + "#" + userId;
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("gameId", AttributeValue.builder().s(gameId).build());
        item.put("score#userId", AttributeValue.builder().s(sortKey).build());
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("score", AttributeValue.builder().n(String.valueOf(score)).build());
        item.put("timestamp", AttributeValue.builder().s(Instant.now().toString()).build());
        metadata.forEach((k, v) -> item.put(k, AttributeValue.builder().s(v).build()));
        
        PutItemRequest request = PutItemRequest.builder()
            .tableName("Leaderboard")
            .item(item)
            .build();
        
        dynamoDbClient.putItem(request);
    }
    
    // Get top scores
    public List<LeaderboardEntry> getTopScores(String gameId, int limit) {
        QueryRequest request = QueryRequest.builder()
            .tableName("Leaderboard")
            .keyConditionExpression("gameId = :gameId")
            .expressionAttributeValues(Map.of(
                ":gameId", AttributeValue.builder().s(gameId).build()
            ))
            .limit(limit)
            .build();
        
        List<LeaderboardEntry> entries = dynamoDbClient.query(request).items().stream()
            .map(this::mapToLeaderboardEntry)
            .collect(Collectors.toList());
        
        // Add rank
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }
        
        return entries;
    }
    
    // Get user rank
    public int getUserRank(String gameId, String userId) {
        // Get user's score
        QueryRequest userScoreRequest = QueryRequest.builder()
            .tableName("Leaderboard")
            .indexName("UserScoreIndex")
            .keyConditionExpression("userId = :userId AND gameId = :gameId")
            .expressionAttributeValues(Map.of(
                ":userId", AttributeValue.builder().s(userId).build(),
                ":gameId", AttributeValue.builder().s(gameId).build()
            ))
            .build();
        
        List<Map<String, AttributeValue>> userScores = dynamoDbClient.query(userScoreRequest).items();
        if (userScores.isEmpty()) {
            return -1;
        }
        
        int userScore = Integer.parseInt(userScores.get(0).get("score").n());
        
        // Count users with higher scores
        QueryRequest rankRequest = QueryRequest.builder()
            .tableName("Leaderboard")
            .keyConditionExpression("gameId = :gameId")
            .filterExpression("score > :userScore")
            .expressionAttributeValues(Map.of(
                ":gameId", AttributeValue.builder().s(gameId).build(),
                ":userScore", AttributeValue.builder().n(String.valueOf(userScore)).build()
            ))
            .select(Select.COUNT)
            .build();
        
        return dynamoDbClient.query(rankRequest).count() + 1;
    }
}
```

---

## IoT Data Storage

### Table Design

```java
// IoT Telemetry Table
PK: deviceId (HASH)
SK: timestamp (RANGE)

// TTL enabled for automatic data expiration
```

### Implementation

```java
@Service
public class IoTDataService {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    // Store telemetry data
    public void storeTelemetry(String deviceId, Map<String, Object> metrics) {
        long timestamp = System.currentTimeMillis();
        long ttl = Instant.now().plus(30, ChronoUnit.DAYS).getEpochSecond();
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("deviceId", AttributeValue.builder().s(deviceId).build());
        item.put("timestamp", AttributeValue.builder().n(String.valueOf(timestamp)).build());
        item.put("ttl", AttributeValue.builder().n(String.valueOf(ttl)).build());
        
        metrics.forEach((key, value) -> {
            if (value instanceof Number) {
                item.put(key, AttributeValue.builder().n(value.toString()).build());
            } else {
                item.put(key, AttributeValue.builder().s(value.toString()).build());
            }
        });
        
        PutItemRequest request = PutItemRequest.builder()
            .tableName("IoTTelemetry")
            .item(item)
            .build();
        
        dynamoDbClient.putItem(request);
    }
    
    // Batch store telemetry
    public void batchStoreTelemetry(List<TelemetryData> dataList) {
        List<WriteRequest> writeRequests = dataList.stream()
            .map(data -> {
                Map<String, AttributeValue> item = convertToItem(data);
                return WriteRequest.builder()
                    .putRequest(PutRequest.builder().item(item).build())
                    .build();
            })
            .collect(Collectors.toList());
        
        // Split into batches of 25
        Lists.partition(writeRequests, 25).forEach(batch -> {
            BatchWriteItemRequest request = BatchWriteItemRequest.builder()
                .requestItems(Map.of("IoTTelemetry", batch))
                .build();
            
            dynamoDbClient.batchWriteItem(request);
        });
    }
    
    // Query device data
    public List<TelemetryData> getDeviceData(String deviceId, long startTime, long endTime) {
        QueryRequest request = QueryRequest.builder()
            .tableName("IoTTelemetry")
            .keyConditionExpression("deviceId = :deviceId AND #ts BETWEEN :start AND :end")
            .expressionAttributeNames(Map.of("#ts", "timestamp"))
            .expressionAttributeValues(Map.of(
                ":deviceId", AttributeValue.builder().s(deviceId).build(),
                ":start", AttributeValue.builder().n(String.valueOf(startTime)).build(),
                ":end", AttributeValue.builder().n(String.valueOf(endTime)).build()
            ))
            .build();
        
        return dynamoDbClient.query(request).items().stream()
            .map(this::mapToTelemetryData)
            .collect(Collectors.toList());
    }
    
    // Aggregate metrics
    public DeviceMetrics getAggregatedMetrics(String deviceId, long startTime, long endTime) {
        List<TelemetryData> data = getDeviceData(deviceId, startTime, endTime);
        
        DoubleSummaryStatistics tempStats = data.stream()
            .mapToDouble(TelemetryData::getTemperature)
            .summaryStatistics();
        
        return DeviceMetrics.builder()
            .deviceId(deviceId)
            .avgTemperature(tempStats.getAverage())
            .minTemperature(tempStats.getMin())
            .maxTemperature(tempStats.getMax())
            .dataPoints(tempStats.getCount())
            .build();
    }
}
```

---

## Session Management

### Table Design

```java
// Sessions Table
PK: sessionId (HASH)
TTL: expirationTime

Attributes:
- userId
- createdAt
- lastAccessedAt
- ipAddress
- userAgent
```

### Implementation

```java
@Service
public class SessionService {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    
    // Create session
    public Session createSession(String userId, String ipAddress, String userAgent) {
        String sessionId = UUID.randomUUID().toString();
        long expirationTime = Instant.now()
            .plus(SESSION_TIMEOUT_MINUTES, ChronoUnit.MINUTES)
            .getEpochSecond();
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("sessionId", AttributeValue.builder().s(sessionId).build());
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());
        item.put("lastAccessedAt", AttributeValue.builder().s(Instant.now().toString()).build());
        item.put("expirationTime", AttributeValue.builder().n(String.valueOf(expirationTime)).build());
        item.put("ipAddress", AttributeValue.builder().s(ipAddress).build());
        item.put("userAgent", AttributeValue.builder().s(userAgent).build());
        
        PutItemRequest request = PutItemRequest.builder()
            .tableName("Sessions")
            .item(item)
            .build();
        
        dynamoDbClient.putItem(request);
        
        return Session.builder()
            .sessionId(sessionId)
            .userId(userId)
            .expirationTime(expirationTime)
            .build();
    }
    
    // Validate and refresh session
    public Session validateSession(String sessionId) {
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
            .tableName("Sessions")
            .key(Map.of("sessionId", AttributeValue.builder().s(sessionId).build()))
            .build());
        
        if (!response.hasItem()) {
            throw new SessionNotFoundException("Session not found");
        }
        
        Map<String, AttributeValue> item = response.item();
        long expirationTime = Long.parseLong(item.get("expirationTime").n());
        
        if (Instant.now().getEpochSecond() > expirationTime) {
            throw new SessionExpiredException("Session expired");
        }
        
        // Refresh session
        long newExpirationTime = Instant.now()
            .plus(SESSION_TIMEOUT_MINUTES, ChronoUnit.MINUTES)
            .getEpochSecond();
        
        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
            .tableName("Sessions")
            .key(Map.of("sessionId", AttributeValue.builder().s(sessionId).build()))
            .updateExpression("SET lastAccessedAt = :now, expirationTime = :expiration")
            .expressionAttributeValues(Map.of(
                ":now", AttributeValue.builder().s(Instant.now().toString()).build(),
                ":expiration", AttributeValue.builder().n(String.valueOf(newExpirationTime)).build()
            ))
            .build();
        
        dynamoDbClient.updateItem(updateRequest);
        
        return mapToSession(item);
    }
    
    // Delete session (logout)
    public void deleteSession(String sessionId) {
        DeleteItemRequest request = DeleteItemRequest.builder()
            .tableName("Sessions")
            .key(Map.of("sessionId", AttributeValue.builder().s(sessionId).build()))
            .build();
        
        dynamoDbClient.deleteItem(request);
    }
}
```

---

## Social Media Feed

### Table Design

```java
// Posts Table
PK: userId (HASH)
SK: timestamp#postId (RANGE)

// GSI: FollowerFeedIndex
PK: followerId (HASH)
SK: timestamp (RANGE)
```

### Implementation

```java
@Service
public class SocialFeedService {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    // Create post
    public Post createPost(String userId, String content, List<String> mediaUrls) {
        String postId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();
        String sortKey = timestamp + "#" + postId;
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("timestamp#postId", AttributeValue.builder().s(sortKey).build());
        item.put("postId", AttributeValue.builder().s(postId).build());
        item.put("content", AttributeValue.builder().s(content).build());
        item.put("timestamp", AttributeValue.builder().s(timestamp).build());
        item.put("likes", AttributeValue.builder().n("0").build());
        item.put("comments", AttributeValue.builder().n("0").build());
        
        if (!mediaUrls.isEmpty()) {
            item.put("mediaUrls", AttributeValue.builder().ss(mediaUrls).build());
        }
        
        PutItemRequest request = PutItemRequest.builder()
            .tableName("Posts")
            .item(item)
            .build();
        
        dynamoDbClient.putItem(request);
        
        // Fan-out to followers
        fanOutToFollowers(userId, postId, timestamp);
        
        return mapToPost(item);
    }
    
    // Fan-out post to followers
    private void fanOutToFollowers(String userId, String postId, String timestamp) {
        List<String> followers = getFollowers(userId);
        
        List<WriteRequest> writeRequests = followers.stream()
            .map(followerId -> {
                Map<String, AttributeValue> feedItem = new HashMap<>();
                feedItem.put("followerId", AttributeValue.builder().s(followerId).build());
                feedItem.put("timestamp", AttributeValue.builder().s(timestamp).build());
                feedItem.put("postId", AttributeValue.builder().s(postId).build());
                feedItem.put("userId", AttributeValue.builder().s(userId).build());
                
                return WriteRequest.builder()
                    .putRequest(PutRequest.builder().item(feedItem).build())
                    .build();
            })
            .collect(Collectors.toList());
        
        // Batch write to feed table
        Lists.partition(writeRequests, 25).forEach(batch -> {
            BatchWriteItemRequest request = BatchWriteItemRequest.builder()
                .requestItems(Map.of("Feed", batch))
                .build();
            
            dynamoDbClient.batchWriteItem(request);
        });
    }
    
    // Get user feed
    public List<Post> getUserFeed(String userId, int limit, String lastEvaluatedKey) {
        QueryRequest.Builder requestBuilder = QueryRequest.builder()
            .tableName("Feed")
            .indexName("FollowerFeedIndex")
            .keyConditionExpression("followerId = :followerId")
            .expressionAttributeValues(Map.of(
                ":followerId", AttributeValue.builder().s(userId).build()
            ))
            .scanIndexForward(false)
            .limit(limit);
        
        if (lastEvaluatedKey != null) {
            requestBuilder.exclusiveStartKey(Map.of(
                "followerId", AttributeValue.builder().s(userId).build(),
                "timestamp", AttributeValue.builder().s(lastEvaluatedKey).build()
            ));
        }
        
        QueryResponse response = dynamoDbClient.query(requestBuilder.build());
        
        // Batch get actual posts
        List<String> postIds = response.items().stream()
            .map(item -> item.get("postId").s())
            .collect(Collectors.toList());
        
        return batchGetPosts(postIds);
    }
    
    // Like post (atomic increment)
    public void likePost(String userId, String postId) {
        UpdateItemRequest request = UpdateItemRequest.builder()
            .tableName("Posts")
            .key(Map.of(
                "userId", AttributeValue.builder().s(userId).build(),
                "timestamp#postId", AttributeValue.builder().s(getPostSortKey(postId)).build()
            ))
            .updateExpression("ADD likes :inc")
            .expressionAttributeValues(Map.of(
                ":inc", AttributeValue.builder().n("1").build()
            ))
            .build();
        
        dynamoDbClient.updateItem(request);
    }
}
```

---

## Real-Time Analytics

### Table Design

```java
// Analytics Table
PK: metricName#date (HASH)
SK: timestamp (RANGE)

// Aggregated metrics stored separately
```

### Implementation

```java
@Service
public class AnalyticsService {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    // Record event
    public void recordEvent(String eventType, Map<String, Object> properties) {
        String date = LocalDate.now().toString();
        String pk = eventType + "#" + date;
        long timestamp = System.currentTimeMillis();
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("metricName#date", AttributeValue.builder().s(pk).build());
        item.put("timestamp", AttributeValue.builder().n(String.valueOf(timestamp)).build());
        
        properties.forEach((key, value) -> {
            if (value instanceof Number) {
                item.put(key, AttributeValue.builder().n(value.toString()).build());
            } else {
                item.put(key, AttributeValue.builder().s(value.toString()).build());
            }
        });
        
        PutItemRequest request = PutItemRequest.builder()
            .tableName("Analytics")
            .item(item)
            .build();
        
        dynamoDbClient.putItem(request);
    }
    
    // Get metrics for date range
    public List<AnalyticsEvent> getMetrics(String eventType, LocalDate startDate, LocalDate endDate) {
        List<AnalyticsEvent> allEvents = new ArrayList<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String pk = eventType + "#" + date.toString();
            
            QueryRequest request = QueryRequest.builder()
                .tableName("Analytics")
                .keyConditionExpression("metricName#date = :pk")
                .expressionAttributeValues(Map.of(
                    ":pk", AttributeValue.builder().s(pk).build()
                ))
                .build();
            
            List<AnalyticsEvent> events = dynamoDbClient.query(request).items().stream()
                .map(this::mapToAnalyticsEvent)
                .collect(Collectors.toList());
            
            allEvents.addAll(events);
        }
        
        return allEvents;
    }
    
    // Aggregate metrics (run periodically)
    public void aggregateMetrics(String eventType, LocalDate date) {
        String pk = eventType + "#" + date.toString();
        
        QueryRequest request = QueryRequest.builder()
            .tableName("Analytics")
            .keyConditionExpression("metricName#date = :pk")
            .expressionAttributeValues(Map.of(
                ":pk", AttributeValue.builder().s(pk).build()
            ))
            .build();
        
        List<Map<String, AttributeValue>> items = dynamoDbClient.query(request).items();
        
        // Calculate aggregates
        long count = items.size();
        double sum = items.stream()
            .mapToDouble(item -> Double.parseDouble(item.get("value").n()))
            .sum();
        double avg = sum / count;
        
        // Store aggregated metrics
        Map<String, AttributeValue> aggregateItem = new HashMap<>();
        aggregateItem.put("metricName#date", AttributeValue.builder().s(pk + "#aggregate").build());
        aggregateItem.put("count", AttributeValue.builder().n(String.valueOf(count)).build());
        aggregateItem.put("sum", AttributeValue.builder().n(String.valueOf(sum)).build());
        aggregateItem.put("average", AttributeValue.builder().n(String.valueOf(avg)).build());
        
        PutItemRequest putRequest = PutItemRequest.builder()
            .tableName("AggregatedMetrics")
            .item(aggregateItem)
            .build();
        
        dynamoDbClient.putItem(putRequest);
    }
}
```

---

## Best Practices Summary

### 1. Choose Right Partition Key
- High cardinality
- Uniform access pattern
- Avoid hot partitions

### 2. Use Composite Sort Keys
- Enable range queries
- Support hierarchical data
- Reduce GSI count

### 3. Implement TTL
- Automatic data expiration
- Reduce storage costs
- No manual cleanup needed

### 4. Use Transactions Wisely
- ACID guarantees
- Up to 100 items
- Higher cost (2x WCU)

### 5. Optimize with GSI
- Query flexibility
- Sparse indexes
- Eventually consistent

### 6. Batch Operations
- Reduce API calls
- Better throughput
- Lower costs

### 7. Enable Streams
- Event-driven architecture
- Real-time processing
- Data replication

### 8. Monitor and Alert
- CloudWatch metrics
- Throttling alerts
- Cost optimization

---

## Conclusion

DynamoDB is ideal for:
- High-scale applications
- Low-latency requirements
- Flexible schema needs
- Serverless architectures
- Global distribution

Choose DynamoDB when you need predictable performance at any scale!
