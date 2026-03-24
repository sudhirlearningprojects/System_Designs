# Kafka Connect Deep Dive - Part 2

## Table of Contents
- [Error Handling and DLQ](#error-handling-and-dlq)
- [Monitoring and Operations](#monitoring-and-operations)
- [Performance Tuning](#performance-tuning)
- [Production Best Practices](#production-best-practices)
- [Troubleshooting Guide](#troubleshooting-guide)
- [Interview Questions](#interview-questions)

---

## Error Handling and DLQ

### Error Tolerance Configuration

```json
{
  "errors.tolerance": "all",
  "errors.log.enable": "true",
  "errors.log.include.messages": "true",
  "errors.retry.timeout": "300000",
  "errors.retry.delay.max.ms": "60000"
}
```

**Options:**
- `errors.tolerance=none`: Stop on first error (default)
- `errors.tolerance=all`: Continue processing, log errors

### Dead Letter Queue (DLQ)

**Configuration:**
```json
{
  "errors.tolerance": "all",
  "errors.deadletterqueue.topic.name": "dlq-azure-blob-logs",
  "errors.deadletterqueue.topic.replication.factor": "3",
  "errors.deadletterqueue.context.headers.enable": "true"
}
```

**DLQ Message Headers:**
```
__connect.errors.topic: application-logs
__connect.errors.partition: 3
__connect.errors.offset: 12345
__connect.errors.connector.name: azure-blob-sink-logs
__connect.errors.task.id: 2
__connect.errors.stage: VALUE_CONVERTER
__connect.errors.class.name: org.apache.kafka.connect.errors.DataException
__connect.errors.exception.message: Failed to deserialize value
__connect.errors.exception.stacktrace: ...
```

### DLQ Consumer Example

```java
@Service
@Slf4j
public class DLQConsumer {
    
    @KafkaListener(topics = "dlq-azure-blob-logs", groupId = "dlq-processor")
    public void processDLQMessage(ConsumerRecord<String, String> record) {
        
        Headers headers = record.headers();
        
        String originalTopic = getHeader(headers, "__connect.errors.topic");
        String errorStage = getHeader(headers, "__connect.errors.stage");
        String errorMessage = getHeader(headers, "__connect.errors.exception.message");
        
        log.error("DLQ Message - Topic: {}, Stage: {}, Error: {}, Value: {}",
            originalTopic, errorStage, errorMessage, record.value());
        
        // Send alert
        alertService.sendAlert(
            "Kafka Connect Error",
            String.format("Failed to process message from %s: %s", originalTopic, errorMessage)
        );
        
        // Store in database for manual review
        dlqRepository.save(new DLQEntry(
            originalTopic,
            record.partition(),
            record.offset(),
            errorStage,
            errorMessage,
            record.value()
        ));
    }
    
    private String getHeader(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        return header != null ? new String(header.value()) : null;
    }
}
```

### Retry Configuration

```json
{
  "errors.retry.timeout": "300000",
  "errors.retry.delay.max.ms": "60000",
  
  "max.retries": "10",
  "retry.backoff.ms": "1000"
}
```

**Retry Behavior:**
```
Attempt 1: Immediate
Attempt 2: 1 second delay
Attempt 3: 2 seconds delay
Attempt 4: 4 seconds delay
...
Attempt 10: 60 seconds delay (max)
```

---

## Monitoring and Operations

### REST API Operations

#### 1. List Connectors
```bash
curl http://localhost:8083/connectors | jq
```

#### 2. Get Connector Status
```bash
curl http://localhost:8083/connectors/azure-blob-sink-logs/status | jq
```

**Response:**
```json
{
  "name": "azure-blob-sink-logs",
  "connector": {
    "state": "RUNNING",
    "worker_id": "connect-worker-1:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "connect-worker-1:8083"
    },
    {
      "id": 1,
      "state": "RUNNING",
      "worker_id": "connect-worker-2:8083"
    }
  ],
  "type": "sink"
}
```

#### 3. Get Connector Configuration
```bash
curl http://localhost:8083/connectors/azure-blob-sink-logs/config | jq
```

#### 4. Update Connector
```bash
curl -X PUT http://localhost:8083/connectors/azure-blob-sink-logs/config \
  -H "Content-Type: application/json" \
  -d '{
    "connector.class": "io.confluent.connect.azure.blob.AzureBlobStorageSinkConnector",
    "tasks.max": "8"
  }'
```

#### 5. Pause Connector
```bash
curl -X PUT http://localhost:8083/connectors/azure-blob-sink-logs/pause
```

#### 6. Resume Connector
```bash
curl -X PUT http://localhost:8083/connectors/azure-blob-sink-logs/resume
```

#### 7. Restart Connector
```bash
curl -X POST http://localhost:8083/connectors/azure-blob-sink-logs/restart
```

#### 8. Restart Task
```bash
curl -X POST http://localhost:8083/connectors/azure-blob-sink-logs/tasks/0/restart
```

#### 9. Delete Connector
```bash
curl -X DELETE http://localhost:8083/connectors/azure-blob-sink-logs
```

### Metrics and Monitoring

#### JMX Metrics

**Enable JMX:**
```bash
export KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false"

bin/connect-distributed.sh config/connect-distributed.properties
```

**Key Metrics:**

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `connector-total-task-count` | Total tasks | - |
| `connector-running-task-count` | Running tasks | < total |
| `connector-paused-task-count` | Paused tasks | > 0 |
| `connector-failed-task-count` | Failed tasks | > 0 |
| `sink-record-send-rate` | Records/sec | < expected |
| `sink-record-send-total` | Total records | - |
| `source-record-poll-rate` | Records/sec | < expected |
| `task-error-total-record-failures` | Failed records | > 0 |

#### Prometheus Metrics

**JMX Exporter Configuration (jmx_exporter.yml):**
```yaml
lowercaseOutputName: true
lowercaseOutputLabelNames: true

rules:
  - pattern: kafka.connect<type=connect-worker-metrics><>(connector-count|task-count|connector-startup-attempts-total|connector-startup-failure-total|connector-startup-success-total)
    name: kafka_connect_worker_$1
    
  - pattern: kafka.connect<type=connector-metrics, connector=(.+)><>(connector-type|connector-class|connector-version|status)
    name: kafka_connect_connector_$2
    labels:
      connector: $1
      
  - pattern: kafka.connect<type=connector-task-metrics, connector=(.+), task=(.+)><>(status|running-ratio|pause-ratio|batch-size-avg|batch-size-max|offset-commit-avg-time-ms|offset-commit-max-time-ms)
    name: kafka_connect_task_$3
    labels:
      connector: $1
      task: $2
      
  - pattern: kafka.connect<type=sink-task-metrics, connector=(.+), task=(.+)><>(sink-record-read-total|sink-record-send-total|sink-record-active-count|sink-record-send-rate|sink-record-read-rate|partition-count|offset-commit-seq-no|offset-commit-completion-total|offset-commit-skip-total)
    name: kafka_connect_sink_task_$3
    labels:
      connector: $1
      task: $2
```

**Start with JMX Exporter:**
```bash
java -javaagent:jmx_prometheus_javaagent.jar=8080:jmx_exporter.yml \
  -jar kafka-connect.jar config/connect-distributed.properties
```

#### Grafana Dashboard

**Sample Prometheus Queries:**

```promql
# Connector status
kafka_connect_connector_status{connector="azure-blob-sink-logs"}

# Task count
sum(kafka_connect_task_status) by (connector)

# Sink record rate
rate(kafka_connect_sink_task_sink_record_send_total[5m])

# Failed records
rate(kafka_connect_task_error_total_record_failures[5m])

# Offset commit latency
kafka_connect_task_offset_commit_avg_time_ms
```

### Health Checks

**Custom Health Check Endpoint:**
```java
@RestController
@RequestMapping("/health")
public class ConnectHealthController {
    
    private final RestTemplate restTemplate;
    
    @GetMapping
    public ResponseEntity<HealthStatus> checkHealth() {
        try {
            // Check connector status
            String url = "http://localhost:8083/connectors/azure-blob-sink-logs/status";
            ConnectorStatus status = restTemplate.getForObject(url, ConnectorStatus.class);
            
            if (!"RUNNING".equals(status.getConnector().getState())) {
                return ResponseEntity.status(503)
                    .body(new HealthStatus("UNHEALTHY", "Connector not running"));
            }
            
            // Check all tasks
            long failedTasks = status.getTasks().stream()
                .filter(task -> !"RUNNING".equals(task.getState()))
                .count();
            
            if (failedTasks > 0) {
                return ResponseEntity.status(503)
                    .body(new HealthStatus("DEGRADED", 
                        String.format("%d tasks failed", failedTasks)));
            }
            
            return ResponseEntity.ok(new HealthStatus("HEALTHY", "All tasks running"));
            
        } catch (Exception e) {
            return ResponseEntity.status(503)
                .body(new HealthStatus("UNHEALTHY", e.getMessage()));
        }
    }
}
```

---

## Performance Tuning

### 1. Task Parallelism

```json
{
  "tasks.max": "6"
}
```

**Guidelines:**
- Match Kafka topic partitions
- One task per partition for optimal throughput
- More tasks = more parallelism but more overhead

### 2. Batch Size

```json
{
  "consumer.max.poll.records": "500",
  "flush.size": "1000"
}
```

**Trade-offs:**
- Larger batches = better throughput, higher latency
- Smaller batches = lower latency, lower throughput

### 3. Buffer Memory

```json
{
  "producer.buffer.memory": "67108864",
  "producer.batch.size": "32768"
}
```

### 4. Compression

```json
{
  "producer.compression.type": "snappy",
  "parquet.codec": "snappy"
}
```

**Compression Types:**
- `snappy`: Fast, moderate compression
- `gzip`: Slower, better compression
- `lz4`: Fastest, good compression
- `zstd`: Best compression, moderate speed

### 5. Flush and Rotation

```json
{
  "flush.size": "1000",
  "rotate.interval.ms": "3600000",
  "rotate.schedule.interval.ms": "3600000"
}
```

**Optimization:**
- Larger flush size = fewer files, better throughput
- Shorter rotation = more files, lower latency

### 6. Consumer Configuration

```json
{
  "consumer.max.poll.records": "500",
  "consumer.max.poll.interval.ms": "300000",
  "consumer.session.timeout.ms": "30000",
  "consumer.fetch.min.bytes": "1024",
  "consumer.fetch.max.wait.ms": "500"
}
```

### Performance Benchmarks

**Test Setup:**
- 3 Kafka brokers
- 3 Connect workers
- 6 tasks
- Topic: 6 partitions, replication factor 3

**Results:**

| Configuration | Throughput | Latency (p99) | File Size |
|---------------|------------|---------------|-----------|
| flush.size=100 | 5K msg/sec | 200ms | 10KB |
| flush.size=1000 | 45K msg/sec | 2s | 100KB |
| flush.size=10000 | 80K msg/sec | 15s | 1MB |

---

## Production Best Practices

### 1. Distributed Mode Configuration

```properties
# connect-distributed.properties

# Kafka cluster
bootstrap.servers=kafka1:9092,kafka2:9092,kafka3:9092

# Connect cluster
group.id=connect-cluster

# Internal topics
config.storage.topic=connect-configs
config.storage.replication.factor=3
offset.storage.topic=connect-offsets
offset.storage.replication.factor=3
offset.storage.partitions=25
status.storage.topic=connect-status
status.storage.replication.factor=3
status.storage.partitions=5

# Converters
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false

# REST API
rest.port=8083
rest.advertised.host.name=connect-worker-1
rest.advertised.port=8083

# Plugin path
plugin.path=/usr/share/java,/usr/local/share/kafka/plugins

# Worker configs
offset.flush.interval.ms=10000
offset.flush.timeout.ms=5000

# Producer configs
producer.compression.type=snappy
producer.max.request.size=1048576

# Consumer configs
consumer.max.poll.records=500
consumer.max.poll.interval.ms=300000
```

### 2. Security Configuration

**SSL/TLS:**
```properties
# SSL for Kafka
security.protocol=SSL
ssl.truststore.location=/var/private/ssl/kafka.client.truststore.jks
ssl.truststore.password=${TRUSTSTORE_PASSWORD}
ssl.keystore.location=/var/private/ssl/kafka.client.keystore.jks
ssl.keystore.password=${KEYSTORE_PASSWORD}
ssl.key.password=${KEY_PASSWORD}

# SSL for REST API
listeners=https://0.0.0.0:8083
rest.advertised.listener=https
ssl.keystore.location=/var/private/ssl/connect.keystore.jks
ssl.keystore.password=${KEYSTORE_PASSWORD}
ssl.key.password=${KEY_PASSWORD}
ssl.truststore.location=/var/private/ssl/connect.truststore.jks
ssl.truststore.password=${TRUSTSTORE_PASSWORD}
```

**SASL Authentication:**
```properties
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
  username="${KAFKA_USERNAME}" \
  password="${KAFKA_PASSWORD}";
```

### 3. Secrets Management

**Using Environment Variables:**
```json
{
  "azblob.account.name": "${AZURE_STORAGE_ACCOUNT}",
  "azblob.account.key": "${AZURE_STORAGE_KEY}"
}
```

**Using External Secrets (Kubernetes):**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: azure-storage-secret
type: Opaque
data:
  account-name: <base64-encoded>
  account-key: <base64-encoded>
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-connect
spec:
  template:
    spec:
      containers:
      - name: kafka-connect
        env:
        - name: AZURE_STORAGE_ACCOUNT
          valueFrom:
            secretKeyRef:
              name: azure-storage-secret
              key: account-name
        - name: AZURE_STORAGE_KEY
          valueFrom:
            secretKeyRef:
              name: azure-storage-secret
              key: account-key
```

### 4. High Availability Setup

**3-Node Cluster:**
```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ Connect Worker 1│  │ Connect Worker 2│  │ Connect Worker 3│
│  - Task 0       │  │  - Task 1       │  │  - Task 2       │
│  - Task 3       │  │  - Task 4       │  │  - Task 5       │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

**Automatic Rebalancing:**
- Worker failure → Tasks redistributed
- New worker added → Tasks rebalanced
- Connector update → Tasks restarted

### 5. Resource Limits

**Docker Compose:**
```yaml
version: '3.8'
services:
  kafka-connect:
    image: confluentinc/cp-kafka-connect:7.5.0
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 8G
        reservations:
          cpus: '2'
          memory: 4G
    environment:
      KAFKA_HEAP_OPTS: "-Xms4G -Xmx6G"
      KAFKA_JVM_PERFORMANCE_OPTS: "-XX:+UseG1GC -XX:MaxGCPauseMillis=20"
```

**Kubernetes:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-connect
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: kafka-connect
        resources:
          requests:
            memory: "4Gi"
            cpu: "2"
          limits:
            memory: "8Gi"
            cpu: "4"
        env:
        - name: KAFKA_HEAP_OPTS
          value: "-Xms4G -Xmx6G"
```

### 6. Monitoring Alerts

**Prometheus Alert Rules:**
```yaml
groups:
- name: kafka_connect
  interval: 30s
  rules:
  - alert: ConnectorDown
    expr: kafka_connect_connector_status != 1
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "Connector {{ $labels.connector }} is down"
      
  - alert: TaskFailed
    expr: kafka_connect_task_status != 1
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "Task {{ $labels.task }} failed for connector {{ $labels.connector }}"
      
  - alert: HighErrorRate
    expr: rate(kafka_connect_task_error_total_record_failures[5m]) > 10
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High error rate for connector {{ $labels.connector }}"
      
  - alert: LowThroughput
    expr: rate(kafka_connect_sink_task_sink_record_send_total[5m]) < 100
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "Low throughput for connector {{ $labels.connector }}"
```

---

## Troubleshooting Guide

### Common Issues

#### 1. Connector Fails to Start

**Symptoms:**
```json
{
  "state": "FAILED",
  "trace": "org.apache.kafka.connect.errors.ConnectException: ..."
}
```

**Solutions:**
- Check connector class is in plugin.path
- Verify configuration parameters
- Check Kafka connectivity
- Review worker logs

**Debug:**
```bash
# Check logs
docker logs kafka-connect | grep ERROR

# Validate connector config
curl -X PUT http://localhost:8083/connector-plugins/AzureBlobStorageSinkConnector/config/validate \
  -H "Content-Type: application/json" \
  -d @azure-blob-sink.json
```

#### 2. Tasks Keep Failing

**Symptoms:**
```
Task keeps restarting, state: FAILED
```

**Solutions:**
- Check DLQ for error details
- Verify external system connectivity (Azure)
- Increase retry timeout
- Check data format compatibility

#### 3. Low Throughput

**Symptoms:**
```
Expected: 10K msg/sec
Actual: 500 msg/sec
```

**Solutions:**
- Increase tasks.max
- Increase flush.size
- Tune consumer.max.poll.records
- Check network bandwidth
- Monitor CPU/memory usage

#### 4. High Latency

**Symptoms:**
```
Messages delayed by 10+ minutes
```

**Solutions:**
- Decrease flush.size
- Decrease rotate.interval.ms
- Check consumer lag
- Verify worker resources

#### 5. Out of Memory

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions:**
- Increase KAFKA_HEAP_OPTS
- Decrease consumer.max.poll.records
- Decrease flush.size
- Add more workers

---

## Interview Questions

### Q1: What is Kafka Connect?

**Answer:**
Kafka Connect is a framework for streaming data between Kafka and external systems without writing code. It provides:
- Source connectors (import data INTO Kafka)
- Sink connectors (export data FROM Kafka)
- Distributed execution
- REST API management
- Automatic scaling and fault tolerance

### Q2: Source vs Sink Connector?

**Answer:**
- **Source Connector**: Reads from external system, writes to Kafka
  - Example: JDBC Source reads from MySQL, writes to Kafka topic
- **Sink Connector**: Reads from Kafka, writes to external system
  - Example: S3 Sink reads from Kafka topic, writes to S3

### Q3: Standalone vs Distributed mode?

**Answer:**
**Standalone:**
- Single worker process
- Config in properties files
- No fault tolerance
- Use for: Development, testing

**Distributed:**
- Multiple workers
- Config via REST API
- Automatic rebalancing
- Fault tolerant
- Use for: Production

### Q4: How does Kafka Connect ensure exactly-once?

**Answer:**
1. **Offset Management**: Stores offsets in Kafka topics
2. **Transactional Writes**: Uses Kafka transactions
3. **Idempotent Producers**: Prevents duplicates
4. **Checkpointing**: Periodic offset commits

### Q5: What are SMTs?

**Answer:**
Single Message Transformations - lightweight data modifications:
- InsertField: Add static fields
- MaskField: Mask sensitive data
- ReplaceField: Rename/remove fields
- TimestampConverter: Convert timestamp formats
- Filter: Conditional processing

Applied per message, no custom code needed.

### Q6: How to handle errors in Kafka Connect?

**Answer:**
1. **Error Tolerance**: `errors.tolerance=all`
2. **Dead Letter Queue**: Failed messages to DLQ topic
3. **Retry Configuration**: Exponential backoff
4. **Logging**: Detailed error logs
5. **Monitoring**: Alerts on error metrics

### Q7: How to scale Kafka Connect?

**Answer:**
1. **Add Workers**: More workers = more capacity
2. **Increase Tasks**: Match topic partitions
3. **Tune Batch Size**: Larger batches = higher throughput
4. **Optimize Connectors**: Connector-specific tuning
5. **Resource Allocation**: Adequate CPU/memory

### Q8: What is the role of converters?

**Answer:**
Converters transform data between Kafka Connect format and byte arrays:
- **JsonConverter**: JSON serialization
- **AvroConverter**: Avro with Schema Registry
- **ProtobufConverter**: Protobuf format
- **StringConverter**: Plain text

### Q9: How to monitor Kafka Connect?

**Answer:**
1. **REST API**: Connector/task status
2. **JMX Metrics**: Throughput, errors, latency
3. **Prometheus**: Metrics export
4. **Grafana**: Visualization
5. **Logs**: Error tracking
6. **Alerts**: Automated notifications

### Q10: Best practices for production?

**Answer:**
1. **Distributed Mode**: High availability
2. **Replication Factor 3**: Internal topics
3. **Error Handling**: DLQ + retry
4. **Monitoring**: Comprehensive metrics
5. **Security**: SSL/SASL authentication
6. **Resource Limits**: Prevent resource exhaustion
7. **Testing**: Validate before production
8. **Documentation**: Configuration management

---

## Summary

### Key Takeaways

1. **Kafka Connect** = Framework for streaming data between Kafka and external systems
2. **No Code Required** = JSON configuration only
3. **Scalable** = Distributed mode with automatic rebalancing
4. **Fault Tolerant** = Automatic recovery and exactly-once semantics
5. **Extensible** = Custom connectors and transformations

### Logstash → Azure Blob Flow

```
Logs → Logstash → Kafka → Connect → Azure Blob
       (Process)  (Buffer) (Sink)   (Storage)
```

### Production Checklist

- [ ] Distributed mode with 3+ workers
- [ ] Replication factor 3 for internal topics
- [ ] Error tolerance and DLQ configured
- [ ] Monitoring and alerts enabled
- [ ] Security (SSL/SASL) configured
- [ ] Resource limits set
- [ ] Backup and recovery plan
- [ ] Documentation complete

---

**Related Documents:**
- [Kafka_Interview_Questions.md](Kafka_Interview_Questions.md)
- [Kafka_Architecture_And_Parallelism.md](Kafka_Architecture_And_Parallelism.md)
- [Kafka_Producer_Consumer_Configs.md](Kafka_Producer_Consumer_Configs.md)
