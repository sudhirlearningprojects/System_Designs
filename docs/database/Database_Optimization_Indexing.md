# Database Optimization and Indexing - Complete Guide

## Table of Contents
1. [Database Indexing](#database-indexing)
2. [MySQL Indexing](#mysql-indexing)
3. [MongoDB Indexing](#mongodb-indexing)
4. [Query Optimization](#query-optimization)
5. [Database Selection Guide](#database-selection-guide)
6. [Enterprise Database Use Cases](#enterprise-database-use-cases)

---

## Database Indexing

### What is an Index?

A data structure that improves query performance by creating a fast lookup path to data.

**Analogy:** Like a book index - instead of reading every page, you look up the index to find the page number.

### How Indexes Work

```
Without Index:
Table Scan - Read every row
Users: [1, John] [2, Jane] [3, Bob] [4, Alice] [5, Charlie]
Query: SELECT * FROM users WHERE name = 'Bob'
Scans: 5 rows (O(n))

With Index on 'name':
B-Tree Index: Alice -> Bob -> Charlie -> Jane -> John
Query: SELECT * FROM users WHERE name = 'Bob'
Scans: 1 row (O(log n))
```

### Index Types

**1. B-Tree Index (Default)**
- Balanced tree structure
- Good for range queries
- Most common type

**2. Hash Index**
- Fast equality lookups
- No range queries
- Memory-based

**3. Full-Text Index**
- Text search
- Word matching
- Relevance scoring

**4. Spatial Index**
- Geographic data
- Location queries
- R-Tree structure

---

## MySQL Indexing

### Creating Indexes

```sql
-- Single column index
CREATE INDEX idx_email ON users(email);

-- Composite index (multiple columns)
CREATE INDEX idx_name_age ON users(last_name, first_name, age);

-- Unique index
CREATE UNIQUE INDEX idx_username ON users(username);

-- Full-text index
CREATE FULLTEXT INDEX idx_description ON products(description);

-- Index on expression
CREATE INDEX idx_lower_email ON users((LOWER(email)));
```

### Viewing Indexes

```sql
-- Show all indexes on a table
SHOW INDEX FROM users;

-- Show index usage
EXPLAIN SELECT * FROM users WHERE email = 'john@example.com';
```

### Index Types in MySQL

#### 1. Primary Key Index

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,  -- Automatically creates clustered index
    name VARCHAR(100)
);

-- Or
CREATE TABLE users (
    id INT,
    name VARCHAR(100),
    PRIMARY KEY (id)
);
```

#### 2. Unique Index

```sql
-- Ensures uniqueness
CREATE UNIQUE INDEX idx_email ON users(email);

-- Or in table definition
CREATE TABLE users (
    id INT PRIMARY KEY,
    email VARCHAR(100) UNIQUE
);
```

#### 3. Composite Index

```sql
-- Index on multiple columns
CREATE INDEX idx_location ON users(country, state, city);

-- Order matters!
-- Good: WHERE country = 'USA' AND state = 'CA'
-- Good: WHERE country = 'USA'
-- Bad: WHERE state = 'CA' (doesn't use index)
```

#### 4. Covering Index

```sql
-- Index includes all columns in query
CREATE INDEX idx_user_info ON users(email, name, age);

-- This query uses only the index (no table lookup)
SELECT name, age FROM users WHERE email = 'john@example.com';
```

### MySQL Index Examples

#### Example 1: Simple Query Optimization

```sql
-- Without index
SELECT * FROM orders WHERE customer_id = 12345;
-- Scans all rows: 1,000,000 rows

-- Create index
CREATE INDEX idx_customer_id ON orders(customer_id);

-- With index
SELECT * FROM orders WHERE customer_id = 12345;
-- Scans only matching rows: 10 rows
```

#### Example 2: Composite Index

```sql
-- Create composite index
CREATE INDEX idx_order_date_status ON orders(order_date, status);

-- Uses index (both columns)
SELECT * FROM orders 
WHERE order_date = '2024-01-15' AND status = 'completed';

-- Uses index (first column only)
SELECT * FROM orders WHERE order_date = '2024-01-15';

-- Does NOT use index (second column only)
SELECT * FROM orders WHERE status = 'completed';
```

#### Example 3: Range Queries

```sql
-- Create index
CREATE INDEX idx_price ON products(price);

-- Uses index
SELECT * FROM products WHERE price BETWEEN 100 AND 500;
SELECT * FROM products WHERE price > 100;
SELECT * FROM products WHERE price < 500;
```

#### Example 4: Full-Text Search

```sql
-- Create full-text index
CREATE FULLTEXT INDEX idx_description ON products(description);

-- Full-text search
SELECT * FROM products 
WHERE MATCH(description) AGAINST('laptop computer' IN NATURAL LANGUAGE MODE);

-- Boolean mode
SELECT * FROM products 
WHERE MATCH(description) AGAINST('+laptop -gaming' IN BOOLEAN MODE);
```

### EXPLAIN Analysis

```sql
-- Analyze query execution
EXPLAIN SELECT * FROM users WHERE email = 'john@example.com';

-- Output:
-- id | select_type | table | type | possible_keys | key | rows | Extra
-- 1  | SIMPLE      | users | ref  | idx_email     | idx_email | 1 | Using index
```

**Key Columns:**
- `type`: Access type (const, ref, range, index, ALL)
- `possible_keys`: Available indexes
- `key`: Actual index used
- `rows`: Estimated rows scanned
- `Extra`: Additional info

### When to Use Indexes

✅ **Use indexes for:**
- Columns in WHERE clause
- Columns in JOIN conditions
- Columns in ORDER BY
- Columns in GROUP BY
- Foreign keys

❌ **Avoid indexes for:**
- Small tables (<1000 rows)
- Columns with low cardinality (few unique values)
- Frequently updated columns
- Columns rarely used in queries

---

## MongoDB Indexing

### Creating Indexes

```javascript
// Single field index
db.users.createIndex({ email: 1 })  // 1 = ascending, -1 = descending

// Compound index
db.users.createIndex({ lastName: 1, firstName: 1, age: 1 })

// Unique index
db.users.createIndex({ username: 1 }, { unique: true })

// Text index
db.products.createIndex({ description: "text" })

// Geospatial index
db.locations.createIndex({ coordinates: "2dsphere" })

// TTL index (auto-delete after time)
db.sessions.createIndex({ createdAt: 1 }, { expireAfterSeconds: 3600 })
```

### Viewing Indexes

```javascript
// List all indexes
db.users.getIndexes()

// Analyze query
db.users.find({ email: "john@example.com" }).explain("executionStats")
```

### MongoDB Index Types

#### 1. Single Field Index

```javascript
// Create index
db.users.createIndex({ email: 1 })

// Query using index
db.users.find({ email: "john@example.com" })

// Sort using index
db.users.find().sort({ email: 1 })
```

#### 2. Compound Index

```javascript
// Create compound index
db.orders.createIndex({ customerId: 1, orderDate: -1, status: 1 })

// Uses index (all fields)
db.orders.find({ customerId: 123, orderDate: ISODate("2024-01-15"), status: "completed" })

// Uses index (prefix)
db.orders.find({ customerId: 123 })
db.orders.find({ customerId: 123, orderDate: ISODate("2024-01-15") })

// Does NOT use index (non-prefix)
db.orders.find({ orderDate: ISODate("2024-01-15") })
db.orders.find({ status: "completed" })
```

#### 3. Multikey Index

```javascript
// Index on array field
db.products.createIndex({ tags: 1 })

// Document
{
  _id: 1,
  name: "Laptop",
  tags: ["electronics", "computers", "portable"]
}

// Query matches any array element
db.products.find({ tags: "electronics" })
```

#### 4. Text Index

```javascript
// Create text index
db.articles.createIndex({ title: "text", content: "text" })

// Text search
db.articles.find({ $text: { $search: "mongodb indexing" } })

// Text search with score
db.articles.find(
  { $text: { $search: "mongodb indexing" } },
  { score: { $meta: "textScore" } }
).sort({ score: { $meta: "textScore" } })
```

#### 5. Geospatial Index

```javascript
// Create 2dsphere index
db.places.createIndex({ location: "2dsphere" })

// Document
{
  _id: 1,
  name: "Coffee Shop",
  location: {
    type: "Point",
    coordinates: [-73.97, 40.77]  // [longitude, latitude]
  }
}

// Find nearby places
db.places.find({
  location: {
    $near: {
      $geometry: {
        type: "Point",
        coordinates: [-73.98, 40.78]
      },
      $maxDistance: 1000  // meters
    }
  }
})
```

#### 6. TTL Index

```javascript
// Auto-delete documents after 1 hour
db.sessions.createIndex({ createdAt: 1 }, { expireAfterSeconds: 3600 })

// Document
{
  _id: 1,
  userId: 123,
  createdAt: ISODate("2024-01-15T10:00:00Z")
}
// Automatically deleted after 1 hour
```

### MongoDB Index Examples

#### Example 1: E-commerce Product Search

```javascript
// Create indexes
db.products.createIndex({ category: 1, price: 1 })
db.products.createIndex({ name: "text", description: "text" })

// Query by category and price range
db.products.find({
  category: "Electronics",
  price: { $gte: 100, $lte: 500 }
}).sort({ price: 1 })

// Text search
db.products.find({
  $text: { $search: "laptop gaming" }
})
```

#### Example 2: User Activity Tracking

```javascript
// Create compound index
db.activities.createIndex({ userId: 1, timestamp: -1 })

// Query user's recent activities
db.activities.find({ userId: 123 })
  .sort({ timestamp: -1 })
  .limit(10)
```

#### Example 3: Location-Based Services

```javascript
// Create geospatial index
db.restaurants.createIndex({ location: "2dsphere" })

// Find restaurants within 5km
db.restaurants.find({
  location: {
    $near: {
      $geometry: {
        type: "Point",
        coordinates: [-122.4194, 37.7749]  // San Francisco
      },
      $maxDistance: 5000
    }
  }
})
```

### Explain Analysis

```javascript
// Analyze query
db.users.find({ email: "john@example.com" }).explain("executionStats")

// Output includes:
// - executionTimeMillis: Query execution time
// - totalDocsExamined: Documents scanned
// - totalKeysExamined: Index keys scanned
// - executionStages: Query execution plan
```

---

## Query Optimization

### MySQL Optimization Techniques

#### 1. Use EXPLAIN

```sql
EXPLAIN SELECT u.name, o.total
FROM users u
JOIN orders o ON u.id = o.user_id
WHERE u.country = 'USA' AND o.status = 'completed';
```

#### 2. Optimize JOINs

```sql
-- Bad: No indexes
SELECT * FROM orders o
JOIN users u ON o.user_id = u.id;

-- Good: Indexes on join columns
CREATE INDEX idx_user_id ON orders(user_id);
CREATE INDEX idx_id ON users(id);  -- Primary key already indexed
```

#### 3. Avoid SELECT *

```sql
-- Bad: Fetches all columns
SELECT * FROM users WHERE id = 1;

-- Good: Fetch only needed columns
SELECT id, name, email FROM users WHERE id = 1;
```

#### 4. Use LIMIT

```sql
-- Bad: Fetches all rows
SELECT * FROM orders ORDER BY created_at DESC;

-- Good: Limit results
SELECT * FROM orders ORDER BY created_at DESC LIMIT 10;
```

#### 5. Optimize Subqueries

```sql
-- Bad: Correlated subquery
SELECT name FROM users u
WHERE (SELECT COUNT(*) FROM orders WHERE user_id = u.id) > 5;

-- Good: Use JOIN
SELECT u.name FROM users u
JOIN (
  SELECT user_id, COUNT(*) as order_count
  FROM orders
  GROUP BY user_id
  HAVING order_count > 5
) o ON u.id = o.user_id;
```

### MongoDB Optimization Techniques

#### 1. Use Projection

```javascript
// Bad: Fetches all fields
db.users.find({ email: "john@example.com" })

// Good: Fetch only needed fields
db.users.find(
  { email: "john@example.com" },
  { name: 1, email: 1, _id: 0 }
)
```

#### 2. Use Covered Queries

```javascript
// Create covering index
db.users.createIndex({ email: 1, name: 1, age: 1 })

// Query uses only index (no document fetch)
db.users.find(
  { email: "john@example.com" },
  { name: 1, age: 1, _id: 0 }
)
```

#### 3. Use Aggregation Pipeline

```javascript
// Efficient aggregation
db.orders.aggregate([
  { $match: { status: "completed" } },
  { $group: { _id: "$customerId", total: { $sum: "$amount" } } },
  { $sort: { total: -1 } },
  { $limit: 10 }
])
```

#### 4. Avoid Large Skip Values

```javascript
// Bad: Slow for large skip values
db.products.find().skip(10000).limit(10)

// Good: Use range queries
db.products.find({ _id: { $gt: lastSeenId } }).limit(10)
```

---

## Database Selection Guide

### Relational Databases (SQL)

#### MySQL

**Best For:**
- Traditional web applications
- E-commerce platforms
- Content management systems
- Financial applications

**Strengths:**
- ACID compliance
- Strong consistency
- Complex joins
- Mature ecosystem

**Use Cases:**
```
✅ Banking systems
✅ E-commerce (orders, inventory)
✅ CRM systems
✅ ERP systems
```

**Example Schema:**
```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    total DECIMAL(10,2),
    status VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

#### PostgreSQL

**Best For:**
- Complex queries
- Data warehousing
- Geospatial applications
- JSON data with SQL

**Strengths:**
- Advanced features (JSON, arrays, full-text search)
- Better performance for complex queries
- Extensible (custom types, functions)
- Strong ACID compliance

**Use Cases:**
```
✅ Analytics platforms
✅ Geospatial applications (PostGIS)
✅ Time-series data
✅ Data warehousing
```

**Example:**
```sql
-- JSON support
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    data JSONB
);

INSERT INTO events (data) VALUES 
('{"user": "john", "action": "login", "timestamp": "2024-01-15"}');

SELECT data->>'user' FROM events WHERE data->>'action' = 'login';
```

---

### NoSQL Databases

#### MongoDB

**Best For:**
- Flexible schema
- Rapid development
- Document-oriented data
- Real-time applications

**Strengths:**
- Schema flexibility
- Horizontal scaling
- Rich query language
- Aggregation framework

**Use Cases:**
```
✅ Content management
✅ Catalogs
✅ User profiles
✅ Real-time analytics
```

**Example:**
```javascript
// Flexible schema
db.users.insertOne({
  name: "John Doe",
  email: "john@example.com",
  address: {
    street: "123 Main St",
    city: "San Francisco"
  },
  tags: ["premium", "verified"],
  metadata: {
    lastLogin: new Date(),
    preferences: { theme: "dark" }
  }
})
```

---

#### Redis

**Best For:**
- Caching
- Session storage
- Real-time analytics
- Message queues

**Strengths:**
- In-memory (extremely fast)
- Rich data structures
- Pub/Sub messaging
- TTL support

**Use Cases:**
```
✅ Session management
✅ Caching layer
✅ Rate limiting
✅ Real-time leaderboards
```

**Example:**
```redis
# Cache user data
SET user:1000 '{"name":"John","email":"john@example.com"}' EX 3600

# Session storage
SETEX session:abc123 1800 '{"userId":1000,"role":"admin"}'

# Rate limiting
INCR rate:user:1000
EXPIRE rate:user:1000 60

# Leaderboard
ZADD leaderboard 1500 "player1"
ZREVRANGE leaderboard 0 9 WITHSCORES
```

---

#### Cassandra

**Best For:**
- Time-series data
- High write throughput
- Distributed systems
- Always-on applications

**Strengths:**
- Linear scalability
- No single point of failure
- High availability
- Tunable consistency

**Use Cases:**
```
✅ IoT data
✅ Time-series metrics
✅ Messaging platforms
✅ Event logging
```

**Example:**
```cql
CREATE TABLE sensor_data (
    sensor_id UUID,
    timestamp TIMESTAMP,
    temperature DOUBLE,
    humidity DOUBLE,
    PRIMARY KEY (sensor_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);

INSERT INTO sensor_data (sensor_id, timestamp, temperature, humidity)
VALUES (uuid(), toTimestamp(now()), 25.5, 60.2);
```

---

#### Elasticsearch

**Best For:**
- Full-text search
- Log analytics
- Real-time search
- Application monitoring

**Strengths:**
- Powerful search capabilities
- Real-time indexing
- Aggregations
- Distributed architecture

**Use Cases:**
```
✅ Search engines
✅ Log analysis (ELK stack)
✅ Application monitoring
✅ Security analytics
```

**Example:**
```json
PUT /products/_doc/1
{
  "name": "Laptop",
  "description": "High-performance gaming laptop",
  "price": 1299.99,
  "category": "Electronics"
}

GET /products/_search
{
  "query": {
    "match": {
      "description": "gaming laptop"
    }
  }
}
```

---

## Enterprise Database Use Cases

### 1. E-Commerce Platform

**Architecture:**
```
MySQL (Primary)
├── Product catalog
├── Orders
├── Inventory
└── User accounts

MongoDB
├── Product reviews
├── User activity logs
└── Shopping cart sessions

Redis
├── Session storage
├── Product cache
└── Rate limiting

Elasticsearch
└── Product search
```

**Example:**
```sql
-- MySQL: Orders
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    total DECIMAL(10,2),
    status VARCHAR(50),
    INDEX idx_user_status (user_id, status)
);
```

```javascript
// MongoDB: Product reviews
db.reviews.createIndex({ productId: 1, rating: -1 })
db.reviews.find({ productId: "PROD123" }).sort({ rating: -1 })
```

---

### 2. Social Media Platform

**Architecture:**
```
PostgreSQL
├── User profiles
├── Relationships (followers)
└── Authentication

MongoDB
├── Posts/feeds
├── Comments
└── User activity

Redis
├── Timeline cache
├── Online users
└── Real-time notifications

Cassandra
└── Message history
```

---

### 3. Financial Services

**Architecture:**
```
PostgreSQL (Primary)
├── Accounts
├── Transactions
├── Audit logs
└── Compliance data

Redis
├── Real-time quotes
├── Session management
└── Rate limiting

Elasticsearch
└── Transaction search
```

**Example:**
```sql
-- PostgreSQL: Transactions with ACID
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
INSERT INTO transactions (from_account, to_account, amount) VALUES (1, 2, 100);
COMMIT;
```

---

### 4. IoT Platform

**Architecture:**
```
Cassandra (Primary)
└── Time-series sensor data

MongoDB
├── Device metadata
└── Configuration

Redis
├── Real-time metrics
└── Alert cache

Elasticsearch
└── Log analysis
```

**Example:**
```cql
-- Cassandra: Time-series data
CREATE TABLE sensor_readings (
    device_id UUID,
    reading_time TIMESTAMP,
    temperature DOUBLE,
    PRIMARY KEY (device_id, reading_time)
) WITH CLUSTERING ORDER BY (reading_time DESC);
```

---

### 5. Analytics Platform

**Architecture:**
```
PostgreSQL
└── Structured data warehouse

MongoDB
└── Raw event data

Redis
└── Real-time counters

Elasticsearch
└── Log aggregation

ClickHouse
└── OLAP queries
```

---

## Summary

### Index Best Practices

**MySQL:**
1. Index foreign keys
2. Use composite indexes wisely
3. Avoid over-indexing
4. Monitor index usage
5. Use covering indexes

**MongoDB:**
1. Index query patterns
2. Use compound indexes for multiple fields
3. Create indexes before bulk inserts
4. Monitor index size
5. Use TTL indexes for temporary data

### Database Selection Matrix

| Use Case | Primary DB | Cache | Search | Analytics |
|----------|-----------|-------|--------|-----------|
| E-commerce | MySQL | Redis | Elasticsearch | PostgreSQL |
| Social Media | PostgreSQL | Redis | Elasticsearch | Cassandra |
| IoT | Cassandra | Redis | Elasticsearch | ClickHouse |
| CMS | MongoDB | Redis | Elasticsearch | MongoDB |
| Banking | PostgreSQL | Redis | - | PostgreSQL |

### Performance Tips

1. **Index strategically** - Not too many, not too few
2. **Monitor queries** - Use EXPLAIN/explain()
3. **Cache frequently** - Use Redis for hot data
4. **Partition data** - Split large tables
5. **Optimize queries** - Avoid N+1, use joins wisely
6. **Scale horizontally** - Shard when needed
7. **Use read replicas** - Distribute read load

---

**Last Updated**: 2024
