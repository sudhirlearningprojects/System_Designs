# Kafka Streams - Confluent Certification Study Guide

## Overview

**Kafka Streams**: Client library for building stream processing applications

**Key Features**:
- Exactly-once processing semantics
- Stateful and stateless operations
- Windowing support
- Interactive queries
- Fault-tolerant state stores
- No external dependencies (just Kafka)

## Core Concepts

### Stream vs Table

**KStream** (Event Stream):
- Unbounded sequence of events
- Each record is independent
- Inserts only

```java
KStream<String, String> stream = builder.stream("input-topic");
// Record 1: key=A, value=1
// Record 2: key=A, value=2
// Both records exist independently
```

**KTable** (Changelog Stream):
- Represents current state
- Updates to same key replace previous value
- Upserts (insert/update)

```java
KTable<String, String> table = builder.table("input-topic");
// Record 1: key=A, value=1
// Record 2: key=A, value=2
// Only latest value (2) is kept for key A
```

**GlobalKTable**:
- Fully replicated on each instance
- All partitions available locally
- Used for reference data

```java
GlobalKTable<String, String> globalTable = builder.globalTable("reference-topic");
```

### Topology

**Definition**: DAG (Directed Acyclic Graph) of stream processors

```java
StreamsBuilder builder = new StreamsBuilder();

// Source
KStream<String, String> source = builder.stream("input-topic");

// Processor
KStream<String, String> processed = source
    .filter((key, value) -> value.length() > 5)
    .mapValues(value -> value.toUpperCase());

// Sink
processed.to("output-topic");

Topology topology = builder.build();
```

## Configuration

### Essential Properties

```java
Properties props = new Properties();

// Required
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-stream-app");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

// Important
props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "exactly_once_v2");
props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");
props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024L);
```

### Application ID

```java
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-app");

// Used for:
// - Consumer group ID
// - State directory prefix
// - Internal topic prefix
```

### Processing Guarantees

**at_least_once** (default):
```java
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "at_least_once");
```

**exactly_once_v2** (recommended):
```java
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "exactly_once_v2");
// Requires Kafka 2.5+
// Uses optimized transactions
```

## Stateless Operations

### Filter

```java
KStream<String, String> filtered = stream.filter(
    (key, value) -> value.length() > 5
);

KStream<String, String> filteredNot = stream.filterNot(
    (key, value) -> value.length() > 5
);
```

### Map

```java
// Transform key and value
KStream<String, Integer> mapped = stream.map(
    (key, value) -> KeyValue.pair(key.toUpperCase(), value.length())
);

// Transform only key
KStream<String, String> mappedKey = stream.selectKey(
    (key, value) -> value.substring(0, 1)
);

// Transform only value
KStream<String, String> mappedValue = stream.mapValues(
    value -> value.toUpperCase()
);
```

### FlatMap

```java
KStream<String, String> flatMapped = stream.flatMap(
    (key, value) -> {
        List<KeyValue<String, String>> result = new ArrayList<>();
        for (String word : value.split(" ")) {
            result.add(KeyValue.pair(key, word));
        }
        return result;
    }
);

KStream<String, String> flatMappedValues = stream.flatMapValues(
    value -> Arrays.asList(value.split(" "))
);
```

### Branch

```java
KStream<String, String>[] branches = stream.branch(
    (key, value) -> value.startsWith("A"),  // Branch 0
    (key, value) -> value.startsWith("B"),  // Branch 1
    (key, value) -> true                    // Branch 2 (default)
);

branches[0].to("topic-A");
branches[1].to("topic-B");
branches[2].to("topic-other");
```

### Merge

```java
KStream<String, String> merged = stream1.merge(stream2);
```

### Peek

```java
// Side effect (logging, debugging)
KStream<String, String> peeked = stream.peek(
    (key, value) -> System.out.println("Key: " + key + ", Value: " + value)
);
```

## Stateful Operations

### GroupBy

```java
// Group by key
KGroupedStream<String, String> grouped = stream.groupByKey();

// Group by custom key
KGroupedStream<String, String> groupedCustom = stream.groupBy(
    (key, value) -> value.substring(0, 1),
    Grouped.with(Serdes.String(), Serdes.String())
);
```

### Aggregate

```java
KTable<String, Long> aggregated = stream
    .groupByKey()
    .aggregate(
        () -> 0L,                           // Initializer
        (key, value, aggregate) -> aggregate + value.length(), // Aggregator
        Materialized.with(Serdes.String(), Serdes.Long())
    );
```

### Count

```java
KTable<String, Long> counted = stream
    .groupByKey()
    .count(Materialized.as("counts-store"));
```

### Reduce

```java
KTable<String, String> reduced = stream
    .groupByKey()
    .reduce(
        (value1, value2) -> value1 + "," + value2,
        Materialized.with(Serdes.String(), Serdes.String())
    );
```

## Joins

### Stream-Stream Join

**Inner Join**:
```java
KStream<String, String> joined = stream1.join(
    stream2,
    (value1, value2) -> value1 + "-" + value2,
    JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5)),
    StreamJoined.with(Serdes.String(), Serdes.String(), Serdes.String())
);
```

**Left Join**:
```java
KStream<String, String> leftJoined = stream1.leftJoin(
    stream2,
    (value1, value2) -> value1 + "-" + (value2 != null ? value2 : "null"),
    JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5))
);
```

**Outer Join**:
```java
KStream<String, String> outerJoined = stream1.outerJoin(
    stream2,
    (value1, value2) -> 
        (value1 != null ? value1 : "null") + "-" + 
        (value2 != null ? value2 : "null"),
    JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5))
);
```

### Stream-Table Join

```java
KStream<String, String> joined = stream.join(
    table,
    (streamValue, tableValue) -> streamValue + "-" + tableValue
);

KStream<String, String> leftJoined = stream.leftJoin(
    table,
    (streamValue, tableValue) -> 
        streamValue + "-" + (tableValue != null ? tableValue : "null")
);
```

### Stream-GlobalTable Join

```java
KStream<String, String> joined = stream.join(
    globalTable,
    (streamKey, streamValue) -> streamValue.substring(0, 1), // Key mapper
    (streamValue, tableValue) -> streamValue + "-" + tableValue
);
```

### Table-Table Join

```java
KTable<String, String> joined = table1.join(
    table2,
    (value1, value2) -> value1 + "-" + value2
);

KTable<String, String> leftJoined = table1.leftJoin(
    table2,
    (value1, value2) -> value1 + "-" + (value2 != null ? value2 : "null")
);

KTable<String, String> outerJoined = table1.outerJoin(
    table2,
    (value1, value2) -> 
        (value1 != null ? value1 : "null") + "-" + 
        (value2 != null ? value2 : "null")
);
```

## Windowing

### Tumbling Window

**Fixed-size, non-overlapping**:
```java
KTable<Windowed<String>, Long> windowed = stream
    .groupByKey()
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
    .count();

// Windows: [0-5), [5-10), [10-15), ...
```

### Hopping Window

**Fixed-size, overlapping**:
```java
KTable<Windowed<String>, Long> windowed = stream
    .groupByKey()
    .windowedBy(TimeWindows
        .ofSizeWithNoGrace(Duration.ofMinutes(5))
        .advanceBy(Duration.ofMinutes(1)))
    .count();

// Windows: [0-5), [1-6), [2-7), [3-8), ...
```

### Sliding Window

**Dynamic size based on record timestamps**:
```java
KTable<Windowed<String>, Long> windowed = stream
    .groupByKey()
    .windowedBy(SlidingWindows
        .ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5)))
    .count();
```

### Session Window

**Dynamic windows based on inactivity gap**:
```java
KTable<Windowed<String>, Long> windowed = stream
    .groupByKey()
    .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(Duration.ofMinutes(5)))
    .count();

// Windows created/merged based on activity
```

### Grace Period

```java
// Allow late records within grace period
TimeWindows.ofSizeAndGrace(
    Duration.ofMinutes(5),  // Window size
    Duration.ofMinutes(1)   // Grace period
);
```

## State Stores

### Types

**KeyValue Store**:
```java
StoreBuilder<KeyValueStore<String, Long>> storeBuilder = 
    Stores.keyValueStoreBuilder(
        Stores.persistentKeyValueStore("my-store"),
        Serdes.String(),
        Serdes.Long()
    );

builder.addStateStore(storeBuilder);
```

**Window Store**:
```java
StoreBuilder<WindowStore<String, Long>> windowStoreBuilder = 
    Stores.windowStoreBuilder(
        Stores.persistentWindowStore("my-window-store", 
            Duration.ofDays(1), 
            Duration.ofMinutes(5), 
            false),
        Serdes.String(),
        Serdes.Long()
    );
```

**Session Store**:
```java
StoreBuilder<SessionStore<String, Long>> sessionStoreBuilder = 
    Stores.sessionStoreBuilder(
        Stores.persistentSessionStore("my-session-store", 
            Duration.ofMinutes(30)),
        Serdes.String(),
        Serdes.Long()
    );
```

### Accessing State Stores

```java
// In Processor API
public class MyProcessor implements Processor<String, String, String, String> {
    private KeyValueStore<String, Long> store;
    
    @Override
    public void init(ProcessorContext<String, String> context) {
        store = context.getStateStore("my-store");
    }
    
    @Override
    public void process(Record<String, String> record) {
        Long count = store.get(record.key());
        count = (count == null) ? 1L : count + 1;
        store.put(record.key(), count);
    }
}
```

### Interactive Queries

```java
// Query state store from external application
KafkaStreams streams = new KafkaStreams(topology, props);
streams.start();

// Get store
ReadOnlyKeyValueStore<String, Long> store = 
    streams.store(
        StoreQueryParameters.fromNameAndType(
            "counts-store",
            QueryableStoreTypes.keyValueStore()
        )
    );

// Query
Long count = store.get("key");

// Range query
KeyValueIterator<String, Long> range = store.range("a", "z");
while (range.hasNext()) {
    KeyValue<String, Long> next = range.next();
    System.out.println(next.key + ": " + next.value);
}
range.close();
```

## Processor API

### Custom Processor

```java
public class MyProcessor implements Processor<String, String, String, String> {
    private ProcessorContext<String, String> context;
    
    @Override
    public void init(ProcessorContext<String, String> context) {
        this.context = context;
    }
    
    @Override
    public void process(Record<String, String> record) {
        // Custom processing logic
        String newValue = record.value().toUpperCase();
        context.forward(record.withValue(newValue));
    }
    
    @Override
    public void close() {
        // Cleanup
    }
}

// Add to topology
builder.stream("input-topic")
    .process(() -> new MyProcessor())
    .to("output-topic");
```

### Punctuator (Scheduled Processing)

```java
public class MyProcessor implements Processor<String, String, String, String> {
    private ProcessorContext<String, String> context;
    
    @Override
    public void init(ProcessorContext<String, String> context) {
        this.context = context;
        
        // Schedule punctuation every 10 seconds
        context.schedule(
            Duration.ofSeconds(10),
            PunctuationType.WALL_CLOCK_TIME,
            timestamp -> {
                // Periodic processing
                System.out.println("Punctuate at: " + timestamp);
            }
        );
    }
    
    @Override
    public void process(Record<String, String> record) {
        // Process record
    }
}
```

## Application Lifecycle

### Starting Application

```java
Properties props = new Properties();
// Configure properties

StreamsBuilder builder = new StreamsBuilder();
// Build topology

Topology topology = builder.build();
KafkaStreams streams = new KafkaStreams(topology, props);

// Set state listener
streams.setStateListener((newState, oldState) -> {
    System.out.println("State changed from " + oldState + " to " + newState);
});

// Set uncaught exception handler
streams.setUncaughtExceptionHandler((thread, throwable) -> {
    System.err.println("Uncaught exception in thread " + thread.getName());
    throwable.printStackTrace();
    return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
});

// Start
streams.start();
```

### Graceful Shutdown

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    streams.close(Duration.ofSeconds(10));
}));
```

### Application States

```
NOT_RUNNING → REBALANCING → RUNNING
                    ↓
                 ERROR
                    ↓
              PENDING_SHUTDOWN → NOT_RUNNING
```

## Testing

### TopologyTestDriver

```java
Properties props = new Properties();
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");

StreamsBuilder builder = new StreamsBuilder();
// Build topology

Topology topology = builder.build();
TopologyTestDriver testDriver = new TopologyTestDriver(topology, props);

// Create test input/output topics
TestInputTopic<String, String> inputTopic = testDriver.createInputTopic(
    "input-topic",
    new StringSerializer(),
    new StringSerializer()
);

TestOutputTopic<String, String> outputTopic = testDriver.createOutputTopic(
    "output-topic",
    new StringDeserializer(),
    new StringDeserializer()
);

// Send test data
inputTopic.pipeInput("key1", "value1");
inputTopic.pipeInput("key2", "value2");

// Read and assert
assertEquals("VALUE1", outputTopic.readValue());
assertEquals("VALUE2", outputTopic.readValue());

testDriver.close();
```

## Best Practices

### 1. Use Appropriate Serdes

```java
props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
```

### 2. Enable Exactly-Once

```java
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "exactly_once_v2");
```

### 3. Configure State Directory

```java
props.put(StreamsConfig.STATE_DIR_CONFIG, "/var/kafka-streams");
```

### 4. Set Appropriate Thread Count

```java
props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 
    Runtime.getRuntime().availableProcessors());
```

### 5. Handle Exceptions

```java
streams.setUncaughtExceptionHandler((thread, throwable) -> {
    // Log and decide: REPLACE_THREAD or SHUTDOWN_APPLICATION
    return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
});
```

## Exam Practice Questions

**Q1**: What is the difference between KStream and KTable?
**A**: KStream is event stream (all records kept); KTable is changelog (latest per key)

**Q2**: Can you join two KStreams without a window?
**A**: No, stream-stream joins require a time window

**Q3**: What is the purpose of GlobalKTable?
**A**: Fully replicated table available on all instances for reference data

**Q4**: What happens during rebalancing in Kafka Streams?
**A**: Partitions reassigned, state stores restored, processing paused temporarily

**Q5**: How do you query state stores from external application?
**A**: Use Interactive Queries with ReadOnlyKeyValueStore

**Q6**: What is the difference between tumbling and hopping windows?
**A**: Tumbling windows don't overlap; hopping windows overlap based on advance interval

**Q7**: What is grace period in windowing?
**A**: Time to accept late-arriving records after window end

**Q8**: Can you use both DSL and Processor API in same topology?
**A**: Yes, they can be mixed

**Q9**: What is the purpose of application.id?
**A**: Consumer group ID, state directory prefix, internal topic prefix

**Q10**: How do you achieve exactly-once processing?
**A**: Set processing.guarantee=exactly_once_v2 and use Kafka 2.5+
