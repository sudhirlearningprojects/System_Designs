# MongoDB Complete Guide - Part 4: Transactions, Replication & Sharding

## 📋 Table of Contents
1. [ACID Transactions](#acid-transactions)
2. [Replication](#replication)
3. [Sharding](#sharding)
4. [Data Modeling](#data-modeling)
5. [Production Best Practices](#production-best-practices)

---

## ACID Transactions

MongoDB supports multi-document ACID transactions (since v4.0).

### Single Document Transactions
```javascript
// Single document operations are always atomic
db.accounts.updateOne(
  { _id: "account1" },
  { $inc: { balance: -100 } }
)
```

### Multi-Document Transactions
```javascript
// Start session
const session = db.getMongo().startSession()

try {
  session.startTransaction()
  
  // Debit from account1
  db.accounts.updateOne(
    { _id: "account1" },
    { $inc: { balance: -100 } },
    { session }
  )
  
  // Credit to account2
  db.accounts.updateOne(
    { _id: "account2" },
    { $inc: { balance: 100 } },
    { session }
  )
  
  // Commit transaction
  session.commitTransaction()
  
} catch (error) {
  // Rollback on error
  session.abortTransaction()
  throw error
} finally {
  session.endSession()
}
```

### Spring Boot Transaction Example
```java
@Service
public class TransferService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Transactional
    public void transferMoney(String fromAccount, String toAccount, double amount) {
        // Debit
        Query debitQuery = Query.query(Criteria.where("_id").is(fromAccount));
        Update debitUpdate = new Update().inc("balance", -amount);
        mongoTemplate.updateFirst(debitQuery, debitUpdate, Account.class);
        
        // Credit
        Query creditQuery = Query.query(Criteria.where("_id").is(toAccount));
        Update creditUpdate = new Update().inc("balance", amount);
        mongoTemplate.updateFirst(creditQuery, creditUpdate, Account.class);
        
        // Both operations commit or rollback together
    }
}
```

### Transaction Best Practices
```javascript
// ✅ DO: Keep transactions short
// ✅ DO: Use transactions only when necessary
// ✅ DO: Handle retries for transient errors
// ❌ DON'T: Hold transactions for long operations
// ❌ DON'T: Use transactions for single document updates
```

---

## Replication

Replica sets provide high availability and data redundancy.

### Replica Set Architecture
```
┌─────────────┐
│   Primary   │ ◄─── Writes
└──────┬──────┘
       │
       ├──────────┬──────────┐
       ▼          ▼          ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│Secondary │ │Secondary │ │Secondary │ ◄─── Reads (optional)
└──────────┘ └──────────┘ └──────────┘
```

### Initialize Replica Set
```javascript
// Connect to MongoDB instance
mongosh --port 27017

// Initialize replica set
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "mongo1:27017" },
    { _id: 1, host: "mongo2:27017" },
    { _id: 2, host: "mongo3:27017" }
  ]
})

// Check status
rs.status()

// Check configuration
rs.conf()
```

### Docker Compose Replica Set
```yaml
version: '3.8'
services:
  mongo1:
    image: mongo:7.0
    command: mongod --replSet rs0 --port 27017
    ports:
      - "27017:27017"
    volumes:
      - mongo1_data:/data/db
    networks:
      - mongo-cluster

  mongo2:
    image: mongo:7.0
    command: mongod --replSet rs0 --port 27017
    ports:
      - "27018:27017"
    volumes:
      - mongo2_data:/data/db
    networks:
      - mongo-cluster

  mongo3:
    image: mongo:7.0
    command: mongod --replSet rs0 --port 27017
    ports:
      - "27019:27017"
    volumes:
      - mongo3_data:/data/db
    networks:
      - mongo-cluster

volumes:
  mongo1_data:
  mongo2_data:
  mongo3_data:

networks:
  mongo-cluster:
```

### Read Preferences
```javascript
// Primary (default) - all reads from primary
db.users.find().readPref("primary")

// PrimaryPreferred - primary if available, else secondary
db.users.find().readPref("primaryPreferred")

// Secondary - read from secondary only
db.users.find().readPref("secondary")

// SecondaryPreferred - secondary if available, else primary
db.users.find().readPref("secondaryPreferred")

// Nearest - lowest network latency
db.users.find().readPref("nearest")
```

### Write Concerns
```javascript
// Majority - acknowledged by majority of replica set
db.orders.insertOne(
  { userId: "user123", amount: 100 },
  { writeConcern: { w: "majority", wtimeout: 5000 } }
)

// w: 1 - acknowledged by primary only (default)
db.orders.insertOne(
  { userId: "user123", amount: 100 },
  { writeConcern: { w: 1 } }
)

// w: 2 - acknowledged by primary + 1 secondary
db.orders.insertOne(
  { userId: "user123", amount: 100 },
  { writeConcern: { w: 2 } }
)
```

### Automatic Failover
```javascript
// When primary fails:
// 1. Secondaries detect failure (10 seconds)
// 2. Election starts
// 3. New primary elected (majority vote)
// 4. Applications reconnect automatically

// Check current primary
rs.status().members.find(m => m.stateStr === "PRIMARY")

// Step down primary (for maintenance)
rs.stepDown(60) // 60 seconds
```

---

## Sharding

Sharding distributes data across multiple servers for horizontal scaling.

### Sharding Architecture
```
┌──────────────┐
│   mongos     │ ◄─── Application
│  (Router)    │
└──────┬───────┘
       │
       ├────────────┬────────────┐
       ▼            ▼            ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│ Shard 1  │  │ Shard 2  │  │ Shard 3  │
│(Replica) │  │(Replica) │  │(Replica) │
└──────────┘  └──────────┘  └──────────┘
       │
       ▼
┌──────────────┐
│Config Servers│
│ (Metadata)   │
└──────────────┘
```

### Enable Sharding
```javascript
// Connect to mongos
mongosh --port 27017

// Enable sharding on database
sh.enableSharding("ecommerce")

// Shard collection by hash
sh.shardCollection(
  "ecommerce.users",
  { userId: "hashed" }
)

// Shard collection by range
sh.shardCollection(
  "ecommerce.orders",
  { userId: 1, createdAt: 1 }
)

// Check sharding status
sh.status()
```

### Shard Key Selection

**Good Shard Keys:**
- High cardinality (many unique values)
- Even distribution
- Query isolation (queries target single shard)

```javascript
// ✅ GOOD: User ID (hash-based)
sh.shardCollection("users", { userId: "hashed" })

// ✅ GOOD: Composite key
sh.shardCollection("orders", { userId: 1, orderId: 1 })

// ❌ BAD: Low cardinality
sh.shardCollection("orders", { status: 1 }) // Only few statuses

// ❌ BAD: Monotonically increasing
sh.shardCollection("orders", { _id: 1 }) // All writes to one shard
```

### Chunk Management
```javascript
// View chunks
db.getSiblingDB("config").chunks.find({ ns: "ecommerce.users" })

// Split chunk manually
sh.splitAt("ecommerce.users", { userId: "user50000" })

// Move chunk between shards
sh.moveChunk(
  "ecommerce.users",
  { userId: "user50000" },
  "shard02"
)

// Enable/disable balancer
sh.stopBalancer()
sh.startBalancer()
sh.getBalancerState()
```

### Targeted vs Broadcast Queries
```javascript
// ✅ Targeted query (uses shard key)
db.users.find({ userId: "user123" })
// Queries only one shard

// ❌ Broadcast query (no shard key)
db.users.find({ email: "john@example.com" })
// Queries all shards
```

---

## Data Modeling

### Embedded vs Referenced

**Embedded Documents (Denormalization)**
```javascript
// One-to-Few: Embed addresses in user
{
  _id: "user123",
  name: "John Doe",
  addresses: [
    { type: "home", street: "123 Main St", city: "NYC" },
    { type: "work", street: "456 Office Blvd", city: "SF" }
  ]
}

// Pros: Single query, atomic updates
// Cons: Document size limit (16MB), data duplication
```

**Referenced Documents (Normalization)**
```javascript
// One-to-Many: Reference orders from user
// Users collection
{
  _id: "user123",
  name: "John Doe"
}

// Orders collection
{
  _id: "order1",
  userId: "user123",
  amount: 100
}

// Pros: No duplication, smaller documents
// Cons: Multiple queries, no atomic updates across collections
```

### Design Patterns

#### 1. Subset Pattern
```javascript
// Store only recent reviews in product
{
  _id: "product123",
  name: "Laptop",
  recentReviews: [
    { userId: "user1", rating: 5, text: "Great!" },
    { userId: "user2", rating: 4, text: "Good" }
  ],
  reviewCount: 1523,
  avgRating: 4.5
}

// All reviews in separate collection
{
  _id: "review1",
  productId: "product123",
  userId: "user1",
  rating: 5,
  text: "Great laptop!"
}
```

#### 2. Bucket Pattern
```javascript
// Time-series data bucketed by hour
{
  _id: "sensor1_2024-01-15-10",
  sensorId: "sensor1",
  date: ISODate("2024-01-15T10:00:00Z"),
  measurements: [
    { time: "10:00", temp: 22.5 },
    { time: "10:01", temp: 22.6 },
    { time: "10:02", temp: 22.4 }
  ],
  count: 60,
  avgTemp: 22.5
}
```

#### 3. Computed Pattern
```javascript
// Pre-compute aggregations
{
  _id: "user123",
  name: "John Doe",
  stats: {
    totalOrders: 45,
    totalSpent: 5420.50,
    avgOrderValue: 120.45,
    lastOrderDate: ISODate("2024-01-15")
  }
}

// Update stats on each order
db.users.updateOne(
  { _id: "user123" },
  {
    $inc: { "stats.totalOrders": 1, "stats.totalSpent": 100 },
    $set: { "stats.lastOrderDate": new Date() }
  }
)
```

#### 4. Extended Reference Pattern
```javascript
// Store frequently accessed fields from referenced document
{
  _id: "order1",
  userId: "user123",
  userInfo: {
    name: "John Doe",
    email: "john@example.com"
  },
  items: [...],
  amount: 100
}

// Avoids lookup for common queries
```

---

## Production Best Practices

### 1. Connection Pooling
```yaml
# Spring Boot application.yml
spring:
  data:
    mongodb:
      uri: mongodb://mongo1:27017,mongo2:27017,mongo3:27017/mydb?replicaSet=rs0
      connection-pool:
        max-size: 100
        min-size: 10
        max-wait-time: 2000
        max-connection-life-time: 300000
        max-connection-idle-time: 60000
```

### 2. Monitoring
```javascript
// Enable profiling
db.setProfilingLevel(1, { slowms: 100 })

// View slow queries
db.system.profile.find({ millis: { $gt: 100 } }).sort({ ts: -1 })

// Server status
db.serverStatus()

// Current operations
db.currentOp()

// Kill long-running operation
db.killOp(opId)
```

### 3. Backup Strategies
```bash
# Mongodump (logical backup)
mongodump --uri="mongodb://localhost:27017/mydb" --out=/backup

# Restore
mongorestore --uri="mongodb://localhost:27017/mydb" /backup/mydb

# Point-in-time backup (replica set)
# Use oplog for continuous backup
mongodump --oplog --out=/backup

# Cloud backup (MongoDB Atlas)
# Automated continuous backups with point-in-time recovery
```

### 4. Security
```javascript
// Create admin user
use admin
db.createUser({
  user: "admin",
  pwd: "securePassword",
  roles: ["root"]
})

// Create application user
use mydb
db.createUser({
  user: "appUser",
  pwd: "appPassword",
  roles: [
    { role: "readWrite", db: "mydb" }
  ]
})

// Enable authentication
// mongod.conf
security:
  authorization: enabled

// Enable TLS/SSL
net:
  tls:
    mode: requireTLS
    certificateKeyFile: /path/to/cert.pem
```

### 5. Schema Validation
```javascript
// Add validation rules
db.createCollection("orders", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["userId", "items", "totalAmount", "status"],
      properties: {
        userId: {
          bsonType: "string",
          description: "User ID is required"
        },
        items: {
          bsonType: "array",
          minItems: 1,
          items: {
            bsonType: "object",
            required: ["productId", "quantity", "price"],
            properties: {
              productId: { bsonType: "string" },
              quantity: { bsonType: "int", minimum: 1 },
              price: { bsonType: "double", minimum: 0 }
            }
          }
        },
        totalAmount: {
          bsonType: "double",
          minimum: 0
        },
        status: {
          enum: ["pending", "processing", "completed", "cancelled"]
        }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
})
```

### 6. Capacity Planning
```javascript
// Calculate storage requirements
db.stats()
// {
//   dataSize: 1073741824,      // 1GB
//   storageSize: 2147483648,   // 2GB (with compression)
//   indexes: 536870912,        // 512MB
//   totalSize: 2684354560      // 2.5GB total
// }

// Estimate growth
// Daily inserts: 1M documents × 1KB = 1GB/day
// Monthly growth: 30GB
// Yearly growth: 365GB
// Plan for 3 years: ~1TB + 50% buffer = 1.5TB
```

### 7. Performance Tuning
```javascript
// Use projection to reduce network transfer
db.users.find(
  { status: "active" },
  { name: 1, email: 1, _id: 0 }
)

// Use lean queries (Mongoose)
User.find({ status: "active" }).lean()

// Batch operations
db.users.bulkWrite([
  { insertOne: { document: { name: "User1" } }},
  { updateOne: { filter: { _id: 1 }, update: { $set: { status: "active" } }}},
  { deleteOne: { filter: { _id: 2 } }}
])

// Use aggregation for complex queries
// Instead of multiple queries + application logic
```

---

## Spring Boot Integration

### Configuration
```java
@Configuration
public class MongoConfig {
    
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
    
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDbFactory) {
        return new MongoTemplate(mongoDbFactory);
    }
}
```

### Repository
```java
@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByEmail(String email);
    
    List<User> findByAgeGreaterThan(int age);
    
    @Query("{ 'address.city': ?0 }")
    List<User> findByCity(String city);
    
    @Aggregation(pipeline = {
        "{ $match: { status: 'active' } }",
        "{ $group: { _id: '$city', count: { $sum: 1 } } }"
    })
    List<CityCount> countByCity();
}
```

### Service with Transactions
```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Transactional
    public Order createOrder(OrderRequest request) {
        // Create order
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setItems(request.getItems());
        order.setStatus(OrderStatus.PENDING);
        order = orderRepository.save(order);
        
        // Update inventory
        for (OrderItem item : request.getItems()) {
            inventoryRepository.decrementStock(
                item.getProductId(),
                item.getQuantity()
            );
        }
        
        return order;
    }
}
```

---

## Next Steps

Continue to [Part 5: Spring Boot Integration](./05_MongoDB_Spring_Boot.md) for complete Spring Boot examples with MongoDB.
