# Distributed Database API Documentation

## Base URL
```
http://localhost:8097/api/v1/db
```

## Authentication
All requests require API key in header:
```
X-API-Key: your-api-key
```

## Endpoints

### 1. Execute Query

**POST** `/query`

Execute a query on the distributed database.

**Request Body:**
```json
{
  "queryId": "q-123",
  "sql": "SELECT * FROM users WHERE user_id = ?",
  "parameters": {
    "user_id": "user123"
  },
  "type": "SELECT",
  "shardingKey": "user123",
  "consistencyLevel": "STRONG",
  "timeoutMs": 5000
}
```

**Response:**
```json
[
  {
    "user_id": "user123",
    "name": "John Doe",
    "email": "john@example.com"
  }
]
```

### 2. Begin Transaction

**POST** `/transaction/begin`

Start a distributed transaction.

**Response:**
```json
{
  "transactionId": "txn-456"
}
```

### 3. Execute in Transaction

**POST** `/transaction/{txnId}/execute`

Execute query within a transaction.

**Request Body:**
```json
{
  "sql": "INSERT INTO orders (order_id, user_id, amount) VALUES (?, ?, ?)",
  "parameters": {
    "order_id": "order123",
    "user_id": "user123",
    "amount": 99.99
  },
  "type": "INSERT",
  "shardingKey": "user123"
}
```

### 4. Commit Transaction

**POST** `/transaction/{txnId}/commit`

Commit the transaction using 2PC.

**Response:**
```json
{
  "success": true
}
```

### 5. Rollback Transaction

**POST** `/transaction/{txnId}/rollback`

Rollback the transaction.

### 6. Register Shard

**POST** `/shards`

Register a new shard in the cluster.

**Request Body:**
```json
{
  "shardId": "shard-1",
  "type": "HASH",
  "strategy": "CONSISTENT_HASH",
  "primaryNode": "node-1",
  "replicaNodes": ["node-2", "node-3"],
  "region": "US-EAST"
}
```

### 7. Get All Shards

**GET** `/shards`

Retrieve all registered shards.

**Response:**
```json
[
  {
    "shardId": "shard-1",
    "type": "HASH",
    "strategy": "CONSISTENT_HASH",
    "primaryNode": "node-1",
    "replicaNodes": ["node-2", "node-3"],
    "status": "ACTIVE",
    "region": "US-EAST"
  }
]
```

### 8. Get Shard Details

**GET** `/shards/{shardId}`

Get details of a specific shard.

## Query Types

- `SELECT`: Read query
- `INSERT`: Insert operation
- `UPDATE`: Update operation
- `DELETE`: Delete operation
- `TRANSACTION`: Multi-statement transaction

## Consistency Levels

- `STRONG`: Read from primary, synchronous replication
- `EVENTUAL`: Read from replica, asynchronous replication
- `CAUSAL`: Maintains causality between operations

## Sharding Strategies

### Hash-Based
```json
{
  "shardingKey": "user123"
}
```

### Tenant-Based
```json
{
  "tenantId": "company-abc"
}
```

### Geo-Based
```json
{
  "region": "US-EAST"
}
```

## Error Codes

| Code | Description |
|------|-------------|
| 400 | Invalid request |
| 404 | Shard not found |
| 500 | Query execution failed |
| 503 | Shard unavailable |
| 504 | Query timeout |

## Examples

### Single-Shard Query
```bash
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM users WHERE user_id = ?",
    "parameters": {"user_id": "user123"},
    "type": "SELECT",
    "shardingKey": "user123",
    "consistencyLevel": "EVENTUAL"
  }'
```

### Multi-Shard Aggregation
```bash
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT COUNT(*) FROM orders",
    "type": "SELECT",
    "consistencyLevel": "EVENTUAL"
  }'
```

### Distributed Transaction
```bash
# Begin transaction
TXN_ID=$(curl -X POST http://localhost:8097/api/v1/db/transaction/begin | jq -r '.transactionId')

# Execute queries
curl -X POST http://localhost:8097/api/v1/db/transaction/$TXN_ID/execute \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "INSERT INTO orders VALUES (?, ?, ?)",
    "parameters": {"order_id": "o1", "user_id": "u1", "amount": 100},
    "type": "INSERT",
    "shardingKey": "u1"
  }'

# Commit
curl -X POST http://localhost:8097/api/v1/db/transaction/$TXN_ID/commit
```
