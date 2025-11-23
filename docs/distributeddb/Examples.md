# Distributed Database - Usage Examples

## Setup

Start the distributed database system:
```bash
./run-systems.sh distributeddb
```

## 1. Register Shards

### Hash-Based Shard
```bash
curl -X POST http://localhost:8097/api/v1/db/shards \
  -H "Content-Type: application/json" \
  -d '{
    "shardId": "shard-1",
    "type": "HASH",
    "strategy": "CONSISTENT_HASH",
    "primaryNode": "node-1",
    "replicaNodes": ["node-2", "node-3"],
    "region": "US-EAST"
  }'
```

### Geo-Based Shard
```bash
curl -X POST http://localhost:8097/api/v1/db/shards \
  -H "Content-Type: application/json" \
  -d '{
    "shardId": "shard-eu",
    "type": "GEO",
    "strategy": "GEO_PROXIMITY",
    "primaryNode": "node-eu-1",
    "replicaNodes": ["node-eu-2"],
    "region": "EU-WEST"
  }'
```

### Tenant-Based Shard
```bash
curl -X POST http://localhost:8097/api/v1/db/shards \
  -H "Content-Type: application/json" \
  -d '{
    "shardId": "shard-tenant-abc",
    "type": "COMPOSITE",
    "strategy": "TENANT_BASED",
    "primaryNode": "node-tenant-1",
    "replicaNodes": ["node-tenant-2"],
    "region": "US-WEST"
  }'
```

## 2. Query Execution

### Single-Shard Query (Strong Consistency)
```bash
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM users WHERE user_id = ?",
    "parameters": {"user_id": "user123"},
    "type": "SELECT",
    "shardingKey": "user123",
    "consistencyLevel": "STRONG"
  }'
```

### Single-Shard Query (Eventual Consistency - Read from Replica)
```bash
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM users WHERE user_id = ?",
    "parameters": {"user_id": "user456"},
    "type": "SELECT",
    "shardingKey": "user456",
    "consistencyLevel": "EVENTUAL"
  }'
```

### Multi-Shard Aggregation Query
```bash
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT COUNT(*) as total FROM orders",
    "type": "SELECT",
    "consistencyLevel": "EVENTUAL"
  }'
```

### Geo-Routed Query
```bash
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM stores WHERE region = ?",
    "parameters": {"region": "EU-WEST"},
    "type": "SELECT",
    "region": "EU-WEST",
    "consistencyLevel": "STRONG"
  }'
```

## 3. Write Operations

### Insert
```bash
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "INSERT INTO users (user_id, name, email) VALUES (?, ?, ?)",
    "parameters": {
      "user_id": "user789",
      "name": "John Doe",
      "email": "john@example.com"
    },
    "type": "INSERT",
    "shardingKey": "user789"
  }'
```

### Update
```bash
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "UPDATE users SET email = ? WHERE user_id = ?",
    "parameters": {
      "email": "newemail@example.com",
      "user_id": "user789"
    },
    "type": "UPDATE",
    "shardingKey": "user789"
  }'
```

### Delete
```bash
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "DELETE FROM users WHERE user_id = ?",
    "parameters": {"user_id": "user789"},
    "type": "DELETE",
    "shardingKey": "user789"
  }'
```

## 4. Distributed Transactions (2PC)

### Begin Transaction
```bash
TXN_ID=$(curl -s -X POST http://localhost:8097/api/v1/db/transaction/begin | jq -r '.transactionId')
echo "Transaction ID: $TXN_ID"
```

### Execute Multiple Queries in Transaction
```bash
# Query 1: Debit from account
curl -X POST http://localhost:8097/api/v1/db/transaction/$TXN_ID/execute \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "UPDATE accounts SET balance = balance - ? WHERE account_id = ?",
    "parameters": {"amount": 100, "account_id": "acc1"},
    "type": "UPDATE",
    "shardingKey": "acc1"
  }'

# Query 2: Credit to account
curl -X POST http://localhost:8097/api/v1/db/transaction/$TXN_ID/execute \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "UPDATE accounts SET balance = balance + ? WHERE account_id = ?",
    "parameters": {"amount": 100, "account_id": "acc2"},
    "type": "UPDATE",
    "shardingKey": "acc2"
  }'
```

### Commit Transaction
```bash
curl -X POST http://localhost:8097/api/v1/db/transaction/$TXN_ID/commit
```

### Rollback Transaction
```bash
curl -X POST http://localhost:8097/api/v1/db/transaction/$TXN_ID/rollback
```

## 5. Shard Management

### Get All Shards
```bash
curl -X GET http://localhost:8097/api/v1/db/shards
```

### Get Specific Shard
```bash
curl -X GET http://localhost:8097/api/v1/db/shards/shard-1
```

## 6. Real-World Use Cases

### E-commerce: User Orders
```bash
# Shard by user_id
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC LIMIT 10",
    "parameters": {"user_id": "user123"},
    "type": "SELECT",
    "shardingKey": "user123",
    "consistencyLevel": "STRONG"
  }'
```

### SaaS: Tenant Isolation
```bash
# Shard by tenant_id
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM documents WHERE tenant_id = ?",
    "parameters": {"tenant_id": "company-abc"},
    "type": "SELECT",
    "tenantId": "company-abc",
    "consistencyLevel": "STRONG"
  }'
```

### IoT: Time-Series Data
```bash
# Range-based sharding by timestamp
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM sensor_data WHERE device_id = ? AND timestamp > ?",
    "parameters": {
      "device_id": "sensor123",
      "timestamp": "2024-01-01"
    },
    "type": "SELECT",
    "shardingKey": "sensor123",
    "consistencyLevel": "EVENTUAL"
  }'
```

### Gaming: Geo-Sharding
```bash
# Route to nearest region
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM game_sessions WHERE player_id = ?",
    "parameters": {"player_id": "player456"},
    "type": "SELECT",
    "region": "EU-WEST",
    "consistencyLevel": "STRONG"
  }'
```

## 7. Performance Testing

### Concurrent Reads
```bash
for i in {1..100}; do
  curl -X POST http://localhost:8097/api/v1/db/query \
    -H "Content-Type: application/json" \
    -d "{
      \"sql\": \"SELECT * FROM users WHERE user_id = ?\",
      \"parameters\": {\"user_id\": \"user$i\"},
      \"type\": \"SELECT\",
      \"shardingKey\": \"user$i\",
      \"consistencyLevel\": \"EVENTUAL\"
    }" &
done
wait
```

### Batch Writes
```bash
for i in {1..50}; do
  curl -X POST http://localhost:8097/api/v1/db/query \
    -H "Content-Type: application/json" \
    -d "{
      \"sql\": \"INSERT INTO users (user_id, name) VALUES (?, ?)\",
      \"parameters\": {\"user_id\": \"user$i\", \"name\": \"User $i\"},
      \"type\": \"INSERT\",
      \"shardingKey\": \"user$i\"
    }" &
done
wait
```

## 8. Monitoring

### Check Shard Health
```bash
curl -X GET http://localhost:8097/api/v1/db/shards | jq '.[] | {shardId, status, lastHealthCheck}'
```

### View Shard Distribution
```bash
curl -X GET http://localhost:8097/api/v1/db/shards | jq '.[] | {shardId, region, primaryNode, replicaCount: (.replicaNodes | length)}'
```

## Best Practices

1. **Choose the Right Sharding Strategy**
   - Hash: Uniform distribution, no hotspots
   - Range: Sequential access patterns
   - Geo: Low latency for regional users
   - Tenant: Data isolation for multi-tenancy

2. **Consistency Level Selection**
   - STRONG: Financial transactions, critical data
   - EVENTUAL: Analytics, dashboards, feeds
   - CAUSAL: Social media, collaborative editing

3. **Connection Pooling**
   - Reuse connections across requests
   - Configure pool size based on load
   - Monitor connection usage

4. **Query Optimization**
   - Always include sharding key when possible
   - Avoid cross-shard joins
   - Use appropriate indexes
   - Batch operations when possible

5. **Failover Planning**
   - Maintain at least 2 replicas per shard
   - Monitor replication lag
   - Test failover procedures regularly
   - Have rollback plans
