# Kafka Connect - Confluent Certification Study Guide

## Overview

**Kafka Connect**: Framework for streaming data between Kafka and external systems

**Benefits**:
- Scalable and fault-tolerant
- No code required (configuration-based)
- Automatic offset management
- Distributed mode for production
- Standalone mode for development

**Types**:
- **Source Connectors**: Import data into Kafka
- **Sink Connectors**: Export data from Kafka

## Architecture

```
Source System → Source Connector → Kafka → Sink Connector → Target System
```

**Components**:
- **Worker**: JVM process running connectors
- **Connector**: High-level abstraction (configuration)
- **Task**: Actual work unit (parallelism)
- **Converter**: Serialization format (JSON, Avro, String)
- **Transform**: Optional data modification

## Running Modes

### Standalone Mode

**Use Case**: Development, testing, single-node

```bash
# Start standalone worker
connect-standalone.sh \
  config/connect-standalone.properties \
  config/connector1.properties \
  config/connector2.properties
```

**Configuration** (connect-standalone.properties):
```properties
bootstrap.servers=localhost:9092
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false
offset.storage.file.filename=/tmp/connect.offsets
```

### Distributed Mode

**Use Case**: Production, scalability, fault tolerance

```bash
# Start distributed worker
connect-distributed.sh config/connect-distributed.properties
```

**Configuration** (connect-distributed.properties):
```properties
bootstrap.servers=localhost:9092
group.id=connect-cluster

# Offset storage (internal topics)
offset.storage.topic=connect-offsets
offset.storage.replication.factor=3
offset.storage.partitions=25

# Config storage
config.storage.topic=connect-configs
config.storage.replication.factor=3

# Status storage
status.storage.topic=connect-status
status.storage.replication.factor=3
status.storage.partitions=5

# Converters
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
```

## REST API

### List Connectors

```bash
curl http://localhost:8083/connectors
```

### Create Connector

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-source-connector",
    "config": {
      "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
      "tasks.max": "1",
      "connection.url": "jdbc:postgresql://localhost:5432/mydb",
      "connection.user": "user",
      "connection.password": "password",
      "table.whitelist": "users",
      "mode": "incrementing",
      "incrementing.column.name": "id",
      "topic.prefix": "db-"
    }
  }'
```

### Get Connector Status

```bash
curl http://localhost:8083/connectors/my-source-connector/status
```

### Get Connector Config

```bash
curl http://localhost:8083/connectors/my-source-connector/config
```

### Update Connector

```bash
curl -X PUT http://localhost:8083/connectors/my-source-connector/config \
  -H "Content-Type: application/json" \
  -d '{
    "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
    "tasks.max": "2",
    ...
  }'
```

### Pause Connector

```bash
curl -X PUT http://localhost:8083/connectors/my-source-connector/pause
```

### Resume Connector

```bash
curl -X PUT http://localhost:8083/connectors/my-source-connector/resume
```

### Restart Connector

```bash
curl -X POST http://localhost:8083/connectors/my-source-connector/restart
```

### Delete Connector

```bash
curl -X DELETE http://localhost:8083/connectors/my-source-connector
```

### List Connector Plugins

```bash
curl http://localhost:8083/connector-plugins
```

## Common Source Connectors

### JDBC Source Connector

**Purpose**: Import data from relational databases

```json
{
  "name": "jdbc-source",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
    "tasks.max": "1",
    "connection.url": "jdbc:postgresql://localhost:5432/mydb",
    "connection.user": "user",
    "connection.password": "password",
    
    "mode": "incrementing",
    "incrementing.column.name": "id",
    
    "table.whitelist": "users,orders",
    "topic.prefix": "db-",
    
    "poll.interval.ms": "5000",
    "batch.max.rows": "1000"
  }
}
```

**Modes**:
- **incrementing**: Track by auto-increment column
- **timestamp**: Track by timestamp column
- **timestamp+incrementing**: Both
- **bulk**: Full table scan (no tracking)

### Debezium CDC Connector

**Purpose**: Capture database changes (CDC)

```json
{
  "name": "debezium-postgres-source",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",
    
    "database.hostname": "localhost",
    "database.port": "5432",
    "database.user": "user",
    "database.password": "password",
    "database.dbname": "mydb",
    "database.server.name": "dbserver1",
    
    "table.include.list": "public.users,public.orders",
    
    "plugin.name": "pgoutput",
    "slot.name": "debezium"
  }
}
```

### File Source Connector

**Purpose**: Read from files

```json
{
  "name": "file-source",
  "config": {
    "connector.class": "FileStreamSource",
    "tasks.max": "1",
    "file": "/tmp/input.txt",
    "topic": "file-topic"
  }
}
```

## Common Sink Connectors

### JDBC Sink Connector

**Purpose**: Export to relational databases

```json
{
  "name": "jdbc-sink",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",
    "tasks.max": "1",
    "connection.url": "jdbc:postgresql://localhost:5432/mydb",
    "connection.user": "user",
    "connection.password": "password",
    
    "topics": "users,orders",
    
    "auto.create": "true",
    "auto.evolve": "true",
    
    "insert.mode": "upsert",
    "pk.mode": "record_key",
    "pk.fields": "id",
    
    "batch.size": "3000"
  }
}
```

**Insert Modes**:
- **insert**: INSERT only
- **upsert**: INSERT or UPDATE
- **update**: UPDATE only

### Elasticsearch Sink Connector

**Purpose**: Index data in Elasticsearch

```json
{
  "name": "elasticsearch-sink",
  "config": {
    "connector.class": "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
    "tasks.max": "1",
    "topics": "users",
    
    "connection.url": "http://localhost:9200",
    "type.name": "_doc",
    
    "key.ignore": "false",
    "schema.ignore": "true",
    
    "batch.size": "2000"
  }
}
```

### S3 Sink Connector

**Purpose**: Write to Amazon S3

```json
{
  "name": "s3-sink",
  "config": {
    "connector.class": "io.confluent.connect.s3.S3SinkConnector",
    "tasks.max": "1",
    "topics": "users",
    
    "s3.bucket.name": "my-bucket",
    "s3.region": "us-east-1",
    
    "flush.size": "1000",
    "rotate.interval.ms": "60000",
    
    "format.class": "io.confluent.connect.s3.format.json.JsonFormat",
    "partitioner.class": "io.confluent.connect.storage.partitioner.TimeBasedPartitioner",
    "path.format": "'year'=YYYY/'month'=MM/'day'=dd",
    "partition.duration.ms": "3600000",
    "timestamp.extractor": "Record"
  }
}
```

## Converters

### JSON Converter

```properties
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false
```

**With Schema**:
```json
{
  "schema": {
    "type": "struct",
    "fields": [
      {"field": "id", "type": "int32"},
      {"field": "name", "type": "string"}
    ]
  },
  "payload": {
    "id": 1,
    "name": "John"
  }
}
```

**Without Schema**:
```json
{
  "id": 1,
  "name": "John"
}
```

### Avro Converter

```properties
key.converter=io.confluent.connect.avro.AvroConverter
value.converter=io.confluent.connect.avro.AvroConverter
key.converter.schema.registry.url=http://localhost:8081
value.converter.schema.registry.url=http://localhost:8081
```

### String Converter

```properties
key.converter=org.apache.kafka.connect.storage.StringConverter
value.converter=org.apache.kafka.connect.storage.StringConverter
```

### ByteArray Converter

```properties
key.converter=org.apache.kafka.connect.converters.ByteArrayConverter
value.converter=org.apache.kafka.connect.converters.ByteArrayConverter
```

## Single Message Transforms (SMT)

### InsertField

```json
{
  "transforms": "InsertTimestamp",
  "transforms.InsertTimestamp.type": "org.apache.kafka.connect.transforms.InsertField$Value",
  "transforms.InsertTimestamp.timestamp.field": "timestamp"
}
```

### ReplaceField

```json
{
  "transforms": "RenameField",
  "transforms.RenameField.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
  "transforms.RenameField.renames": "old_name:new_name"
}
```

### MaskField

```json
{
  "transforms": "MaskSSN",
  "transforms.MaskSSN.type": "org.apache.kafka.connect.transforms.MaskField$Value",
  "transforms.MaskSSN.fields": "ssn",
  "transforms.MaskSSN.replacement": "***-**-****"
}
```

### Filter

```json
{
  "transforms": "FilterDeleted",
  "transforms.FilterDeleted.type": "org.apache.kafka.connect.transforms.Filter",
  "transforms.FilterDeleted.predicate": "IsDeleted",
  
  "predicates": "IsDeleted",
  "predicates.IsDeleted.type": "org.apache.kafka.connect.transforms.predicates.TopicNameMatches",
  "predicates.IsDeleted.pattern": ".*deleted.*"
}
```

### Cast

```json
{
  "transforms": "Cast",
  "transforms.Cast.type": "org.apache.kafka.connect.transforms.Cast$Value",
  "transforms.Cast.spec": "age:int32,salary:float64"
}
```

### TimestampConverter

```json
{
  "transforms": "ConvertTimestamp",
  "transforms.ConvertTimestamp.type": "org.apache.kafka.connect.transforms.TimestampConverter$Value",
  "transforms.ConvertTimestamp.field": "created_at",
  "transforms.ConvertTimestamp.target.type": "Timestamp",
  "transforms.ConvertTimestamp.format": "yyyy-MM-dd HH:mm:ss"
}
```

### Chaining Transforms

```json
{
  "transforms": "InsertTimestamp,MaskSSN,RenameField",
  "transforms.InsertTimestamp.type": "...",
  "transforms.MaskSSN.type": "...",
  "transforms.RenameField.type": "..."
}
```

## Error Handling

### Dead Letter Queue

```json
{
  "errors.tolerance": "all",
  "errors.deadletterqueue.topic.name": "dlq-topic",
  "errors.deadletterqueue.topic.replication.factor": "3",
  "errors.deadletterqueue.context.headers.enable": "true"
}
```

### Retry Configuration

```json
{
  "errors.retry.timeout": "300000",
  "errors.retry.delay.max.ms": "60000"
}
```

### Log Errors

```json
{
  "errors.tolerance": "all",
  "errors.log.enable": "true",
  "errors.log.include.messages": "true"
}
```

## Monitoring

### Connector Metrics

```bash
# JMX metrics
kafka.connect:type=connector-metrics,connector="{connector}"

# Task metrics
kafka.connect:type=task-metrics,connector="{connector}",task="{task}"

# Worker metrics
kafka.connect:type=connect-worker-metrics
```

### Key Metrics

```
# Source connectors
source-record-poll-rate
source-record-write-rate

# Sink connectors
sink-record-read-rate
sink-record-send-rate

# Tasks
status (running, failed, paused)
offset-commit-completion-rate
```

## Configuration Best Practices

### 1. Set Appropriate Task Count

```json
{
  "tasks.max": "3"
}
// Balance: More tasks = more parallelism, more overhead
```

### 2. Configure Batch Sizes

```json
{
  "batch.size": "3000",
  "consumer.max.poll.records": "500"
}
```

### 3. Use Avro for Production

```properties
value.converter=io.confluent.connect.avro.AvroConverter
value.converter.schema.registry.url=http://localhost:8081
```

### 4. Enable Error Handling

```json
{
  "errors.tolerance": "all",
  "errors.deadletterqueue.topic.name": "dlq-topic"
}
```

### 5. Set Replication Factors

```properties
offset.storage.replication.factor=3
config.storage.replication.factor=3
status.storage.replication.factor=3
```

## Custom Connector Development

### Source Connector

```java
public class MySourceConnector extends SourceConnector {
    @Override
    public void start(Map<String, String> props) {
        // Initialize connector
    }
    
    @Override
    public Class<? extends Task> taskClass() {
        return MySourceTask.class;
    }
    
    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        // Return task configurations
    }
    
    @Override
    public void stop() {
        // Cleanup
    }
    
    @Override
    public ConfigDef config() {
        // Define configuration
    }
    
    @Override
    public String version() {
        return "1.0";
    }
}
```

### Source Task

```java
public class MySourceTask extends SourceTask {
    @Override
    public void start(Map<String, String> props) {
        // Initialize task
    }
    
    @Override
    public List<SourceRecord> poll() {
        // Fetch data and return records
        List<SourceRecord> records = new ArrayList<>();
        
        SourceRecord record = new SourceRecord(
            sourcePartition,
            sourceOffset,
            topic,
            Schema.STRING_SCHEMA,
            key,
            Schema.STRING_SCHEMA,
            value
        );
        
        records.add(record);
        return records;
    }
    
    @Override
    public void stop() {
        // Cleanup
    }
    
    @Override
    public String version() {
        return "1.0";
    }
}
```

## Exam Practice Questions

**Q1**: What is the difference between standalone and distributed mode?
**A**: Standalone for dev/testing (single node); distributed for production (scalable, fault-tolerant)

**Q2**: Where are connector offsets stored in distributed mode?
**A**: Internal Kafka topic (offset.storage.topic)

**Q3**: What is the purpose of tasks.max?
**A**: Maximum number of parallel tasks for connector

**Q4**: Can you change connector configuration without restarting?
**A**: Yes, use REST API PUT /connectors/{name}/config

**Q5**: What is a Single Message Transform (SMT)?
**A**: Lightweight transformation applied to each record

**Q6**: What happens when errors.tolerance=all?
**A**: Connector continues processing despite errors (can use DLQ)

**Q7**: What is the default converter if not specified?
**A**: JsonConverter

**Q8**: Can source and sink connectors run in same Connect cluster?
**A**: Yes, they can coexist

**Q9**: What is the purpose of Dead Letter Queue?
**A**: Store records that failed processing for later analysis

**Q10**: How do you scale a connector?
**A**: Increase tasks.max and add more worker nodes
