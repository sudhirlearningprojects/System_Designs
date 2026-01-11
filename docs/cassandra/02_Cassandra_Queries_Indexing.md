# Apache Cassandra Complete Guide - Part 2: Advanced Queries & Indexing

## 📋 Table of Contents
1. [Secondary Indexes](#secondary-indexes)
2. [Materialized Views](#materialized-views)
3. [Advanced Query Patterns](#advanced-query-patterns)
4. [Consistency Levels](#consistency-levels)
5. [Performance Optimization](#performance-optimization)

---

## Secondary Indexes

### Create Secondary Index
```cql
-- Index on regular column
CREATE INDEX ON users (email);
CREATE INDEX ON products (category);

-- Named index
CREATE INDEX idx_users_age ON users (age);

-- Index on collection
CREATE INDEX ON products (tags);
CREATE INDEX ON products (KEYS(attributes));
CREATE INDEX ON products (VALUES(attributes));

-- Drop index
DROP INDEX idx_users_age;
```

### When to Use Secondary Indexes
```cql
-- ✅ GOOD: Low cardinality, small dataset
CREATE INDEX ON users (status);  -- Few statuses
SELECT * FROM users WHERE status = 'active';

-- ❌ BAD: High cardinality
CREATE INDEX ON users (user_id);  -- Use as partition key instead

-- ❌ BAD: High write throughput
-- Secondary indexes slow down writes
```

### SASI (SSTable Attached Secondary Index)
```cql
-- Better performance than regular secondary index
CREATE CUSTOM INDEX ON users (email)
USING 'org.apache.cassandra.index.sasi.SASIIndex'
WITH OPTIONS = {
    'mode': 'CONTAINS',
    'analyzer_class': 'org.apache.cassandra.index.sasi.analyzer.StandardAnalyzer',
    'case_sensitive': 'false'
};

-- Supports LIKE queries
SELECT * FROM users WHERE email LIKE '%@gmail.com';
```

---

## Materialized Views

### Create Materialized View
```cql
-- Base table
CREATE TABLE orders (
    user_id UUID,
    order_id TIMEUUID,
    status TEXT,
    total_amount DECIMAL,
    created_at TIMESTAMP,
    PRIMARY KEY (user_id, order_id)
);

-- Materialized view for querying by status
CREATE MATERIALIZED VIEW orders_by_status AS
SELECT user_id, order_id, status, total_amount, created_at
FROM orders
WHERE status IS NOT NULL AND order_id IS NOT NULL AND user_id IS NOT NULL
PRIMARY KEY (status, order_id, user_id);

-- Query materialized view
SELECT * FROM orders_by_status WHERE status = 'PENDING';

-- Drop materialized view
DROP MATERIALIZED VIEW orders_by_status;
```

### Materialized View Best Practices
```cql
-- ✅ GOOD: Different query pattern
CREATE MATERIALIZED VIEW products_by_category AS
SELECT product_id, category, name, price
FROM products
WHERE category IS NOT NULL AND product_id IS NOT NULL
PRIMARY KEY (category, product_id);

-- ❌ BAD: Same partition key as base table
-- Use secondary index instead

-- ❌ BAD: High write throughput
-- Materialized views add write overhead
```

---

## Advanced Query Patterns

### Range Queries
```cql
-- Query with clustering column range
SELECT * FROM orders_by_user
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000
AND order_id > minTimeuuid('2024-01-01')
AND order_id < maxTimeuuid('2024-12-31');

-- Time-based range
SELECT * FROM sensor_readings
WHERE device_id = 123e4567-e89b-12d3-a456-426614174000
AND reading_time >= '2024-01-01' AND reading_time < '2024-02-01';
```

### IN Queries
```cql
-- IN on partition key (queries multiple partitions)
SELECT * FROM users WHERE user_id IN (uuid1, uuid2, uuid3);

-- IN on clustering column (within same partition)
SELECT * FROM orders_by_user
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000
AND order_id IN (timeuuid1, timeuuid2);
```

### Pagination
```cql
-- Token-based pagination (efficient)
SELECT * FROM users WHERE token(user_id) > token(last_user_id) LIMIT 100;

-- Clustering column pagination
SELECT * FROM orders_by_user
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000
AND order_id < last_order_id
LIMIT 20;
```

### Aggregations
```cql
-- COUNT
SELECT COUNT(*) FROM users;

-- COUNT with WHERE
SELECT COUNT(*) FROM orders_by_user
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000;

-- MIN, MAX, SUM, AVG (limited support)
SELECT MAX(created_at) FROM orders_by_user
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000;
```

### Batch Operations
```cql
-- Logged batch (atomic, same partition)
BEGIN BATCH
    INSERT INTO orders_by_user (user_id, order_id, status) VALUES (uuid1, now(), 'PENDING');
    UPDATE users SET order_count = order_count + 1 WHERE user_id = uuid1;
APPLY BATCH;

-- Unlogged batch (better performance, no atomicity)
BEGIN UNLOGGED BATCH
    INSERT INTO orders_by_user (user_id, order_id, status) VALUES (uuid1, now(), 'PENDING');
    INSERT INTO orders_by_status (status, order_id, user_id) VALUES ('PENDING', now(), uuid1);
APPLY BATCH;

-- Counter batch
BEGIN COUNTER BATCH
    UPDATE product_views SET view_count = view_count + 1 WHERE product_id = uuid1;
    UPDATE category_views SET view_count = view_count + 1 WHERE category = 'electronics';
APPLY BATCH;
```

---

## Consistency Levels

### Read Consistency Levels
```cql
-- ONE: Fastest, least consistent
SELECT * FROM users WHERE user_id = uuid1 USING CONSISTENCY ONE;

-- QUORUM: Majority of replicas (default)
SELECT * FROM users WHERE user_id = uuid1 USING CONSISTENCY QUORUM;

-- ALL: Slowest, most consistent
SELECT * FROM users WHERE user_id = uuid1 USING CONSISTENCY ALL;

-- LOCAL_QUORUM: Majority in local datacenter
SELECT * FROM users WHERE user_id = uuid1 USING CONSISTENCY LOCAL_QUORUM;

-- LOCAL_ONE: One replica in local datacenter
SELECT * FROM users WHERE user_id = uuid1 USING CONSISTENCY LOCAL_ONE;
```

### Write Consistency Levels
```cql
-- ONE: Fastest writes
INSERT INTO users (user_id, email, name) 
VALUES (uuid(), 'john@example.com', 'John')
USING CONSISTENCY ONE;

-- QUORUM: Balanced
INSERT INTO users (user_id, email, name) 
VALUES (uuid(), 'john@example.com', 'John')
USING CONSISTENCY QUORUM;

-- ALL: Slowest, most durable
INSERT INTO users (user_id, email, name) 
VALUES (uuid(), 'john@example.com', 'John')
USING CONSISTENCY ALL;
```

### Consistency Level Trade-offs
```
Consistency Level | Read Latency | Write Latency | Consistency
------------------|--------------|---------------|-------------
ONE               | Lowest       | Lowest        | Eventual
QUORUM            | Medium       | Medium        | Strong
ALL               | Highest      | Highest       | Strongest
LOCAL_QUORUM      | Low          | Low           | Strong (local)
```

---

## Performance Optimization

### 1. Partition Size
```cql
-- ❌ BAD: Large partition (>100MB)
CREATE TABLE user_events (
    user_id UUID,
    event_time TIMESTAMP,
    event_type TEXT,
    PRIMARY KEY (user_id, event_time)
);
-- All events for one user in single partition

-- ✅ GOOD: Bucketed partition
CREATE TABLE user_events (
    user_id UUID,
    bucket DATE,
    event_time TIMESTAMP,
    event_type TEXT,
    PRIMARY KEY ((user_id, bucket), event_time)
);
-- Events split by date
```

### 2. Read Before Write
```cql
-- ❌ BAD: Read before write
SELECT * FROM users WHERE user_id = uuid1;
UPDATE users SET name = 'Updated' WHERE user_id = uuid1;

-- ✅ GOOD: Direct write (upsert)
UPDATE users SET name = 'Updated' WHERE user_id = uuid1;
```

### 3. Lightweight Transactions (LWT)
```cql
-- Use sparingly (expensive)
INSERT INTO users (user_id, email, name)
VALUES (uuid(), 'john@example.com', 'John')
IF NOT EXISTS;

UPDATE users SET name = 'Updated'
WHERE user_id = uuid1
IF name = 'Old Name';
```

### 4. TTL (Time To Live)
```cql
-- Auto-expire data
INSERT INTO session_tokens (token_id, user_id, created_at)
VALUES (uuid(), uuid1, toTimestamp(now()))
USING TTL 3600;  -- Expires in 1 hour

-- Update TTL
UPDATE session_tokens USING TTL 7200
SET user_id = uuid1
WHERE token_id = uuid2;
```

### 5. Compaction Strategies
```cql
-- Size-Tiered (default, good for writes)
CREATE TABLE logs (
    log_id TIMEUUID PRIMARY KEY,
    message TEXT
) WITH compaction = {
    'class': 'SizeTieredCompactionStrategy'
};

-- Leveled (good for reads)
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email TEXT,
    name TEXT
) WITH compaction = {
    'class': 'LeveledCompactionStrategy'
};

-- Time-Window (good for time-series)
CREATE TABLE sensor_data (
    device_id UUID,
    reading_time TIMESTAMP,
    value FLOAT,
    PRIMARY KEY (device_id, reading_time)
) WITH compaction = {
    'class': 'TimeWindowCompactionStrategy',
    'compaction_window_unit': 'DAYS',
    'compaction_window_size': 1
};
```

### 6. Bloom Filters
```cql
-- Reduce false positives
CREATE TABLE products (
    product_id UUID PRIMARY KEY,
    name TEXT,
    price DECIMAL
) WITH bloom_filter_fp_chance = 0.01;
```

### 7. Compression
```cql
-- Enable compression
CREATE TABLE logs (
    log_id TIMEUUID PRIMARY KEY,
    message TEXT
) WITH compression = {
    'class': 'LZ4Compressor',
    'chunk_length_in_kb': 64
};
```

---

## Real-World Examples

### 1. User Session Management
```cql
-- Session store with TTL
CREATE TABLE user_sessions (
    session_id UUID PRIMARY KEY,
    user_id UUID,
    ip_address TEXT,
    user_agent TEXT,
    created_at TIMESTAMP,
    last_activity TIMESTAMP
);

-- Insert session with 24-hour TTL
INSERT INTO user_sessions (session_id, user_id, ip_address, created_at)
VALUES (uuid(), uuid1, '192.168.1.1', toTimestamp(now()))
USING TTL 86400;

-- Update last activity
UPDATE user_sessions USING TTL 86400
SET last_activity = toTimestamp(now())
WHERE session_id = uuid1;
```

### 2. Product Catalog with Search
```cql
-- Products by category
CREATE TABLE products_by_category (
    category TEXT,
    product_id UUID,
    name TEXT,
    price DECIMAL,
    stock INT,
    PRIMARY KEY (category, product_id)
);

-- Products by price range (bucketed)
CREATE TABLE products_by_price (
    price_bucket TEXT,  -- '0-50', '50-100', etc.
    product_id UUID,
    name TEXT,
    price DECIMAL,
    category TEXT,
    PRIMARY KEY (price_bucket, product_id)
);

-- Query products in category
SELECT * FROM products_by_category WHERE category = 'electronics' LIMIT 20;

-- Query products in price range
SELECT * FROM products_by_price WHERE price_bucket = '50-100' LIMIT 20;
```

### 3. Time-Series Metrics
```cql
-- Metrics bucketed by hour
CREATE TABLE metrics (
    metric_name TEXT,
    bucket_hour TIMESTAMP,
    metric_time TIMESTAMP,
    value DOUBLE,
    tags MAP<TEXT, TEXT>,
    PRIMARY KEY ((metric_name, bucket_hour), metric_time)
) WITH CLUSTERING ORDER BY (metric_time DESC)
AND compaction = {
    'class': 'TimeWindowCompactionStrategy',
    'compaction_window_unit': 'HOURS',
    'compaction_window_size': 1
};

-- Insert metric
INSERT INTO metrics (metric_name, bucket_hour, metric_time, value, tags)
VALUES (
    'cpu_usage',
    toTimestamp(now()) - (toTimestamp(now()) % 3600000),
    toTimestamp(now()),
    75.5,
    {'host': 'server1', 'region': 'us-east-1'}
);

-- Query last hour
SELECT * FROM metrics
WHERE metric_name = 'cpu_usage'
AND bucket_hour = toTimestamp(now()) - (toTimestamp(now()) % 3600000);
```

### 4. Message Queue
```cql
-- Messages by queue
CREATE TABLE messages (
    queue_name TEXT,
    message_id TIMEUUID,
    payload TEXT,
    status TEXT,
    created_at TIMESTAMP,
    PRIMARY KEY (queue_name, message_id)
) WITH CLUSTERING ORDER BY (message_id ASC);

-- Enqueue message
INSERT INTO messages (queue_name, message_id, payload, status, created_at)
VALUES ('email_queue', now(), '{"to":"user@example.com"}', 'PENDING', toTimestamp(now()));

-- Dequeue message (get oldest)
SELECT * FROM messages 
WHERE queue_name = 'email_queue' 
AND status = 'PENDING'
LIMIT 1;

-- Mark as processed
UPDATE messages 
SET status = 'PROCESSED'
WHERE queue_name = 'email_queue' AND message_id = timeuuid1;
```

---

## Next Steps

Continue to [Part 3: Cluster Management & Operations](./03_Cassandra_Cluster_Operations.md)
