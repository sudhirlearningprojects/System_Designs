# Kafka Producer API - Confluent Certification Study Guide

## Producer Configuration

### Essential Properties

```java
Properties props = new Properties();

// Required
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

// Important
props.put("acks", "all");                    // 0, 1, or all
props.put("retries", 3);                     // Retry count
props.put("batch.size", 16384);              // Bytes
props.put("linger.ms", 10);                  // Wait time for batching
props.put("buffer.memory", 33554432);        // 32 MB
props.put("compression.type", "snappy");     // none, gzip, snappy, lz4, zstd
props.put("max.in.flight.requests.per.connection", 5);
props.put("enable.idempotence", true);       // Exactly-once semantics
```

### Acknowledgment Modes

**acks=0** (Fire and forget)
- No acknowledgment from broker
- Fastest, least reliable
- Possible message loss

**acks=1** (Leader acknowledgment)
- Leader writes to local log
- Moderate speed and reliability
- Possible loss if leader fails before replication

**acks=all/-1** (All ISR acknowledgment)
- Leader + all in-sync replicas acknowledge
- Slowest, most reliable
- Use with min.insync.replicas=2

### Idempotence

**Purpose**: Prevent duplicate messages on retry

**Configuration**:
```java
props.put("enable.idempotence", true);

// Automatically sets:
// acks=all
// retries=Integer.MAX_VALUE
// max.in.flight.requests.per.connection=5
```

**How it works**:
- Producer assigns sequence number to each message
- Broker tracks sequence numbers per producer
- Duplicates detected and ignored

### Transactions

**Use Case**: Atomic writes across multiple partitions/topics

**Configuration**:
```java
props.put("transactional.id", "my-transactional-id");
props.put("enable.idempotence", true); // Required

KafkaProducer<String, String> producer = new KafkaProducer<>(props);

// Initialize transactions
producer.initTransactions();

try {
    producer.beginTransaction();
    
    producer.send(new ProducerRecord<>("topic1", "key", "value1"));
    producer.send(new ProducerRecord<>("topic2", "key", "value2"));
    
    producer.commitTransaction();
} catch (Exception e) {
    producer.abortTransaction();
}
```

## Sending Messages

### Fire and Forget

```java
ProducerRecord<String, String> record = 
    new ProducerRecord<>("topic", "key", "value");
producer.send(record);
```

### Synchronous Send

```java
try {
    RecordMetadata metadata = producer.send(record).get();
    System.out.println("Offset: " + metadata.offset());
    System.out.println("Partition: " + metadata.partition());
} catch (Exception e) {
    e.printStackTrace();
}
```

### Asynchronous Send with Callback

```java
producer.send(record, new Callback() {
    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            exception.printStackTrace();
        } else {
            System.out.println("Sent to partition " + metadata.partition() 
                             + " with offset " + metadata.offset());
        }
    }
});
```

## Serialization

### Built-in Serializers

```java
// String
StringSerializer

// Integer
IntegerSerializer

// Long
LongSerializer

// ByteArray
ByteArraySerializer

// Bytes
BytesSerializer
```

### Custom Serializer

```java
public class UserSerializer implements Serializer<User> {
    @Override
    public byte[] serialize(String topic, User user) {
        if (user == null) return null;
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            dos.writeUTF(user.getName());
            dos.writeInt(user.getAge());
            
            return baos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Error serializing User", e);
        }
    }
}
```

### Avro Serialization (Confluent)

```java
props.put("key.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
props.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
props.put("schema.registry.url", "http://localhost:8081");

// Send Avro record
GenericRecord avroRecord = new GenericData.Record(schema);
avroRecord.put("name", "John");
avroRecord.put("age", 30);

producer.send(new ProducerRecord<>("topic", avroRecord));
```

## Partitioning

### Default Partitioner

**With Key**:
```java
// murmur2(key) % num_partitions
producer.send(new ProducerRecord<>("topic", "key", "value"));
```

**Without Key**:
```java
// Sticky partitioning (batches to same partition)
producer.send(new ProducerRecord<>("topic", "value"));
```

### Custom Partitioner

```java
public class CustomPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();
        
        if (key == null) {
            return ThreadLocalRandom.current().nextInt(numPartitions);
        }
        
        // Custom logic: VIP customers to partition 0
        if (key.toString().startsWith("VIP")) {
            return 0;
        }
        
        return Math.abs(key.hashCode()) % numPartitions;
    }
    
    @Override
    public void configure(Map<String, ?> configs) {}
    
    @Override
    public void close() {}
}

// Configure
props.put("partitioner.class", "com.example.CustomPartitioner");
```

## Performance Tuning

### Batching

```java
// Batch size in bytes
props.put("batch.size", 16384);  // 16 KB

// Wait time before sending batch
props.put("linger.ms", 10);      // 10 ms

// Trade-off: Higher values = better throughput, higher latency
```

### Compression

```java
props.put("compression.type", "snappy");

// Options:
// - none: No compression (default)
// - gzip: High compression, CPU intensive
// - snappy: Balanced (recommended)
// - lz4: Fast compression
// - zstd: Best compression (Kafka 2.1+)
```

### Buffer Memory

```java
// Total memory for buffering
props.put("buffer.memory", 33554432);  // 32 MB

// If buffer full, send() blocks for max.block.ms
props.put("max.block.ms", 60000);      // 60 seconds
```

### In-Flight Requests

```java
// Max unacknowledged requests per connection
props.put("max.in.flight.requests.per.connection", 5);

// Set to 1 for strict ordering (with retries)
// Higher values = better throughput
```

## Error Handling

### Retriable Errors

```java
props.put("retries", 3);
props.put("retry.backoff.ms", 100);

// Retriable errors:
// - NotLeaderForPartitionException
// - NetworkException
// - TimeoutException
```

### Non-Retriable Errors

```java
// These throw exceptions immediately:
// - RecordTooLargeException
// - SerializationException
// - InvalidTopicException
```

### Error Handling Pattern

```java
producer.send(record, (metadata, exception) -> {
    if (exception != null) {
        if (exception instanceof RetriableException) {
            // Already retried, log and alert
            logger.error("Failed after retries", exception);
        } else {
            // Non-retriable, handle immediately
            logger.error("Non-retriable error", exception);
        }
    }
});
```

## Producer Lifecycle

### Initialization

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", StringSerializer.class.getName());
props.put("value.serializer", StringSerializer.class.getName());

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

### Sending Messages

```java
for (int i = 0; i < 100; i++) {
    ProducerRecord<String, String> record = 
        new ProducerRecord<>("topic", "key-" + i, "value-" + i);
    producer.send(record);
}
```

### Flushing

```java
// Force send all buffered records
producer.flush();
```

### Closing

```java
// Close and flush
producer.close();

// Close with timeout
producer.close(Duration.ofSeconds(10));
```

## Metrics and Monitoring

### Key Metrics

```java
// Record send rate
record-send-rate

// Byte send rate
byte-rate

// Compression ratio
compression-rate-avg

// Record error rate
record-error-rate

// Request latency
request-latency-avg

// Batch size
batch-size-avg

// Records per request
records-per-request-avg
```

### Accessing Metrics

```java
Map<MetricName, ? extends Metric> metrics = producer.metrics();

for (Map.Entry<MetricName, ? extends Metric> entry : metrics.entrySet()) {
    System.out.println(entry.getKey().name() + ": " + entry.getValue().metricValue());
}
```

## Best Practices

### 1. Always Close Producer

```java
try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
    // Send messages
} // Auto-closes
```

### 2. Use Idempotence

```java
props.put("enable.idempotence", true);
```

### 3. Handle Callbacks

```java
producer.send(record, (metadata, exception) -> {
    if (exception != null) {
        // Log and handle error
    }
});
```

### 4. Configure Appropriate Timeouts

```java
props.put("request.timeout.ms", 30000);
props.put("delivery.timeout.ms", 120000);
props.put("max.block.ms", 60000);
```

### 5. Monitor Producer Metrics

```java
// Track record-send-rate, record-error-rate, request-latency-avg
```

## Exam Practice Questions

**Q1**: What happens when acks=all and min.insync.replicas=2, but only 1 replica is available?
**A**: Producer receives NotEnoughReplicasException

**Q2**: With enable.idempotence=true, what is the max value for max.in.flight.requests.per.connection?
**A**: 5 (automatically set)

**Q3**: Which compression type offers the best balance of speed and compression ratio?
**A**: snappy

**Q4**: What is the default partitioning strategy when key is null in recent Kafka versions?
**A**: Sticky partitioning (batches to same partition)

**Q5**: How do you ensure strict message ordering with retries enabled?
**A**: Set max.in.flight.requests.per.connection=1 and enable.idempotence=true

**Q6**: What is the purpose of linger.ms?
**A**: Wait time before sending batch to improve throughput by batching more records

**Q7**: Which acks setting provides the highest durability?
**A**: acks=all (or acks=-1)

**Q8**: What happens when buffer.memory is exhausted?
**A**: send() blocks for max.block.ms, then throws TimeoutException

**Q9**: Can you use transactions without enabling idempotence?
**A**: No, idempotence is required for transactions

**Q10**: What is the difference between flush() and close()?
**A**: flush() sends buffered records but keeps producer open; close() flushes and releases resources
