# Apache Cassandra Complete Guide - Part 3: Cluster Management & Operations

## 📋 Table of Contents
1. [Cluster Architecture](#cluster-architecture)
2. [Replication Strategies](#replication-strategies)
3. [Node Operations](#node-operations)
4. [Monitoring & Maintenance](#monitoring--maintenance)
5. [Backup & Recovery](#backup--recovery)

---

## Cluster Architecture

### Ring Architecture
```
Node 1 (Token: 0)
    ↓
Node 2 (Token: 42535295865117307932921825928971026432)
    ↓
Node 3 (Token: 85070591730234615865843651857942052864)
    ↓
Node 1 (Token: 0) -- Ring completes
```

### Data Distribution
```cql
-- Data distributed based on partition key hash
-- Token range: -2^63 to 2^63-1

-- Example: user_id hashed to token
user_id: 123e4567-e89b-12d3-a456-426614174000
Token: 42535295865117307932921825928971026432
Node: Node 2 (owns this token range)
```

### Virtual Nodes (vnodes)
```yaml
# cassandra.yaml
num_tokens: 256  # Default, each node owns 256 token ranges
```

---

## Replication Strategies

### SimpleStrategy (Single Datacenter)
```cql
CREATE KEYSPACE ecommerce
WITH replication = {
    'class': 'SimpleStrategy',
    'replication_factor': 3
};

-- Data replicated to 3 nodes
-- Node 1: Primary
-- Node 2: Replica 1
-- Node 3: Replica 2
```

### NetworkTopologyStrategy (Multiple Datacenters)
```cql
CREATE KEYSPACE ecommerce
WITH replication = {
    'class': 'NetworkTopologyStrategy',
    'us-east': 3,
    'us-west': 2,
    'eu-west': 2
};

-- 3 replicas in us-east
-- 2 replicas in us-west
-- 2 replicas in eu-west
-- Total: 7 replicas across 3 datacenters
```

### Replication Factor Guidelines
```
RF = 1: No fault tolerance (don't use in production)
RF = 2: Tolerates 1 node failure
RF = 3: Tolerates 2 node failures (recommended)
RF = 5: Tolerates 4 node failures (high availability)
```

---

## Node Operations

### Add Node to Cluster
```bash
# Configure new node (cassandra.yaml)
cluster_name: 'MyCluster'
seeds: "node1_ip,node2_ip"
listen_address: new_node_ip
rpc_address: new_node_ip

# Start Cassandra
cassandra -f

# Check cluster status
nodetool status

# Output:
# UN  192.168.1.1  100GB  256  ?  uuid1  rack1
# UN  192.168.1.2  100GB  256  ?  uuid2  rack1
# UJ  192.168.1.3  0GB    256  ?  uuid3  rack1  (Joining)
```

### Remove Node (Decommission)
```bash
# On node to remove
nodetool decommission

# Streams data to other nodes
# Node automatically removed from cluster
```

### Replace Dead Node
```bash
# Configure replacement node
# cassandra.yaml
replace_address: dead_node_ip

# Start Cassandra
cassandra -f

# Node takes over dead node's token ranges
```

### Repair Node
```bash
# Full repair (all data)
nodetool repair

# Repair specific keyspace
nodetool repair ecommerce

# Repair specific table
nodetool repair ecommerce users

# Incremental repair (faster)
nodetool repair -inc
```

### Cleanup After Scaling
```bash
# Remove data no longer owned by node
nodetool cleanup

# Run after adding nodes to cluster
```

---

## Monitoring & Maintenance

### Cluster Status
```bash
# Cluster overview
nodetool status

# Output:
# Datacenter: datacenter1
# Status=Up/Down
# State=Normal/Leaving/Joining/Moving
# UN  192.168.1.1  100GB  256  50.0%  uuid1  rack1
# UN  192.168.1.2  100GB  256  50.0%  uuid2  rack1

# Node info
nodetool info

# Ring information
nodetool ring
```

### Performance Metrics
```bash
# Table statistics
nodetool tablestats ecommerce.users

# Thread pool stats
nodetool tpstats

# Compaction stats
nodetool compactionstats

# Garbage collection stats
nodetool gcstats
```

### Monitoring Queries
```cql
-- System tables
SELECT * FROM system.peers;
SELECT * FROM system.local;

-- Table metrics
SELECT * FROM system_schema.tables WHERE keyspace_name = 'ecommerce';

-- Compaction history
SELECT * FROM system.compaction_history;
```

### JMX Monitoring
```bash
# Enable JMX (cassandra-env.sh)
JMX_PORT="7199"

# Connect with JConsole
jconsole localhost:7199

# Metrics to monitor:
# - Read/Write latency
# - Read/Write throughput
# - Pending compactions
# - Heap memory usage
# - GC pause time
```

### Logging
```bash
# Log location
/var/log/cassandra/system.log

# Enable debug logging
nodetool setlogginglevel org.apache.cassandra DEBUG

# View logs
tail -f /var/log/cassandra/system.log
```

---

## Backup & Recovery

### Snapshot Backup
```bash
# Create snapshot
nodetool snapshot -t backup_20240115 ecommerce

# Snapshot location
/var/lib/cassandra/data/ecommerce/users-uuid/snapshots/backup_20240115/

# List snapshots
nodetool listsnapshots

# Clear old snapshots
nodetool clearsnapshot -t backup_20240115
```

### Incremental Backup
```bash
# Enable incremental backups (cassandra.yaml)
incremental_backups: true

# Backup location
/var/lib/cassandra/data/ecommerce/users-uuid/backups/

# Copy backups to remote storage
aws s3 sync /var/lib/cassandra/data/ecommerce/users-uuid/backups/ s3://bucket/backups/
```

### Restore from Snapshot
```bash
# Stop Cassandra
nodetool drain
service cassandra stop

# Clear current data
rm -rf /var/lib/cassandra/data/ecommerce/users-uuid/*

# Copy snapshot data
cp -r /var/lib/cassandra/data/ecommerce/users-uuid/snapshots/backup_20240115/* \
      /var/lib/cassandra/data/ecommerce/users-uuid/

# Start Cassandra
service cassandra start

# Repair to ensure consistency
nodetool repair ecommerce users
```

### Point-in-Time Recovery
```bash
# 1. Restore from snapshot
# 2. Replay commit logs

# Copy commit logs
cp /var/lib/cassandra/commitlog/* /restore/commitlog/

# Restore snapshot
# Start Cassandra with commit log replay
```

---

## Production Best Practices

### 1. Hardware Recommendations
```
CPU: 8-16 cores
RAM: 32-64 GB
Disk: SSD (NVMe preferred)
Network: 10 Gbps
```

### 2. Configuration Tuning
```yaml
# cassandra.yaml

# Memory
memtable_heap_space_in_mb: 2048
memtable_offheap_space_in_mb: 2048

# Commit log
commitlog_sync: periodic
commitlog_sync_period_in_ms: 10000
commitlog_segment_size_in_mb: 32

# Compaction
concurrent_compactors: 4
compaction_throughput_mb_per_sec: 64

# Read/Write
concurrent_reads: 32
concurrent_writes: 32
concurrent_counter_writes: 32

# Timeouts
read_request_timeout_in_ms: 5000
write_request_timeout_in_ms: 2000
```

### 3. JVM Settings
```bash
# jvm.options

# Heap size (50% of RAM, max 32GB)
-Xms16G
-Xmx16G

# GC settings (G1GC)
-XX:+UseG1GC
-XX:G1RSetUpdatingPauseTimePercent=5
-XX:MaxGCPauseMillis=500

# GC logging
-Xlog:gc*:file=/var/log/cassandra/gc.log:time,uptime:filecount=10,filesize=10m
```

### 4. Security
```yaml
# cassandra.yaml

# Authentication
authenticator: PasswordAuthenticator
authorizer: CassandraAuthorizer

# Encryption (at rest)
transparent_data_encryption_options:
  enabled: true
  chunk_length_kb: 64
  cipher: AES/CBC/PKCS5Padding
  key_alias: cassandra_key

# Encryption (in transit)
client_encryption_options:
  enabled: true
  keystore: /path/to/keystore
  keystore_password: password

server_encryption_options:
  internode_encryption: all
  keystore: /path/to/keystore
  keystore_password: password
```

### 5. Monitoring Alerts
```
Alert on:
- Node down (UN -> DN)
- High read/write latency (>100ms p99)
- Pending compactions (>10)
- Disk usage (>80%)
- Heap usage (>75%)
- GC pause time (>1s)
- Dropped messages (>0)
```

### 6. Capacity Planning
```
Storage calculation:
- Data size: 1TB
- Replication factor: 3
- Total storage: 3TB
- Overhead (20%): 3.6TB
- Number of nodes: 3.6TB / 1TB per node = 4 nodes

Throughput calculation:
- Write throughput: 10K writes/sec
- Per node: 10K / 4 nodes = 2.5K writes/sec
- With RF=3: 2.5K * 3 = 7.5K writes/sec per node
```

---

## Troubleshooting

### Common Issues

#### 1. High Read Latency
```bash
# Check compaction
nodetool compactionstats

# Check cache hit rate
nodetool info | grep "Key Cache"

# Increase cache size (cassandra.yaml)
key_cache_size_in_mb: 1024
row_cache_size_in_mb: 512
```

#### 2. High Write Latency
```bash
# Check commit log disk
iostat -x 1

# Move commit log to separate disk
commitlog_directory: /mnt/commitlog

# Increase memtable size
memtable_heap_space_in_mb: 4096
```

#### 3. Out of Memory
```bash
# Check heap usage
nodetool info | grep "Heap Memory"

# Increase heap size (jvm.options)
-Xms32G
-Xmx32G

# Reduce cache sizes
key_cache_size_in_mb: 512
```

#### 4. Tombstone Warnings
```bash
# Check tombstone count
nodetool tablestats ecommerce.users | grep "Tombstones"

# Reduce gc_grace_seconds
ALTER TABLE users WITH gc_grace_seconds = 86400;

# Run repair more frequently
nodetool repair -pr ecommerce users
```

---

## Next Steps

Continue to [Part 4: Spring Boot Integration](./04_Cassandra_Spring_Boot.md)
