# Apache Flink - Complete Study Guide

Comprehensive study materials for Apache Flink stream processing framework.

## 📚 Study Modules

### [01. Flink Fundamentals](01_Flink_Fundamentals.md)
- Core concepts (streams, events, time semantics)
- Watermarks and late data handling
- Architecture (JobManager, TaskManager, Client)
- Execution model and parallelism
- DataStream API basics
- Windows (tumbling, sliding, session, global)
- State management overview
- Checkpointing and fault tolerance

### [02. DataStream API](02_DataStream_API.md)
- Transformations (map, flatMap, filter, keyBy)
- Rich functions and runtime context
- Process functions (ProcessFunction, KeyedProcessFunction, CoProcessFunction)
- Side outputs for multiple streams
- Joins (window join, interval join, coGroup, connect)
- Async I/O for external lookups
- Broadcast state for reference data
- Iteration for feedback loops

### [03. Table API & SQL](03_Table_API_SQL.md)
- Environment setup and table creation
- Table API operations (select, where, groupBy, join)
- SQL queries (SELECT, aggregations, joins, windows)
- Time attributes (event time, processing time)
- Window aggregations (tumbling, sliding, session)
- Over windows and Top-N queries
- User-defined functions (scalar, table, aggregate)
- Catalogs (in-memory, Hive, JDBC)
- Conversion between Table and DataStream

### [04. State & Checkpointing](04_State_Checkpointing.md)
- Keyed state (ValueState, ListState, MapState, ReducingState, AggregatingState)
- Operator state (ListState, UnionListState, BroadcastState)
- State TTL for automatic cleanup
- Checkpointing configuration and process
- State backends (HashMap, RocksDB)
- Savepoints for planned maintenance
- State schema evolution
- State Processor API
- Queryable state

### [05. Connectors & Deployment](05_Connectors_Deployment.md)
- Source connectors (Kafka, File, JDBC, custom)
- Sink connectors (Kafka, File, JDBC, Elasticsearch, custom)
- Deployment modes (Standalone, YARN, Kubernetes)
- Resource configuration (memory, parallelism)
- High availability setup
- Monitoring and metrics
- Metric reporters (Prometheus, InfluxDB)

## 🎯 Learning Path

### Week 1-2: Fundamentals
- [ ] Understand stream processing concepts
- [ ] Learn time semantics and watermarks
- [ ] Master basic DataStream API
- [ ] Practice: Word count with event time windows

### Week 3-4: Advanced DataStream
- [ ] Process functions and timers
- [ ] State management patterns
- [ ] Joins and side outputs
- [ ] Practice: Real-time fraud detection

### Week 5-6: Table API & SQL
- [ ] SQL queries on streams
- [ ] Window aggregations
- [ ] User-defined functions
- [ ] Practice: Real-time analytics dashboard

### Week 7-8: State & Fault Tolerance
- [ ] State backends and checkpointing
- [ ] Savepoints and recovery
- [ ] State evolution
- [ ] Practice: Stateful stream processing with recovery

### Week 9-10: Production Deployment
- [ ] Connector configuration
- [ ] Deployment on Kubernetes
- [ ] Monitoring and tuning
- [ ] Practice: Deploy production pipeline

## 🛠️ Setup Environment

### Local Development

```bash
# Download Flink
wget https://dlcdn.apache.org/flink/flink-1.18.0/flink-1.18.0-bin-scala_2.12.tgz
tar -xzf flink-1.18.0-bin-scala_2.12.tgz
cd flink-1.18.0

# Start cluster
./bin/start-cluster.sh

# Verify
http://localhost:8081

# Stop cluster
./bin/stop-cluster.sh
```

### Maven Dependencies

```xml
<properties>
    <flink.version>1.18.0</flink.version>
    <scala.binary.version>2.12</scala.binary.version>
</properties>

<dependencies>
    <!-- Flink Core -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-streaming-java</artifactId>
        <version>${flink.version}</version>
    </dependency>
    
    <!-- Flink Clients -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-clients</artifactId>
        <version>${flink.version}</version>
    </dependency>
    
    <!-- Kafka Connector -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-connector-kafka</artifactId>
        <version>3.0.1-1.18</version>
    </dependency>
    
    <!-- Table API -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-api-java-bridge</artifactId>
        <version>${flink.version}</version>
    </dependency>
    
    <!-- Table Runtime -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-runtime</artifactId>
        <version>${flink.version}</version>
    </dependency>
    
    <!-- RocksDB State Backend -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-statebackend-rocksdb</artifactId>
        <version>${flink.version}</version>
    </dependency>
</dependencies>
```

## 💡 Practice Projects

### Project 1: Real-Time Word Count
```java
// Count words from socket stream with 5-second tumbling windows
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

DataStream<String> text = env.socketTextStream("localhost", 9999);

DataStream<Tuple2<String, Integer>> counts = text
    .flatMap((String line, Collector<Tuple2<String, Integer>> out) -> {
        for (String word : line.split(" ")) {
            out.collect(Tuple2.of(word, 1));
        }
    })
    .returns(Types.TUPLE(Types.STRING, Types.INT))
    .keyBy(t -> t.f0)
    .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
    .sum(1);

counts.print();
env.execute("Word Count");
```

### Project 2: Fraud Detection
```java
// Detect suspicious transactions (>$1000 within 1 minute)
DataStream<Transaction> transactions = ...;

DataStream<Alert> alerts = transactions
    .keyBy(Transaction::getAccountId)
    .process(new KeyedProcessFunction<String, Transaction, Alert>() {
        private ValueState<Double> sumState;
        private ValueState<Long> timerState;
        
        @Override
        public void processElement(Transaction tx, Context ctx, Collector<Alert> out) {
            Double sum = sumState.value();
            sum = (sum == null) ? tx.getAmount() : sum + tx.getAmount();
            sumState.update(sum);
            
            if (timerState.value() == null) {
                long timer = ctx.timestamp() + 60000;
                ctx.timerService().registerEventTimeTimer(timer);
                timerState.update(timer);
            }
            
            if (sum > 1000) {
                out.collect(new Alert(tx.getAccountId(), sum));
                sumState.clear();
                ctx.timerService().deleteEventTimeTimer(timerState.value());
                timerState.clear();
            }
        }
        
        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<Alert> out) {
            sumState.clear();
            timerState.clear();
        }
    });
```

### Project 3: Real-Time Analytics
```java
// Calculate hourly sales by category using SQL
StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

tableEnv.executeSql(
    "CREATE TABLE sales (" +
    "  order_id STRING," +
    "  category STRING," +
    "  amount DECIMAL(10, 2)," +
    "  order_time TIMESTAMP(3)," +
    "  WATERMARK FOR order_time AS order_time - INTERVAL '5' SECOND" +
    ") WITH (" +
    "  'connector' = 'kafka'," +
    "  'topic' = 'sales'," +
    "  'properties.bootstrap.servers' = 'localhost:9092'," +
    "  'format' = 'json'" +
    ")"
);

Table result = tableEnv.sqlQuery(
    "SELECT " +
    "  category, " +
    "  TUMBLE_START(order_time, INTERVAL '1' HOUR) as window_start, " +
    "  SUM(amount) as total_sales, " +
    "  COUNT(*) as order_count " +
    "FROM sales " +
    "GROUP BY category, TUMBLE(order_time, INTERVAL '1' HOUR)"
);
```

## 📊 Key Concepts Comparison

### Flink vs Kafka Streams

| Feature | Flink | Kafka Streams |
|---------|-------|---------------|
| **Architecture** | Cluster-based | Library (embedded) |
| **State Backend** | Memory, RocksDB | RocksDB |
| **Deployment** | Standalone, YARN, K8s | Application process |
| **Scalability** | Horizontal (add nodes) | Horizontal (add instances) |
| **SQL Support** | Full SQL support | ksqlDB (separate) |
| **Batch Processing** | Native support | No |
| **Exactly-Once** | Yes | Yes |
| **Latency** | Sub-second | Sub-second |

### Flink vs Spark Streaming

| Feature | Flink | Spark Streaming |
|---------|-------|-----------------|
| **Processing Model** | True streaming | Micro-batching |
| **Latency** | Milliseconds | Seconds |
| **State Management** | Native, efficient | Limited |
| **Event Time** | First-class support | Limited |
| **Watermarks** | Built-in | Manual |
| **SQL** | Streaming SQL | Structured Streaming |
| **Batch** | Unified API | Separate API |

## 🔍 Common Use Cases

### 1. Real-Time Analytics
- Dashboards with live metrics
- Aggregations over time windows
- Top-N queries

### 2. Event-Driven Applications
- Fraud detection
- Anomaly detection
- Real-time recommendations

### 3. Data Pipelines
- ETL from Kafka to data warehouse
- Stream enrichment with reference data
- Data quality monitoring

### 4. Complex Event Processing
- Pattern detection (CEP)
- Correlation of events
- Stateful computations

## 📈 Performance Tuning

### Checklist

- [ ] Set appropriate parallelism (CPU cores × 2-4)
- [ ] Configure memory properly (heap vs managed)
- [ ] Use RocksDB for large state
- [ ] Enable incremental checkpoints
- [ ] Set checkpoint interval (10-60 seconds)
- [ ] Use unaligned checkpoints for low latency
- [ ] Configure watermark intervals
- [ ] Monitor backpressure
- [ ] Use async I/O for external calls
- [ ] Optimize serialization (Kryo, Avro)

### Memory Configuration

```yaml
# For 16GB TaskManager
taskmanager.memory.process.size: 16384m
taskmanager.memory.flink.size: 13824m
taskmanager.memory.task.heap.size: 4096m
taskmanager.memory.managed.size: 8192m
taskmanager.memory.network.min: 1024m
taskmanager.memory.network.max: 1536m
```

## 🎓 Certification & Resources

### Official Resources
- [Apache Flink Documentation](https://flink.apache.org/docs/stable/)
- [Flink Training](https://flink.apache.org/training.html)
- [Flink Forward Conference](https://www.flink-forward.org/)

### Books
- "Stream Processing with Apache Flink" by Fabian Hueske & Vasiliki Kalavri
- "Learning Apache Flink" by Tanmay Deshpande

### Online Courses
- Ververica Academy (free)
- Udemy: Apache Flink courses
- Coursera: Big Data courses with Flink

### Community
- [Flink Mailing Lists](https://flink.apache.org/community.html#mailing-lists)
- [Flink Slack](https://flink.apache.org/community.html#slack)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/apache-flink)

## ✅ Skills Checklist

### Beginner
- [ ] Understand stream processing concepts
- [ ] Write basic DataStream programs
- [ ] Use built-in transformations
- [ ] Configure sources and sinks
- [ ] Run jobs locally

### Intermediate
- [ ] Implement stateful processing
- [ ] Use process functions with timers
- [ ] Configure checkpointing
- [ ] Write SQL queries on streams
- [ ] Deploy on cluster

### Advanced
- [ ] Optimize performance and memory
- [ ] Implement custom connectors
- [ ] Handle state evolution
- [ ] Configure HA and recovery
- [ ] Monitor and troubleshoot production jobs

## 🚀 Next Steps

1. **Complete all modules** sequentially
2. **Build practice projects** hands-on
3. **Deploy on Kubernetes** for production experience
4. **Contribute to open source** Flink projects
5. **Join community** and attend Flink Forward

---

**Happy Learning! 🎉**

For questions or contributions, visit the [Apache Flink Community](https://flink.apache.org/community.html).
