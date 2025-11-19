# Flink State & Checkpointing - Complete Study Guide

## State Types

### Keyed State

**Scoped to specific key, accessible only in keyed streams**

**ValueState<T>**:
```java
ValueStateDescriptor<Integer> descriptor = 
    new ValueStateDescriptor<>("count", Integer.class);
ValueState<Integer> state = getRuntimeContext().getState(descriptor);

// Operations
Integer value = state.value();
state.update(100);
state.clear();
```

**ListState<T>**:
```java
ListStateDescriptor<String> descriptor = 
    new ListStateDescriptor<>("list", String.class);
ListState<String> state = getRuntimeContext().getListState(descriptor);

// Operations
state.add("item");
state.addAll(Arrays.asList("a", "b", "c"));
Iterable<String> items = state.get();
state.update(Arrays.asList("new", "list"));
state.clear();
```

**MapState<UK, UV>**:
```java
MapStateDescriptor<String, Integer> descriptor = 
    new MapStateDescriptor<>("map", String.class, Integer.class);
MapState<String, Integer> state = getRuntimeContext().getMapState(descriptor);

// Operations
state.put("key", 100);
Integer value = state.get("key");
boolean contains = state.contains("key");
state.remove("key");

// Iterate
for (Map.Entry<String, Integer> entry : state.entries()) {
    String key = entry.getKey();
    Integer val = entry.getValue();
}

state.clear();
```

**ReducingState<T>**:
```java
ReducingStateDescriptor<Integer> descriptor = 
    new ReducingStateDescriptor<>("sum", (a, b) -> a + b, Integer.class);
ReducingState<Integer> state = getRuntimeContext().getReducingState(descriptor);

// Operations
state.add(10);
state.add(20);
Integer sum = state.get(); // 30
state.clear();
```

**AggregatingState<IN, OUT>**:
```java
AggregatingStateDescriptor<Integer, Tuple2<Integer, Integer>, Double> descriptor = 
    new AggregatingStateDescriptor<>(
        "average",
        new AverageAggregateFunction(),
        Types.TUPLE(Types.INT, Types.INT)
    );
AggregatingState<Integer, Double> state = 
    getRuntimeContext().getAggregatingState(descriptor);

state.add(10);
state.add(20);
Double avg = state.get(); // 15.0
```

### Operator State

**Scoped to operator instance, not keyed**

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
    
    @Override
    public void run(SourceContext<Event> ctx) throws Exception {
        while (running) {
            Event event = fetchEvent(offset);
            ctx.collect(event);
            offset++;
        }
    }
}
```

**UnionListState** (all state to all instances):
```java
ListStateDescriptor<Rule> descriptor = 
    new ListStateDescriptor<>("rules", Rule.class);

ListState<Rule> state = context.getOperatorStateStore()
    .getUnionListState(descriptor);
```

**BroadcastState** (read-only in non-broadcast side):
```java
MapStateDescriptor<String, Rule> descriptor = 
    new MapStateDescriptor<>("rules", String.class, Rule.class);

BroadcastStream<Rule> broadcast = ruleStream.broadcast(descriptor);

dataStream
    .connect(broadcast)
    .process(new BroadcastProcessFunction<Event, Rule, Result>() {
        @Override
        public void processElement(Event event, ReadOnlyContext ctx, 
                                  Collector<Result> out) throws Exception {
            ReadOnlyBroadcastState<String, Rule> rules = 
                ctx.getBroadcastState(descriptor);
            // Read-only access
        }
        
        @Override
        public void processBroadcastElement(Rule rule, Context ctx, 
                                           Collector<Result> out) throws Exception {
            BroadcastState<String, Rule> rules = 
                ctx.getBroadcastState(descriptor);
            rules.put(rule.getId(), rule);
        }
    });
```

## State TTL (Time-To-Live)

**Automatic state cleanup**:
```java
StateTtlConfig ttlConfig = StateTtlConfig
    .newBuilder(Time.hours(1))
    .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
    .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
    .build();

ValueStateDescriptor<String> descriptor = 
    new ValueStateDescriptor<>("state", String.class);
descriptor.enableTimeToLive(ttlConfig);

ValueState<String> state = getRuntimeContext().getState(descriptor);
```

**TTL Options**:
```java
// Update time
UpdateType.OnCreateAndWrite  // Update on create and write
UpdateType.OnReadAndWrite    // Update on read and write
UpdateType.Disabled          // Never update

// Visibility
StateVisibility.NeverReturnExpired      // Never return expired
StateVisibility.ReturnExpiredIfNotCleanedUp // Return if not cleaned

// Cleanup strategies
ttlConfig.cleanupFullSnapshot()         // Cleanup on full snapshot
ttlConfig.cleanupIncrementally(10, true) // Incremental cleanup
ttlConfig.cleanupInRocksdbCompactFilter(1000) // RocksDB compaction
```

## Checkpointing

### Configuration

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

// Enable checkpointing (interval in ms)
env.enableCheckpointing(10000);

CheckpointConfig config = env.getCheckpointConfig();

// Checkpoint mode
config.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
// config.setCheckpointingMode(CheckpointingMode.AT_LEAST_ONCE);

// Minimum pause between checkpoints
config.setMinPauseBetweenCheckpoints(5000);

// Checkpoint timeout
config.setCheckpointTimeout(60000);

// Max concurrent checkpoints
config.setMaxConcurrentCheckpoints(1);

// Externalized checkpoints
config.enableExternalizedCheckpoints(
    ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
);
// config.enableExternalizedCheckpoints(
//     ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION
// );

// Tolerate checkpoint failures
config.setTolerableCheckpointFailureNumber(3);

// Unaligned checkpoints (lower latency)
config.enableUnalignedCheckpoints();
config.setAlignmentTimeout(Duration.ofSeconds(10));
```

### Checkpoint Process

**Aligned Checkpoints** (default):
```
1. JobManager triggers checkpoint
2. Sources inject barriers into streams
3. Operators align barriers from all inputs
4. Operator snapshots state
5. Barrier forwarded downstream
6. Sinks acknowledge to JobManager
```

**Unaligned Checkpoints** (low latency):
```
1. Barriers overtake in-flight records
2. In-flight data included in checkpoint
3. Lower latency, larger checkpoint size
```

### Checkpoint Storage

**JobManagerCheckpointStorage** (small state):
```java
env.getCheckpointConfig().setCheckpointStorage(
    new JobManagerCheckpointStorage()
);
```

**FileSystemCheckpointStorage** (production):
```java
env.getCheckpointConfig().setCheckpointStorage(
    new FileSystemCheckpointStorage("hdfs://namenode:port/flink/checkpoints")
);
```

## State Backends

### HashMapStateBackend

**In-memory, heap-based**:
```java
env.setStateBackend(new HashMapStateBackend());

// Configure
Configuration config = new Configuration();
config.set(StateBackendOptions.STATE_BACKEND, "hashmap");
```

**Characteristics**:
- Fast access
- Limited by heap size
- Async snapshots to external storage
- Good for small state

### EmbeddedRocksDBStateBackend

**Disk-based, off-heap**:
```java
env.setStateBackend(new EmbeddedRocksDBStateBackend());

// With checkpoint storage
env.setStateBackend(new EmbeddedRocksDBStateBackend(
    "hdfs://namenode:port/flink/checkpoints"
));

// Configure
Configuration config = new Configuration();
config.set(StateBackendOptions.STATE_BACKEND, "rocksdb");
```

**Characteristics**:
- Scales to large state (TBs)
- Slower than heap (serialization overhead)
- Incremental checkpoints
- Good for large state

**RocksDB Options**:
```java
RocksDBStateBackend backend = new RocksDBStateBackend(checkpointPath);

// Predefined options
backend.setPredefinedOptions(PredefinedOptions.SPINNING_DISK_OPTIMIZED);
// backend.setPredefinedOptions(PredefinedOptions.FLASH_SSD_OPTIMIZED);

// Incremental checkpoints
backend.enableIncrementalCheckpointing(true);

// Custom options
backend.setOptions(new OptionsFactory() {
    @Override
    public DBOptions createDBOptions(DBOptions currentOptions, 
                                     Collection<AutoCloseable> handlesToClose) {
        return currentOptions.setMaxBackgroundJobs(4);
    }
    
    @Override
    public ColumnFamilyOptions createColumnOptions(
            ColumnFamilyOptions currentOptions,
            Collection<AutoCloseable> handlesToClose) {
        return currentOptions.setCompactionStyle(CompactionStyle.LEVEL);
    }
});
```

## Savepoints

**Manual checkpoints for planned maintenance**:

### Creating Savepoint

```bash
# Trigger savepoint
./bin/flink savepoint <job-id> [target-directory]

# Example
./bin/flink savepoint abc123 hdfs:///flink/savepoints

# Cancel with savepoint
./bin/flink cancel -s hdfs:///flink/savepoints abc123
```

### Restoring from Savepoint

```bash
# Start from savepoint
./bin/flink run -s hdfs:///flink/savepoints/savepoint-abc123 myapp.jar

# Allow non-restored state (for schema evolution)
./bin/flink run -s hdfs:///flink/savepoints/savepoint-abc123 \
    -n myapp.jar
```

### Savepoint Format

**Canonical Format** (portable):
```java
env.getCheckpointConfig().setCheckpointStorage(
    new FileSystemCheckpointStorage("hdfs:///checkpoints", true)
);
```

**Native Format** (faster, not portable):
```java
env.getCheckpointConfig().setCheckpointStorage(
    new FileSystemCheckpointStorage("hdfs:///checkpoints", false)
);
```

## State Schema Evolution

### Compatible Changes

**Adding field with default**:
```java
// Old state
public class OldState {
    public String name;
    public int age;
}

// New state (compatible)
public class NewState {
    public String name;
    public int age;
    public String email = ""; // Default value
}
```

**Removing field**:
```java
// Old state
public class OldState {
    public String name;
    public int age;
    public String email;
}

// New state (compatible)
public class NewState {
    public String name;
    public int age;
}
```

### State Processor API

**Read and modify savepoints offline**:
```java
ExecutionEnvironment batchEnv = ExecutionEnvironment.getExecutionEnvironment();

ExistingSavepoint savepoint = Savepoint.load(
    batchEnv,
    "hdfs:///savepoints/savepoint-abc123",
    new HashMapStateBackend()
);

// Read operator state
DataSet<Integer> counts = savepoint.readKeyedState(
    "my-operator-uid",
    new ReaderFunction()
);

// Transform state
DataSet<Integer> newCounts = counts.map(x -> x * 2);

// Write new savepoint
BootstrapTransformation<Integer> transformation = 
    OperatorTransformation
        .bootstrapWith(newCounts)
        .keyBy(x -> x)
        .transform(new StateBootstrapFunction());

Savepoint
    .create(new HashMapStateBackend(), 128)
    .withOperator("my-operator-uid", transformation)
    .write("hdfs:///savepoints/new-savepoint");

batchEnv.execute("Modify Savepoint");
```

## Queryable State

**Query state from external applications**:

### Enable Queryable State

```java
// In flink-conf.yaml
queryable-state.enable: true
queryable-state.proxy.ports: 9069
queryable-state.server.ports: 9067

// Make state queryable
ValueStateDescriptor<Long> descriptor = 
    new ValueStateDescriptor<>("count", Long.class);
descriptor.setQueryable("query-name");

ValueState<Long> state = getRuntimeContext().getState(descriptor);
```

### Query State

```java
QueryableStateClient client = new QueryableStateClient(
    "localhost",
    9069
);

CompletableFuture<ValueState<Long>> future = client.getKvState(
    JobID.fromHexString("abc123"),
    "query-name",
    "key",
    Types.STRING,
    new ValueStateDescriptor<>("count", Long.class)
);

ValueState<Long> state = future.get();
Long count = state.value();
```

## Best Practices

1. **Use RocksDB** for large state (>1GB per key)
2. **Enable incremental checkpoints** with RocksDB
3. **Set appropriate checkpoint interval** (10-60 seconds)
4. **Use state TTL** to prevent unbounded growth
5. **Assign operator UIDs** for savepoint compatibility
6. **Monitor checkpoint duration** and size
7. **Use unaligned checkpoints** for low latency
8. **Clean up old checkpoints** to save storage

## Monitoring

### Checkpoint Metrics

```
# Checkpoint duration
checkpoint_duration

# Checkpoint size
checkpoint_size

# Checkpoint alignment time
checkpoint_alignment_time

# Number of checkpoints
checkpoints_completed
checkpoints_failed

# State size
state_size
```

### State Metrics

```
# Keyed state size
keyed_state_size

# Operator state size
operator_state_size

# RocksDB metrics
rocksdb_block_cache_usage
rocksdb_mem_table_flush_pending
```

## Practice Questions

**Q1**: What is the difference between keyed state and operator state?
**A**: Keyed state is scoped to key; operator state is scoped to operator instance

**Q2**: When should you use RocksDB state backend?
**A**: For large state (>1GB per key) that doesn't fit in memory

**Q3**: What is the purpose of state TTL?
**A**: Automatic cleanup of expired state to prevent unbounded growth

**Q4**: What is the difference between checkpoint and savepoint?
**A**: Checkpoint is automatic for fault tolerance; savepoint is manual for planned maintenance

**Q5**: Can you change state schema between savepoints?
**A**: Yes, with compatible changes (add field with default, remove field)

**Q6**: What is the purpose of operator UID?
**A**: Identifies operator for savepoint compatibility during job updates

**Q7**: What are unaligned checkpoints?
**A**: Barriers overtake in-flight records for lower latency

**Q8**: How do you enable incremental checkpoints?
**A**: Use RocksDB backend with enableIncrementalCheckpointing(true)

**Q9**: What happens if checkpoint fails?
**A**: Job continues, next checkpoint attempted (configurable tolerance)

**Q10**: Can you query state from external application?
**A**: Yes, using Queryable State feature
