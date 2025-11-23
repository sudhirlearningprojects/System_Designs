# Distributed Database System Design

## Overview

A highly scalable, available, and fault-tolerant distributed database system built on PostgreSQL with multi-criteria sharding, automatic failover, and strong consistency guarantees.

## Key Features

- **Multi-Criteria Sharding**: Hash, Range, Geo-location, Tenant-based
- **High Availability**: Automatic failover with leader election
- **Read Scalability**: Read replicas with load balancing
- **Strong Consistency**: Two-Phase Commit for distributed transactions
- **Fault Tolerance**: Health monitoring and automatic recovery
- **Query Routing**: Intelligent routing based on sharding criteria

## Architecture

### High-Level Design

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Applications                     │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Query Coordinator                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Shard Router │  │ Transaction  │  │   Failover   │      │
│  │              │  │ Coordinator  │  │   Service    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└──────────────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Shard 1    │  │   Shard 2    │  │   Shard N    │
│  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │
│  │Primary │  │  │  │Primary │  │  │  │Primary │  │
│  └────┬───┘  │  │  └────┬───┘  │  │  └────┬───┘  │
│       │      │  │       │      │  │       │      │
│  ┌────▼───┐  │  │  ┌────▼───┐  │  │  ┌────▼───┐  │
│  │Replica1│  │  │  │Replica1│  │  │  │Replica1│  │
│  └────────┘  │  │  └────────┘  │  │  └────────┘  │
│  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │
│  │Replica2│  │  │  │Replica2│  │  │  │Replica2│  │
│  └────────┘  │  │  └────────┘  │  │  └────────┘  │
└──────────────┘  └──────────────┘  └──────────────┘
```

## Sharding Strategies

### 1. Hash-Based Sharding
```
Shard = hash(sharding_key) % num_shards
```
- **Use Case**: Uniform data distribution
- **Example**: User ID, Order ID

### 2. Range-Based Sharding
```
Shard = find_range(key_value)
```
- **Use Case**: Time-series data, sequential IDs
- **Example**: Date ranges, ID ranges

### 3. Geo-Location Sharding
```
Shard = geo_region(location)
```
- **Use Case**: Location-based services
- **Example**: US-East, EU-West, APAC

### 4. Tenant-Based Sharding
```
Shard = tenant_mapping(tenant_id)
```
- **Use Case**: Multi-tenant SaaS applications
- **Example**: Company ID, Organization ID

## Replication

### Master-Slave Replication
- **Primary**: Handles all writes
- **Replicas**: Handle read queries
- **Async Replication**: Low latency writes
- **Sync Replication**: Strong consistency (optional)

### Failover Process
1. Health check detects primary failure
2. Promote replica to primary
3. Update shard registry
4. Sync remaining replicas

## Distributed Transactions

### Two-Phase Commit (2PC)

**Phase 1: Prepare**
```
Coordinator → All Shards: PREPARE
All Shards → Coordinator: READY/ABORT
```

**Phase 2: Commit**
```
If all READY:
  Coordinator → All Shards: COMMIT
Else:
  Coordinator → All Shards: ROLLBACK
```

## Query Routing

### Single-Shard Query
```java
// Route by hash
QueryRequest request = QueryRequest.builder()
    .sql("SELECT * FROM users WHERE user_id = ?")
    .shardingKey("user123")
    .build();
```

### Multi-Shard Query
```java
// Broadcast to all shards
QueryRequest request = QueryRequest.builder()
    .sql("SELECT COUNT(*) FROM orders")
    .build();
```

### Geo-Routed Query
```java
// Route to specific region
QueryRequest request = QueryRequest.builder()
    .sql("SELECT * FROM stores WHERE region = ?")
    .region("US-EAST")
    .build();
```

## Consistency Models

### Strong Consistency
- Synchronous replication
- Read from primary
- Higher latency

### Eventual Consistency
- Asynchronous replication
- Read from replicas
- Lower latency

### Causal Consistency
- Maintains causality
- Session guarantees

## Scale Calculations

### Storage Capacity
- **Shards**: 100
- **Replicas per Shard**: 3
- **Storage per Node**: 1TB
- **Total Capacity**: 100TB (33TB usable with 3x replication)

### Query Throughput
- **Reads per Shard**: 10K QPS
- **Total Read Capacity**: 1M QPS (100 shards)
- **Writes per Shard**: 5K QPS
- **Total Write Capacity**: 500K QPS

### Latency
- **Single-Shard Query**: <10ms
- **Multi-Shard Query**: <50ms
- **Cross-Shard Transaction**: <100ms

## Deployment

### Docker Compose
```yaml
version: '3.8'
services:
  coordinator:
    image: distributeddb:latest
    ports:
      - "8097:8097"
  
  shard-1-primary:
    image: postgres:14
    environment:
      POSTGRES_DB: shard1
  
  shard-1-replica:
    image: postgres:14
    environment:
      POSTGRES_DB: shard1
```

## Monitoring

### Health Metrics
- Shard availability
- Replication lag
- Query latency
- Connection pool usage

### Alerts
- Primary node failure
- Replication lag > 5s
- Query timeout
- Connection pool exhaustion

## Future Enhancements

1. **Auto-Sharding**: Automatic shard splitting
2. **Cross-Region Replication**: Global distribution
3. **Query Optimization**: Cost-based query planner
4. **Backup & Recovery**: Point-in-time recovery
5. **Security**: Encryption at rest and in transit
