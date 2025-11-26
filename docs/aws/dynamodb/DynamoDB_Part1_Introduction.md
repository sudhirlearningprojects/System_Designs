# Amazon DynamoDB - Deep Dive (Part 1: Introduction & Core Concepts)

## Table of Contents
1. [Introduction](#introduction)
2. [Core Concepts](#core-concepts)
3. [Data Model](#data-model)
4. [Primary Keys](#primary-keys)
5. [Capacity Modes](#capacity-modes)
6. [Consistency Models](#consistency-models)

---

## Introduction

**Amazon DynamoDB** is a fully managed, serverless NoSQL database service that provides fast and predictable performance with seamless scalability. It's designed for applications that need consistent, single-digit millisecond latency at any scale.

### Key Features

- **Fully Managed**: No servers to provision, patch, or manage
- **Serverless**: Automatic scaling based on demand
- **High Performance**: Single-digit millisecond latency
- **High Availability**: 99.99% availability SLA (99.999% with Global Tables)
- **Flexible Schema**: No fixed schema required
- **Built-in Security**: Encryption at rest and in transit
- **Event-driven**: DynamoDB Streams for change data capture

### When to Use DynamoDB

✅ **Use DynamoDB when:**
- Need consistent, low-latency performance at scale
- Building serverless applications
- Require flexible schema
- Need automatic scaling
- Building real-time applications
- Require global distribution

❌ **Don't use DynamoDB when:**
- Need complex joins and aggregations
- Require ACID transactions across multiple tables
- Need full SQL support
- Have unpredictable access patterns
- Require ad-hoc queries

---

## Core Concepts

### Tables

A table is a collection of items. Each table must have a primary key.

```java
// Create table
CreateTableRequest request = CreateTableRequest.builder()
    .tableName("Users")
    .keySchema(
        KeySchemaElement.builder()
            .attributeName("userId")
            .keyType(KeyType.HASH)
            .build()
    )
    .attributeDefinitions(
        AttributeDefinition.builder()
            .attributeName("userId")
            .attributeType(ScalarAttributeType.S)
            .build()
    )
    .billingMode(BillingMode.PAY_PER_REQUEST)
    .build();

dynamoDbClient.createTable(request);
```

### Items

An item is a collection of attributes (similar to a row in relational databases).

```java
// Put item
Map<String, AttributeValue> item = new HashMap<>();
item.put("userId", AttributeValue.builder().s("user123").build());
item.put("name", AttributeValue.builder().s("John Doe").build());
item.put("email", AttributeValue.builder().s("john@example.com").build());
item.put("age", AttributeValue.builder().n("30").build());

PutItemRequest putRequest = PutItemRequest.builder()
    .tableName("Users")
    .item(item)
    .build();

dynamoDbClient.putItem(putRequest);
```

### Attributes

An attribute is a fundamental data element (similar to a column).

**Supported Data Types:**
- **Scalar**: String (S), Number (N), Binary (B), Boolean (BOOL), Null (NULL)
- **Document**: List (L), Map (M)
- **Set**: String Set (SS), Number Set (NS), Binary Set (BS)

---

## Data Model

### Example: E-Commerce Application

```java
// Order item structure
{
    "orderId": "ORD-12345",              // Partition Key
    "customerId": "CUST-789",
    "orderDate": "2024-01-15T10:30:00Z",
    "status": "SHIPPED",
    "totalAmount": 299.99,
    "items": [
        {
            "productId": "PROD-001",
            "name": "Laptop",
            "quantity": 1,
            "price": 299.99
        }
    ],
    "shippingAddress": {
        "street": "123 Main St",
        "city": "Seattle",
        "state": "WA",
        "zipCode": "98101"
    }
}
```

### Nested Attributes

```java
// Create item with nested attributes
Map<String, AttributeValue> address = new HashMap<>();
address.put("street", AttributeValue.builder().s("123 Main St").build());
address.put("city", AttributeValue.builder().s("Seattle").build());

Map<String, AttributeValue> item = new HashMap<>();
item.put("orderId", AttributeValue.builder().s("ORD-12345").build());
item.put("shippingAddress", AttributeValue.builder().m(address).build());
```

---

## Primary Keys

### 1. Partition Key (Simple Primary Key)

Single attribute that uniquely identifies an item.

```java
// Table with partition key only
CreateTableRequest request = CreateTableRequest.builder()
    .tableName("Products")
    .keySchema(
        KeySchemaElement.builder()
            .attributeName("productId")
            .keyType(KeyType.HASH)
            .build()
    )
    .attributeDefinitions(
        AttributeDefinition.builder()
            .attributeName("productId")
            .attributeType(ScalarAttributeType.S)
            .build()
    )
    .billingMode(BillingMode.PAY_PER_REQUEST)
    .build();
```

### 2. Composite Primary Key (Partition Key + Sort Key)

Combination of partition key and sort key for unique identification.

```java
// Table with composite key
CreateTableRequest request = CreateTableRequest.builder()
    .tableName("Orders")
    .keySchema(
        KeySchemaElement.builder()
            .attributeName("customerId")
            .keyType(KeyType.HASH)
            .build(),
        KeySchemaElement.builder()
            .attributeName("orderDate")
            .keyType(KeyType.RANGE)
            .build()
    )
    .attributeDefinitions(
        AttributeDefinition.builder()
            .attributeName("customerId")
            .attributeType(ScalarAttributeType.S)
            .build(),
        AttributeDefinition.builder()
            .attributeName("orderDate")
            .attributeType(ScalarAttributeType.S)
            .build()
    )
    .billingMode(BillingMode.PAY_PER_REQUEST)
    .build();
```

### Key Design Best Practices

**Good Partition Key:**
- High cardinality (many unique values)
- Uniform access pattern
- Evenly distributed requests

```java
// GOOD: User ID as partition key
"userId": "user-12345"

// BAD: Status as partition key (low cardinality)
"status": "ACTIVE"  // Only few values: ACTIVE, INACTIVE
```

**Good Sort Key:**
- Enables range queries
- Supports hierarchical data
- Allows sorting

```java
// GOOD: Timestamp as sort key
"orderDate": "2024-01-15T10:30:00Z"

// GOOD: Hierarchical sort key
"category#subcategory": "Electronics#Laptops"
```

---

## Capacity Modes

### 1. On-Demand Mode

Pay per request with automatic scaling.

```java
CreateTableRequest request = CreateTableRequest.builder()
    .tableName("Users")
    .billingMode(BillingMode.PAY_PER_REQUEST)
    .build();
```

**When to use:**
- Unpredictable workloads
- New applications with unknown traffic
- Spiky traffic patterns
- Serverless applications

**Pricing:**
- $1.25 per million write requests
- $0.25 per million read requests

### 2. Provisioned Mode

Specify read and write capacity units.

```java
CreateTableRequest request = CreateTableRequest.builder()
    .tableName("Users")
    .billingMode(BillingMode.PROVISIONED)
    .provisionedThroughput(ProvisionedThroughput.builder()
        .readCapacityUnits(5L)
        .writeCapacityUnits(5L)
        .build())
    .build();
```

**Capacity Units:**
- **1 RCU** = 1 strongly consistent read/sec (4 KB)
- **1 RCU** = 2 eventually consistent reads/sec (4 KB)
- **1 WCU** = 1 write/sec (1 KB)

**When to use:**
- Predictable workloads
- Consistent traffic
- Cost optimization with reserved capacity

### Auto Scaling

```java
// Enable auto scaling
PutScalingPolicyRequest request = PutScalingPolicyRequest.builder()
    .serviceNamespace(ServiceNamespace.DYNAMODB)
    .resourceId("table/Users")
    .scalableDimension(ScalableDimension.DYNAMODB_TABLE_READ_CAPACITY_UNITS)
    .policyName("MyScalingPolicy")
    .policyType(PolicyType.TARGET_TRACKING_SCALING)
    .targetTrackingScalingPolicyConfiguration(
        TargetTrackingScalingPolicyConfiguration.builder()
            .targetValue(70.0)  // 70% utilization
            .predefinedMetricSpecification(
                PredefinedMetricSpecification.builder()
                    .predefinedMetricType(MetricType.DYNAMO_DB_READ_CAPACITY_UTILIZATION)
                    .build())
            .build())
    .build();
```

---

## Consistency Models

### 1. Eventually Consistent Reads (Default)

Response might not reflect recently completed write.

```java
GetItemRequest request = GetItemRequest.builder()
    .tableName("Users")
    .key(Map.of("userId", AttributeValue.builder().s("user123").build()))
    .consistentRead(false)  // Eventually consistent
    .build();
```

**Characteristics:**
- Lower latency
- Higher throughput
- Lower cost (half the RCU)
- Data replicated across 3 AZs

### 2. Strongly Consistent Reads

Returns most up-to-date data.

```java
GetItemRequest request = GetItemRequest.builder()
    .tableName("Users")
    .key(Map.of("userId", AttributeValue.builder().s("user123").build()))
    .consistentRead(true)  // Strongly consistent
    .build();
```

**Characteristics:**
- Higher latency
- Lower throughput
- Higher cost (double the RCU)
- Guaranteed latest data

### 3. Transactional Reads/Writes

ACID transactions across multiple items.

```java
// Transactional write
TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
    .transactItems(
        TransactWriteItem.builder()
            .put(Put.builder()
                .tableName("Accounts")
                .item(Map.of(
                    "accountId", AttributeValue.builder().s("ACC-1").build(),
                    "balance", AttributeValue.builder().n("900").build()
                ))
                .build())
            .build(),
        TransactWriteItem.builder()
            .put(Put.builder()
                .tableName("Accounts")
                .item(Map.of(
                    "accountId", AttributeValue.builder().s("ACC-2").build(),
                    "balance", AttributeValue.builder().n("1100").build()
                ))
                .build())
            .build()
    )
    .build();

dynamoDbClient.transactWriteItems(request);
```

**Use Cases:**
- Financial transactions
- Inventory management
- Multi-item updates
- Atomic operations

---

## Performance Characteristics

### Latency

| Operation | Latency |
|-----------|---------|
| GetItem | < 10ms (p99) |
| PutItem | < 10ms (p99) |
| Query | < 10ms (p99) |
| Scan | Varies (full table) |
| BatchGetItem | < 10ms per batch |

### Throughput

- **On-Demand**: Unlimited (with throttling protection)
- **Provisioned**: Based on configured capacity
- **Burst Capacity**: 300 seconds of unused capacity

### Limits

| Resource | Limit |
|----------|-------|
| Item Size | 400 KB |
| Partition Key | 2048 bytes |
| Sort Key | 1024 bytes |
| Table Name | 3-255 characters |
| Attribute Name | 64 KB |
| Nested Depth | 32 levels |

---

## Cost Optimization

### 1. Choose Right Capacity Mode

```java
// On-Demand for unpredictable workloads
// Provisioned for predictable workloads with auto-scaling
```

### 2. Use Eventually Consistent Reads

```java
// Costs 50% less than strongly consistent reads
GetItemRequest request = GetItemRequest.builder()
    .consistentRead(false)
    .build();
```

### 3. Use Projection Expressions

```java
// Fetch only required attributes
GetItemRequest request = GetItemRequest.builder()
    .tableName("Users")
    .key(Map.of("userId", AttributeValue.builder().s("user123").build()))
    .projectionExpression("userId, name, email")  // Only fetch these
    .build();
```

### 4. Enable TTL for Temporary Data

```java
// Automatically delete expired items (no cost)
UpdateTimeToLiveRequest request = UpdateTimeToLiveRequest.builder()
    .tableName("Sessions")
    .timeToLiveSpecification(TimeToLiveSpecification.builder()
        .enabled(true)
        .attributeName("expirationTime")
        .build())
    .build();
```

### 5. Use DynamoDB Accelerator (DAX)

In-memory cache for microsecond latency.

```java
// DAX client
AmazonDaxClient daxClient = AmazonDaxClientBuilder.standard()
    .withEndpointConfiguration("mycluster.dax-clusters.us-east-1.amazonaws.com:8111")
    .build();
```

---

## Security Best Practices

### 1. IAM Policies

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:Query"
      ],
      "Resource": "arn:aws:dynamodb:us-east-1:123456789012:table/Users",
      "Condition": {
        "ForAllValues:StringEquals": {
          "dynamodb:LeadingKeys": ["${aws:username}"]
        }
      }
    }
  ]
}
```

### 2. Encryption at Rest

```java
// Enable encryption
CreateTableRequest request = CreateTableRequest.builder()
    .tableName("Users")
    .sseSpecification(SSESpecification.builder()
        .enabled(true)
        .sseType(SSEType.KMS)
        .kmsMasterKeyId("arn:aws:kms:us-east-1:123456789012:key/12345678")
        .build())
    .build();
```

### 3. VPC Endpoints

```java
// Access DynamoDB from VPC without internet gateway
// Configure VPC endpoint in AWS Console
```

### 4. Fine-Grained Access Control

```java
// Item-level permissions using IAM conditions
"Condition": {
    "ForAllValues:StringEquals": {
        "dynamodb:LeadingKeys": ["${cognito-identity.amazonaws.com:sub}"]
    }
}
```

---

## Monitoring and Observability

### CloudWatch Metrics

```java
// Key metrics to monitor
- ConsumedReadCapacityUnits
- ConsumedWriteCapacityUnits
- UserErrors (400 errors)
- SystemErrors (500 errors)
- ThrottledRequests
- ConditionalCheckFailedRequests
```

### CloudWatch Alarms

```java
PutMetricAlarmRequest request = PutMetricAlarmRequest.builder()
    .alarmName("DynamoDB-HighThrottle")
    .metricName("UserErrors")
    .namespace("AWS/DynamoDB")
    .statistic(Statistic.SUM)
    .period(300)
    .evaluationPeriods(2)
    .threshold(10.0)
    .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
    .build();
```

### X-Ray Tracing

```java
// Enable X-Ray for request tracing
@XRayEnabled
public class DynamoDBService {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    public User getUser(String userId) {
        // Automatically traced by X-Ray
        return dynamoDbClient.getItem(...);
    }
}
```

---

## Next Steps

Continue to:
- **Part 2**: Operations (CRUD, Queries, Scans, Indexes)
- **Part 3**: Advanced Features (Streams, Global Tables, Transactions)
- **Part 4**: Integration with AWS Services
- **Part 5**: Enterprise Use Cases and Patterns
