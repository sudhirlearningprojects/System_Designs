# Kafka Connect Deep Dive - Logstash to Azure Blob Storage

## Table of Contents
- [Introduction to Kafka Connect](#introduction-to-kafka-connect)
- [Kafka Connect Architecture](#kafka-connect-architecture)
- [Connectors Overview](#connectors-overview)
- [Source vs Sink Connectors](#source-vs-sink-connectors)
- [Kafka Connect Modes](#kafka-connect-modes)
- [Real-World Example: Logstash to Azure Blob](#real-world-example-logstash-to-azure-blob)
- [Configuration Deep Dive](#configuration-deep-dive)
- [Transformations (SMTs)](#transformations-smts)
- [Error Handling and DLQ](#error-handling-and-dlq)
- [Monitoring and Operations](#monitoring-and-operations)
- [Production Best Practices](#production-best-practices)
- [Interview Questions](#interview-questions)

---

## Introduction to Kafka Connect

### What is Kafka Connect?

Kafka Connect is a **framework for streaming data** between Apache Kafka and external systems (databases, file systems, cloud storage, search indexes, etc.) in a scalable and reliable way.

### Key Features

- ✅ **Declarative Configuration**: JSON-based, no code required
- ✅ **Scalability**: Distributed mode for high throughput
- ✅ **Fault Tolerance**: Automatic recovery and rebalancing
- ✅ **Exactly-Once Semantics**: No data loss or duplication
- ✅ **Schema Management**: Integration with Schema Registry
- ✅ **Transformations**: Built-in data transformations (SMTs)
- ✅ **REST API**: Easy management and monitoring

### Why Use Kafka Connect?

**Without Kafka Connect:**
```java
// Custom producer code for each source
while (true) {
    LogEntry log = logstash.readLog();
    producer.send(new ProducerRecord<>("logs", log));
}

// Custom consumer code for each sink
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        azureBlob.upload(record.value());
    }
}
```

**With Kafka Connect:**
```json
{
  "name": "logstash-source",
  "config": {
    "connector.class": "io.confluent.connect.http.HttpSourceConnector",
    "tasks.max": "3"
  }
}
```

---

## Kafka Connect Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Kafka Connect Cluster                     │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Worker 1   │  │   Worker 2   │  │   Worker 3   │          │
│  │              │  │              │  │              │          │
│  │  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │          │
│  │  │ Task 1 │  │  │  │ Task 2 │  │  │  │ Task 3 │  │          │
│  │  └────────┘  │  │  └────────┘  │  │  └────────┘  │          │
│  │  ┌────────┐  │  │  ┌────────┐  │  │              │          │
│  │  │ Task 4 │  │  │  │ Task 5 │  │  │              │          │
│  │  └────────┘  │  │  └────────┘  │  │              │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
           ▲                                        │
           │                                        │
           │ Source                                 │ Sink
           │ Connector                              │ Connector
           │                                        ▼
    ┌──────────────┐                        ┌──────────────┐
    │   Logstash   │                        │ Azure Blob   │
    │   (Source)   │                        │  (Sink)      │
    └──────────────┘                        └──────────────┘
```

### Components

#### 1. **Workers**
- JVM processes that execute connectors and tasks
- Can run in standalone or distributed mode
- Handle REST API requests
- Manage task distribution and rebalancing

#### 2. **Connectors**
- High-level abstraction defining how to copy data
- Split work into tasks
- Manage configuration and lifecycle

#### 3. **Tasks**
- Actual units of work that copy data
- Multiple tasks per connector for parallelism
- Each task is independent

#### 4. **Converters**
- Transform data between Kafka Connect format and byte arrays
- Types: JSON, Avro, Protobuf, String

#### 5. **Transformations (SMTs)**
- Lightweight data modifications
- Applied per message
- Examples: field extraction, routing, filtering

---

## Connectors Overview

### Connector Types

| Type | Purpose | Examples |
|------|---------|----------|
| **Source** | Import data INTO Kafka | JDBC, File, HTTP, Logstash |
| **Sink** | Export data FROM Kafka | S3, Azure Blob, Elasticsearch, JDBC |

### Popular Connectors

**Source Connectors:**
- JDBC Source (databases)
- Debezium (CDC - Change Data Capture)
- File Source
- HTTP Source
- MongoDB Source
- Salesforce Source

**Sink Connectors:**
- S3 Sink
- Azure Blob Sink
- Elasticsearch Sink
- JDBC Sink
- HDFS Sink
- BigQuery Sink

---

## Source vs Sink Connectors

### Source Connector Flow

```
External System → Source Connector → Kafka Topic
    (MySQL)           (JDBC)          (users-topic)
```

**Example: JDBC Source**
```json
{
  "name": "mysql-source",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
    "connection.url": "jdbc:mysql://localhost:3306/mydb",
    "mode": "incrementing",
    "incrementing.column.name": "id",
    "topic.prefix": "mysql-",
    "poll.interval.ms": "1000"
  }
}
```

### Sink Connector Flow

```
Kafka Topic → Sink Connector → External System
(logs-topic)   (Azure Blob)    (Azure Storage)
```

**Example: S3 Sink**
```json
{
  "name": "s3-sink",
  "config": {
    "connector.class": "io.confluent.connect.s3.S3SinkConnector",
    "topics": "logs-topic",
    "s3.bucket.name": "my-logs-bucket",
    "flush.size": "1000"
  }
}
```

---

## Kafka Connect Modes

### 1. Standalone Mode

**Use Case:** Development, testing, single-machine deployments

**Characteristics:**
- Single worker process
- Configuration in properties files
- No fault tolerance
- Simple to set up

**Start Standalone:**
```bash
bin/connect-standalone.sh \
  config/connect-standalone.properties \
  config/connector1.properties \
  config/connector2.properties
```

**connect-standalone.properties:**
```properties
bootstrap.servers=localhost:9092
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
offset.storage.file.filename=/tmp/connect.offsets
```

### 2. Distributed Mode (Production)

**Use Case:** Production, high availability, scalability

**Characteristics:**
- Multiple worker processes
- Configuration via REST API
- Automatic rebalancing
- Fault tolerant
- Scalable

**Start Distributed:**
```bash
bin/connect-distributed.sh config/connect-distributed.properties
```

**connect-distributed.properties:**
```properties
bootstrap.servers=kafka1:9092,kafka2:9092,kafka3:9092
group.id=connect-cluster

# Kafka topics for storing connector state
config.storage.topic=connect-configs
config.storage.replication.factor=3

offset.storage.topic=connect-offsets
offset.storage.replication.factor=3

status.storage.topic=connect-status
status.storage.replication.factor=3

key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
```

---

## Real-World Example: Logstash to Azure Blob

### Use Case

**Scenario:** Process application logs with Logstash, send to Kafka, then archive to Azure Blob Storage for long-term retention and analytics.

```
Application Logs → Logstash → Kafka → Kafka Connect → Azure Blob Storage
   (JSON)         (Process)   (Topic)   (Sink)         (Parquet files)
```

### Architecture

```
┌─────────────────┐
│  Application    │
│  (Logs)         │
└────────┬────────┘
         │ JSON logs
         ▼
┌─────────────────┐
│   Logstash      │
│  - Parse logs   │
│  - Enrich data  │
│  - Filter       │
└────────┬────────┘
         │ Kafka Output Plugin
         ▼
┌─────────────────────────────────────┐
│         Kafka Cluster               │
│  Topic: application-logs            │
│  Partitions: 6                      │
│  Replication: 3                     │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Kafka Connect Cluster          │
│  - Azure Blob Sink Connector        │
│  - 3 Workers                        │
│  - 6 Tasks (1 per partition)        │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Azure Blob Storage             │
│  Container: application-logs        │
│  Format: Parquet                    │
│  Partitioning: date/hour            │
└─────────────────────────────────────┘
```

### Step 1: Application Logging

**Spring Boot Application:**
```java
@RestController
@Slf4j
public class OrderController {
    
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            Order order = orderService.createOrder(request);
            
            // Structured logging
            log.info("Order created successfully. " +
                "orderId={}, userId={}, amount={}, duration={}ms",
                order.getId(),
                request.getUserId(),
                order.getTotalAmount(),
                System.currentTimeMillis() - startTime
            );
            
            return ResponseEntity.ok(order);
            
        } catch (Exception e) {
            log.error("Order creation failed. " +
                "userId={}, error={}, duration={}ms",
                request.getUserId(),
                e.getMessage(),
                System.currentTimeMillis() - startTime,
                e
            );
            throw e;
        }
    }
}
```

**Log Output (JSON format via Logback):**
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.example.OrderController",
  "message": "Order created successfully",
  "orderId": "ORD-12345",
  "userId": "USER-789",
  "amount": 99.99,
  "duration": 245,
  "thread": "http-nio-8080-exec-1",
  "application": "order-service",
  "environment": "production"
}
```

### Step 2: Logstash Configuration

**logstash.conf:**
```ruby
input {
  # Read from application log files
  file {
    path => "/var/log/application/*.log"
    start_position => "beginning"
    codec => json
    type => "application-log"
  }
  
  # Or read from Filebeat
  beats {
    port => 5044
    type => "application-log"
  }
}

filter {
  # Parse timestamp
  date {
    match => ["timestamp", "ISO8601"]
    target => "@timestamp"
  }
  
  # Add geolocation if IP present
  if [clientIp] {
    geoip {
      source => "clientIp"
      target => "geoip"
    }
  }
  
  # Enrich with additional metadata
  mutate {
    add_field => {
      "processed_at" => "%{@timestamp}"
      "pipeline_version" => "1.0"
    }
  }
  
  # Filter out health check logs
  if [endpoint] == "/health" {
    drop { }
  }
  
  # Classify log severity
  if [level] == "ERROR" or [level] == "FATAL" {
    mutate {
      add_tag => ["alert"]
      add_field => { "severity" => "high" }
    }
  }
}

output {
  # Send to Kafka
  kafka {
    bootstrap_servers => "kafka1:9092,kafka2:9092,kafka3:9092"
    topic_id => "application-logs"
    codec => json
    compression_type => "snappy"
    
    # Partitioning strategy
    partition_key_field => "userId"
    
    # Producer configs
    acks => "all"
    retries => 3
    max_in_flight_requests_per_connection => 1
  }
  
  # Also send to Elasticsearch for real-time search (optional)
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "logs-%{+YYYY.MM.dd}"
  }
  
  # Debug output (remove in production)
  stdout {
    codec => rubydebug
  }
}
```

**Start Logstash:**
```bash
bin/logstash -f config/logstash.conf
```

### Step 3: Kafka Topic Configuration

**Create Kafka Topic:**
```bash
kafka-topics.sh --create \
  --bootstrap-server kafka1:9092 \
  --topic application-logs \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config compression.type=snappy \
  --config segment.ms=3600000
```

**Topic Configuration Explained:**
- `partitions=6`: Parallel processing (match with connector tasks)
- `replication-factor=3`: High availability
- `retention.ms=604800000`: 7 days retention in Kafka
- `compression.type=snappy`: Fast compression
- `segment.ms=3600000`: 1-hour segments for efficient cleanup

### Step 4: Azure Blob Storage Setup

**Create Azure Storage Account:**
```bash
# Using Azure CLI
az storage account create \
  --name mylogsstorageaccount \
  --resource-group my-resource-group \
  --location eastus \
  --sku Standard_LRS \
  --kind StorageV2

# Create container
az storage container create \
  --name application-logs \
  --account-name mylogsstorageaccount \
  --public-access off
```

**Container Structure:**
```
application-logs/
├── year=2024/
│   ├── month=01/
│   │   ├── day=15/
│   │   │   ├── hour=10/
│   │   │   │   ├── application-logs+0+0000000000.parquet
│   │   │   │   ├── application-logs+1+0000000000.parquet
│   │   │   │   └── application-logs+2+0000000000.parquet
│   │   │   └── hour=11/
│   │   └── day=16/
│   └── month=02/
```

### Step 5: Kafka Connect Azure Blob Sink Configuration

**Install Azure Blob Sink Connector:**
```bash
# Download connector
confluent-hub install confluentinc/kafka-connect-azure-blob-storage:latest

# Or manually download and extract to plugins directory
wget https://d1i4a15mxbxib1.cloudfront.net/api/plugins/confluentinc/kafka-connect-azure-blob-storage/versions/1.6.10/confluentinc-kafka-connect-azure-blob-storage-1.6.10.zip
unzip confluentinc-kafka-connect-azure-blob-storage-1.6.10.zip -d /usr/share/java/
```

**Connector Configuration (azure-blob-sink.json):**
```json
{
  "name": "azure-blob-sink-logs",
  "config": {
    "connector.class": "io.confluent.connect.azure.blob.AzureBlobStorageSinkConnector",
    "tasks.max": "6",
    
    "topics": "application-logs",
    
    "azblob.account.name": "mylogsstorageaccount",
    "azblob.account.key": "${AZURE_STORAGE_KEY}",
    "azblob.container.name": "application-logs",
    
    "format.class": "io.confluent.connect.azure.blob.format.parquet.ParquetFormat",
    "parquet.codec": "snappy",
    
    "flush.size": "1000",
    "rotate.interval.ms": "3600000",
    "rotate.schedule.interval.ms": "3600000",
    
    "partitioner.class": "io.confluent.connect.storage.partitioner.TimeBasedPartitioner",
    "path.format": "'year'=YYYY/'month'=MM/'day'=dd/'hour'=HH",
    "partition.duration.ms": "3600000",
    "timestamp.extractor": "Record",
    "timestamp.field": "timestamp",
    
    "schema.compatibility": "NONE",
    "schema.generator.class": "io.confluent.connect.storage.hive.schema.DefaultSchemaGenerator",
    
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter.schemas.enable": "false",
    
    "errors.tolerance": "all",
    "errors.log.enable": "true",
    "errors.log.include.messages": "true",
    "errors.deadletterqueue.topic.name": "dlq-azure-blob-logs",
    "errors.deadletterqueue.topic.replication.factor": "3",
    
    "transforms": "AddMetadata,ExtractTimestamp",
    "transforms.AddMetadata.type": "org.apache.kafka.connect.transforms.InsertField$Value",
    "transforms.AddMetadata.static.field": "ingestion_time",
    "transforms.AddMetadata.static.value": "${timestamp}",
    "transforms.ExtractTimestamp.type": "org.apache.kafka.connect.transforms.TimestampConverter$Value",
    "transforms.ExtractTimestamp.field": "timestamp",
    "transforms.ExtractTimestamp.target.type": "Timestamp"
  }
}
```

**Configuration Breakdown:**

| Parameter | Value | Purpose |
|-----------|-------|---------|
| `tasks.max` | 6 | Match Kafka topic partitions |
| `format.class` | Parquet | Columnar format for analytics |
| `flush.size` | 1000 | Write after 1000 records |
| `rotate.interval.ms` | 3600000 | New file every hour |
| `path.format` | year/month/day/hour | Hive-style partitioning |
| `errors.tolerance` | all | Continue on errors |
| `errors.deadletterqueue.topic.name` | dlq-azure-blob-logs | Failed records topic |

### Step 6: Deploy Connector

**Using REST API:**
```bash
# Create connector
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @azure-blob-sink.json

# Check status
curl http://localhost:8083/connectors/azure-blob-sink-logs/status | jq

# Response
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
  ]
}
```

### Step 7: Verify Data Flow

**Check Kafka Topic:**
```bash
kafka-console-consumer.sh \
  --bootstrap-server kafka1:9092 \
  --topic application-logs \
  --from-beginning \
  --max-messages 10
```

**Check Azure Blob Storage:**
```bash
# List files
az storage blob list \
  --container-name application-logs \
  --account-name mylogsstorageaccount \
  --output table

# Download sample file
az storage blob download \
  --container-name application-logs \
  --name "year=2024/month=01/day=15/hour=10/application-logs+0+0000000000.parquet" \
  --file sample.parquet \
  --account-name mylogsstorageaccount
```

**Read Parquet File (Python):**
```python
import pandas as pd
import pyarrow.parquet as pq

# Read parquet file
df = pd.read_parquet('sample.parquet')

print(f"Total records: {len(df)}")
print(f"Columns: {df.columns.tolist()}")
print(df.head())

# Query logs
errors = df[df['level'] == 'ERROR']
print(f"Error count: {len(errors)}")
```

---

## Configuration Deep Dive

### Common Configuration Parameters

#### Connection Settings
```json
{
  "bootstrap.servers": "kafka1:9092,kafka2:9092,kafka3:9092",
  "group.id": "connect-cluster",
  "client.id": "connect-worker-1"
}
```

#### Converter Settings
```json
{
  "key.converter": "org.apache.kafka.connect.json.JsonConverter",
  "key.converter.schemas.enable": "false",
  "value.converter": "io.confluent.connect.avro.AvroConverter",
  "value.converter.schema.registry.url": "http://schema-registry:8081"
}
```

#### Task Configuration
```json
{
  "tasks.max": "6",
  "connector.class": "io.confluent.connect.azure.blob.AzureBlobStorageSinkConnector"
}
```

#### Flush and Rotation
```json
{
  "flush.size": "1000",
  "rotate.interval.ms": "3600000",
  "rotate.schedule.interval.ms": "3600000"
}
```

### Converter Types

#### 1. JSON Converter
```json
{
  "value.converter": "org.apache.kafka.connect.json.JsonConverter",
  "value.converter.schemas.enable": "false"
}
```

**Use Case:** Simple JSON data, no schema evolution

#### 2. Avro Converter
```json
{
  "value.converter": "io.confluent.connect.avro.AvroConverter",
  "value.converter.schema.registry.url": "http://schema-registry:8081"
}
```

**Use Case:** Schema evolution, compact binary format

#### 3. Protobuf Converter
```json
{
  "value.converter": "io.confluent.connect.protobuf.ProtobufConverter",
  "value.converter.schema.registry.url": "http://schema-registry:8081"
}
```

**Use Case:** Google Protobuf schemas

#### 4. String Converter
```json
{
  "value.converter": "org.apache.kafka.connect.storage.StringConverter"
}
```

**Use Case:** Plain text data

---

## Transformations (SMTs)

### Single Message Transformations

SMTs allow lightweight data modifications without custom code.

### 1. InsertField - Add Static Fields

```json
{
  "transforms": "AddMetadata",
  "transforms.AddMetadata.type": "org.apache.kafka.connect.transforms.InsertField$Value",
  "transforms.AddMetadata.static.field": "data_source",
  "transforms.AddMetadata.static.value": "logstash",
  "transforms.AddMetadata.timestamp.field": "ingestion_time"
}
```

**Before:**
```json
{
  "orderId": "ORD-123",
  "amount": 99.99
}
```

**After:**
```json
{
  "orderId": "ORD-123",
  "amount": 99.99,
  "data_source": "logstash",
  "ingestion_time": 1705315845123
}
```

### 2. MaskField - Mask Sensitive Data

```json
{
  "transforms": "MaskPII",
  "transforms.MaskPII.type": "org.apache.kafka.connect.transforms.MaskField$Value",
  "transforms.MaskPII.fields": "email,phone,ssn"
}
```

### 3. ExtractField - Extract Nested Field

```json
{
  "transforms": "ExtractUserId",
  "transforms.ExtractUserId.type": "org.apache.kafka.connect.transforms.ExtractField$Value",
  "transforms.ExtractUserId.field": "user.id"
}
```

### 4. ReplaceField - Rename/Remove Fields

```json
{
  "transforms": "RenameFields",
  "transforms.RenameFields.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
  "transforms.RenameFields.renames": "old_name:new_name,user_id:userId",
  "transforms.RenameFields.exclude": "internal_field,temp_data"
}
```

### 5. TimestampConverter - Convert Timestamp Format

```json
{
  "transforms": "ConvertTimestamp",
  "transforms.ConvertTimestamp.type": "org.apache.kafka.connect.transforms.TimestampConverter$Value",
  "transforms.ConvertTimestamp.field": "timestamp",
  "transforms.ConvertTimestamp.target.type": "Timestamp",
  "transforms.ConvertTimestamp.format": "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
}
```

### 6. Filter - Conditional Processing

```json
{
  "transforms": "FilterHealthChecks",
  "transforms.FilterHealthChecks.type": "io.confluent.connect.transforms.Filter$Value",
  "transforms.FilterHealthChecks.filter.condition": "$[?(@.endpoint == '/health')]",
  "transforms.FilterHealthChecks.filter.type": "exclude"
}
```

### 7. Chain Multiple Transformations

```json
{
  "transforms": "AddMetadata,MaskPII,ConvertTimestamp,RenameFields",
  
  "transforms.AddMetadata.type": "org.apache.kafka.connect.transforms.InsertField$Value",
  "transforms.AddMetadata.static.field": "source",
  "transforms.AddMetadata.static.value": "logstash",
  
  "transforms.MaskPII.type": "org.apache.kafka.connect.transforms.MaskField$Value",
  "transforms.MaskPII.fields": "email,phone",
  
  "transforms.ConvertTimestamp.type": "org.apache.kafka.connect.transforms.TimestampConverter$Value",
  "transforms.ConvertTimestamp.field": "timestamp",
  "transforms.ConvertTimestamp.target.type": "Timestamp",
  
  "transforms.RenameFields.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
  "transforms.RenameFields.renames": "user_id:userId,order_id:orderId"
}
```

---

## Continue to Part 2

Part 2 will cover:
- Error Handling and Dead Letter Queue
- Monitoring and Operations
- Performance Tuning
- Production Best Practices
- Troubleshooting Guide
- Interview Questions
