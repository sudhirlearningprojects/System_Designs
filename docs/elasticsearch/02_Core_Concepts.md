# Core Concepts & Architecture

Deep dive into Elasticsearch's fundamental concepts and distributed architecture.

## Table of Contents
- [Documents](#documents)
- [Indices](#indices)
- [Shards](#shards)
- [Nodes](#nodes)
- [Clusters](#clusters)
- [Inverted Index](#inverted-index)
- [Distributed Architecture](#distributed-architecture)

## Documents

A **document** is the basic unit of information in Elasticsearch, represented as JSON.

### Document Structure

```json
{
  "_index": "products",
  "_id": "1",
  "_version": 1,
  "_seq_no": 0,
  "_primary_term": 1,
  "_source": {
    "name": "Laptop",
    "brand": "Dell",
    "price": 899.99,
    "specs": {
      "ram": "16GB",
      "storage": "512GB SSD"
    },
    "tags": ["electronics", "computers"],
    "created_at": "2024-01-15T10:30:00Z"
  }
}
```

### Metadata Fields

| Field | Description |
|-------|-------------|
| `_index` | Index name where document resides |
| `_id` | Unique document identifier |
| `_version` | Version number (increments on updates) |
| `_seq_no` | Sequence number for optimistic concurrency |
| `_primary_term` | Primary shard term |
| `_source` | Original JSON document |
| `_score` | Relevance score (in search results) |

### Document ID

**Auto-generated ID:**
```bash
POST /products/_doc
{
  "name": "Mouse"
}
# Elasticsearch generates: "1a2b3c4d5e6f"
```

**Custom ID:**
```bash
PUT /products/_doc/laptop-001
{
  "name": "Laptop"
}
```

### Document Versioning

Elasticsearch uses **optimistic concurrency control**:

```bash
# Initial index
PUT /products/_doc/1
{
  "name": "Laptop",
  "price": 899.99
}
# Returns: "_version": 1

# Update
POST /products/_update/1
{
  "doc": { "price": 799.99 }
}
# Returns: "_version": 2

# Conditional update (only if version is 2)
POST /products/_update/1?if_seq_no=1&if_primary_term=1
{
  "doc": { "price": 699.99 }
}
```

## Indices

An **index** is a collection of documents with similar characteristics.

### Index Naming Conventions

✅ **Good names:**
- `products`
- `logs-2024-01-15`
- `user-events`

❌ **Bad names:**
- `Products` (uppercase)
- `logs_2024.01.15` (dots can cause issues)
- `-products` (starts with hyphen)

### Index Settings

```bash
PUT /products
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2,
    "refresh_interval": "1s",
    "max_result_window": 10000
  },
  "mappings": {
    "properties": {
      "name": { "type": "text" },
      "price": { "type": "float" },
      "created_at": { "type": "date" }
    }
  }
}
```

### Index Lifecycle

```bash
# Create index
PUT /products

# Check if index exists
HEAD /products

# Get index info
GET /products

# Delete index
DELETE /products

# Close index (saves resources, not searchable)
POST /products/_close

# Reopen index
POST /products/_open
```

### Index Aliases

Aliases provide flexibility for zero-downtime reindexing:

```bash
# Create alias
POST /_aliases
{
  "actions": [
    { "add": { "index": "products-v1", "alias": "products" } }
  ]
}

# Switch alias to new index (zero downtime)
POST /_aliases
{
  "actions": [
    { "remove": { "index": "products-v1", "alias": "products" } },
    { "add": { "index": "products-v2", "alias": "products" } }
  ]
}

# Filtered alias (only electronics)
POST /_aliases
{
  "actions": [
    {
      "add": {
        "index": "products",
        "alias": "electronics",
        "filter": { "term": { "category": "electronics" } }
      }
    }
  ]
}
```

## Shards

A **shard** is a self-contained Lucene index that holds a subset of an index's data.

### Why Sharding?

1. **Horizontal scalability**: Distribute data across nodes
2. **Parallel processing**: Execute queries on multiple shards simultaneously
3. **Handle large datasets**: Single node can't hold petabytes

### Primary vs Replica Shards

```
Index: products (3 primary shards, 2 replicas)

┌─────────────────────────────────────────────────┐
│                   Node 1                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │ P0       │  │ R1       │  │ R2       │     │
│  │ (Primary)│  │ (Replica)│  │ (Replica)│     │
│  └──────────┘  └──────────┘  └──────────┘     │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│                   Node 2                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │ P1       │  │ R0       │  │ R2       │     │
│  │ (Primary)│  │ (Replica)│  │ (Replica)│     │
│  └──────────┘  └──────────┘  └──────────┘     │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│                   Node 3                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │ P2       │  │ R0       │  │ R1       │     │
│  │ (Primary)│  │ (Replica)│  │ (Replica)│     │
│  └──────────┘  └──────────┘  └──────────┘     │
└─────────────────────────────────────────────────┘
```

**Primary Shard:**
- Handles write operations
- Fixed number (cannot change after index creation)
- Each document belongs to exactly one primary shard

**Replica Shard:**
- Copy of primary shard
- Handles read operations
- Provides redundancy and high availability
- Can be changed dynamically

### Shard Routing

Documents are routed to shards using a hash function:

```
shard_num = hash(document_id) % number_of_primary_shards
```

**Example:**
```bash
# Index with 3 primary shards
PUT /products/_doc/laptop-001
{
  "name": "Laptop"
}

# Routing calculation:
# shard = hash("laptop-001") % 3
# shard = 2 (document goes to shard 2)
```

**Custom routing:**
```bash
PUT /products/_doc/laptop-001?routing=electronics
{
  "name": "Laptop"
}
# All electronics products go to same shard
```

### Shard Sizing Best Practices

**Rule of thumb:**
- Shard size: 10-50 GB
- Shards per node: < 20 per GB of heap
- Total shards: Avoid thousands of tiny shards

**Example calculation:**
```
Dataset: 1 TB
Shard size target: 30 GB
Number of shards: 1000 GB / 30 GB ≈ 33 shards

With 2 replicas:
Total shards: 33 × (1 + 2) = 99 shards
Minimum nodes: 99 / 20 = 5 nodes (with 1 GB heap each)
```

### Over-sharding Problem

❌ **Too many shards:**
```bash
PUT /products
{
  "settings": {
    "number_of_shards": 100  # BAD for small dataset
  }
}
```

**Problems:**
- High overhead (each shard is a Lucene index)
- Slow cluster state updates
- Memory waste
- Slower searches (query all shards)

✅ **Right-sized shards:**
```bash
PUT /products
{
  "settings": {
    "number_of_shards": 3  # GOOD for moderate dataset
  }
}
```

## Nodes

A **node** is a single Elasticsearch server instance.

### Node Types

#### 1. Master Node
**Role:** Cluster management
- Manages cluster state
- Creates/deletes indices
- Allocates shards to nodes
- Tracks node membership

```yaml
# elasticsearch.yml
node.roles: [ master ]
```

#### 2. Data Node
**Role:** Store data and execute queries
- Holds shards
- Executes search queries
- Performs aggregations
- Handles CRUD operations

```yaml
node.roles: [ data ]
```

#### 3. Ingest Node
**Role:** Pre-process documents before indexing
- Transform and enrich data
- Parse logs
- Convert formats

```yaml
node.roles: [ ingest ]
```

#### 4. Coordinating Node
**Role:** Route requests and merge results
- Receives client requests
- Distributes queries to data nodes
- Merges results
- Returns response to client

```yaml
node.roles: [ ]  # No roles = coordinating only
```

#### 5. Machine Learning Node
**Role:** Run ML jobs
- Anomaly detection
- Forecasting
- Data frame analytics

```yaml
node.roles: [ ml ]
```

### Typical Cluster Setup

**Small cluster (< 10 nodes):**
```
3 nodes: master + data + ingest
```

**Medium cluster (10-50 nodes):**
```
3 dedicated master nodes
10+ data nodes
2 coordinating nodes
```

**Large cluster (50+ nodes):**
```
3 dedicated master nodes
50+ data nodes (hot/warm/cold tiers)
5+ coordinating nodes
2+ ingest nodes
2+ ML nodes
```

### Node Discovery

Nodes discover each other using:

**Unicast (default):**
```yaml
# elasticsearch.yml
discovery.seed_hosts:
  - 192.168.1.10:9300
  - 192.168.1.11:9300
  - 192.168.1.12:9300
```

**Cluster formation:**
```yaml
cluster.initial_master_nodes:
  - master-node-1
  - master-node-2
  - master-node-3
```

## Clusters

A **cluster** is a collection of nodes that work together.

### Cluster State

The cluster state contains:
- All indices and their settings
- Shard allocation (which shard on which node)
- Cluster settings
- Index templates
- Aliases

```bash
GET /_cluster/state

# Response (simplified):
{
  "cluster_name": "production",
  "version": 123,
  "master_node": "node-1",
  "nodes": {
    "node-1": { "name": "master-1" },
    "node-2": { "name": "data-1" }
  },
  "routing_table": {
    "indices": {
      "products": {
        "shards": {
          "0": [
            { "state": "STARTED", "node": "node-2" }
          ]
        }
      }
    }
  }
}
```

### Cluster Health

```bash
GET /_cluster/health

{
  "cluster_name": "production",
  "status": "green",
  "timed_out": false,
  "number_of_nodes": 3,
  "number_of_data_nodes": 2,
  "active_primary_shards": 10,
  "active_shards": 20,
  "relocating_shards": 0,
  "initializing_shards": 0,
  "unassigned_shards": 0
}
```

**Status meanings:**
- 🟢 **Green**: All primary and replica shards allocated
- 🟡 **Yellow**: All primary shards allocated, some replicas missing
- 🔴 **Red**: Some primary shards not allocated (data loss risk)

### Split Brain Problem

**Problem:** Network partition causes multiple masters

```
Before partition:
┌─────────────────────────────┐
│  Master + Node1 + Node2     │
└─────────────────────────────┘

After partition:
┌──────────────┐    ┌──────────────┐
│  Master      │    │  Node1+Node2 │
│  (thinks it's│    │  (elect new  │
│   the master)│    │   master)    │
└──────────────┘    └──────────────┘
```

**Solution:** Quorum-based master election

```yaml
# Minimum master nodes = (total_master_nodes / 2) + 1
discovery.zen.minimum_master_nodes: 2  # For 3 master nodes
```

## Inverted Index

The **inverted index** is the core data structure that makes Elasticsearch fast.

### How It Works

**Original documents:**
```
Doc 1: "The quick brown fox"
Doc 2: "The lazy dog"
Doc 3: "Quick brown dogs"
```

**Inverted index:**
```
Term      | Document IDs | Frequency
----------|--------------|----------
brown     | [1, 3]       | 2
dog       | [2]          | 1
dogs      | [3]          | 1
fox       | [1]          | 1
lazy      | [2]          | 1
quick     | [1, 3]       | 2
the       | [1, 2]       | 2
```

### Search Example

**Query:** "quick dog"

**Process:**
1. Tokenize: ["quick", "dog"]
2. Lookup in inverted index:
   - "quick" → [1, 3]
   - "dog" → [2]
3. Union: [1, 2, 3]
4. Score and rank documents

### Analysis Process

```
Original text: "The Quick BROWN fox!"

↓ Character filters (lowercase, remove punctuation)

"the quick brown fox"

↓ Tokenizer (split on whitespace)

["the", "quick", "brown", "fox"]

↓ Token filters (remove stopwords, stemming)

["quick", "brown", "fox"]

↓ Store in inverted index
```

### Field Data Types

**Text vs Keyword:**

```bash
PUT /products
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text",           # Full-text search
        "fields": {
          "keyword": {
            "type": "keyword"      # Exact match, sorting, aggregations
          }
        }
      }
    }
  }
}
```

**Text field (analyzed):**
- "MacBook Pro 16-inch" → ["macbook", "pro", "16", "inch"]
- Use for: Full-text search

**Keyword field (not analyzed):**
- "MacBook Pro 16-inch" → "MacBook Pro 16-inch"
- Use for: Exact match, sorting, aggregations

## Distributed Architecture

### Write Operation Flow

```
1. Client → Coordinating Node
2. Coordinating Node → Primary Shard
3. Primary Shard → Replica Shards (parallel)
4. Replica Shards → Acknowledge
5. Primary Shard → Acknowledge
6. Coordinating Node → Client
```

**Detailed example:**
```bash
PUT /products/_doc/1
{
  "name": "Laptop"
}
```

```
┌─────────┐
│ Client  │
└────┬────┘
     │ 1. Index request
     ▼
┌─────────────────┐
│ Coordinating    │
│ Node            │
└────┬────────────┘
     │ 2. Route to shard (hash-based)
     │    shard = hash("1") % 3 = 2
     ▼
┌─────────────────┐
│ Primary Shard 2 │
│ (Node 2)        │
└────┬────────────┘
     │ 3. Replicate (parallel)
     ├──────────────┬──────────────┐
     ▼              ▼              ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│ Replica  │  │ Replica  │  │ Replica  │
│ Shard 2  │  │ Shard 2  │  │ Shard 2  │
│ (Node 1) │  │ (Node 3) │  │ (Node 4) │
└──────────┘  └──────────┘  └──────────┘
     │              │              │
     └──────────────┴──────────────┘
                    │ 4. Acknowledge
                    ▼
              ┌──────────┐
              │ Client   │
              └──────────┘
```

### Read Operation Flow

```
1. Client → Coordinating Node
2. Coordinating Node → All Shards (primary or replica)
3. Each Shard → Execute query locally
4. Shards → Return results to Coordinating Node
5. Coordinating Node → Merge and sort results
6. Coordinating Node → Client
```

**Detailed example:**
```bash
GET /products/_search
{
  "query": { "match": { "name": "laptop" } }
}
```

```
┌─────────┐
│ Client  │
└────┬────┘
     │ 1. Search request
     ▼
┌─────────────────┐
│ Coordinating    │
│ Node            │
└────┬────────────┘
     │ 2. Broadcast to all shards
     ├──────────────┬──────────────┬──────────────┐
     ▼              ▼              ▼              ▼
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ Shard 0  │  │ Shard 1  │  │ Shard 2  │  │ Shard 3  │
│ (Node 1) │  │ (Node 2) │  │ (Node 3) │  │ (Node 4) │
└────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
     │ 3. Execute query locally
     │              │              │              │
     └──────────────┴──────────────┴──────────────┘
                    │ 4. Return top results
                    ▼
              ┌──────────────┐
              │ Coordinating │
              │ Node         │
              └──────┬───────┘
                     │ 5. Merge, sort, paginate
                     ▼
              ┌──────────┐
              │ Client   │
              └──────────┘
```

### Consistency Model

Elasticsearch uses **eventual consistency** with tunable consistency levels:

**Write consistency:**
```bash
PUT /products/_doc/1?wait_for_active_shards=2
{
  "name": "Laptop"
}
# Wait for 2 shards (primary + 1 replica) before returning
```

**Read consistency:**
```bash
GET /products/_doc/1?preference=_primary
# Read from primary shard only (stronger consistency)

GET /products/_doc/1?preference=_local
# Read from local shard (better performance)
```

## Summary

| Concept | Description | Analogy |
|---------|-------------|---------|
| **Document** | JSON object with data | Row in SQL table |
| **Index** | Collection of documents | Database in SQL |
| **Shard** | Subset of index data | Partition in distributed DB |
| **Node** | Single Elasticsearch instance | Server in cluster |
| **Cluster** | Collection of nodes | Database cluster |
| **Inverted Index** | Term → Document mapping | Book index |

---

**Previous**: [← Introduction](01_Introduction.md) | **Next**: [Indexing & Document Management →](03_Indexing.md)
