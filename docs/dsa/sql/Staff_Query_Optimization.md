# Query Optimization - Performance Tuning

**Level**: Staff/Principal Engineer  
**Companies**: All FAANG  
**Concepts**: EXPLAIN plans, Indexing, Query rewriting

## Problem

Optimize a slow query that finds top-selling products by category in the last 30 days.

## Slow Query (Before)

```sql
SELECT 
    p.category,
    p.product_name,
    SUM(oi.quantity * oi.price) AS revenue
FROM products p
JOIN order_items oi ON p.product_id = oi.product_id
JOIN orders o ON oi.order_id = o.order_id
WHERE o.order_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY p.category, p.product_name
ORDER BY revenue DESC;
```

**Issues**:
- Full table scan on orders (10M rows)
- No index on order_date
- Expensive JOIN operations
- Sorting large result set

## Execution Plan Analysis

```sql
EXPLAIN ANALYZE
SELECT ...;
```

**Output**:
```
Sort  (cost=500000..510000 rows=1000000)
  ->  HashAggregate  (cost=400000..450000)
        ->  Hash Join  (cost=300000..350000)
              ->  Seq Scan on orders  (cost=0..100000 rows=10000000)
                    Filter: (order_date >= ...)
```

## Optimization Steps

### 1. Add Indexes

```sql
-- Index on filter column
CREATE INDEX idx_orders_date ON orders(order_date);

-- Composite index for JOIN + filter
CREATE INDEX idx_orders_date_id ON orders(order_date, order_id);

-- Covering index
CREATE INDEX idx_order_items_covering 
ON order_items(order_id, product_id, quantity, price);
```

### 2. Rewrite with Materialized CTE

```sql
WITH recent_orders AS MATERIALIZED (
    SELECT order_id
    FROM orders
    WHERE order_date >= CURRENT_DATE - INTERVAL '30 days'
)
SELECT 
    p.category,
    p.product_name,
    SUM(oi.quantity * oi.price) AS revenue
FROM products p
JOIN order_items oi ON p.product_id = oi.product_id
JOIN recent_orders ro ON oi.order_id = ro.order_id
GROUP BY p.category, p.product_name
ORDER BY revenue DESC
LIMIT 100;
```

### 3. Partition Table

```sql
-- Partition orders by month
CREATE TABLE orders_partitioned (
    order_id INT,
    order_date DATE,
    ...
) PARTITION BY RANGE (order_date);

CREATE TABLE orders_2024_01 PARTITION OF orders_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

### 4. Materialized View

```sql
CREATE MATERIALIZED VIEW daily_product_revenue AS
SELECT 
    DATE_TRUNC('day', o.order_date) AS date,
    p.category,
    p.product_id,
    p.product_name,
    SUM(oi.quantity * oi.price) AS revenue
FROM products p
JOIN order_items oi ON p.product_id = oi.product_id
JOIN orders o ON oi.order_id = o.order_id
GROUP BY date, p.category, p.product_id, p.product_name;

-- Refresh daily
REFRESH MATERIALIZED VIEW CONCURRENTLY daily_product_revenue;

-- Query becomes simple
SELECT category, product_name, SUM(revenue) AS total_revenue
FROM daily_product_revenue
WHERE date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY category, product_name
ORDER BY total_revenue DESC;
```

## Performance Comparison

| Approach | Execution Time | Rows Scanned | Cost |
|----------|----------------|--------------|------|
| Original | 45s | 10M | 500K |
| With indexes | 8s | 500K | 100K |
| Materialized CTE | 3s | 500K | 50K |
| Partitioned table | 1.5s | 100K | 20K |
| Materialized view | 0.2s | 1K | 1K |

## Advanced Techniques

### 1. Query Hints (PostgreSQL)
```sql
SELECT /*+ SeqScan(orders) */ ...
```

### 2. Parallel Query
```sql
SET max_parallel_workers_per_gather = 4;
```

### 3. Statistics Update
```sql
ANALYZE orders;
ANALYZE order_items;
```

### 4. Connection Pooling
```java
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(20);
config.setConnectionTimeout(30000);
```

## Monitoring Queries

```sql
-- Find slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0;
```

## Key Takeaways

✅ Always run EXPLAIN ANALYZE  
✅ Index filter and JOIN columns  
✅ Use partitioning for large tables  
✅ Consider materialized views for complex aggregations  
✅ Monitor query performance in production
