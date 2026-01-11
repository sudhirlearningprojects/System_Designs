# PostgreSQL Complete Guide - Part 4: Transactions & Concurrency

## 📋 Table of Contents
1. [ACID Transactions](#acid-transactions)
2. [Isolation Levels](#isolation-levels)
3. [Locking Mechanisms](#locking-mechanisms)
4. [Replication](#replication)
5. [Production Best Practices](#production-best-practices)

---

## ACID Transactions

### Basic Transactions
```sql
-- Start transaction
BEGIN;

-- Execute queries
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;

-- Commit transaction
COMMIT;

-- Or rollback on error
ROLLBACK;
```

### Transaction with Error Handling
```sql
BEGIN;

-- Debit account
UPDATE accounts SET balance = balance - 100 
WHERE id = 1 AND balance >= 100;

-- Check if update succeeded
DO $$
BEGIN
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Insufficient balance';
    END IF;
END $$;

-- Credit account
UPDATE accounts SET balance = balance + 100 WHERE id = 2;

COMMIT;
```

### Savepoints
```sql
BEGIN;

INSERT INTO users (email, name) VALUES ('user1@example.com', 'User 1');

SAVEPOINT sp1;

INSERT INTO users (email, name) VALUES ('user2@example.com', 'User 2');

-- Rollback to savepoint (keeps first insert)
ROLLBACK TO SAVEPOINT sp1;

COMMIT;
```

---

## Isolation Levels

### Read Uncommitted (Not supported in PostgreSQL)
PostgreSQL treats this as Read Committed.

### Read Committed (Default)
```sql
-- Session 1
BEGIN;
UPDATE products SET stock = stock - 1 WHERE id = 1;
-- Not committed yet

-- Session 2
BEGIN;
SELECT stock FROM products WHERE id = 1;
-- Sees old value (before Session 1's update)
COMMIT;

-- Session 1
COMMIT;
```

### Repeatable Read
```sql
-- Session 1
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT stock FROM products WHERE id = 1;  -- Returns 10

-- Session 2
BEGIN;
UPDATE products SET stock = 5 WHERE id = 1;
COMMIT;

-- Session 1
SELECT stock FROM products WHERE id = 1;  -- Still returns 10 (repeatable read)
COMMIT;
```

### Serializable
```sql
-- Session 1
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;
SELECT SUM(balance) FROM accounts;

-- Session 2
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;
INSERT INTO accounts (balance) VALUES (100);
COMMIT;

-- Session 1
SELECT SUM(balance) FROM accounts;  -- May fail with serialization error
COMMIT;
```

### Setting Isolation Level
```sql
-- For current transaction
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;

-- For session
SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL REPEATABLE READ;

-- Check current level
SHOW transaction_isolation;
```

---

## Locking Mechanisms

### Row-Level Locks

```sql
-- FOR UPDATE: Exclusive lock (blocks other locks)
BEGIN;
SELECT * FROM products WHERE id = 1 FOR UPDATE;
UPDATE products SET stock = stock - 1 WHERE id = 1;
COMMIT;

-- FOR SHARE: Shared lock (allows other FOR SHARE, blocks FOR UPDATE)
BEGIN;
SELECT * FROM products WHERE id = 1 FOR SHARE;
-- Other sessions can read but not update
COMMIT;

-- FOR NO KEY UPDATE: Allows foreign key checks
BEGIN;
SELECT * FROM orders WHERE id = 1 FOR NO KEY UPDATE;
UPDATE orders SET status = 'shipped' WHERE id = 1;
COMMIT;

-- SKIP LOCKED: Skip locked rows
SELECT * FROM queue_items 
WHERE processed = false 
FOR UPDATE SKIP LOCKED 
LIMIT 10;
```

### Table-Level Locks

```sql
-- ACCESS SHARE: Acquired by SELECT
BEGIN;
LOCK TABLE products IN ACCESS SHARE MODE;
SELECT * FROM products;
COMMIT;

-- ROW EXCLUSIVE: Acquired by UPDATE, DELETE, INSERT
BEGIN;
LOCK TABLE products IN ROW EXCLUSIVE MODE;
UPDATE products SET stock = stock - 1 WHERE id = 1;
COMMIT;

-- EXCLUSIVE: Blocks all concurrent access except ACCESS SHARE
BEGIN;
LOCK TABLE products IN EXCLUSIVE MODE;
-- Perform maintenance operations
COMMIT;
```

### Advisory Locks

```sql
-- Session-level advisory lock
SELECT pg_advisory_lock(123);
-- Critical section
SELECT pg_advisory_unlock(123);

-- Transaction-level advisory lock
BEGIN;
SELECT pg_advisory_xact_lock(123);
-- Critical section
COMMIT;  -- Automatically releases lock

-- Try lock (non-blocking)
SELECT pg_try_advisory_lock(123);  -- Returns true if acquired
```

### Deadlock Handling

```sql
-- PostgreSQL automatically detects and resolves deadlocks
-- One transaction will be aborted with error

-- Example deadlock scenario:
-- Session 1
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
-- Waiting for lock on id = 2

-- Session 2
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 2;
UPDATE accounts SET balance = balance + 100 WHERE id = 1;  -- Deadlock!
-- One session will be aborted

-- Best practice: Always acquire locks in same order
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;
```

---

## Replication

### Streaming Replication Setup

```sql
-- Primary server (postgresql.conf)
wal_level = replica
max_wal_senders = 10
wal_keep_size = 1GB

-- Create replication user
CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'password';

-- pg_hba.conf
host replication replicator 192.168.1.0/24 md5

-- Standby server (recovery.conf or postgresql.auto.conf)
primary_conninfo = 'host=primary_host port=5432 user=replicator password=password'
```

### Logical Replication

```sql
-- Publisher (primary)
CREATE PUBLICATION my_publication FOR TABLE users, orders;

-- Subscriber (replica)
CREATE SUBSCRIPTION my_subscription
CONNECTION 'host=primary_host dbname=mydb user=replicator password=password'
PUBLICATION my_publication;

-- Monitor replication
SELECT * FROM pg_stat_replication;
SELECT * FROM pg_stat_subscription;
```

### Read Replicas

```sql
-- Configure hot standby (postgresql.conf on replica)
hot_standby = on

-- Query replica
-- Connect to replica server
SELECT * FROM users;  -- Read-only queries

-- Check if server is in recovery (replica)
SELECT pg_is_in_recovery();
```

---

## Production Best Practices

### 1. Connection Pooling

```yaml
# Spring Boot application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce
    username: app_user
    password: password
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 2. Query Timeout

```sql
-- Set statement timeout (milliseconds)
SET statement_timeout = 30000;  -- 30 seconds

-- Set for session
SET SESSION statement_timeout = 30000;

-- Set in postgresql.conf
statement_timeout = 30000
```

### 3. Monitoring

```sql
-- Active connections
SELECT count(*) FROM pg_stat_activity;

-- Long-running queries
SELECT pid, usename, state, query, now() - query_start as duration
FROM pg_stat_activity
WHERE state = 'active' AND now() - query_start > interval '5 minutes'
ORDER BY duration DESC;

-- Kill query
SELECT pg_cancel_backend(pid);  -- Graceful
SELECT pg_terminate_backend(pid);  -- Forceful

-- Blocking queries
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement,
       blocking_activity.query AS blocking_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks 
    ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
    AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
    AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
    AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
    AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
    AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
    AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

### 4. Backup Strategies

```bash
# Logical backup (pg_dump)
pg_dump -U admin -d ecommerce -F c -f backup.dump

# Restore
pg_restore -U admin -d ecommerce backup.dump

# Backup specific tables
pg_dump -U admin -d ecommerce -t users -t orders -F c -f tables_backup.dump

# Physical backup (pg_basebackup)
pg_basebackup -D /backup/data -F tar -z -P -U replicator

# Point-in-time recovery (PITR)
# Enable WAL archiving in postgresql.conf
archive_mode = on
archive_command = 'cp %p /archive/%f'
```

### 5. Security

```sql
-- Create application user with limited privileges
CREATE USER app_user WITH PASSWORD 'secure_password';

-- Grant specific permissions
GRANT CONNECT ON DATABASE ecommerce TO app_user;
GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- Revoke public access
REVOKE ALL ON DATABASE ecommerce FROM PUBLIC;

-- Enable SSL (postgresql.conf)
ssl = on
ssl_cert_file = 'server.crt'
ssl_key_file = 'server.key'

-- Row-level security
CREATE POLICY user_policy ON orders
FOR ALL TO app_user
USING (user_id = current_user_id());

ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
```

### 6. Vacuum and Maintenance

```sql
-- Manual vacuum
VACUUM ANALYZE users;

-- Full vacuum (locks table)
VACUUM FULL users;

-- Auto-vacuum configuration (postgresql.conf)
autovacuum = on
autovacuum_max_workers = 3
autovacuum_naptime = 1min

-- Check bloat
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
       n_dead_tup
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;

-- Reindex
REINDEX TABLE users;
REINDEX INDEX idx_users_email;
```

### 7. Performance Tuning

```sql
-- postgresql.conf settings
shared_buffers = 256MB              -- 25% of RAM
effective_cache_size = 1GB          -- 50-75% of RAM
maintenance_work_mem = 64MB
work_mem = 4MB
max_connections = 100
random_page_cost = 1.1              -- For SSD
effective_io_concurrency = 200      -- For SSD

-- Enable query statistics
shared_preload_libraries = 'pg_stat_statements'
pg_stat_statements.track = all

-- View slow queries
SELECT query, calls, total_exec_time, mean_exec_time, max_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

### 8. Partitioning

```sql
-- Range partitioning by date
CREATE TABLE orders (
    id SERIAL,
    user_id INTEGER,
    total_amount DECIMAL(10, 2),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create partitions
CREATE TABLE orders_2024_01 PARTITION OF orders
FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE orders_2024_02 PARTITION OF orders
FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- List partitioning
CREATE TABLE users (
    id SERIAL,
    email VARCHAR(255),
    country VARCHAR(2),
    PRIMARY KEY (id, country)
) PARTITION BY LIST (country);

CREATE TABLE users_us PARTITION OF users FOR VALUES IN ('US');
CREATE TABLE users_uk PARTITION OF users FOR VALUES IN ('UK');
```

---

## Spring Boot Integration

### Configuration
```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/ecommerce");
        config.setUsername("app_user");
        config.setPassword("password");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        return new HikariDataSource(config);
    }
}
```

### Transaction Management
```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order createOrder(OrderRequest request) {
        // Create order
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setTotalAmount(request.getTotalAmount());
        order = orderRepository.save(order);
        
        // Update product stock
        for (OrderItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
            
            if (product.getStock() < item.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock");
            }
            
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }
        
        return order;
    }
    
    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}
```

---

## Next Steps

Continue to [Part 5: Spring Boot Integration](./05_PostgreSQL_Spring_Boot.md)
