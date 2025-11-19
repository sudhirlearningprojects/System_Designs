# Schema Registry - Confluent Certification Study Guide

## Overview

**Schema Registry**: Centralized service for managing schemas

**Benefits**:
- Schema versioning and evolution
- Compatibility checking
- Reduced message size (schema ID instead of full schema)
- Data governance and documentation

**Supported Formats**:
- Avro (most common)
- JSON Schema
- Protobuf

## Architecture

```
Producer → Schema Registry (register/validate) → Kafka
                                                    ↓
Consumer ← Schema Registry (fetch schema) ← Kafka
```

**Storage**: Schemas stored in internal Kafka topic `_schemas`

## Avro Basics

### Schema Definition

```json
{
  "type": "record",
  "name": "User",
  "namespace": "com.example",
  "fields": [
    {"name": "id", "type": "int"},
    {"name": "name", "type": "string"},
    {"name": "email", "type": ["null", "string"], "default": null}
  ]
}
```

### Primitive Types

```
null, boolean, int, long, float, double, bytes, string
```

### Complex Types

**Record**:
```json
{
  "type": "record",
  "name": "Address",
  "fields": [
    {"name": "street", "type": "string"},
    {"name": "city", "type": "string"}
  ]
}
```

**Array**:
```json
{"type": "array", "items": "string"}
```

**Map**:
```json
{"type": "map", "values": "int"}
```

**Union** (nullable):
```json
{"type": ["null", "string"], "default": null}
```

**Enum**:
```json
{
  "type": "enum",
  "name": "Status",
  "symbols": ["ACTIVE", "INACTIVE", "PENDING"]
}
```

## Schema Registry API

### Register Schema

```bash
curl -X POST http://localhost:8081/subjects/user-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"},{\"name\":\"name\",\"type\":\"string\"}]}"
  }'

# Response: {"id": 1}
```

### Get Schema by ID

```bash
curl http://localhost:8081/schemas/ids/1

# Response:
# {
#   "schema": "{\"type\":\"record\",\"name\":\"User\",...}"
# }
```

### Get Latest Schema

```bash
curl http://localhost:8081/subjects/user-value/versions/latest
```

### List All Subjects

```bash
curl http://localhost:8081/subjects

# Response: ["user-value", "order-value"]
```

### List Versions

```bash
curl http://localhost:8081/subjects/user-value/versions

# Response: [1, 2, 3]
```

### Delete Schema

```bash
# Soft delete (can be recovered)
curl -X DELETE http://localhost:8081/subjects/user-value/versions/1

# Hard delete (permanent)
curl -X DELETE http://localhost:8081/subjects/user-value/versions/1?permanent=true
```

## Compatibility Modes

### BACKWARD (default)

**Rule**: New schema can read old data
**Use Case**: Consumer upgrade before producer

```
Old Schema: {id: int, name: string}
New Schema: {id: int, name: string, email: string (default: null)}
✓ Can read old data (email will be null)
```

**Allowed Changes**:
- Add field with default
- Remove field

### FORWARD

**Rule**: Old schema can read new data
**Use Case**: Producer upgrade before consumer

```
Old Schema: {id: int, name: string, email: string}
New Schema: {id: int, name: string}
✓ Old schema ignores missing email field
```

**Allowed Changes**:
- Remove field with default
- Add field

### FULL

**Rule**: Both backward and forward compatible
**Use Case**: Flexible upgrade order

**Allowed Changes**:
- Add field with default
- Remove field with default

### NONE

**Rule**: No compatibility checking
**Use Case**: Development/testing only

### BACKWARD_TRANSITIVE

**Rule**: New schema compatible with ALL previous versions

### FORWARD_TRANSITIVE

**Rule**: All previous schemas compatible with new schema

### FULL_TRANSITIVE

**Rule**: Both backward and forward transitive

## Setting Compatibility

### Global Level

```bash
curl -X PUT http://localhost:8081/config \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "BACKWARD"}'
```

### Subject Level

```bash
curl -X PUT http://localhost:8081/config/user-value \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "FULL"}'
```

### Check Compatibility

```bash
curl -X POST http://localhost:8081/compatibility/subjects/user-value/versions/latest \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"}]}"
  }'

# Response: {"is_compatible": true}
```

## Producer with Schema Registry

### Configuration

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
props.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
props.put("schema.registry.url", "http://localhost:8081");

// Optional: Auto-register schemas
props.put("auto.register.schemas", "true");  // default: true

// Optional: Use latest version
props.put("use.latest.version", "false");    // default: false
```

### Sending Avro Records

**Generic Record**:
```java
String schemaString = "{\"type\":\"record\",\"name\":\"User\"," +
    "\"fields\":[{\"name\":\"id\",\"type\":\"int\"}," +
    "{\"name\":\"name\",\"type\":\"string\"}]}";

Schema schema = new Schema.Parser().parse(schemaString);

GenericRecord user = new GenericData.Record(schema);
user.put("id", 1);
user.put("name", "John");

ProducerRecord<String, GenericRecord> record = 
    new ProducerRecord<>("users", "key1", user);

producer.send(record);
```

**Specific Record** (generated class):
```java
// Generated from Avro schema
User user = User.newBuilder()
    .setId(1)
    .setName("John")
    .build();

ProducerRecord<String, User> record = 
    new ProducerRecord<>("users", "key1", user);

producer.send(record);
```

## Consumer with Schema Registry

### Configuration

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("group.id", "user-consumer");
props.put("key.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer");
props.put("value.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer");
props.put("schema.registry.url", "http://localhost:8081");

// For specific records
props.put("specific.avro.reader", "true");
```

### Consuming Avro Records

**Generic Record**:
```java
KafkaConsumer<String, GenericRecord> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("users"));

while (true) {
    ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, GenericRecord> record : records) {
        GenericRecord user = record.value();
        System.out.println("ID: " + user.get("id"));
        System.out.println("Name: " + user.get("name"));
    }
}
```

**Specific Record**:
```java
KafkaConsumer<String, User> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("users"));

while (true) {
    ConsumerRecords<String, User> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, User> record : records) {
        User user = record.value();
        System.out.println("ID: " + user.getId());
        System.out.println("Name: " + user.getName());
    }
}
```

## Schema Evolution Examples

### Adding Field with Default (BACKWARD)

**Old Schema**:
```json
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "int"},
    {"name": "name", "type": "string"}
  ]
}
```

**New Schema**:
```json
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "int"},
    {"name": "name", "type": "string"},
    {"name": "email", "type": ["null", "string"], "default": null}
  ]
}
```

✓ New consumer can read old data (email = null)

### Removing Field (BACKWARD)

**Old Schema**:
```json
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "int"},
    {"name": "name", "type": "string"},
    {"name": "email", "type": ["null", "string"], "default": null}
  ]
}
```

**New Schema**:
```json
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "int"},
    {"name": "name", "type": "string"}
  ]
}
```

✓ New consumer ignores email field from old data

### Changing Field Type (INCOMPATIBLE)

```json
// Old: {"name": "age", "type": "int"}
// New: {"name": "age", "type": "string"}
✗ Incompatible - cannot convert int to string
```

## Kafka Streams with Schema Registry

### Configuration

```java
Properties props = new Properties();
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-app");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put("schema.registry.url", "http://localhost:8081");

// Serdes
Map<String, String> serdeConfig = Collections.singletonMap(
    "schema.registry.url", "http://localhost:8081"
);

Serde<GenericRecord> valueSerde = new GenericAvroSerde();
valueSerde.configure(serdeConfig, false); // false = value serde

props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class);
```

### Processing Avro Records

```java
StreamsBuilder builder = new StreamsBuilder();

KStream<String, GenericRecord> stream = builder.stream("users");

KStream<String, GenericRecord> processed = stream
    .filter((key, user) -> (Integer) user.get("id") > 100)
    .mapValues(user -> {
        GenericRecord newUser = new GenericData.Record(user.getSchema());
        newUser.put("id", user.get("id"));
        newUser.put("name", ((String) user.get("name")).toUpperCase());
        return newUser;
    });

processed.to("users-processed");
```

## Subject Naming Strategies

### TopicNameStrategy (default)

```
Subject: <topic>-key or <topic>-value
Example: users-value
```

### RecordNameStrategy

```
Subject: <record-name>
Example: com.example.User
```

### TopicRecordNameStrategy

```
Subject: <topic>-<record-name>
Example: users-com.example.User
```

**Configuration**:
```java
props.put("value.subject.name.strategy", 
    "io.confluent.kafka.serializers.subject.RecordNameStrategy");
```

## Best Practices

### 1. Always Use Default Values

```json
{"name": "email", "type": ["null", "string"], "default": null}
```

### 2. Set Appropriate Compatibility Mode

```bash
# For most cases
curl -X PUT http://localhost:8081/config \
  -d '{"compatibility": "BACKWARD"}'
```

### 3. Version Schemas Carefully

```
v1: Add fields with defaults
v2: Remove optional fields
v3: Never change field types
```

### 4. Use Specific Records in Production

```java
// Better type safety and performance
props.put("specific.avro.reader", "true");
```

### 5. Monitor Schema Registry

```bash
# Check health
curl http://localhost:8081/

# Monitor metrics
curl http://localhost:8081/metrics
```

## Common Issues

### Schema Not Found

```
Error: Schema not found for subject
Solution: Register schema or enable auto.register.schemas=true
```

### Incompatible Schema

```
Error: Schema being registered is incompatible
Solution: Check compatibility mode and schema changes
```

### Serialization Error

```
Error: Failed to serialize Avro data
Solution: Verify schema matches data structure
```

## Exam Practice Questions

**Q1**: What is the default compatibility mode?
**A**: BACKWARD

**Q2**: Where are schemas stored?
**A**: Internal Kafka topic `_schemas`

**Q3**: What does BACKWARD compatibility allow?
**A**: Add field with default, remove field

**Q4**: Can you change field type in BACKWARD mode?
**A**: No, field type changes are incompatible

**Q5**: What is the difference between BACKWARD and BACKWARD_TRANSITIVE?
**A**: BACKWARD checks last version; BACKWARD_TRANSITIVE checks all versions

**Q6**: What is the purpose of schema ID in messages?
**A**: Reference to schema in registry, reduces message size

**Q7**: What does specific.avro.reader=true do?
**A**: Deserializes to generated specific classes instead of GenericRecord

**Q8**: Can you have different compatibility modes per subject?
**A**: Yes, subject-level overrides global setting

**Q9**: What is the default subject naming strategy?
**A**: TopicNameStrategy (<topic>-key or <topic>-value)

**Q10**: How do you prevent automatic schema registration?
**A**: Set auto.register.schemas=false
