# Kafka Partition Failure & Recovery

## Overview

When a Kafka partition fails (broker crash, disk failure, network partition), Kafka's replication and leader election mechanisms ensure **zero data loss** and **high availability**.

---

## Kafka Replication Architecture

### Partition Replicas

```
Topic: orders, Partition: 0, Replication Factor: 3

┌─────────────────────────────────────────────────────────┐
│  Broker 1 (Leader)                                      │
│  ┌──────────────────────────────────────┐              │
│  │ Partition 0 (Leader Replica)         │              │
│  │ Offset: 0 → 10000                    │ ◄─── Producers write here
│  │ ISR: [1, 2, 3]                       │              │
│  └──────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────┘
                    │
                    │ Replication
                    ▼
┌─────────────────────────────────────────────────────────┐
│  Broker 2 (Follower)                                    │
│  ┌──────────────────────────────────────┐              │
│  │ Partition 0 (Follower Replica)       │              │
│  │ Offset: 0 → 9998                     │ ◄─── Syncing │
│  └──────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────┘
                    │
                    │ Replication
                    ▼
┌─────────────────────────────────────────────────────────┐
│  Broker 3 (Follower)                                    │
│  ┌──────────────────────────────────────┐              │
│  │ Partition 0 (Follower Replica)       │              │
│  │ Offset: 0 → 9999                     │ ◄─── Syncing │
│  └──────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────┘
```

**Key Concepts**:
- **Leader**: Handles all reads/writes
- **Follower**: Replicates data from leader
- **ISR (In-Sync Replica)**: Replicas that are fully caught up with leader
- **Replication Factor**: Number of copies (typically 3)

---

## Failure Scenarios & Recovery

### Scenario 1: Leader Broker Crashes

**Before Failure**:
```
Broker 1 (Leader) - Partition 0 - Offset: 10000 ✓
Broker 2 (Follower) - Partition 0 - Offset: 9999 ✓
Broker 3 (Follower) - Partition 0 - Offset: 9998 ✓
ISR: [1, 2, 3]
```

**Failure Occurs**:
```
Broker 1 (Leader) - CRASHED ❌
Broker 2 (Follower) - Partition 0 - Offset: 9999 ✓
Broker 3 (Follower) - Partition 0 - Offset: 9998 ✓
```

**Recovery Steps**:

1. **ZooKeeper/KRaft Detects Failure** (within 6 seconds by default)
   ```
   Controller detects: Broker 1 heartbeat timeout
   ISR updated: [2, 3] (remove Broker 1)
   ```

2. **Leader Election** (within milliseconds)
   ```
   Controller selects new leader from ISR
   New Leader: Broker 2 (highest offset in ISR)
   ISR: [2, 3]
   ```

3. **Metadata Update**
   ```
   Controller broadcasts to all brokers:
   - Partition 0 leader: Broker 2
   - ISR: [2, 3]
   ```

4. **Producers/Consumers Reconnect**
   ```
   Producers: Refresh metadata → Connect to Broker 2
   Consumers: Refresh metadata → Connect to Broker 2
   ```

**After Recovery**:
```
Broker 1 (Leader) - CRASHED ❌
Broker 2 (NEW Leader) - Partition 0 - Offset: 9999 ✓
Broker 3 (Follower) - Partition 0 - Offset: 9998 → 9999 ✓
ISR: [2, 3]
```

**Timeline**:
- Detection: 6 seconds (default session timeout)
- Election: 50-200ms
- Total downtime: ~6-7 seconds

---

### Scenario 2: Follower Broker Crashes

**Before Failure**:
```
Broker 1 (Leader) - Partition 0 - Offset: 10000 ✓
Broker 2 (Follower) - Partition 0 - Offset: 9999 ✓
Broker 3 (Follower) - CRASHED ❌
ISR: [1, 2, 3]
```

**Recovery Steps**:

1. **Remove from ISR**
   ```
   Controller detects: Broker 3 heartbeat timeout
   ISR updated: [1, 2] (remove Broker 3)
   ```

2. **No Leader Election Needed**
   ```
   Leader remains: Broker 1
   System continues normally
   ```

**After Recovery**:
```
Broker 1 (Leader) - Partition 0 - Offset: 10000 ✓
Broker 2 (Follower) - Partition 0 - Offset: 9999 ✓
Broker 3 (Follower) - CRASHED ❌
ISR: [1, 2]
```

**Impact**: Zero downtime (leader still available)

---

### Scenario 3: Disk Failure on Leader

**Failure**:
```
Broker 1 (Leader) - Disk corrupted ❌
Broker 2 (Follower) - Partition 0 - Offset: 9999 ✓
Broker 3 (Follower) - Partition 0 - Offset: 9998 ✓
```

**Recovery Steps**:

1. **Broker Detects Disk Failure**
   ```
   Broker 1: I/O error detected
   Broker 1: Shutdown partition 0
   Broker 1: Notify controller
   ```

2. **Leader Election**
   ```
   Controller: Remove Broker 1 from ISR
   Controller: Elect Broker 2 as new leader
   ISR: [2, 3]
   ```

3. **Manual Intervention Required**
   ```bash
   # Replace disk
   # Restore data from replicas
   kafka-reassign-partitions.sh --execute --reassignment-json-file reassign.json
   ```

---

### Scenario 4: Network Partition (Split Brain)

**Scenario**: Network split isolates Broker 1 from Broker 2 & 3

```
Network Partition
     │
     ▼
┌─────────────┐         ┌─────────────┬─────────────┐
│  Broker 1   │   ✗     │  Broker 2   │  Broker 3   │
│  (Leader)   │         │  (Follower) │  (Follower) │
└─────────────┘         └─────────────┴─────────────┘
     │                           │
     │                           │
     ▼                           ▼
  Isolated                  Majority Quorum
```

**Recovery Steps**:

1. **Quorum-Based Decision**
   ```
   Broker 1: Isolated (minority) → Steps down as leader
   Broker 2 & 3: Majority quorum → Elect new leader
   ```

2. **New Leader Election**
   ```
   Controller (with Broker 2 & 3): Elect Broker 2 as leader
   ISR: [2, 3]
   ```

3. **Broker 1 Rejoins**
   ```
   Broker 1: Detects it's no longer leader
   Broker 1: Truncates uncommitted data
   Broker 1: Syncs from new leader (Broker 2)
   Broker 1: Rejoins ISR
   ```

**Protection**: Prevents split-brain with quorum-based consensus

---

### Scenario 5: All Replicas Fail (Catastrophic)

**Failure**:
```
Broker 1 (Leader) - CRASHED ❌
Broker 2 (Follower) - CRASHED ❌
Broker 3 (Follower) - CRASHED ❌
ISR: []
```

**Recovery Options**:

**Option 1: Wait for ISR Replica (Safest)**
```properties
# Configuration
unclean.leader.election.enable=false (default)

# Behavior
- Wait for any broker in ISR to come back online
- No data loss
- Downtime until recovery
```

**Option 2: Unclean Leader Election (Data Loss Risk)**
```properties
# Configuration
unclean.leader.election.enable=true

# Behavior
- Elect any available replica (even out-of-sync)
- Potential data loss
- Faster recovery
```

**Example**:
```
Broker 2 comes back online first (Offset: 9500)
Broker 1 had Offset: 10000 before crash

With unclean.leader.election.enable=true:
- Broker 2 becomes leader
- Data loss: Offsets 9501-10000 (500 messages lost)

With unclean.leader.election.enable=false:
- Wait for Broker 1 to recover
- No data loss
```

---

## Producer Behavior During Failure

### Producer Configuration

```java
@Configuration
public class KafkaProducerConfig {
    
    @Bean
    public ProducerFactory<String, Order> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "broker1:9092,broker2:9092,broker3:9092");
        
        // Durability settings
        config.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all ISR replicas
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); // Retry indefinitely
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Prevent duplicates
        
        // Timeout settings
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // 2 minutes
        
        return new DefaultKafkaProducerFactory<>(config);
    }
}
```

### Producer Retry Flow

```
Producer sends message to Broker 1 (Leader)
         │
         ▼
   Broker 1 crashes ❌
         │
         ▼
   Producer gets error: NOT_LEADER_FOR_PARTITION
         │
         ▼
   Producer refreshes metadata
         │
         ▼
   Producer discovers new leader: Broker 2
         │
         ▼
   Producer retries message to Broker 2 ✓
         │
         ▼
   Message successfully written
```

**Code Example**:
```java
@Service
public class OrderProducer {
    
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    public void sendOrder(Order order) {
        kafkaTemplate.send("orders", order.getId(), order)
            .addCallback(
                result -> log.info("Message sent: offset={}", result.getRecordMetadata().offset()),
                ex -> {
                    if (ex instanceof NotLeaderForPartitionException) {
                        log.warn("Leader changed, retrying...");
                        // Automatic retry by Kafka producer
                    } else {
                        log.error("Send failed: {}", ex.getMessage());
                    }
                }
            );
    }
}
```

---

## Consumer Behavior During Failure

### Consumer Configuration

```java
@Configuration
public class KafkaConsumerConfig {
    
    @Bean
    public ConsumerFactory<String, Order> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "broker1:9092,broker2:9092,broker3:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "order-consumer-group");
        
        // Offset management
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Session management
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000); // 10 seconds
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000); // 3 seconds
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
}
```

### Consumer Rebalance Flow

```
Consumer reads from Broker 1 (Leader)
         │
         ▼
   Broker 1 crashes ❌
         │
         ▼
   Consumer detects failure (heartbeat timeout)
         │
         ▼
   Consumer triggers rebalance
         │
         ▼
   Consumer refreshes metadata
         │
         ▼
   Consumer discovers new leader: Broker 2
         │
         ▼
   Consumer resumes reading from Broker 2 ✓
         │
         ▼
   Consumer continues from last committed offset
```

**Code Example**:
```java
@Service
public class OrderConsumer {
    
    @KafkaListener(
        topics = "orders",
        groupId = "order-consumer-group"
    )
    public void consume(ConsumerRecord<String, Order> record, Acknowledgment ack) {
        try {
            processOrder(record.value());
            ack.acknowledge(); // Commit offset after successful processing
        } catch (Exception e) {
            log.error("Processing failed: {}", e.getMessage());
            // Don't commit offset, will retry on rebalance
        }
    }
    
    @KafkaListener(
        topics = "orders",
        groupId = "order-consumer-group"
    )
    public void handleRebalance(ConsumerRebalanceListener listener) {
        listener.onPartitionsRevoked(partitions -> {
            log.info("Partitions revoked: {}", partitions);
            // Commit offsets before rebalance
        });
        
        listener.onPartitionsAssigned(partitions -> {
            log.info("Partitions assigned: {}", partitions);
            // Resume from last committed offset
        });
    }
}
```

---

## Monitoring & Alerting

### Key Metrics to Monitor

```java
@Service
public class KafkaMonitoringService {
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorKafkaHealth() {
        // 1. Under-replicated partitions
        int underReplicatedPartitions = getUnderReplicatedPartitions();
        if (underReplicatedPartitions > 0) {
            alertService.sendAlert("Under-replicated partitions: " + underReplicatedPartitions);
        }
        
        // 2. Offline partitions
        int offlinePartitions = getOfflinePartitions();
        if (offlinePartitions > 0) {
            alertService.sendCriticalAlert("Offline partitions: " + offlinePartitions);
        }
        
        // 3. ISR shrink rate
        double isrShrinkRate = getISRShrinkRate();
        if (isrShrinkRate > 0.1) { // 10% threshold
            alertService.sendAlert("High ISR shrink rate: " + isrShrinkRate);
        }
        
        // 4. Leader election rate
        double leaderElectionRate = getLeaderElectionRate();
        if (leaderElectionRate > 1.0) { // More than 1 per minute
            alertService.sendAlert("High leader election rate: " + leaderElectionRate);
        }
    }
}
```

### JMX Metrics

```bash
# Under-replicated partitions
kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions

# Offline partitions
kafka.controller:type=KafkaController,name=OfflinePartitionsCount

# ISR shrink rate
kafka.server:type=ReplicaManager,name=IsrShrinksPerSec

# Leader election rate
kafka.controller:type=ControllerStats,name=LeaderElectionRateAndTimeMs
```

---

## Best Practices for Fault Tolerance

### 1. Replication Configuration

```properties
# Topic configuration
replication.factor=3 # At least 3 replicas
min.insync.replicas=2 # At least 2 replicas must acknowledge

# Producer configuration
acks=all # Wait for all ISR replicas
enable.idempotence=true # Prevent duplicates
retries=Integer.MAX_VALUE # Retry indefinitely

# Broker configuration
unclean.leader.election.enable=false # Prevent data loss
auto.leader.rebalance.enable=true # Automatic leader rebalancing
```

### 2. Multi-AZ Deployment

```
Availability Zone 1        Availability Zone 2        Availability Zone 3
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│   Broker 1      │       │   Broker 2      │       │   Broker 3      │
│   (Leader)      │       │   (Follower)    │       │   (Follower)    │
└─────────────────┘       └─────────────────┘       └─────────────────┘
```

**Configuration**:
```properties
# Rack awareness
broker.rack=us-east-1a # Broker 1
broker.rack=us-east-1b # Broker 2
broker.rack=us-east-1c # Broker 3

# Replica placement
replica.selector.class=org.apache.kafka.common.replica.RackAwareReplicaSelector
```

### 3. Monitoring & Alerting

```yaml
# Prometheus alerts
groups:
  - name: kafka_alerts
    rules:
      - alert: KafkaPartitionOffline
        expr: kafka_controller_offline_partitions_count > 0
        for: 1m
        annotations:
          summary: "Kafka partition offline"
          
      - alert: KafkaUnderReplicatedPartitions
        expr: kafka_server_replicamanager_underreplicatedpartitions > 0
        for: 5m
        annotations:
          summary: "Kafka under-replicated partitions"
          
      - alert: KafkaISRShrink
        expr: rate(kafka_server_replicamanager_isrshrinks_total[5m]) > 0
        annotations:
          summary: "Kafka ISR shrinking"
```

### 4. Disaster Recovery

```bash
# Backup configuration
# 1. Enable MirrorMaker 2.0 for cross-cluster replication
kafka-mirror-maker.sh --consumer.config source-consumer.properties \
                      --producer.config target-producer.properties \
                      --whitelist "orders.*"

# 2. Regular snapshots of ZooKeeper/KRaft metadata
kafka-metadata-shell.sh --snapshot /path/to/snapshot

# 3. Backup consumer offsets
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
                         --group order-consumer-group \
                         --describe > offsets-backup.txt
```

---

## Recovery Time Objectives (RTO)

| Failure Type | Detection Time | Recovery Time | Total RTO | Data Loss |
|--------------|----------------|---------------|-----------|-----------|
| Leader crash | 6 seconds | 50-200ms | ~6-7 seconds | None (with acks=all) |
| Follower crash | 6 seconds | 0ms | 0 seconds | None |
| Disk failure | Immediate | 50-200ms | ~1 second | None (with replicas) |
| Network partition | 6 seconds | 50-200ms | ~6-7 seconds | None |
| All replicas fail | N/A | Manual intervention | Hours/Days | Depends on config |

---

## Summary

**Kafka's Fault Tolerance Guarantees**:
1. ✅ **Zero data loss** with `acks=all` and `min.insync.replicas=2`
2. ✅ **Automatic failover** within 6-7 seconds
3. ✅ **No split-brain** with quorum-based consensus
4. ✅ **Transparent recovery** for producers/consumers
5. ✅ **Multi-AZ resilience** with rack awareness

**Key Takeaways**:
- Replication Factor ≥ 3 for production
- `min.insync.replicas=2` to prevent data loss
- `acks=all` for durability
- Monitor under-replicated partitions
- Deploy across multiple availability zones
- Regular disaster recovery drills
