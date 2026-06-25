# Deep Dive: Building RAG with Confluent Kafka, Flink & MongoDB

## Part 2: Confluent Kafka Setup & Producers

---

## Docker Compose (Local Development)

```yaml
# docker-compose.yml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on: [zookeeper]
    ports: ["9092:9092"]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"

  schema-registry:
    image: confluentinc/cp-schema-registry:7.6.0
    depends_on: [kafka]
    ports: ["8081:8081"]
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: kafka:9092

  kafka-connect:
    image: confluentinc/cp-kafka-connect:7.6.0
    depends_on: [kafka, schema-registry]
    ports: ["8083:8083"]
    environment:
      CONNECT_BOOTSTRAP_SERVERS: kafka:9092
      CONNECT_REST_PORT: 8083
      CONNECT_GROUP_ID: "rag-connect"
      CONNECT_CONFIG_STORAGE_TOPIC: _connect-configs
      CONNECT_OFFSET_STORAGE_TOPIC: _connect-offsets
      CONNECT_STATUS_STORAGE_TOPIC: _connect-status
      CONNECT_KEY_CONVERTER: org.apache.kafka.connect.storage.StringConverter
      CONNECT_VALUE_CONVERTER: io.confluent.connect.avro.AvroConverter
      CONNECT_VALUE_CONVERTER_SCHEMA_REGISTRY_URL: http://schema-registry:8081
      CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR: 1
      CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR: 1
      CONNECT_STATUS_STORAGE_REPLICATION_FACTOR: 1
    volumes:
      - ./connectors:/usr/share/confluent-hub-components

  mongodb:
    image: mongodb/mongodb-atlas-local:7.0
    ports: ["27017:27017"]
    environment:
      MONGODB_INITDB_ROOT_USERNAME: admin
      MONGODB_INITDB_ROOT_PASSWORD: password

  flink-jobmanager:
    image: flink:1.19-java11
    ports: ["8082:8081"]
    command: jobmanager
    environment:
      FLINK_PROPERTIES: |
        jobmanager.rpc.address: flink-jobmanager

  flink-taskmanager:
    image: flink:1.19-java11
    depends_on: [flink-jobmanager]
    command: taskmanager
    environment:
      FLINK_PROPERTIES: |
        jobmanager.rpc.address: flink-jobmanager
        taskmanager.numberOfTaskSlots: 4
```

---

## Kafka Topic Configuration

```python
from confluent_kafka.admin import AdminClient, NewTopic

admin = AdminClient({"bootstrap.servers": "localhost:9092"})

topics = [
    NewTopic(
        "raw.documents",
        num_partitions=12,  # Parallelism for ingestion
        replication_factor=1,
        config={
            "retention.ms": str(7 * 24 * 60 * 60 * 1000),  # 7 days
            "cleanup.policy": "delete",
            "max.message.bytes": str(10 * 1024 * 1024),  # 10MB for large docs
        }
    ),
    NewTopic(
        "cleaned.documents",
        num_partitions=12,
        replication_factor=1,
        config={"retention.ms": str(3 * 24 * 60 * 60 * 1000)}  # 3 days
    ),
    NewTopic(
        "chunked.documents",
        num_partitions=24,  # More partitions for parallel embedding
        replication_factor=1,
        config={"retention.ms": str(3 * 24 * 60 * 60 * 1000)}
    ),
    NewTopic(
        "embeddings",
        num_partitions=12,
        replication_factor=1,
        config={"retention.ms": str(24 * 60 * 60 * 1000)}  # 1 day
    ),
    NewTopic(
        "dlq.documents",  # Dead letter queue for failed processing
        num_partitions=3,
        replication_factor=1,
        config={"retention.ms": str(30 * 24 * 60 * 60 * 1000)}  # 30 days
    ),
]

futures = admin.create_topics(topics)
for topic, future in futures.items():
    try:
        future.result()
        print(f"Created topic: {topic}")
    except Exception as e:
        print(f"Failed to create {topic}: {e}")
```

---

## Schema Registry (Avro Schemas)

```python
# schemas/raw_document.avsc
RAW_DOCUMENT_SCHEMA = """
{
  "type": "record",
  "name": "RawDocument",
  "namespace": "com.rag.documents",
  "fields": [
    {"name": "document_id", "type": "string"},
    {"name": "source_type", "type": {"type": "enum", "name": "SourceType", 
      "symbols": ["CONFLUENCE", "SLACK", "S3", "DATABASE", "WEBHOOK", "API"]}},
    {"name": "source_id", "type": "string"},
    {"name": "content", "type": "string"},
    {"name": "content_type", "type": "string", "default": "text/plain"},
    {"name": "metadata", "type": {"type": "map", "values": "string"}},
    {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "operation", "type": {"type": "enum", "name": "Operation",
      "symbols": ["CREATE", "UPDATE", "DELETE"]}},
    {"name": "tenant_id", "type": "string"},
    {"name": "access_control", "type": {"type": "array", "items": "string"}, "default": []}
  ]
}
"""

# schemas/chunk.avsc
CHUNK_SCHEMA = """
{
  "type": "record",
  "name": "DocumentChunk",
  "namespace": "com.rag.documents",
  "fields": [
    {"name": "chunk_id", "type": "string"},
    {"name": "document_id", "type": "string"},
    {"name": "chunk_index", "type": "int"},
    {"name": "total_chunks", "type": "int"},
    {"name": "content", "type": "string"},
    {"name": "content_hash", "type": "string"},
    {"name": "metadata", "type": {"type": "map", "values": "string"}},
    {"name": "tenant_id", "type": "string"},
    {"name": "access_control", "type": {"type": "array", "items": "string"}},
    {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
"""

# schemas/embedding.avsc
EMBEDDING_SCHEMA = """
{
  "type": "record",
  "name": "ChunkEmbedding",
  "namespace": "com.rag.documents",
  "fields": [
    {"name": "chunk_id", "type": "string"},
    {"name": "document_id", "type": "string"},
    {"name": "content", "type": "string"},
    {"name": "embedding", "type": {"type": "array", "items": "float"}},
    {"name": "embedding_model", "type": "string"},
    {"name": "dimensions", "type": "int"},
    {"name": "metadata", "type": {"type": "map", "values": "string"}},
    {"name": "tenant_id", "type": "string"},
    {"name": "access_control", "type": {"type": "array", "items": "string"}},
    {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
"""
```

---

## Document Producers

### Generic Document Producer
```python
import json
import uuid
import time
from confluent_kafka import Producer
from confluent_kafka.serialization import SerializationContext, MessageField
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer

class DocumentProducer:
    """Publishes documents to Kafka for RAG processing."""
    
    def __init__(self, bootstrap_servers: str, schema_registry_url: str):
        self.schema_registry = SchemaRegistryClient({"url": schema_registry_url})
        self.serializer = AvroSerializer(
            self.schema_registry, RAW_DOCUMENT_SCHEMA,
            to_dict=lambda doc, ctx: doc
        )
        self.producer = Producer({
            "bootstrap.servers": bootstrap_servers,
            "linger.ms": 100,          # Batch for throughput
            "batch.size": 65536,
            "compression.type": "lz4",
            "acks": "all",             # Durability
            "enable.idempotence": True, # Exactly-once
        })
    
    def publish_document(self, document: dict, operation: str = "CREATE"):
        """Publish a document event to Kafka."""
        event = {
            "document_id": document.get("id", str(uuid.uuid4())),
            "source_type": document["source_type"],
            "source_id": document["source_id"],
            "content": document["content"],
            "content_type": document.get("content_type", "text/plain"),
            "metadata": document.get("metadata", {}),
            "timestamp": int(time.time() * 1000),
            "operation": operation,
            "tenant_id": document["tenant_id"],
            "access_control": document.get("access_control", []),
        }
        
        self.producer.produce(
            topic="raw.documents",
            key=event["document_id"],  # Partition by document ID (ordering)
            value=self.serializer(event, SerializationContext("raw.documents", MessageField.VALUE)),
            on_delivery=self._delivery_callback,
        )
        self.producer.flush()
    
    def publish_delete(self, document_id: str, tenant_id: str):
        """Publish a document deletion event (tombstone)."""
        self.publish_document({
            "id": document_id,
            "source_type": "API",
            "source_id": document_id,
            "content": "",
            "tenant_id": tenant_id,
        }, operation="DELETE")
    
    def _delivery_callback(self, err, msg):
        if err:
            print(f"Delivery failed: {err}")
        else:
            print(f"Delivered to {msg.topic()}[{msg.partition()}] @ offset {msg.offset()}")
```

### CDC Producer (PostgreSQL → Kafka via Debezium)
```json
// Connect to Kafka Connect REST API to set up CDC
// POST http://localhost:8083/connectors

{
  "name": "postgres-cdc-source",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "debezium",
    "database.password": "<password>",
    "database.dbname": "app_db",
    "database.server.name": "app",
    "table.include.list": "public.documents,public.articles,public.faqs",
    "topic.prefix": "cdc",
    "plugin.name": "pgoutput",
    "slot.name": "rag_slot",
    "transforms": "route",
    "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.route.regex": "cdc\\.public\\.(.*)",
    "transforms.route.replacement": "raw.documents"
  }
}
```

### Confluence Source Connector
```python
import requests
import time

class ConfluenceProducer:
    """Poll Confluence for changes and publish to Kafka."""
    
    def __init__(self, confluence_url: str, token: str, producer: DocumentProducer):
        self.base_url = confluence_url
        self.token = token
        self.producer = producer
        self.headers = {"Authorization": f"Bearer {token}", "Accept": "application/json"}
    
    def poll_changes(self, space_key: str, since_timestamp: int):
        """Fetch recently modified pages."""
        url = f"{self.base_url}/rest/api/content"
        params = {
            "spaceKey": space_key,
            "expand": "body.storage,metadata.labels,version",
            "orderby": "lastmodified desc",
            "limit": 50,
        }
        
        response = requests.get(url, headers=self.headers, params=params)
        pages = response.json()["results"]
        
        for page in pages:
            modified_ts = parse_timestamp(page["version"]["when"])
            if modified_ts <= since_timestamp:
                continue
            
            self.producer.publish_document({
                "source_type": "CONFLUENCE",
                "source_id": f"confluence:{space_key}:{page['id']}",
                "content": page["body"]["storage"]["value"],
                "content_type": "text/html",
                "metadata": {
                    "title": page["title"],
                    "space": space_key,
                    "url": f"{self.base_url}/pages/{page['id']}",
                    "author": page["version"]["by"]["displayName"],
                    "version": str(page["version"]["number"]),
                },
                "tenant_id": "default",
                "access_control": [f"space:{space_key}"],
            })
    
    def run_continuous(self, space_keys: list, poll_interval: int = 60):
        """Continuously poll for changes."""
        last_poll = int(time.time() * 1000) - (poll_interval * 1000)
        
        while True:
            for space in space_keys:
                self.poll_changes(space, last_poll)
            last_poll = int(time.time() * 1000)
            time.sleep(poll_interval)
```

### S3 Event Producer (via S3 Notifications → SQS → Kafka)
```python
import boto3
from confluent_kafka import Producer

class S3EventProducer:
    """Process S3 upload events and publish document content to Kafka."""
    
    def __init__(self, producer: DocumentProducer):
        self.s3 = boto3.client("s3")
        self.sqs = boto3.client("sqs")
        self.producer = producer
    
    def process_s3_events(self, queue_url: str):
        """Poll SQS for S3 event notifications."""
        while True:
            response = self.sqs.receive_message(
                QueueUrl=queue_url,
                MaxNumberOfMessages=10,
                WaitTimeSeconds=20,
            )
            
            for message in response.get("Messages", []):
                event = json.loads(message["Body"])
                for record in event.get("Records", []):
                    bucket = record["s3"]["bucket"]["name"]
                    key = record["s3"]["object"]["key"]
                    
                    # Download and publish
                    obj = self.s3.get_object(Bucket=bucket, Key=key)
                    content = obj["Body"].read().decode("utf-8", errors="ignore")
                    
                    self.producer.publish_document({
                        "source_type": "S3",
                        "source_id": f"s3://{bucket}/{key}",
                        "content": content,
                        "content_type": obj["ContentType"],
                        "metadata": {
                            "bucket": bucket,
                            "key": key,
                            "size": str(obj["ContentLength"]),
                            "last_modified": obj["LastModified"].isoformat(),
                        },
                        "tenant_id": self._extract_tenant(key),
                    })
                
                # Delete processed message
                self.sqs.delete_message(QueueUrl=queue_url, ReceiptHandle=message["ReceiptHandle"])
```

---

## Kafka Connect: MongoDB Sink (Final Stage)

```json
// POST http://localhost:8083/connectors
{
  "name": "mongodb-vector-sink",
  "config": {
    "connector.class": "com.mongodb.kafka.connect.MongoSinkConnector",
    "connection.uri": "mongodb+srv://user:<password>@cluster.mongodb.net",
    "database": "rag_db",
    "collection": "document_chunks",
    "topics": "embeddings",
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "value.converter": "io.confluent.connect.avro.AvroConverter",
    "value.converter.schema.registry.url": "http://schema-registry:8081",
    "document.id.strategy": "com.mongodb.kafka.connect.sink.processor.id.strategy.ProvidedInKeyStrategy",
    "writemodel.strategy": "com.mongodb.kafka.connect.sink.writemodel.strategy.ReplaceOneDefaultStrategy",
    "max.batch.size": "100",
    "bulk.write.ordered": "false"
  }
}
```

---

## Next: [Part 3 - Flink Stream Processing Pipeline](./kafka-flink-mongodb-rag-part3.md)
