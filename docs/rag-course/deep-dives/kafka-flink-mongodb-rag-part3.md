# Deep Dive: Building RAG with Confluent Kafka, Flink & MongoDB

## Part 3: Flink Stream Processing Pipeline

---

## Flink's Role in the RAG Pipeline

Flink handles all real-time transformations between raw documents and embeddings:

```
raw.documents → [Parse & Clean] → [Chunk] → [Enrich Metadata] → [Embed] → embeddings
```

Flink provides:
- **Exactly-once semantics** with Kafka (no duplicate chunks)
- **Backpressure management** (when embedding API is slow)
- **Stateful processing** (deduplication, windowed batching)
- **Parallel execution** across task slots

---

## Flink Job 1: Document Parser & Cleaner

### Java Implementation (Production-Grade)

```java
package com.rag.flink;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.common.functions.MapFunction;

public class DocumentParserJob {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(60000); // 60s checkpoints for exactly-once
        
        // Source: raw.documents topic
        KafkaSource<RawDocument> source = KafkaSource.<RawDocument>builder()
            .setBootstrapServers("localhost:9092")
            .setTopics("raw.documents")
            .setGroupId("flink-document-parser")
            .setValueOnlyDeserializer(new RawDocumentDeserializer())
            .build();
        
        DataStream<RawDocument> rawDocs = env.fromSource(
            source, WatermarkStrategy.noWatermarks(), "Kafka Source"
        );
        
        // Parse and clean
        DataStream<CleanedDocument> cleanedDocs = rawDocs
            .filter(doc -> !doc.getOperation().equals("DELETE"))
            .map(new DocumentParserFunction())
            .filter(doc -> doc.getContent().length() > 50); // Skip empty/tiny docs
        
        // Sink: cleaned.documents topic
        KafkaSink<CleanedDocument> sink = KafkaSink.<CleanedDocument>builder()
            .setBootstrapServers("localhost:9092")
            .setRecordSerializer(
                KafkaRecordSerializationSchema.builder()
                    .setTopic("cleaned.documents")
                    .setKeySerializationSchema(new CleanedDocumentKeySerializer())
                    .setValueSerializationSchema(new CleanedDocumentValueSerializer())
                    .build()
            )
            .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
            .build();
        
        cleanedDocs.sinkTo(sink);
        
        // Handle deletions separately (propagate tombstones)
        rawDocs
            .filter(doc -> doc.getOperation().equals("DELETE"))
            .map(doc -> new DeletionEvent(doc.getDocumentId(), doc.getTenantId()))
            .sinkTo(deletionSink);
        
        env.execute("RAG Document Parser");
    }
}
```

### Document Parser Function
```java
public class DocumentParserFunction implements MapFunction<RawDocument, CleanedDocument> {
    
    @Override
    public CleanedDocument map(RawDocument raw) throws Exception {
        String cleanContent;
        
        switch (raw.getContentType()) {
            case "text/html":
                cleanContent = parseHtml(raw.getContent());
                break;
            case "application/pdf":
                cleanContent = parsePdfBase64(raw.getContent());
                break;
            case "text/markdown":
                cleanContent = parseMarkdown(raw.getContent());
                break;
            default:
                cleanContent = cleanPlainText(raw.getContent());
        }
        
        return CleanedDocument.builder()
            .documentId(raw.getDocumentId())
            .content(cleanContent)
            .metadata(raw.getMetadata())
            .tenantId(raw.getTenantId())
            .accessControl(raw.getAccessControl())
            .timestamp(raw.getTimestamp())
            .build();
    }
    
    private String parseHtml(String html) {
        // Jsoup for HTML cleaning
        Document doc = Jsoup.parse(html);
        doc.select("script, style, nav, footer, header").remove();
        return doc.text();
    }
    
    private String cleanPlainText(String text) {
        return text.replaceAll("\\s+", " ")
                   .replaceAll("[^\\p{Print}]", "")
                   .trim();
    }
}
```

---

## Flink Job 2: Chunking Engine

### PyFlink Implementation (More Flexible for NLP)

```python
from pyflink.datastream import StreamExecutionEnvironment
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaOffsetsInitializer
from pyflink.common.serialization import SimpleStringSchema
from pyflink.datastream.functions import MapFunction, FlatMapFunction
import json
import hashlib
import uuid

class ChunkingFunction(FlatMapFunction):
    """Split documents into semantic chunks."""
    
    def open(self, runtime_context):
        from langchain.text_splitter import RecursiveCharacterTextSplitter
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=1000,
            chunk_overlap=200,
            separators=["\n\n", "\n", ". ", " ", ""],
        )
    
    def flat_map(self, value):
        doc = json.loads(value)
        content = doc["content"]
        
        # Split into chunks
        chunks = self.splitter.split_text(content)
        total_chunks = len(chunks)
        
        for idx, chunk_text in enumerate(chunks):
            chunk_id = f"{doc['document_id']}_{idx}"
            content_hash = hashlib.sha256(chunk_text.encode()).hexdigest()
            
            chunk_event = {
                "chunk_id": chunk_id,
                "document_id": doc["document_id"],
                "chunk_index": idx,
                "total_chunks": total_chunks,
                "content": chunk_text,
                "content_hash": content_hash,
                "metadata": {
                    **doc.get("metadata", {}),
                    "chunk_index": str(idx),
                    "total_chunks": str(total_chunks),
                },
                "tenant_id": doc["tenant_id"],
                "access_control": doc.get("access_control", []),
                "timestamp": doc["timestamp"],
            }
            
            yield json.dumps(chunk_event)


def create_chunking_job():
    env = StreamExecutionEnvironment.get_execution_environment()
    env.set_parallelism(4)
    env.enable_checkpointing(60000)
    
    # Kafka source
    source = KafkaSource.builder() \
        .set_bootstrap_servers("localhost:9092") \
        .set_topics("cleaned.documents") \
        .set_group_id("flink-chunker") \
        .set_starting_offsets(KafkaOffsetsInitializer.committed_offsets()) \
        .set_value_only_deserializer(SimpleStringSchema()) \
        .build()
    
    # Kafka sink
    sink = KafkaSink.builder() \
        .set_bootstrap_servers("localhost:9092") \
        .set_record_serializer(
            KafkaRecordSerializationSchema.builder()
                .set_topic("chunked.documents")
                .set_value_serialization_schema(SimpleStringSchema())
                .build()
        ) \
        .set_delivery_guarantee(DeliveryGuarantee.EXACTLY_ONCE) \
        .build()
    
    # Pipeline
    ds = env.from_source(source, WatermarkStrategy.no_watermarks(), "Kafka Source")
    ds.flat_map(ChunkingFunction()).sink_to(sink)
    
    env.execute("RAG Chunking Job")

if __name__ == "__main__":
    create_chunking_job()
```

---

## Flink Job 3: Embedding Generation (with Batching & Rate Limiting)

This is the most complex job — it needs to:
1. Batch chunks for efficient API calls
2. Handle rate limits with backpressure
3. Manage API failures with retries
4. Support multiple embedding providers

```python
from pyflink.datastream import StreamExecutionEnvironment
from pyflink.datastream.functions import ProcessFunction, KeyedProcessFunction
from pyflink.datastream.state import ValueStateDescriptor, ListStateDescriptor
from pyflink.common.typeinfo import Types
import json
import time

class BatchEmbeddingFunction(KeyedProcessFunction):
    """Batch chunks and generate embeddings with rate limit management."""
    
    BATCH_SIZE = 50        # Chunks per API call
    BATCH_TIMEOUT_MS = 5000  # Max wait time before flushing partial batch
    MAX_RETRIES = 3
    
    def open(self, runtime_context):
        import openai
        self.client = openai.OpenAI()
        self.model = "text-embedding-3-small"
        self.dimensions = 1024
        
        # State: accumulate chunks until batch is full
        self.buffer_state = runtime_context.get_list_state(
            ListStateDescriptor("chunk_buffer", Types.STRING())
        )
        self.timer_registered = runtime_context.get_state(
            ValueStateDescriptor("timer_registered", Types.BOOLEAN())
        )
    
    def process_element(self, value, ctx):
        """Accumulate chunks in state, flush when batch is full."""
        self.buffer_state.add(value)
        
        # Register timer for partial batch flush
        if not self.timer_registered.value():
            ctx.timer_service().register_processing_time_timer(
                ctx.timer_service().current_processing_time() + self.BATCH_TIMEOUT_MS
            )
            self.timer_registered.update(True)
        
        # Check if batch is full
        buffer = list(self.buffer_state.get())
        if len(buffer) >= self.BATCH_SIZE:
            self._flush_batch(buffer, ctx)
    
    def on_timer(self, timestamp, ctx):
        """Flush partial batch on timeout."""
        buffer = list(self.buffer_state.get())
        if buffer:
            self._flush_batch(buffer, ctx)
        self.timer_registered.update(False)
    
    def _flush_batch(self, buffer: list, ctx):
        """Generate embeddings for a batch of chunks."""
        chunks = [json.loads(item) for item in buffer]
        texts = [chunk["content"] for chunk in chunks]
        
        # Call embedding API with retry
        embeddings = self._embed_with_retry(texts)
        
        if embeddings:
            for chunk, embedding in zip(chunks, embeddings):
                result = {
                    "chunk_id": chunk["chunk_id"],
                    "document_id": chunk["document_id"],
                    "content": chunk["content"],
                    "embedding": embedding,
                    "embedding_model": self.model,
                    "dimensions": self.dimensions,
                    "metadata": chunk["metadata"],
                    "tenant_id": chunk["tenant_id"],
                    "access_control": chunk.get("access_control", []),
                    "timestamp": chunk["timestamp"],
                }
                ctx.output(json.dumps(result))
        
        # Clear buffer
        self.buffer_state.clear()
        self.timer_registered.update(False)
    
    def _embed_with_retry(self, texts: list[str]) -> list[list[float]] | None:
        """Embed with exponential backoff retry."""
        for attempt in range(self.MAX_RETRIES):
            try:
                response = self.client.embeddings.create(
                    model=self.model,
                    input=texts,
                    dimensions=self.dimensions,
                )
                return [item.embedding for item in response.data]
            except Exception as e:
                if "rate_limit" in str(e).lower():
                    wait = 2 ** attempt
                    time.sleep(wait)
                else:
                    print(f"Embedding error (attempt {attempt}): {e}")
                    if attempt == self.MAX_RETRIES - 1:
                        # Send to DLQ
                        return None
        return None


def create_embedding_job():
    env = StreamExecutionEnvironment.get_execution_environment()
    env.set_parallelism(8)  # More parallelism for embedding throughput
    env.enable_checkpointing(30000)
    
    source = KafkaSource.builder() \
        .set_bootstrap_servers("localhost:9092") \
        .set_topics("chunked.documents") \
        .set_group_id("flink-embedder") \
        .set_value_only_deserializer(SimpleStringSchema()) \
        .build()
    
    sink = KafkaSink.builder() \
        .set_bootstrap_servers("localhost:9092") \
        .set_record_serializer(
            KafkaRecordSerializationSchema.builder()
                .set_topic("embeddings")
                .set_value_serialization_schema(SimpleStringSchema())
                .build()
        ) \
        .build()
    
    ds = env.from_source(source, WatermarkStrategy.no_watermarks(), "Chunked Docs")
    
    # Key by tenant_id for per-tenant rate limiting
    ds.key_by(lambda x: json.loads(x).get("tenant_id", "default")) \
      .process(BatchEmbeddingFunction()) \
      .sink_to(sink)
    
    env.execute("RAG Embedding Job")
```

---

## Flink Job: Deduplication (Stateful)

Prevent re-processing identical chunks when documents are re-ingested:

```python
class DeduplicationFunction(KeyedProcessFunction):
    """Skip chunks that haven't changed (based on content hash)."""
    
    TTL_MS = 7 * 24 * 60 * 60 * 1000  # 7 days state retention
    
    def open(self, runtime_context):
        from pyflink.datastream.state import ValueStateDescriptor, StateTtlConfig
        
        ttl_config = StateTtlConfig.new_builder(Time.days(7)) \
            .set_update_type(StateTtlConfig.UpdateType.OnCreateAndWrite) \
            .build()
        
        desc = ValueStateDescriptor("content_hash", Types.STRING())
        desc.enable_time_to_live(ttl_config)
        self.hash_state = runtime_context.get_state(desc)
    
    def process_element(self, value, ctx):
        chunk = json.loads(value)
        content_hash = chunk["content_hash"]
        
        # Check if we've seen this exact content before
        stored_hash = self.hash_state.value()
        if stored_hash == content_hash:
            # Skip - content hasn't changed
            return
        
        # New or updated content - process it
        self.hash_state.update(content_hash)
        yield value
```

---

## Flink Job: Document Deletion Propagation

```python
class DeletionHandler(ProcessFunction):
    """Handle document deletions by removing chunks from downstream."""
    
    def process_element(self, value, ctx):
        event = json.loads(value)
        
        if event.get("operation") == "DELETE":
            # Publish deletion markers for all chunks of this document
            deletion_event = {
                "document_id": event["document_id"],
                "tenant_id": event["tenant_id"],
                "operation": "DELETE",
                "timestamp": int(time.time() * 1000),
            }
            # This will trigger MongoDB delete via sink connector
            yield json.dumps(deletion_event)
```

---

## Flink SQL Alternative (Simpler for Basic Pipelines)

```sql
-- Flink SQL for simpler transformations
CREATE TABLE raw_documents (
    document_id STRING,
    content STRING,
    content_type STRING,
    metadata MAP<STRING, STRING>,
    tenant_id STRING,
    `timestamp` BIGINT,
    operation STRING
) WITH (
    'connector' = 'kafka',
    'topic' = 'raw.documents',
    'properties.bootstrap.servers' = 'localhost:9092',
    'format' = 'json',
    'scan.startup.mode' = 'latest-offset'
);

CREATE TABLE cleaned_documents (
    document_id STRING,
    content STRING,
    metadata MAP<STRING, STRING>,
    tenant_id STRING,
    `timestamp` BIGINT
) WITH (
    'connector' = 'kafka',
    'topic' = 'cleaned.documents',
    'properties.bootstrap.servers' = 'localhost:9092',
    'format' = 'json'
);

-- Simple cleaning pipeline in SQL
INSERT INTO cleaned_documents
SELECT 
    document_id,
    REGEXP_REPLACE(content, '<[^>]+>', '') AS content,  -- Strip HTML
    metadata,
    tenant_id,
    `timestamp`
FROM raw_documents
WHERE operation <> 'DELETE'
  AND CHAR_LENGTH(content) > 50;
```

---

## Monitoring Flink Jobs

```python
# Flink exposes metrics via REST API
# http://localhost:8082/jobs/{job_id}/metrics

# Key metrics to monitor:
# - records-lag-max: Consumer lag (how far behind real-time)
# - numRecordsInPerSecond: Throughput
# - numRecordsOutPerSecond: Output rate
# - checkpointDuration: Checkpoint time (impacts latency)
# - numRestarts: Job restart count (stability)
```

---

## Next: [Part 4 - MongoDB Atlas Vector Search](./kafka-flink-mongodb-rag-part4.md)
