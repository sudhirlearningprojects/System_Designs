# Apache Flink Fundamentals - Complete Study Guide

## What is Apache Flink?

**Apache Flink**: Distributed stream processing framework for stateful computations over unbounded and bounded data streams

**Key Characteristics**:
- True stream processing (not micro-batching)
- Event time processing
- Exactly-once state consistency
- Low latency (milliseconds)
- High throughput (millions of events/sec)
- Unified batch and stream processing

## Core Concepts

### Streams

**Unbounded Streams** (continuous):
- No defined start or end
- Must be continuously processed
- Examples: sensor data, user clicks, transactions

**Bounded Streams** (batch):
- Fixed size dataset
- Can be processed completely
- Examples: historical data, files

### Events

**Structure**:
```java
Event {
    timestamp;      // When event occurred
    payload;        // Event data
    watermark;      // Progress indicator
}
```

### Time Semantics

**Event Time**: When event actually occurred
**Processing Time**: When event is processed by system
**Ingestion Time**: When event enters Flink

```java
// Event Time (recommended)
env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

// Processing Time
env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
```

### Watermarks

**Definition**: Timestamp indicating all events up to that time have arrived

**Purpose**: Handle out-of-order events

```
Events:     E1(t=1) → E3(t=3) → E2(t=2) → E4(t=4)
Watermark:          W(t=1)    W(t=2)    W(t=3)

Window [0-5): Closes when W(t=5) arrives
```

**Generating Watermarks**:
```java
// Periodic watermarks (every 200ms)
DataStream<Event> stream = source
    .assignTimestampsAndWatermarks(
        WatermarkStrategy.<Event>forBoundedOutOfOrderness(Duration.ofSeconds(5))
            .withTimestampAssigner((event, timestamp) -> event.getTimestamp())
    );

// Punctuated watermarks (per event)
WatermarkStrategy.<Event>forGenerator(ctx -> new PunctuatedWatermarkGenerator())
```

## Architecture

### Components

**JobManager** (Master):
- Coordinates distributed execution
- Schedules tasks
- Manages checkpoints
- Handles failures

**TaskManager** (Worker):
- Executes tasks
- Manages memory buffers
- Exchanges data between operators

**Client**:
- Submits jobs
- Retrieves results

```
Client → JobManager → TaskManager 1
                   → TaskManager 2
                   → TaskManager 3
```

### Execution Model

**Job Graph**: Logical plan (operators and streams)
**Execution Graph**: Physical plan (parallelized tasks)

```
Source → Map → KeyBy → Window → Sink
  ↓       ↓      ↓       ↓       ↓
Task1   Task2  Task3   Task4   Task5
(p=2)   (p=2)  (p=4)   (p=4)   (p=1)
```

### Parallelism

**Operator Parallelism**: Number of parallel instances

```java
// Global parallelism
env.setParallelism(4);

// Operator-level parallelism
stream.map(new MyMapper()).setParallelism(8);
```

**Task Slots**: Resource unit in TaskManager

```java
// 3 TaskManagers × 4 slots = 12 parallel tasks
taskmanager.numberOfTaskSlots: 4
```

## DataStream API Basics

### Creating Streams

**From Collection**:
```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

DataStream<String> stream = env.fromElements("a", "b", "c");

List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
DataStream<Integer> stream = env.fromCollection(data);
```

**From Socket**:
```java
DataStream<String> stream = env.socketTextStream("localhost", 9999);
```

**From Kafka**:
```java
Properties props = new Properties();
props.setProperty("bootstrap.servers", "localhost:9092");
props.setProperty("group.id", "flink-consumer");

FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>(
    "topic",
    new SimpleStringSchema(),
    props
);

DataStream<String> stream = env.addSource(consumer);
```

**From File**:
```java
DataStream<String> stream = env.readTextFile("file:///path/to/file.txt");
```

### Basic Transformations

**Map** (1-to-1):
```java
DataStream<Integer> doubled = stream.map(x -> x * 2);

// With MapFunction
stream.map(new MapFunction<String, Integer>() {
    @Override
    public Integer map(String value) {
        return value.length();
    }
});
```

**FlatMap** (1-to-N):
```java
DataStream<String> words = stream.flatMap(
    (String line, Collector<String> out) -> {
        for (String word : line.split(" ")) {
            out.collect(word);
        }
    }
).returns(Types.STRING);
```

**Filter**:
```java
DataStream<Integer> filtered = stream.filter(x -> x > 10);
```

**KeyBy** (partitioning):
```java
KeyedStream<Tuple2<String, Integer>, String> keyed = 
    stream.keyBy(value -> value.f0);

// Multiple keys
stream.keyBy(value -> Tuple2.of(value.f0, value.f1));
```

### Aggregations

**Sum/Min/Max**:
```java
DataStream<Tuple2<String, Integer>> input = ...;

// Sum by field position
input.keyBy(0).sum(1);

// Min/Max
input.keyBy(0).min(1);
input.keyBy(0).max(1);

// MinBy/MaxBy (returns entire record)
input.keyBy(0).minBy(1);
```

**Reduce**:
```java
KeyedStream<Tuple2<String, Integer>, String> keyed = ...;

DataStream<Tuple2<String, Integer>> reduced = keyed.reduce(
    (value1, value2) -> new Tuple2<>(value1.f0, value1.f1 + value2.f1)
);
```

**Aggregate**:
```java
keyed.aggregate(new AggregateFunction<Event, Accumulator, Result>() {
    @Override
    public Accumulator createAccumulator() {
        return new Accumulator();
    }
    
    @Override
    public Accumulator add(Event value, Accumulator accumulator) {
        accumulator.add(value);
        return accumulator;
    }
    
    @Override
    public Result getResult(Accumulator accumulator) {
        return accumulator.getResult();
    }
    
    @Override
    public Accumulator merge(Accumulator a, Accumulator b) {
        return a.merge(b);
    }
});
```

## Windows

### Window Types

**Tumbling Window** (fixed, non-overlapping):
```java
stream
    .keyBy(...)
    .window(TumblingEventTimeWindows.of(Time.minutes(5)))
    .sum(1);

// [0-5), [5-10), [10-15), ...
```

**Sliding Window** (fixed, overlapping):
```java
stream
    .keyBy(...)
    .window(SlidingEventTimeWindows.of(Time.minutes(10), Time.minutes(5)))
    .sum(1);

// [0-10), [5-15), [10-20), ...
```

**Session Window** (dynamic, gap-based):
```java
stream
    .keyBy(...)
    .window(EventTimeSessionWindows.withGap(Time.minutes(10)))
    .sum(1);

// Windows created based on inactivity gap
```

**Global Window** (custom trigger):
```java
stream
    .keyBy(...)
    .window(GlobalWindows.create())
    .trigger(CountTrigger.of(100))
    .sum(1);
```

### Window Functions

**Reduce**:
```java
windowedStream.reduce((v1, v2) -> v1 + v2);
```

**Aggregate**:
```java
windowedStream.aggregate(new MyAggregateFunction());
```

**Process** (full access):
```java
windowedStream.process(new ProcessWindowFunction<IN, OUT, KEY, W>() {
    @Override
    public void process(KEY key, Context context, 
                       Iterable<IN> elements, Collector<OUT> out) {
        // Access window metadata
        long windowStart = context.window().getStart();
        long windowEnd = context.window().getEnd();
        
        // Process all elements
        int count = 0;
        for (IN element : elements) {
            count++;
        }
        
        out.collect(new OUT(key, count, windowStart));
    }
});
```

**Incremental + Process** (efficient):
```java
windowedStream.aggregate(
    new MyAggregateFunction(),
    new ProcessWindowFunction<ACC, OUT, KEY, W>() {
        @Override
        public void process(KEY key, Context context, 
                           Iterable<ACC> elements, Collector<OUT> out) {
            ACC result = elements.iterator().next();
            out.collect(new OUT(key, result, context.window().getStart()));
        }
    }
);
```

## State Management

### Keyed State

**ValueState** (single value):
```java
public class MyFunction extends RichFlatMapFunction<Event, Result> {
    private transient ValueState<Integer> countState;
    
    @Override
    public void open(Configuration parameters) {
        ValueStateDescriptor<Integer> descriptor = 
            new ValueStateDescriptor<>("count", Integer.class);
        countState = getRuntimeContext().getState(descriptor);
    }
    
    @Override
    public void flatMap(Event event, Collector<Result> out) throws Exception {
        Integer count = countState.value();
        count = (count == null) ? 1 : count + 1;
        countState.update(count);
        
        if (count >= 10) {
            out.collect(new Result(event.key, count));
            countState.clear();
        }
    }
}
```

**ListState** (list of values):
```java
ListStateDescriptor<String> descriptor = 
    new ListStateDescriptor<>("list", String.class);
ListState<String> listState = getRuntimeContext().getListState(descriptor);

listState.add("value");
for (String value : listState.get()) {
    // Process
}
listState.clear();
```

**MapState** (key-value pairs):
```java
MapStateDescriptor<String, Integer> descriptor = 
    new MapStateDescriptor<>("map", String.class, Integer.class);
MapState<String, Integer> mapState = getRuntimeContext().getMapState(descriptor);

mapState.put("key", 100);
Integer value = mapState.get("key");
mapState.remove("key");
```

**ReducingState** (aggregated value):
```java
ReducingStateDescriptor<Integer> descriptor = 
    new ReducingStateDescriptor<>("sum", (a, b) -> a + b, Integer.class);
ReducingState<Integer> reducingState = getRuntimeContext().getReducingState(descriptor);

reducingState.add(10);
Integer sum = reducingState.get();
```

### Operator State

**ListState** (redistributed on rescale):
```java
public class MySource implements SourceFunction<Event>, CheckpointedFunction {
    private transient ListState<Long> offsetState;
    private Long offset = 0L;
    
    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        offsetState.clear();
        offsetState.add(offset);
    }
    
    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        ListStateDescriptor<Long> descriptor = 
            new ListStateDescriptor<>("offset", Long.class);
        offsetState = context.getOperatorStateStore().getListState(descriptor);
        
        if (context.isRestored()) {
            for (Long o : offsetState.get()) {
                offset = o;
            }
        }
    }
}
```

## Checkpointing & Fault Tolerance

### Enabling Checkpoints

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

// Enable checkpointing every 10 seconds
env.enableCheckpointing(10000);

// Checkpoint configuration
env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
env.getCheckpointConfig().setMinPauseBetweenCheckpoints(5000);
env.getCheckpointConfig().setCheckpointTimeout(60000);
env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
env.getCheckpointConfig().enableExternalizedCheckpoints(
    ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
);
```

### State Backends

**MemoryStateBackend** (development):
```java
env.setStateBackend(new MemoryStateBackend());
```

**FsStateBackend** (production):
```java
env.setStateBackend(new FsStateBackend("hdfs://namenode:port/flink/checkpoints"));
```

**RocksDBStateBackend** (large state):
```java
env.setStateBackend(new RocksDBStateBackend("hdfs://namenode:port/flink/checkpoints"));
```

## Execution

### Local Execution

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

// Build pipeline
DataStream<String> stream = env.socketTextStream("localhost", 9999);
stream.print();

// Execute
env.execute("My Flink Job");
```

### Cluster Execution

```bash
# Submit job
./bin/flink run -c com.example.MyJob myapp.jar

# With parallelism
./bin/flink run -p 4 -c com.example.MyJob myapp.jar

# List jobs
./bin/flink list

# Cancel job
./bin/flink cancel <job-id>

# Savepoint
./bin/flink savepoint <job-id> hdfs:///savepoints
./bin/flink run -s hdfs:///savepoints/savepoint-123 myapp.jar
```

## Best Practices

1. **Use Event Time** for correctness
2. **Set appropriate parallelism** based on data volume
3. **Enable checkpointing** for fault tolerance
4. **Use RocksDB** for large state
5. **Configure watermarks** properly for late data
6. **Monitor backpressure** and adjust resources
7. **Use keyed state** over operator state when possible
8. **Test with bounded streams** before production

## Practice Questions

**Q1**: What is the difference between event time and processing time?
**A**: Event time is when event occurred; processing time is when Flink processes it

**Q2**: What is a watermark?
**A**: Timestamp indicating all events up to that time have arrived

**Q3**: What are the three state backends?
**A**: MemoryStateBackend, FsStateBackend, RocksDBStateBackend

**Q4**: What is the difference between tumbling and sliding windows?
**A**: Tumbling windows don't overlap; sliding windows overlap based on slide interval

**Q5**: What does keyBy() do?
**A**: Partitions stream by key, routing records with same key to same parallel instance

**Q6**: What is the purpose of checkpointing?
**A**: Fault tolerance - periodic snapshots of state for recovery

**Q7**: Can you have multiple parallel instances of a keyed operator?
**A**: Yes, each instance handles subset of keys

**Q8**: What is the difference between reduce() and aggregate()?
**A**: reduce() requires same input/output type; aggregate() allows different types

**Q9**: What happens when a TaskManager fails?
**A**: JobManager restarts tasks on other TaskManagers from last checkpoint

**Q10**: What is the purpose of setParallelism()?
**A**: Controls number of parallel instances for operator or entire job
