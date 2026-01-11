# PostgreSQL Complete Guide - Part 2: Indexing & Query Optimization

## 📋 Table of Contents
1. [Index Fundamentals](#index-fundamentals)
2. [Index Types](#index-types)
3. [Query Optimization](#query-optimization)
4. [EXPLAIN and ANALYZE](#explain-and-analyze)
5. [Performance Best Practices](#performance-best-practices)

---

## Index Fundamentals

Indexes speed up data retrieval by creating efficient lookup structures.

### Why Indexes Matter
```sql
-- Without index: Sequential scan (slow)
SELECT * FROM users WHERE email = 'john@example.com';
-- Scans all 1M rows

-- With index: Index scan (fast)
CREATE INDEX idx_users_email ON users(email);
SELECT * FROM users WHERE email = 'john@example.com';
-- Scans only 1 row
```

### Create Index
```sql
-- Single column index
CREATE INDEX idx_users_email ON users(email);

-- Composite index
CREATE INDEX idx_orders_user_status ON orders(user_id, status);

-- Unique index
CREATE UNIQUE INDEX idx_users_email_unique ON users(email);

-- Partial index
CREATE INDEX idx_active_users ON users(email) WHERE status = 'active';

-- Expression index
CREATE INDEX idx_users_lower_email ON users(LOWER(email));

-- Concurrent index (non-blocking)
CREATE INDEX CONCURRENTLY idx_users_name ON users(name);
```

### Manage Indexes
```sql
-- List indexes
\di

-- List indexes for table
\d users

-- Drop index
DROP INDEX idx_users_email;

-- Reindex (rebuild)
REINDEX INDEX idx_users_email;
REINDEX TABLE users;

-- Index size
SELECT pg_size_pretty(pg_relation_size('idx_users_email'));
```

---

## Index Types

### 1. B-Tree Index (Default)
```sql
-- Best for: Equality and range queries
CREATE INDEX idx_users_age ON users(age);

-- Supports queries
SELECT * FROM users WHERE age = 30;
SELECT * FROM users WHERE age > 25;
SELECT * FROM users WHERE age BETWEEN 25 AND 35;
SELECT * FROM users ORDER BY age;
```

### 2. Hash Index
```sql
-- Best for: Equality comparisons only
CREATE INDEX idx_users_email_hash ON users USING HASH (email);

-- Supports queries
SELECT * FROM users WHERE email = 'john@example.com'; -- ✅
SELECT * FROM users WHERE email > 'john@example.com'; -- ❌
```

### 3. GIN Index (Generalized Inverted Index)
```sql
-- Best for: Arrays, JSONB, full-text search
CREATE INDEX idx_products_tags ON products USING GIN (tags);
CREATE INDEX idx_products_data ON products USING GIN (data);

-- Array queries
SELECT * FROM products WHERE tags @> ARRAY['electronics'];
SELECT * FROM products WHERE 'laptop' = ANY(tags);

-- JSONB queries
SELECT * FROM products WHERE data @> '{"category": "electronics"}';
SELECT * FROM products WHERE data->>'name' = 'Laptop';
```

### 4. GiST Index (Generalized Search Tree)
```sql
-- Best for: Geometric data, full-text search, range types
CREATE INDEX idx_locations ON stores USING GIST (location);

-- Geometric queries
SELECT * FROM stores WHERE location && box '((0,0),(1,1))';
```

### 5. BRIN Index (Block Range Index)
```sql
-- Best for: Very large tables with natural ordering
CREATE INDEX idx_orders_created_at ON orders USING BRIN (created_at);

-- Good for time-series data
SELECT * FROM orders WHERE created_at > '2024-01-01';
```

### 6. Partial Index
```sql
-- Index only subset of rows
CREATE INDEX idx_active_users ON users(email) WHERE status = 'active';
CREATE INDEX idx_pending_orders ON orders(user_id) WHERE status = 'pending';

-- Smaller index, faster queries for filtered data
SELECT * FROM users WHERE email = 'john@example.com' AND status = 'active';
```

### 7. Expression Index
```sql
-- Index on computed values
CREATE INDEX idx_users_lower_email ON users(LOWER(email));
CREATE INDEX idx_products_discounted_price ON products((price * 0.9));

-- Queries must match expression
SELECT * FROM users WHERE LOWER(email) = 'john@example.com';
```

### 8. Covering Index (Include Columns)
```sql
-- Include non-key columns in index
CREATE INDEX idx_users_email_include 
ON users(email) INCLUDE (name, age);

-- Index-only scan (no table access)
SELECT name, age FROM users WHERE email = 'john@example.com';
```

---

## Query Optimization

### Index Selection Strategy

```sql
-- Bad: No index
SELECT * FROM orders WHERE user_id = 123 AND status = 'pending';
-- Sequential scan: 1M rows

-- Good: Single column index
CREATE INDEX idx_orders_user_id ON orders(user_id);
-- Index scan: 1000 rows, then filter

-- Better: Composite index
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
-- Index scan: 10 rows
```

### Column Order in Composite Index

```sql
-- Query
SELECT * FROM orders 
WHERE user_id = 123 AND status = 'pending'
ORDER BY created_at DESC;

-- Optimal index: Filter columns first, then sort column
CREATE INDEX idx_orders_composite 
ON orders(user_id, status, created_at DESC);
```

### Index Prefix Rule
```sql
-- Index: (user_id, status, created_at)
CREATE INDEX idx_orders ON orders(user_id, status, created_at);

-- Uses index (left-to-right prefix)
SELECT * FROM orders WHERE user_id = 123; -- ✅
SELECT * FROM orders WHERE user_id = 123 AND status = 'pending'; -- ✅
SELECT * FROM orders WHERE user_id = 123 AND status = 'pending' 
    AND created_at > '2024-01-01'; -- ✅

-- Doesn't use index
SELECT * FROM orders WHERE status = 'pending'; -- ❌
SELECT * FROM orders WHERE created_at > '2024-01-01'; -- ❌
```

### Covering Queries
```sql
-- Create covering index
CREATE INDEX idx_users_email_cover 
ON users(email) INCLUDE (name, age);

-- Index-only scan (fastest)
SELECT name, age FROM users WHERE email = 'john@example.com';
-- No table access needed
```

---

## EXPLAIN and ANALYZE

### Basic EXPLAIN
```sql
-- Query plan
EXPLAIN SELECT * FROM users WHERE email = 'john@example.com';

-- Execution stats
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'john@example.com';

-- Detailed output
EXPLAIN (ANALYZE, BUFFERS, VERBOSE) 
SELECT * FROM users WHERE email = 'john@example.com';
```

### Understanding EXPLAIN Output

```sql
EXPLAIN ANALYZE
SELECT * FROM users WHERE age > 30;

-- Output:
Seq Scan on users  (cost=0.00..18334.00 rows=500000 width=100) 
                   (actual time=0.012..145.234 rows=500000 loops=1)
  Filter: (age > 30)
  Rows Removed by Filter: 500000
Planning Time: 0.123 ms
Execution Time: 165.456 ms
```

**Key Metrics:**
- **cost**: Estimated cost (startup..total)
- **rows**: Estimated rows returned
- **width**: Average row size in bytes
- **actual time**: Real execution time (startup..total)
- **loops**: Number of times node executed

### Common Scan Types

```sql
-- Sequential Scan (bad for large tables)
Seq Scan on users
-- Scans entire table

-- Index Scan (good)
Index Scan using idx_users_email on users
-- Uses index, then fetches rows

-- Index Only Scan (best)
Index Only Scan using idx_users_email_cover on users
-- Uses index only, no table access

-- Bitmap Index Scan (multiple indexes)
Bitmap Heap Scan on users
  -> Bitmap Index Scan on idx_users_age
  -> Bitmap Index Scan on idx_users_status

-- Nested Loop (JOIN)
Nested Loop
  -> Seq Scan on orders
  -> Index Scan using idx_users_id on users

-- Hash Join (large tables)
Hash Join
  -> Seq Scan on orders
  -> Hash
    -> Seq Scan on users

-- Merge Join (sorted data)
Merge Join
  -> Index Scan using idx_orders_user_id on orders
  -> Index Scan using idx_users_id on users
```

---

## Performance Best Practices

### 1. Index Strategy

```sql
-- ✅ DO: Create indexes for frequent queries
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);

-- ✅ DO: Use composite indexes
CREATE INDEX idx_orders_user_status ON orders(user_id, status);

-- ❌ DON'T: Over-index (slows writes)
-- Max 5-10 indexes per table

-- ✅ DO: Use partial indexes for filtered queries
CREATE INDEX idx_pending_orders ON orders(user_id) WHERE status = 'pending';

-- ✅ DO: Drop unused indexes
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0;

DROP INDEX idx_unused;
```

### 2. Query Optimization

```sql
-- ❌ BAD: SELECT *
SELECT * FROM users WHERE email = 'john@example.com';

-- ✅ GOOD: Select only needed columns
SELECT id, name, email FROM users WHERE email = 'john@example.com';

-- ❌ BAD: Function on indexed column
SELECT * FROM users WHERE LOWER(email) = 'john@example.com';

-- ✅ GOOD: Use expression index or store lowercase
CREATE INDEX idx_users_lower_email ON users(LOWER(email));

-- ❌ BAD: OR conditions (doesn't use index well)
SELECT * FROM users WHERE age = 30 OR status = 'active';

-- ✅ GOOD: Use UNION
SELECT * FROM users WHERE age = 30
UNION
SELECT * FROM users WHERE status = 'active';

-- ❌ BAD: NOT IN with subquery
SELECT * FROM users WHERE id NOT IN (SELECT user_id FROM orders);

-- ✅ GOOD: Use NOT EXISTS
SELECT * FROM users u 
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);
```

### 3. JOIN Optimization

```sql
-- ✅ GOOD: Index foreign keys
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- ✅ GOOD: Filter before JOIN
SELECT u.name, o.total_amount
FROM users u
JOIN orders o ON u.id = o.user_id
WHERE o.status = 'completed' AND o.created_at > '2024-01-01';

-- ✅ GOOD: Use appropriate JOIN type
-- INNER JOIN: Only matching rows
-- LEFT JOIN: All left rows + matching right rows
-- Use INNER JOIN when possible (faster)
```

### 4. Pagination

```sql
-- ❌ BAD: Large OFFSET
SELECT * FROM products ORDER BY id LIMIT 20 OFFSET 10000;
-- Scans and discards 10000 rows

-- ✅ GOOD: Keyset pagination
SELECT * FROM products 
WHERE id > 10000 
ORDER BY id 
LIMIT 20;
```

### 5. Aggregation Optimization

```sql
-- ✅ GOOD: Filter before aggregation
SELECT user_id, COUNT(*) 
FROM orders 
WHERE status = 'completed'
GROUP BY user_id;

-- ✅ GOOD: Use indexes for GROUP BY
CREATE INDEX idx_orders_user_id ON orders(user_id);

-- ✅ GOOD: Use HAVING for post-aggregation filters
SELECT user_id, COUNT(*) as order_count
FROM orders
GROUP BY user_id
HAVING COUNT(*) > 10;
```

### 6. Connection Pooling

```sql
-- PostgreSQL configuration (postgresql.conf)
max_connections = 100
shared_buffers = 256MB
effective_cache_size = 1GB
maintenance_work_mem = 64MB
work_mem = 4MB
```

### 7. Vacuum and Analyze

```sql
-- Update statistics for query planner
ANALYZE users;
ANALYZE;  -- All tables

-- Reclaim space and update statistics
VACUUM ANALYZE users;

-- Full vacuum (locks table)
VACUUM FULL users;

-- Auto-vacuum (enabled by default)
-- Runs automatically in background
```

### 8. Monitoring

```sql
-- Current queries
SELECT pid, usename, state, query, query_start
FROM pg_stat_activity
WHERE state = 'active';

-- Slow queries
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Table statistics
SELECT schemaname, tablename, seq_scan, idx_scan, 
       n_tup_ins, n_tup_upd, n_tup_del
FROM pg_stat_user_tables;

-- Index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Database size
SELECT pg_size_pretty(pg_database_size('ecommerce'));

-- Table size
SELECT pg_size_pretty(pg_total_relation_size('users'));
```

---

## Real-World Example: E-commerce Optimization

### Before Optimization
```sql
-- Slow query (2500ms)
SELECT u.name, COUNT(o.id) as order_count, SUM(o.total_amount) as total_spent
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE o.status = 'completed' AND o.created_at > '2024-01-01'
GROUP BY u.id, u.name
ORDER BY total_spent DESC
LIMIT 10;

-- EXPLAIN shows:
Seq Scan on orders  (cost=0.00..50000.00 rows=1000000)
Hash Join  (cost=10000.00..60000.00)
```

### After Optimization
```sql
-- Create indexes
CREATE INDEX idx_orders_status_created ON orders(status, created_at);
CREATE INDEX idx_orders_user_id ON orders(user_id);

-- Optimized query (15ms)
SELECT u.name, COUNT(o.id) as order_count, SUM(o.total_amount) as total_spent
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE o.status = 'completed' AND o.created_at > '2024-01-01'
GROUP BY u.id, u.name
ORDER BY total_spent DESC
LIMIT 10;

-- EXPLAIN shows:
Index Scan using idx_orders_status_created on orders
Index Scan using idx_orders_user_id on orders
```

**Result**: 166x faster (2500ms → 15ms)

---

## Next Steps

Continue to [Part 3: Advanced Queries & JOINs](./03_PostgreSQL_Advanced_Queries.md)
