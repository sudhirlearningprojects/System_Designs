# Introduction to Elasticsearch

## What is Elasticsearch?

Elasticsearch is a **distributed, RESTful search and analytics engine** capable of addressing a growing number of use cases. As the heart of the Elastic Stack, it centrally stores your data for lightning-fast search, fine-tuned relevancy, and powerful analytics that scale with ease.

### Built on Apache Lucene

Elasticsearch is built on top of **Apache Lucene**, a high-performance, full-featured text search engine library written in Java. While Lucene is powerful, it's complex to use directly. Elasticsearch wraps Lucene with:
- RESTful API
- Distributed architecture
- Automatic sharding and replication
- JSON document storage
- Real-time indexing and search

## History & Evolution

### Timeline

- **2004**: Apache Lucene released
- **2010**: Elasticsearch 0.4 released by Shay Banon
- **2012**: Elasticsearch 0.19 - First production-ready version
- **2013**: Elasticsearch 1.0 released
- **2015**: Elastic company formed, ELK Stack popularized
- **2017**: Elasticsearch 6.0 - Removal of multiple types per index
- **2019**: Elasticsearch 7.0 - Default to 1 shard per index
- **2021**: Elasticsearch 8.0 - Vector search, improved security
- **2023**: Elasticsearch 8.11 - Enhanced ML capabilities

### Why Was Elasticsearch Created?

Shay Banon created Elasticsearch to solve a simple problem: **helping his wife search through recipes**. He needed:
- Fast full-text search
- Easy to scale
- Simple to use
- Real-time results

Traditional databases (MySQL, PostgreSQL) were too slow for full-text search on large datasets.

## Core Philosophy

### 1. **Distributed by Default**
Every index is automatically sharded and replicated across nodes for high availability and performance.

### 2. **Near Real-Time (NRT)**
Documents are searchable within ~1 second of being indexed (refresh interval).

### 3. **Schema-Free (Dynamic Mapping)**
No need to define schema upfront. Elasticsearch automatically detects field types.

### 4. **RESTful API**
All operations via simple HTTP requests with JSON payloads.

### 5. **Horizontal Scalability**
Add more nodes to scale linearly. No downtime required.

## Elasticsearch vs Traditional Databases

| Feature | Elasticsearch | Relational DB (MySQL/PostgreSQL) |
|---------|---------------|----------------------------------|
| **Primary Use** | Search & Analytics | Transactional data |
| **Data Model** | Document (JSON) | Tables with rows |
| **Schema** | Dynamic (schema-free) | Fixed schema required |
| **Search** | Full-text search with relevance | Basic LIKE queries |
| **Scalability** | Horizontal (add nodes) | Vertical (bigger server) |
| **ACID** | ❌ Not ACID compliant | ✅ ACID compliant |
| **Joins** | Limited (nested/parent-child) | Full JOIN support |
| **Speed** | Optimized for reads | Balanced read/write |
| **Use Case** | Search, logs, analytics | Transactions, consistency |

### When to Use Elasticsearch

✅ **Use Elasticsearch when you need:**
- Full-text search with relevance scoring
- Log aggregation and analysis
- Real-time analytics and dashboards
- Geo-spatial search
- Application search (products, documents, etc.)
- Time-series data analysis
- Security analytics (SIEM)

❌ **Don't use Elasticsearch when you need:**
- ACID transactions
- Strong consistency guarantees
- Complex joins across multiple entities
- Primary data store for critical transactional data
- Frequent updates to the same document

## Key Concepts Overview

### 1. **Document**
A document is a JSON object that contains your data. It's the basic unit of information.

```json
{
  "_index": "products",
  "_id": "1",
  "_source": {
    "name": "Laptop",
    "brand": "Dell",
    "price": 899.99,
    "category": "Electronics"
  }
}
```

### 2. **Index**
An index is a collection of documents with similar characteristics. Think of it as a "database" in SQL terms.

- `products` index → stores product documents
- `logs-2024-01` index → stores log documents for January 2024

### 3. **Shard**
An index is divided into multiple shards for horizontal scalability. Each shard is a self-contained Lucene index.

- **Primary shard**: Original shard containing data
- **Replica shard**: Copy of primary shard for redundancy

### 4. **Node**
A single Elasticsearch server instance. Multiple nodes form a cluster.

### 5. **Cluster**
A collection of nodes that work together to store data and provide search capabilities.

## Architecture at a Glance

```
┌─────────────────────────────────────────────────────────┐
│                    Elasticsearch Cluster                 │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Node 1     │  │   Node 2     │  │   Node 3     │ │
│  │  (Master)    │  │   (Data)     │  │   (Data)     │ │
│  │              │  │              │  │              │ │
│  │  Shard P0    │  │  Shard P1    │  │  Shard R0    │ │
│  │  Shard R1    │  │  Shard R2    │  │  Shard P2    │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
│                                                          │
└─────────────────────────────────────────────────────────┘
         ▲                    ▲                    ▲
         │                    │                    │
         └────────────────────┴────────────────────┘
                        RESTful API
                      (HTTP/JSON)
```

## How Elasticsearch Works (High-Level)

### Indexing Flow

```
1. Client sends document → 2. Routing to shard → 3. Index to Lucene → 4. Refresh (make searchable)
```

**Example:**
```bash
POST /products/_doc/1
{
  "name": "Laptop",
  "price": 899.99
}
```

1. Document sent to coordinating node
2. Node calculates shard: `shard = hash(document_id) % num_primary_shards`
3. Document indexed to primary shard
4. Replicated to replica shards
5. After refresh interval (~1s), document is searchable

### Search Flow

```
1. Client sends query → 2. Broadcast to all shards → 3. Each shard searches → 4. Results merged → 5. Return to client
```

**Example:**
```bash
GET /products/_search
{
  "query": {
    "match": { "name": "laptop" }
  }
}
```

1. Query sent to coordinating node
2. Query broadcast to all shards (primary or replica)
3. Each shard executes query locally
4. Results collected and merged (sorted by relevance)
5. Top results returned to client

## Real-World Example: E-commerce Search

### Problem
An e-commerce site with 10 million products needs:
- Fast product search (< 100ms)
- Typo tolerance ("laptp" → "laptop")
- Filters (price range, brand, category)
- Sorting by relevance, price, popularity
- Faceted search (show counts per category)

### Traditional Database Approach (MySQL)

```sql
SELECT * FROM products 
WHERE name LIKE '%laptop%' 
  AND price BETWEEN 500 AND 1000
  AND brand = 'Dell'
ORDER BY relevance DESC
LIMIT 20;
```

**Problems:**
- `LIKE '%laptop%'` requires full table scan (slow)
- No relevance scoring
- No typo tolerance
- Slow on millions of rows

### Elasticsearch Approach

```json
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "laptop" } }
      ],
      "filter": [
        { "range": { "price": { "gte": 500, "lte": 1000 } } },
        { "term": { "brand": "Dell" } }
      ]
    }
  },
  "aggs": {
    "categories": {
      "terms": { "field": "category" }
    }
  }
}
```

**Benefits:**
- ✅ Full-text search with relevance scoring
- ✅ Typo tolerance (fuzzy matching)
- ✅ Fast filters (< 100ms)
- ✅ Aggregations (faceted search)
- ✅ Scales to billions of documents

## Installation & Setup

### Option 1: Docker (Recommended for Development)

```bash
# Single node cluster
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0

# Verify installation
curl http://localhost:9200

# Response:
{
  "name" : "elasticsearch",
  "cluster_name" : "docker-cluster",
  "version" : {
    "number" : "8.11.0"
  },
  "tagline" : "You Know, for Search"
}
```

### Option 2: Download & Install

```bash
# Download
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.11.0-linux-x86_64.tar.gz

# Extract
tar -xzf elasticsearch-8.11.0-linux-x86_64.tar.gz
cd elasticsearch-8.11.0/

# Start
./bin/elasticsearch
```

### Option 3: Elastic Cloud (Managed Service)

Sign up at [cloud.elastic.co](https://cloud.elastic.co) for a fully managed Elasticsearch cluster.

## First Steps with Elasticsearch

### 1. Check Cluster Health

```bash
GET /_cluster/health

# Response:
{
  "cluster_name": "docker-cluster",
  "status": "green",
  "number_of_nodes": 1,
  "active_primary_shards": 0,
  "active_shards": 0
}
```

**Status meanings:**
- 🟢 **Green**: All shards allocated
- 🟡 **Yellow**: All primary shards allocated, some replicas missing
- 🔴 **Red**: Some primary shards not allocated

### 2. Create an Index

```bash
PUT /products
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1
  }
}
```

### 3. Index a Document

```bash
POST /products/_doc/1
{
  "name": "Laptop",
  "brand": "Dell",
  "price": 899.99,
  "category": "Electronics",
  "in_stock": true
}
```

### 4. Get a Document

```bash
GET /products/_doc/1

# Response:
{
  "_index": "products",
  "_id": "1",
  "_version": 1,
  "found": true,
  "_source": {
    "name": "Laptop",
    "brand": "Dell",
    "price": 899.99
  }
}
```

### 5. Search Documents

```bash
GET /products/_search
{
  "query": {
    "match": {
      "name": "laptop"
    }
  }
}
```

### 6. Update a Document

```bash
POST /products/_update/1
{
  "doc": {
    "price": 799.99
  }
}
```

### 7. Delete a Document

```bash
DELETE /products/_doc/1
```

## Elasticsearch Terminology Mapping

| Elasticsearch | Relational DB | MongoDB |
|---------------|---------------|---------|
| Index | Database | Database |
| Document | Row | Document |
| Field | Column | Field |
| Mapping | Schema | Schema |
| Query DSL | SQL | Query Language |
| Shard | Partition | Shard |

## Common Misconceptions

### ❌ Myth 1: "Elasticsearch is a database"
**Reality**: Elasticsearch is a search engine, not a primary database. It lacks ACID guarantees and should be used alongside a primary database.

### ❌ Myth 2: "Elasticsearch is only for logs"
**Reality**: While popular for logs (ELK stack), Elasticsearch excels at full-text search, analytics, and many other use cases.

### ❌ Myth 3: "Elasticsearch is real-time"
**Reality**: It's "near real-time" with a default 1-second refresh interval. Documents aren't immediately searchable.

### ❌ Myth 4: "Elasticsearch is easy to scale"
**Reality**: While it scales horizontally, proper scaling requires understanding sharding, replication, and cluster management.

### ❌ Myth 5: "Elasticsearch doesn't need tuning"
**Reality**: Production deployments require careful tuning of JVM heap, shard sizes, refresh intervals, and more.

## Next Steps

Now that you understand the basics, dive deeper into:
- [Core Concepts & Architecture](02_Core_Concepts.md) - Detailed architecture
- [Indexing & Document Management](03_Indexing.md) - How to index data
- [Search & Query DSL](04_Search_Query_DSL.md) - Powerful search queries

---

**Previous**: [← README](README.md) | **Next**: [Core Concepts & Architecture →](02_Core_Concepts.md)
