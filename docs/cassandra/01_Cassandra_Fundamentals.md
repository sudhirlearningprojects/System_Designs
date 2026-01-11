# Apache Cassandra Complete Guide - Part 1: Fundamentals & Data Modeling

## 📋 Table of Contents
1. [Introduction](#introduction)
2. [Installation & Setup](#installation--setup)
3. [Keyspaces & Tables](#keyspaces--tables)
4. [Data Modeling Principles](#data-modeling-principles)
5. [CRUD Operations](#crud-operations)

---

## Introduction

Apache Cassandra is a distributed NoSQL database designed for high availability and linear scalability with no single point of failure.

### Key Features
- **Distributed**: Data automatically distributed across cluster
- **Scalable**: Linear scalability (add nodes = add capacity)
- **High Availability**: No single point of failure
- **Tunable Consistency**: Choose between consistency and availability
- **Write-Optimized**: Extremely fast writes
- **Fault Tolerant**: Replication across multiple data centers

### When to Use Cassandra
✅ High write throughput (millions of writes/sec)  
✅ Time-series data (IoT, logs, metrics)  
✅ Large datasets (petabytes)  
✅ Geographic distribution  
✅ Always-on applications (99.999% uptime)  
✅ Linear scalability requirements  

❌ Complex JOINs (use PostgreSQL)  
❌ ACID transactions across partitions  
❌ Ad-hoc queries (use Elasticsearch)  
❌ Small datasets (<100GB)  

---

## Installation & Setup

### Using Docker (Recommended)
```bash
# Pull Cassandra image
docker pull cassandra:5.0

# Run Cassandra container
docker run -d \
  --name cassandra \
  -p 9042:9042 \
  -e CASSANDRA_CLUSTER_NAME=MyCluster \
  -v cassandra_data:/var/lib/cassandra \
  cassandra:5.0

# Connect to CQL shell
docker exec -it cassandra cqlsh
```

### Multi-Node Cluster (Docker Compose)
```yaml
version: '3.8'
services:
  cassandra-1:
    image: cassandra:5.0
    environment:
      - CASSANDRA_CLUSTER_NAME=MyCluster
      - CASSANDRA_SEEDS=cassandra-1
    ports:
      - "9042:9042"
    volumes:
      - cassandra1_data:/var/lib/cassandra

  cassandra-2:
    image: cassandra:5.0
    environment:
      - CASSANDRA_CLUSTER_NAME=MyCluster
      - CASSANDRA_SEEDS=cassandra-1
    depends_on:
      - cassandra-1
    volumes:
      - cassandra2_data:/var/lib/cassandra

  cassandra-3:
    image: cassandra:5.0
    environment:
      - CASSANDRA_CLUSTER_NAME=MyCluster
      - CASSANDRA_SEEDS=cassandra-1
    depends_on:
      - cassandra-1
    volumes:
      - cassandra3_data:/var/lib/cassandra

volumes:
  cassandra1_data:
  cassandra2_data:
  cassandra3_data:
```

### Connection String
```
# Local connection
127.0.0.1:9042

# DataStax Astra (Cloud)
secure-connect-bundle.zip
```

---

## Keyspaces & Tables

### Create Keyspace
```cql
-- Simple strategy (single datacenter)
CREATE KEYSPACE ecommerce
WITH replication = {
    'class': 'SimpleStrategy',
    'replication_factor': 3
};

-- Network topology (multiple datacenters)
CREATE KEYSPACE ecommerce
WITH replication = {
    'class': 'NetworkTopologyStrategy',
    'dc1': 3,
    'dc2': 2
};

-- Use keyspace
USE ecommerce;

-- List keyspaces
DESCRIBE KEYSPACES;

-- Drop keyspace
DROP KEYSPACE ecommerce;
```

### Create Tables
```cql
-- Users table
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email TEXT,
    name TEXT,
    age INT,
    created_at TIMESTAMP
);

-- Products table
CREATE TABLE products (
    product_id UUID PRIMARY KEY,
    sku TEXT,
    name TEXT,
    price DECIMAL,
    stock INT,
    category TEXT,
    tags SET<TEXT>,
    created_at TIMESTAMP
);

-- Orders by user (query pattern: get orders by user_id)
CREATE TABLE orders_by_user (
    user_id UUID,
    order_id TIMEUUID,
    total_amount DECIMAL,
    status TEXT,
    created_at TIMESTAMP,
    PRIMARY KEY (user_id, order_id)
) WITH CLUSTERING ORDER BY (order_id DESC);

-- Orders by status (query pattern: get orders by status)
CREATE TABLE orders_by_status (
    status TEXT,
    order_id TIMEUUID,
    user_id UUID,
    total_amount DECIMAL,
    created_at TIMESTAMP,
    PRIMARY KEY (status, order_id)
) WITH CLUSTERING ORDER BY (order_id DESC);
```

### Table Management
```cql
-- Describe table
DESCRIBE TABLE users;

-- Alter table
ALTER TABLE users ADD phone TEXT;
ALTER TABLE users DROP phone;

-- Drop table
DROP TABLE users;
```

---

## Data Modeling Principles

### 1. Query-First Design
**Rule**: Design tables based on queries, not entities.

```cql
-- Bad: Single table (like relational DB)
CREATE TABLE orders (
    order_id UUID PRIMARY KEY,
    user_id UUID,
    status TEXT,
    created_at TIMESTAMP
);
-- Can't efficiently query by user_id or status

-- Good: Multiple tables for different queries
CREATE TABLE orders_by_user (
    user_id UUID,
    order_id TIMEUUID,
    status TEXT,
    PRIMARY KEY (user_id, order_id)
);

CREATE TABLE orders_by_status (
    status TEXT,
    order_id TIMEUUID,
    user_id UUID,
    PRIMARY KEY (status, order_id)
);
```

### 2. Partition Key Selection
**Rule**: Choose partition key for even data distribution.

```cql
-- Bad: Low cardinality (hot partitions)
CREATE TABLE orders (
    status TEXT,  -- Only few statuses
    order_id UUID,
    PRIMARY KEY (status, order_id)
);

-- Good: High cardinality
CREATE TABLE orders (
    user_id UUID,  -- Many users
    order_id TIMEUUID,
    PRIMARY KEY (user_id, order_id)
);

-- Good: Composite partition key
CREATE TABLE orders (
    year INT,
    month INT,
    order_id TIMEUUID,
    PRIMARY KEY ((year, month), order_id)
);
```

### 3. Denormalization
**Rule**: Duplicate data to avoid JOINs.

```cql
-- Store user info in orders table
CREATE TABLE orders (
    user_id UUID,
    order_id TIMEUUID,
    user_name TEXT,      -- Denormalized
    user_email TEXT,     -- Denormalized
    total_amount DECIMAL,
    PRIMARY KEY (user_id, order_id)
);
```

### 4. Primary Key Structure
```cql
-- PRIMARY KEY = Partition Key + Clustering Columns

-- Single partition key
PRIMARY KEY (user_id)

-- Composite partition key
PRIMARY KEY ((user_id, year))

-- Partition key + clustering columns
PRIMARY KEY (user_id, order_id)

-- Composite partition key + clustering columns
PRIMARY KEY ((user_id, year), month, order_id)
```

---

## CRUD Operations

### Insert Data

```cql
-- Insert single row
INSERT INTO users (user_id, email, name, age, created_at)
VALUES (uuid(), 'john@example.com', 'John Doe', 30, toTimestamp(now()));

-- Insert with TTL (time to live)
INSERT INTO users (user_id, email, name)
VALUES (uuid(), 'temp@example.com', 'Temp User')
USING TTL 86400;  -- Expires in 24 hours

-- Insert if not exists
INSERT INTO users (user_id, email, name)
VALUES (uuid(), 'john@example.com', 'John Doe')
IF NOT EXISTS;

-- Batch insert (same partition)
BEGIN BATCH
    INSERT INTO users (user_id, email, name) VALUES (uuid(), 'user1@example.com', 'User 1');
    INSERT INTO users (user_id, email, name) VALUES (uuid(), 'user2@example.com', 'User 2');
APPLY BATCH;
```

### Read Data

```cql
-- Select all columns
SELECT * FROM users;

-- Select specific columns
SELECT user_id, email, name FROM users;

-- WHERE with partition key (efficient)
SELECT * FROM orders_by_user WHERE user_id = 123e4567-e89b-12d3-a456-426614174000;

-- WHERE with partition key + clustering column
SELECT * FROM orders_by_user 
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000 
AND order_id > minTimeuuid('2024-01-01');

-- LIMIT
SELECT * FROM users LIMIT 10;

-- ALLOW FILTERING (avoid in production - slow)
SELECT * FROM users WHERE age > 30 ALLOW FILTERING;

-- Token-based pagination
SELECT * FROM users WHERE token(user_id) > token(last_user_id) LIMIT 100;
```

### Update Data

```cql
-- Update row
UPDATE users 
SET name = 'John Updated', age = 31
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000;

-- Update with TTL
UPDATE users USING TTL 3600
SET name = 'Temp Name'
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000;

-- Update if exists
UPDATE users 
SET name = 'John Updated'
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000
IF EXISTS;

-- Update collection (add to set)
UPDATE products 
SET tags = tags + {'new-tag'}
WHERE product_id = 123e4567-e89b-12d3-a456-426614174000;

-- Update collection (remove from set)
UPDATE products 
SET tags = tags - {'old-tag'}
WHERE product_id = 123e4567-e89b-12d3-a456-426614174000;

-- Counter update
UPDATE product_views 
SET view_count = view_count + 1
WHERE product_id = 123e4567-e89b-12d3-a456-426614174000;
```

### Delete Data

```cql
-- Delete row
DELETE FROM users WHERE user_id = 123e4567-e89b-12d3-a456-426614174000;

-- Delete specific columns
DELETE name, age FROM users WHERE user_id = 123e4567-e89b-12d3-a456-426614174000;

-- Delete if exists
DELETE FROM users 
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000
IF EXISTS;

-- Delete with timestamp (tombstone)
DELETE FROM users 
USING TIMESTAMP 1234567890
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000;
```

---

## Data Types

### Basic Types
```cql
CREATE TABLE data_types_example (
    id UUID PRIMARY KEY,
    
    -- Numeric
    int_col INT,
    bigint_col BIGINT,
    float_col FLOAT,
    double_col DOUBLE,
    decimal_col DECIMAL,
    
    -- String
    text_col TEXT,
    varchar_col VARCHAR,
    ascii_col ASCII,
    
    -- Boolean
    bool_col BOOLEAN,
    
    -- Date/Time
    timestamp_col TIMESTAMP,
    date_col DATE,
    time_col TIME,
    
    -- UUID
    uuid_col UUID,
    timeuuid_col TIMEUUID,
    
    -- Binary
    blob_col BLOB
);
```

### Collection Types
```cql
CREATE TABLE collections_example (
    id UUID PRIMARY KEY,
    
    -- Set (unique values)
    tags SET<TEXT>,
    
    -- List (ordered, duplicates allowed)
    comments LIST<TEXT>,
    
    -- Map (key-value pairs)
    attributes MAP<TEXT, TEXT>
);

-- Insert collections
INSERT INTO collections_example (id, tags, comments, attributes)
VALUES (
    uuid(),
    {'tag1', 'tag2'},
    ['comment1', 'comment2'],
    {'color': 'red', 'size': 'large'}
);

-- Query collections
SELECT * FROM collections_example WHERE tags CONTAINS 'tag1';
SELECT * FROM collections_example WHERE attributes['color'] = 'red';
```

### User-Defined Types (UDT)
```cql
-- Create UDT
CREATE TYPE address (
    street TEXT,
    city TEXT,
    state TEXT,
    zip_code TEXT
);

-- Use UDT in table
CREATE TABLE users_with_address (
    user_id UUID PRIMARY KEY,
    name TEXT,
    home_address FROZEN<address>,
    work_address FROZEN<address>
);

-- Insert with UDT
INSERT INTO users_with_address (user_id, name, home_address)
VALUES (
    uuid(),
    'John Doe',
    {street: '123 Main St', city: 'NYC', state: 'NY', zip_code: '10001'}
);
```

---

## Practical Examples

### Time-Series Data (IoT Sensors)
```cql
-- Sensor readings by device and time
CREATE TABLE sensor_readings (
    device_id UUID,
    reading_time TIMESTAMP,
    temperature FLOAT,
    humidity FLOAT,
    battery_level INT,
    PRIMARY KEY (device_id, reading_time)
) WITH CLUSTERING ORDER BY (reading_time DESC);

-- Insert reading
INSERT INTO sensor_readings (device_id, reading_time, temperature, humidity, battery_level)
VALUES (uuid(), toTimestamp(now()), 22.5, 65.0, 85);

-- Query last 24 hours
SELECT * FROM sensor_readings
WHERE device_id = 123e4567-e89b-12d3-a456-426614174000
AND reading_time > toTimestamp(now()) - 86400000;
```

### User Activity Log
```cql
-- Activity log partitioned by user and date
CREATE TABLE user_activity (
    user_id UUID,
    activity_date DATE,
    activity_time TIMEUUID,
    activity_type TEXT,
    details TEXT,
    PRIMARY KEY ((user_id, activity_date), activity_time)
) WITH CLUSTERING ORDER BY (activity_time DESC);

-- Insert activity
INSERT INTO user_activity (user_id, activity_date, activity_time, activity_type, details)
VALUES (
    uuid(),
    toDate(now()),
    now(),
    'LOGIN',
    'User logged in from mobile app'
);

-- Query today's activities
SELECT * FROM user_activity
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000
AND activity_date = toDate(now());
```

### E-commerce Orders
```cql
-- Orders by user
CREATE TABLE orders_by_user (
    user_id UUID,
    order_id TIMEUUID,
    order_date DATE,
    total_amount DECIMAL,
    status TEXT,
    items LIST<FROZEN<order_item>>,
    PRIMARY KEY (user_id, order_id)
) WITH CLUSTERING ORDER BY (order_id DESC);

-- Order item UDT
CREATE TYPE order_item (
    product_id UUID,
    product_name TEXT,
    quantity INT,
    price DECIMAL
);

-- Insert order
INSERT INTO orders_by_user (user_id, order_id, order_date, total_amount, status, items)
VALUES (
    uuid(),
    now(),
    toDate(now()),
    299.99,
    'PENDING',
    [
        {product_id: uuid(), product_name: 'Laptop', quantity: 1, price: 299.99}
    ]
);
```

---

## Next Steps

Continue to [Part 2: Advanced Queries & Indexing](./02_Cassandra_Queries_Indexing.md)
