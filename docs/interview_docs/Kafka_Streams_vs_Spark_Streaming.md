# Kafka Streams vs Spark Streaming - Complete Comparison

## Quick Comparison Table

| Feature | Kafka Streams | Spark Streaming |
|---------|---------------|-----------------|
| **Type** | Library (embedded in app) | Framework (separate cluster) |
| **Deployment** | Part of application | Standalone cluster |
| **Processing Model** | True stream (event-by-event) | Micro-batch (DStream) or Continuous |
| **Latency** | Sub-millisecond | Seconds (micro-batch), sub-second (structured) |
| **Throughput** | Medium | Very High |
| **State Management** | RocksDB (local + changelog) | In-memory + checkpointing |
| **Fault Tolerance** | Kafka topic replication | RDD lineage + checkpointing |
| **Scalability** | Horizontal (Kafka partitions) | Horizontal (Spark executors) |
| **Exactly-Once** | Yes (native) | Yes (with idempotent writes) |
| **Data Source** | Kafka only | Kafka, HDFS, S3, Kinesis, Socket |
| **Complexity** | Low | High |
| **Resource Usage** | Lightweight | Heavy (JVM overhead) |
| **Use Case** | Real-time event processing | Batch + Stream, Complex analytics |

---

## 1. Architecture

### Kafka Streams Architecture

```
┌─────────────────────────────────────────┐
│         Application Instance 1          │
│  ┌───────────────────────────────────┐  │
│  │      Kafka Streams Library        │  │
│  │  ┌─────────────┐  ┌────────────┐ │  │
│  │  │ Stream Task │  │ State Store│ │  │
│  │  │ (Partition) │  │  (RocksDB) │ │  │
│  │  └─────────────┘  └────────────┘ │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
              ↕
┌─────────────────────────────────────────┐
│           Kafka Cluster                 │
│  Topic: input-topic (3 partitions)      │
│  Topic: output-topic (3 partitions)     │
│  Topic: changelog-topic (state backup)  │
└─────────────────────────────────────────┘
```

**Key Points**:
- Library runs inside your application
- No separate cluster needed
- Each instance processes assigned partitions
- State stored locally (RocksDB) + backed up to Kafka

---

### Spark Streaming Architecture

```
┌─────────────────────────────────────────┐
│          Spark Cluster                  │
│  ┌───────────────────────────────────┐  │
│  │        Driver Program             │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │   Streaming Context         │  │  │
│  │  │   (Batch Scheduler)         │  │  │
│  │  └─────────────────────────────┘  │  │
│  └───────────────────────────────────┘  │
│                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────┐  │
│  │Executor 1│  │Executor 2│  │Exec 3│  │
│  │ RDD Task │  │ RDD Task │  │ Task │  │
│  └──────────┘  └──────────┘  └──────┘  │
└─────────────────────────────────────────┘
              ↕
┌─────────────────────────────────────────┐
│      Data Sources (Kafka, HDFS, S3)    │
└─────────────────────────────────────────┘
```

**Key Points**:
- Separate cluster (Driver + Executors)
- Requires cluster management (YARN, Mesos, K8s)
- Processes data in micro-batches
- State stored in memory + HDFS checkpoints

---

## 2. Processing Model

### Kafka Streams (True Streaming)

```java
// Event-by-event processing
StreamsBuilder builder = new StreamsBuilder();

KStream<String, String> stream = builder.stream("input-topic");

stream
    .filter((key, value) -> value.length() > 10)
    .mapValues(value -> value.toUpperCase())
    .to("output-topic");

// Each event processed immediately as it arrives
```

**Processing Flow**:
```
Event 1 → Process → Output
Event 2 → Process → Output
Event 3 → Process → Output
```

**Latency**: Sub-millisecond to milliseconds

---

### Spark Streaming (Micro-Batch)

```scala
// Micro-batch processing (DStream)
val ssc = new StreamingContext(sparkConf, Seconds(5))

val stream = KafkaUtils.createDirectStream(...)

stream
  .filter(_.length > 10)
  .map(_.toUpperCase)
  .foreachRDD { rdd =>
    rdd.saveAsTextFile("output")
  }

// Events batched every 5 seconds
```

**Processing Flow**:
```
Batch 1 (0-5s): [Event1, Event2, Event3] → Process → Output
Batch 2 (5-10s): [Event4, Event5, Event6] → Process → Output
```

**Latency**: Seconds (batch interval)

---

### Spark Structured Streaming (Continuous)

```scala
// Continuous processing (lower latency)
val df = spark
  .readStream
  .format("kafka")
  .option("kafka.bootstrap.servers", "localhost:9092")
  .option("subscribe", "input-topic")
  .load()

df
  .filter($"value".length > 10)
  .writeStream
  .format("kafka")
  .option("topic", "output-topic")
  .option("checkpointLocation", "/tmp/checkpoint")
  .start()

// Continuous mode: ~100ms latency
```

**Latency**: 100ms - 1 second

---

## 3. State Management

### Kafka Streams State

```java
StreamsBuilder builder = new StreamsBuilder();

// Stateful aggregation
KTable<String, Long> wordCounts = builder
    .stream("input-topic")
    .flatMapValues(value -> Arrays.asList(value.split(" ")))
    .groupBy((key, word) -> word)
    .count(Materialized.as("word-counts-store")); // RocksDB state store

// State stored locally + backed up to Kafka changelog topic
// Automatic state recovery on failure
```

**State Storage**:
- **Local**: RocksDB (embedded key-value store)
- **Backup**: Kafka changelog topic (automatic)
- **Recovery**: Restore from changelog on restart

**State Size**: Limited by local disk

---

### Spark Streaming State

```scala
// Stateful aggregation with updateStateByKey
val wordCounts = stream
  .flatMap(_.split(" "))
  .map(word => (word, 1))
  .updateStateByKey[Int] { (values, state) =>
    val currentCount = values.sum
    val previousCount = state.getOrElse(0)
    Some(currentCount + previousCount)
  }

// State stored in memory + HDFS checkpoints
```

**State Storage**:
- **Primary**: In-memory (executor memory)
- **Backup**: HDFS/S3 checkpoints (periodic)
- **Recovery**: Restore from checkpoint + replay

**State Size**: Limited by cluster memory

---

## 4. Windowing Operations

### Kafka Streams Windowing

```java
StreamsBuilder builder = new StreamsBuilder();

KStream<String, String> stream = builder.stream("input-topic");

// Tumbling window (5 minutes)
stream
    .groupByKey()
    .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
    .count()
    .toStream()
    .to("output-topic");

// Hopping window (5 min window, 1 min advance)
stream
    .groupByKey()
    .windowedBy(TimeWindows.of(Duration.ofMinutes(5))
                           .advanceBy(Duration.ofMinutes(1)))
    .count();

// Session window (30 min inactivity gap)
stream
    .groupByKey()
    .windowedBy(SessionWindows.with(Duration.ofMinutes(30)))
    .count();
```

---

### Spark Streaming Windowing

```scala
import org.apache.spark.sql.functions._

// Tumbling window (5 minutes)
df
  .groupBy(window($"timestamp", "5 minutes"))
  .count()

// Sliding window (5 min window, 1 min slide)
df
  .groupBy(window($"timestamp", "5 minutes", "1 minute"))
  .count()

// Session window (30 min gap)
df
  .groupBy(session_window($"timestamp", "30 minutes"))
  .count()
```

---

## 5. Exactly-Once Semantics

### Kafka Streams

```java
Properties props = new Properties();
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, 
          StreamsConfig.EXACTLY_ONCE_V2);

// Exactly-once enabled by default in newer versions
// Uses Kafka transactions internally
// No additional code needed
```

**How it works**:
- Kafka transactions for atomic writes
- Idempotent producers
- Transactional reads/writes
- Automatic offset management

---

### Spark Streaming

```scala
// Structured Streaming with idempotent writes
df
  .writeStream
  .format("kafka")
  .option("kafka.bootstrap.servers", "localhost:9092")
  .option("topic", "output-topic")
  .option("checkpointLocation", "/tmp/checkpoint")
  .outputMode("append")
  .start()

// Exactly-once requires:
// 1. Checkpointing enabled
// 2. Idempotent sink (Kafka with transactional writes)
// 3. Replayable source
```

**How it works**:
- Checkpointing for offset tracking
- Idempotent writes to sink
- Source replay on failure
- Manual configuration needed

---

## 6. Code Examples - Word Count

### Kafka Streams Word Count

```java
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import java.util.Properties;
import java.util.Arrays;

public class WordCountKafkaStreams {
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "word-count-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        
        StreamsBuilder builder = new StreamsBuilder();
        
        KStream<String, String> textLines = builder.stream("input-topic");
        
        KTable<String, Long> wordCounts = textLines
            .flatMapValues(line -> Arrays.asList(line.toLowerCase().split("\\W+")))
            .groupBy((key, word) -> word)
            .count();
        
        wordCounts.toStream().to("output-topic");
        
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
```

---

### Spark Streaming Word Count (DStream)

```scala
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010._

object WordCountSparkStreaming {
  
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("WordCount")
    val ssc = new StreamingContext(conf, Seconds(5))
    
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "word-count-group"
    )
    
    val topics = Array("input-topic")
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )
    
    val wordCounts = stream
      .map(record => record.value)
      .flatMap(_.toLowerCase.split("\\W+"))
      .map(word => (word, 1))
      .reduceByKey(_ + _)
    
    wordCounts.print()
    
    ssc.start()
    ssc.awaitTermination()
  }
}
```

---

### Spark Structured Streaming Word Count

```scala
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object WordCountStructuredStreaming {
  
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("WordCount")
      .getOrCreate()
    
    import spark.implicits._
    
    val lines = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "input-topic")
      .load()
      .selectExpr("CAST(value AS STRING)")
    
    val wordCounts = lines
      .flatMap(_.getString(0).toLowerCase.split("\\W+"))
      .groupBy("value")
      .count()
    
    val query = wordCounts
      .writeStream
      .outputMode("complete")
      .format("console")
      .start()
    
    query.awaitTermination()
  }
}
```

---

## 7. Join Operations

### Kafka Streams Join

```java
// Stream-Stream Join
KStream<String, String> leftStream = builder.stream("left-topic");
KStream<String, String> rightStream = builder.stream("right-topic");

KStream<String, String> joined = leftStream.join(
    rightStream,
    (leftValue, rightValue) -> leftValue + ":" + rightValue,
    JoinWindows.of(Duration.ofMinutes(5))
);

// Stream-Table Join
KTable<String, String> table = builder.table("user-table");
KStream<String, String> enriched = leftStream.join(
    table,
    (streamValue, tableValue) -> streamValue + ":" + tableValue
);
```

---

### Spark Streaming Join

```scala
// Stream-Stream Join
val stream1 = spark.readStream.format("kafka")...
val stream2 = spark.readStream.format("kafka")...

val joined = stream1.join(
  stream2,
  expr("stream1.key = stream2.key AND " +
       "stream1.timestamp >= stream2.timestamp AND " +
       "stream1.timestamp <= stream2.timestamp + interval 5 minutes")
)

// Stream-Static Join
val staticDF = spark.read.parquet("users.parquet")
val enriched = stream1.join(staticDF, "userId")
```

---

## 8. Performance Comparison

### Latency Benchmark

| Operation | Kafka Streams | Spark DStream | Spark Structured |
|-----------|---------------|---------------|------------------|
| **Simple Filter** | 1-5 ms | 1-5 seconds | 100-500 ms |
| **Aggregation** | 5-20 ms | 5-10 seconds | 500 ms - 2 sec |
| **Join** | 10-50 ms | 10-20 seconds | 1-5 seconds |
| **Windowing** | 10-100 ms | 5-15 seconds | 1-3 seconds |

---

### Throughput Benchmark

| System | Events/Second | Notes |
|--------|---------------|-------|
| **Kafka Streams** | 100K - 500K | Single instance |
| **Spark Streaming** | 1M - 10M+ | Full cluster |

---

## 9. Deployment

### Kafka Streams Deployment

```bash
# Simple JAR deployment
java -jar word-count-app.jar

# Multiple instances (auto-scaling)
java -jar word-count-app.jar  # Instance 1
java -jar word-count-app.jar  # Instance 2
java -jar word-count-app.jar  # Instance 3

# Kubernetes deployment
kubectl apply -f kafka-streams-deployment.yaml
```

**Advantages**:
- No cluster setup
- Easy horizontal scaling
- Lightweight (just a JAR)

---

### Spark Streaming Deployment

```bash
# Submit to Spark cluster
spark-submit \
  --class com.example.WordCount \
  --master yarn \
  --deploy-mode cluster \
  --executor-memory 4G \
  --num-executors 10 \
  word-count-app.jar

# Kubernetes deployment
spark-submit \
  --master k8s://https://kubernetes:443 \
  --deploy-mode cluster \
  --conf spark.executor.instances=5 \
  word-count-app.jar
```

**Requirements**:
- Spark cluster (standalone, YARN, Mesos, K8s)
- Cluster manager
- Resource allocation

---

## 10. When to Use What?

### Use Kafka Streams When:

✅ **Low latency required** (milliseconds)
✅ **Simple to medium complexity** processing
✅ **Kafka-only data source**
✅ **Lightweight deployment** preferred
✅ **Easy scaling** needed
✅ **Microservices architecture**
✅ **Event-driven applications**

**Examples**:
- Real-time fraud detection
- IoT sensor data processing
- User activity tracking
- Real-time recommendations
- Microservices event processing

---

### Use Spark Streaming When:

✅ **High throughput** required (millions/sec)
✅ **Complex analytics** needed
✅ **Multiple data sources** (Kafka, HDFS, S3, Kinesis)
✅ **Batch + Stream** processing
✅ **Machine learning** integration
✅ **Large-scale aggregations**
✅ **Existing Spark ecosystem**

**Examples**:
- Large-scale ETL pipelines
- Real-time analytics dashboards
- ML model training/inference
- Log aggregation and analysis
- Complex event processing
- Data lake ingestion

---

## 11. Resource Requirements

### Kafka Streams

```yaml
# Minimal resources
CPU: 1-2 cores per instance
Memory: 512MB - 2GB
Disk: 10GB (for RocksDB state)
Network: Standard

# Scaling: Add more instances
```

---

### Spark Streaming

```yaml
# Cluster resources
Driver:
  CPU: 2-4 cores
  Memory: 4-8GB

Executors (per executor):
  CPU: 4-8 cores
  Memory: 8-16GB
  Instances: 5-50+

Total: 50-500GB+ memory
```

---

## 12. Fault Tolerance

### Kafka Streams

```java
// Automatic fault tolerance
// 1. State backed up to Kafka changelog topic
// 2. On failure, state restored from changelog
// 3. Processing resumes from last committed offset

// No additional configuration needed
```

**Recovery Time**: Seconds to minutes (depends on state size)

---

### Spark Streaming

```scala
// Checkpointing required
streamingContext.checkpoint("/tmp/checkpoint")

// On failure:
// 1. Restore from checkpoint
// 2. Replay data from source
// 3. Recompute RDD lineage

// Recovery time: Minutes to hours
```

**Recovery Time**: Minutes to hours (depends on checkpoint interval)

---

## Summary

| Aspect | Kafka Streams | Spark Streaming |
|--------|---------------|-----------------|
| **Best For** | Low-latency, simple processing | High-throughput, complex analytics |
| **Complexity** | Low | High |
| **Setup** | Easy (just a library) | Complex (cluster required) |
| **Latency** | Milliseconds | Seconds |
| **Throughput** | Medium | Very High |
| **Scaling** | Easy (add instances) | Complex (cluster management) |
| **Cost** | Low | High |
| **Learning Curve** | Easy | Steep |
| **Ecosystem** | Kafka-centric | Spark ecosystem (ML, SQL, Graph) |

**Recommendation**: 
- Start with **Kafka Streams** for most real-time use cases
- Use **Spark Streaming** when you need complex analytics, ML, or already have Spark infrastructure
