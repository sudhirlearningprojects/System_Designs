# Production Best Practices

Comprehensive guide to deploying, operating, and optimizing Elasticsearch in production environments.

## Table of Contents
- [Architecture & Design](#architecture--design)
- [Hardware & Infrastructure](#hardware--infrastructure)
- [Index Design](#index-design)
- [Query Optimization](#query-optimization)
- [Monitoring & Alerting](#monitoring--alerting)
- [Security](#security)
- [Backup & Disaster Recovery](#backup--disaster-recovery)
- [Common Pitfalls](#common-pitfalls)

## Architecture & Design

### Cluster Sizing

**Small cluster (< 1TB data):**
```
3 nodes: master + data + ingest
- 16GB RAM per node
- 4 CPU cores
- 500GB SSD
```

**Medium cluster (1-10TB data):**
```
3 dedicated master nodes (4GB RAM, 2 CPU)
5-10 data nodes (32GB RAM, 8 CPU, 1TB SSD)
2 coordinating nodes (16GB RAM, 4 CPU)
```

**Large cluster (10TB+ data):**
```
3 dedicated master nodes
20+ data nodes (hot/warm/cold tiers)
5+ coordinating nodes
2+ ingest nodes
```

### Node Roles Best Practices

✅ **DO:**
- Separate master and data nodes in production
- Use dedicated coordinating nodes for heavy query load
- Implement hot-warm-cold architecture for time-series data

❌ **DON'T:**
- Run master-eligible nodes on same hardware as data nodes
- Use single-role nodes in small clusters (overhead)
- Mix different hardware specs in same tier

### Hot-Warm-Cold Architecture

```
┌─────────────────────────────────────────────────┐
│  Hot Tier (Recent data, frequent writes)       │
│  - Fast SSDs                                    │
│  - High CPU/RAM                                 │
│  - Last 7 days                                  │
└────────┬────────────────────────────────────────┘
         │ ILM transition after 7 days
         ▼
┌─────────────────────────────────────────────────┐
│  Warm Tier (Older data, read-only)             │
│  - Standard SSDs                                │
│  - Medium CPU/RAM                               │
│  - 7-30 days                                    │
└────────┬────────────────────────────────────────┘
         │ ILM transition after 30 days
         ▼
┌─────────────────────────────────────────────────┐
│  Cold Tier (Archive, rare access)              │
│  - HDDs or S3                                   │
│  - Low CPU/RAM                                  │
│  - 30-90 days                                   │
└────────┬────────────────────────────────────────┘
         │ ILM delete after 90 days
         ▼
       Delete
```

**ILM Policy:**
```json
PUT _ilm/policy/logs-policy
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d",
            "max_docs": 10000000
          },
          "set_priority": {
            "priority": 100
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": {
            "number_of_shards": 1
          },
          "forcemerge": {
            "max_num_segments": 1
          },
          "set_priority": {
            "priority": 50
          }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "searchable_snapshot": {
            "snapshot_repository": "cold-repo"
          },
          "set_priority": {
            "priority": 0
          }
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

## Hardware & Infrastructure

### Memory (Most Critical)

**Heap size:**
```bash
# elasticsearch.yml or jvm.options
-Xms16g
-Xmx16g  # Same as Xms (avoid resizing)
```

**Rules:**
- Set heap to 50% of RAM (max 31GB)
- Leave 50% for OS file system cache
- Never exceed 31GB (compressed pointers threshold)

**Example:**
```
64GB RAM server:
- 31GB heap
- 33GB for OS cache
```

### Storage

**SSD is mandatory for:**
- Hot tier data nodes
- Master nodes
- Coordinating nodes

**HDD acceptable for:**
- Cold tier (with searchable snapshots)
- Backup storage

**RAID configuration:**
- RAID 0 for performance (Elasticsearch handles replication)
- RAID 10 for extra safety (but expensive)

### Network

**Requirements:**
- 10 Gbps network for large clusters
- Low latency (< 1ms between nodes)
- Dedicated network for cluster communication

**Configuration:**
```yaml
# elasticsearch.yml
network.host: 0.0.0.0
http.port: 9200
transport.port: 9300
```

## Index Design

### Shard Sizing

**Golden rules:**
- Shard size: 10-50GB (optimal: 20-30GB)
- Shards per node: < 20 per GB of heap
- Total shards: Minimize (overhead per shard)

**Calculation example:**
```
Dataset: 500GB
Target shard size: 25GB
Number of shards: 500 / 25 = 20 shards

With 2 replicas:
Total shards: 20 × (1 + 2) = 60 shards

Heap per node: 16GB
Max shards per node: 16 × 20 = 320 shards
Minimum nodes: 60 / 320 = 1 node (but use 3+ for HA)
```

### Time-Series Indices

**Pattern:**
```
logs-2024-01-15
logs-2024-01-16
logs-2024-01-17
```

**Benefits:**
- Easy to delete old data
- Optimize settings per time period
- Better performance (smaller indices)

**Index template:**
```json
PUT _index_template/logs-template
{
  "index_patterns": ["logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "refresh_interval": "30s",
      "index.lifecycle.name": "logs-policy"
    },
    "mappings": {
      "properties": {
        "@timestamp": { "type": "date" },
        "message": { "type": "text" },
        "level": { "type": "keyword" }
      }
    }
  }
}
```

### Mapping Best Practices

✅ **DO:**
```json
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "status": { "type": "keyword" },
      "price": { "type": "float" },
      "created_at": { "type": "date" }
    }
  }
}
```

❌ **DON'T:**
```json
{
  "mappings": {
    "dynamic": "true",  // Avoid dynamic mapping in production
    "properties": {
      "name": { "type": "text" },  // Missing .keyword field
      "status": { "type": "text" },  // Should be keyword
      "price": { "type": "text" }  // Should be numeric
    }
  }
}
```

### Disable Features You Don't Need

```json
{
  "mappings": {
    "properties": {
      "description": {
        "type": "text",
        "index": false,  // Don't index if not searching
        "norms": false,  // Disable scoring if not needed
        "doc_values": false  // Disable if not sorting/aggregating
      }
    }
  }
}
```

## Query Optimization

### Use Filter Context

❌ **Slow (query context):**
```json
{
  "query": {
    "bool": {
      "must": [
        { "term": { "status": "active" } },
        { "range": { "price": { "gte": 100 } } }
      ]
    }
  }
}
```

✅ **Fast (filter context):**
```json
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "status": "active" } },
        { "range": { "price": { "gte": 100 } } }
      ]
    }
  }
}
```

### Avoid Deep Pagination

❌ **Slow:**
```json
{
  "from": 10000,
  "size": 20
}
```

✅ **Fast (search_after):**
```json
{
  "size": 20,
  "search_after": [1000, "doc_123"],
  "sort": [
    { "price": "asc" },
    { "_id": "asc" }
  ]
}
```

### Use Routing

```json
// Index with routing
PUT /products/_doc/1?routing=electronics
{
  "name": "Laptop",
  "category": "electronics"
}

// Search with routing (faster)
GET /products/_search?routing=electronics
{
  "query": {
    "match": { "name": "laptop" }
  }
}
```

### Limit Result Size

```json
{
  "size": 20,  // Don't fetch more than needed
  "_source": ["name", "price"],  // Only return needed fields
  "track_total_hits": false  // Disable if not needed
}
```

## Monitoring & Alerting

### Key Metrics to Monitor

**Cluster health:**
```bash
GET /_cluster/health
```

**Node stats:**
```bash
GET /_nodes/stats
```

**Index stats:**
```bash
GET /_stats
```

### Critical Alerts

**1. Cluster status RED:**
```
Alert: Cluster status is RED
Condition: cluster.status == "red"
Action: Immediate investigation (data loss risk)
```

**2. High heap usage:**
```
Alert: High heap usage
Condition: jvm.mem.heap_used_percent > 85%
Action: Scale up or optimize queries
```

**3. High CPU:**
```
Alert: High CPU usage
Condition: os.cpu.percent > 80%
Action: Investigate heavy queries or indexing
```

**4. Disk space:**
```
Alert: Low disk space
Condition: fs.total.available_in_bytes < 10GB
Action: Add storage or delete old indices
```

**5. Slow queries:**
```
Alert: Slow queries detected
Condition: search.query_time_in_millis > 1000
Action: Optimize queries or add resources
```

### Monitoring Stack

**Prometheus + Grafana:**
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'elasticsearch'
    static_configs:
      - targets: ['localhost:9200']
    metrics_path: '/_prometheus/metrics'
```

**Elastic Stack Monitoring:**
```yaml
# elasticsearch.yml
xpack.monitoring.enabled: true
xpack.monitoring.collection.enabled: true
```

## Security

### Enable Security

```yaml
# elasticsearch.yml
xpack.security.enabled: true
xpack.security.transport.ssl.enabled: true
xpack.security.http.ssl.enabled: true
```

### User Authentication

```bash
# Create user
POST /_security/user/john
{
  "password": "strong_password",
  "roles": ["kibana_admin", "monitoring_user"],
  "full_name": "John Doe",
  "email": "john@example.com"
}
```

### Role-Based Access Control

```bash
# Create role
POST /_security/role/logs_reader
{
  "cluster": ["monitor"],
  "indices": [
    {
      "names": ["logs-*"],
      "privileges": ["read", "view_index_metadata"]
    }
  ]
}
```

### API Key Authentication

```bash
# Create API key
POST /_security/api_key
{
  "name": "my-api-key",
  "role_descriptors": {
    "logs_reader": {
      "cluster": ["monitor"],
      "index": [
        {
          "names": ["logs-*"],
          "privileges": ["read"]
        }
      ]
    }
  }
}
```

### Network Security

```yaml
# elasticsearch.yml
network.host: 0.0.0.0
http.host: 0.0.0.0
transport.host: 0.0.0.0

# Firewall rules
# Allow 9200 (HTTP) only from application servers
# Allow 9300 (Transport) only from cluster nodes
```

## Backup & Disaster Recovery

### Snapshot Repository

```bash
# Register repository
PUT /_snapshot/my_backup
{
  "type": "s3",
  "settings": {
    "bucket": "my-elasticsearch-backups",
    "region": "us-east-1",
    "base_path": "snapshots"
  }
}
```

### Create Snapshot

```bash
# Manual snapshot
PUT /_snapshot/my_backup/snapshot_1
{
  "indices": "logs-*,products",
  "ignore_unavailable": true,
  "include_global_state": false
}

# Automated snapshot policy
PUT /_slm/policy/daily-snapshots
{
  "schedule": "0 0 * * *",
  "name": "<daily-snap-{now/d}>",
  "repository": "my_backup",
  "config": {
    "indices": ["*"],
    "ignore_unavailable": true,
    "include_global_state": false
  },
  "retention": {
    "expire_after": "30d",
    "min_count": 5,
    "max_count": 50
  }
}
```

### Restore Snapshot

```bash
# Restore all indices
POST /_snapshot/my_backup/snapshot_1/_restore

# Restore specific indices
POST /_snapshot/my_backup/snapshot_1/_restore
{
  "indices": "logs-2024-01-15",
  "ignore_unavailable": true,
  "include_global_state": false,
  "rename_pattern": "(.+)",
  "rename_replacement": "restored_$1"
}
```

## Common Pitfalls

### 1. Too Many Shards

❌ **Problem:**
```
1000 indices × 5 shards × 2 replicas = 15,000 shards
```

**Impact:**
- Slow cluster state updates
- High memory overhead
- Slow searches

✅ **Solution:**
- Reduce number of shards
- Use rollover for time-series data
- Shrink old indices

### 2. Large Heap Size

❌ **Problem:**
```
-Xms64g -Xmx64g  # Too large!
```

**Impact:**
- Long GC pauses
- Compressed pointers disabled
- Poor performance

✅ **Solution:**
```
-Xms31g -Xmx31g  # Max recommended
```

### 3. No Replicas

❌ **Problem:**
```json
{
  "settings": {
    "number_of_replicas": 0
  }
}
```

**Impact:**
- No redundancy
- Data loss on node failure
- No load balancing for reads

✅ **Solution:**
```json
{
  "settings": {
    "number_of_replicas": 1  # Minimum for production
  }
}
```

### 4. Frequent Refresh

❌ **Problem:**
```json
{
  "settings": {
    "refresh_interval": "1s"  # Default, too frequent for bulk indexing
  }
}
```

**Impact:**
- High CPU usage
- Slow indexing

✅ **Solution:**
```json
{
  "settings": {
    "refresh_interval": "30s"  # Or disable during bulk indexing
  }
}
```

### 5. Not Using Bulk API

❌ **Slow:**
```bash
for doc in documents:
    POST /index/_doc
    { "data": doc }
```

✅ **Fast:**
```bash
POST /_bulk
{ "index": { "_index": "products" } }
{ "name": "Product 1" }
{ "index": { "_index": "products" } }
{ "name": "Product 2" }
```

### 6. Ignoring Cluster Health

❌ **Problem:**
- Not monitoring cluster status
- Ignoring yellow/red status

**Impact:**
- Data loss
- Performance degradation
- Downtime

✅ **Solution:**
- Set up monitoring and alerts
- Investigate yellow/red status immediately
- Regular health checks

## Production Checklist

### Pre-Deployment

- [ ] Cluster sizing calculated
- [ ] Hardware meets requirements
- [ ] Network configured (10 Gbps)
- [ ] Security enabled (SSL, authentication)
- [ ] Monitoring set up (Prometheus/Grafana)
- [ ] Backup strategy defined
- [ ] ILM policies configured
- [ ] Index templates created
- [ ] Disaster recovery plan documented

### Post-Deployment

- [ ] Cluster health green
- [ ] All nodes joined cluster
- [ ] Indices created successfully
- [ ] Queries performing well (< 100ms)
- [ ] Indexing rate acceptable
- [ ] Monitoring dashboards working
- [ ] Alerts configured
- [ ] Backups running
- [ ] Documentation updated
- [ ] Team trained

### Ongoing Maintenance

- [ ] Monitor cluster health daily
- [ ] Review slow queries weekly
- [ ] Check disk space weekly
- [ ] Test backups monthly
- [ ] Update Elasticsearch quarterly
- [ ] Review and optimize indices quarterly
- [ ] Capacity planning quarterly
- [ ] Security audit annually

---

**Previous**: [← Integration Examples](11_Integration_Examples.md) | **Back to**: [README](README.md)
