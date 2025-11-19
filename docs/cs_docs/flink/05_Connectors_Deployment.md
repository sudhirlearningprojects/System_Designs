# Flink Connectors & Deployment - Complete Study Guide

## Source Connectors

### Kafka Source

```java
KafkaSource<String> source = KafkaSource.<String>builder()
    .setBootstrapServers("localhost:9092")
    .setTopics("input-topic")
    .setGroupId("flink-consumer")
    .setStartingOffsets(OffsetsInitializer.earliest())
    .setValueOnlyDeserializer(new SimpleStringSchema())
    .build();

DataStream<String> stream = env.fromSource(
    source,
    WatermarkStrategy.noWatermarks(),
    "Kafka Source"
);
```

**With Watermarks**:
```java
WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy
    .<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
    .withTimestampAssigner((event, timestamp) -> extractTimestamp(event));

DataStream<String> stream = env.fromSource(
    source,
    watermarkStrategy,
    "Kafka Source"
);
```

**Starting Offsets**:
```java
// Earliest
.setStartingOffsets(OffsetsInitializer.earliest())

// Latest
.setStartingOffsets(OffsetsInitializer.latest())

// Specific offsets
Map<TopicPartition, Long> offsets = new HashMap<>();
offsets.put(new TopicPartition("topic", 0), 100L);
.setStartingOffsets(OffsetsInitializer.offsets(offsets))

// Timestamp
.setStartingOffsets(OffsetsInitializer.timestamp(1234567890L))
```

### File Source

```java
FileSource<String> source = FileSource
    .forRecordStreamFormat(new TextLineInputFormat(), new Path("input/"))
    .build();

DataStream<String> stream = env.fromSource(
    source,
    WatermarkStrategy.noWatermarks(),
    "File Source"
);
```

**Continuous Monitoring**:
```java
FileSource<String> source = FileSource
    .forRecordStreamFormat(new TextLineInputFormat(), new Path("input/"))
    .monitorContinuously(Duration.ofSeconds(10))
    .build();
```

### JDBC Source

```java
JdbcConnectionOptions connectionOptions = new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
    .withUrl("jdbc:postgresql://localhost:5432/mydb")
    .withDriverName("org.postgresql.Driver")
    .withUsername("user")
    .withPassword("password")
    .build();

DataStream<Row> stream = env.createInput(
    JdbcInputFormat.buildJdbcInputFormat()
        .setDrivername("org.postgresql.Driver")
        .setDBUrl("jdbc:postgresql://localhost:5432/mydb")
        .setUsername("user")
        .setPassword("password")
        .setQuery("SELECT * FROM users")
        .setRowTypeInfo(new RowTypeInfo(Types.INT, Types.STRING))
        .finish()
);
```

### Custom Source

```java
public class CustomSource implements SourceFunction<Event> {
    private volatile boolean running = true;
    
    @Override
    public void run(SourceContext<Event> ctx) throws Exception {
        while (running) {
            Event event = fetchEvent();
            ctx.collect(event);
        }
    }
    
    @Override
    public void cancel() {
        running = false;
    }
}

DataStream<Event> stream = env.addSource(new CustomSource());
```

## Sink Connectors

### Kafka Sink

```java
KafkaSink<String> sink = KafkaSink.<String>builder()
    .setBootstrapServers("localhost:9092")
    .setRecordSerializer(KafkaRecordSerializationSchema.builder()
        .setTopic("output-topic")
        .setValueSerializationSchema(new SimpleStringSchema())
        .build()
    )
    .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
    .setTransactionalIdPrefix("flink-")
    .build();

stream.sinkTo(sink);
```

**Delivery Guarantees**:
```java
// At-least-once
.setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)

// Exactly-once (requires transactions)
.setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
.setTransactionalIdPrefix("flink-")

// None (fire and forget)
.setDeliveryGuarantee(DeliveryGuarantee.NONE)
```

### File Sink

```java
FileSink<String> sink = FileSink
    .forRowFormat(new Path("output/"), new SimpleStringEncoder<String>("UTF-8"))
    .withRollingPolicy(
        DefaultRollingPolicy.builder()
            .withRolloverInterval(Duration.ofMinutes(15))
            .withInactivityInterval(Duration.ofMinutes(5))
            .withMaxPartSize(MemorySize.ofMebiBytes(128))
            .build()
    )
    .build();

stream.sinkTo(sink);
```

**Bulk Format** (Parquet, ORC):
```java
FileSink<Event> sink = FileSink
    .forBulkFormat(
        new Path("output/"),
        ParquetAvroWriters.forReflectRecord(Event.class)
    )
    .build();
```

### JDBC Sink

```java
SinkFunction<Event> sink = JdbcSink.sink(
    "INSERT INTO events (id, name, value) VALUES (?, ?, ?)",
    (statement, event) -> {
        statement.setLong(1, event.getId());
        statement.setString(2, event.getName());
        statement.setDouble(3, event.getValue());
    },
    JdbcExecutionOptions.builder()
        .withBatchSize(1000)
        .withBatchIntervalMs(200)
        .withMaxRetries(5)
        .build(),
    new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
        .withUrl("jdbc:postgresql://localhost:5432/mydb")
        .withDriverName("org.postgresql.Driver")
        .withUsername("user")
        .withPassword("password")
        .build()
);

stream.addSink(sink);
```

### Elasticsearch Sink

```java
List<HttpHost> httpHosts = Arrays.asList(
    new HttpHost("localhost", 9200, "http")
);

ElasticsearchSink.Builder<Event> builder = new ElasticsearchSink.Builder<>(
    httpHosts,
    new ElasticsearchSinkFunction<Event>() {
        @Override
        public void process(Event event, RuntimeContext ctx, RequestIndexer indexer) {
            Map<String, Object> json = new HashMap<>();
            json.put("id", event.getId());
            json.put("name", event.getName());
            json.put("value", event.getValue());
            
            IndexRequest request = Requests.indexRequest()
                .index("events")
                .id(String.valueOf(event.getId()))
                .source(json);
            
            indexer.add(request);
        }
    }
);

builder.setBulkFlushMaxActions(1000);
builder.setBulkFlushInterval(5000);

stream.addSink(builder.build());
```

### Custom Sink

```java
public class CustomSink extends RichSinkFunction<Event> {
    private transient Connection connection;
    
    @Override
    public void open(Configuration parameters) throws Exception {
        connection = DriverManager.getConnection("jdbc:...");
    }
    
    @Override
    public void invoke(Event event, Context context) throws Exception {
        PreparedStatement stmt = connection.prepareStatement(
            "INSERT INTO events VALUES (?, ?, ?)"
        );
        stmt.setLong(1, event.getId());
        stmt.setString(2, event.getName());
        stmt.setDouble(3, event.getValue());
        stmt.executeUpdate();
    }
    
    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}

stream.addSink(new CustomSink());
```

## Deployment Modes

### Standalone Cluster

**Start Cluster**:
```bash
# Start JobManager
./bin/jobmanager.sh start

# Start TaskManager
./bin/taskmanager.sh start

# Submit job
./bin/flink run -c com.example.MyJob myapp.jar
```

**Configuration** (flink-conf.yaml):
```yaml
jobmanager.rpc.address: localhost
jobmanager.rpc.port: 6123
jobmanager.memory.process.size: 1600m
taskmanager.memory.process.size: 1728m
taskmanager.numberOfTaskSlots: 4
parallelism.default: 1
```

### YARN Deployment

**Session Mode**:
```bash
# Start YARN session
./bin/yarn-session.sh -n 2 -tm 2048 -s 4

# Submit job
./bin/flink run -c com.example.MyJob myapp.jar

# Stop session
echo "stop" | ./bin/yarn-session.sh -id application_123_456
```

**Per-Job Mode**:
```bash
./bin/flink run -m yarn-cluster \
    -yn 2 \
    -ytm 2048 \
    -ys 4 \
    -c com.example.MyJob \
    myapp.jar
```

**Application Mode**:
```bash
./bin/flink run-application -t yarn-application \
    -Dyarn.application.name=MyApp \
    -Djobmanager.memory.process.size=2048m \
    -Dtaskmanager.memory.process.size=4096m \
    -Dtaskmanager.numberOfTaskSlots=4 \
    -c com.example.MyJob \
    myapp.jar
```

### Kubernetes Deployment

**Session Mode**:
```bash
# Start session cluster
./bin/kubernetes-session.sh \
    -Dkubernetes.cluster-id=my-flink-cluster \
    -Dtaskmanager.memory.process.size=4096m \
    -Dkubernetes.taskmanager.cpu=2 \
    -Dtaskmanager.numberOfTaskSlots=4 \
    -Dresourcemanager.taskmanager-timeout=3600000

# Submit job
./bin/flink run -c com.example.MyJob myapp.jar
```

**Application Mode**:
```bash
./bin/flink run-application -t kubernetes-application \
    -Dkubernetes.cluster-id=my-app \
    -Dkubernetes.container.image=flink:1.17 \
    -Djobmanager.memory.process.size=2048m \
    -Dtaskmanager.memory.process.size=4096m \
    -Dtaskmanager.numberOfTaskSlots=4 \
    -c com.example.MyJob \
    local:///opt/flink/usrlib/myapp.jar
```

**Kubernetes Manifest**:
```yaml
apiVersion: flink.apache.org/v1beta1
kind: FlinkDeployment
metadata:
  name: my-flink-app
spec:
  image: flink:1.17
  flinkVersion: v1_17
  flinkConfiguration:
    taskmanager.numberOfTaskSlots: "4"
  serviceAccount: flink
  jobManager:
    resource:
      memory: "2048m"
      cpu: 1
  taskManager:
    resource:
      memory: "4096m"
      cpu: 2
  job:
    jarURI: local:///opt/flink/usrlib/myapp.jar
    entryClass: com.example.MyJob
    parallelism: 4
```

## Resource Configuration

### Memory Configuration

```yaml
# JobManager
jobmanager.memory.process.size: 1600m
jobmanager.memory.flink.size: 1280m
jobmanager.memory.jvm-overhead.min: 192m
jobmanager.memory.jvm-overhead.max: 320m

# TaskManager
taskmanager.memory.process.size: 4096m
taskmanager.memory.flink.size: 3456m
taskmanager.memory.framework.heap.size: 128m
taskmanager.memory.task.heap.size: 1024m
taskmanager.memory.managed.size: 2048m
taskmanager.memory.network.min: 256m
taskmanager.memory.network.max: 512m
taskmanager.memory.jvm-overhead.min: 192m
taskmanager.memory.jvm-overhead.max: 640m
```

### Parallelism Configuration

```java
// Global parallelism
env.setParallelism(4);

// Operator parallelism
stream.map(new MyMapper()).setParallelism(8);

// Max parallelism (for rescaling)
env.setMaxParallelism(128);
```

**Configuration**:
```yaml
parallelism.default: 4
taskmanager.numberOfTaskSlots: 4
```

## High Availability

### ZooKeeper HA

```yaml
high-availability: zookeeper
high-availability.zookeeper.quorum: localhost:2181
high-availability.zookeeper.path.root: /flink
high-availability.cluster-id: /my-cluster
high-availability.storageDir: hdfs:///flink/ha/
```

### Kubernetes HA

```yaml
high-availability: kubernetes
high-availability.storageDir: hdfs:///flink/ha/
kubernetes.cluster-id: my-flink-cluster
```

## Monitoring

### Metrics

**Built-in Metrics**:
```
# System
System.CPU.Usage
System.Memory.Used
System.Network.ReceiveRate

# Job
Job.numRestarts
Job.uptime
Job.lastCheckpointDuration

# Task
Task.numRecordsIn
Task.numRecordsOut
Task.numBytesIn
Task.numBytesOut

# Checkpoint
Checkpoint.duration
Checkpoint.size
Checkpoint.alignment
```

**Custom Metrics**:
```java
public class MyMapper extends RichMapFunction<String, String> {
    private transient Counter counter;
    private transient Meter meter;
    
    @Override
    public void open(Configuration parameters) {
        counter = getRuntimeContext()
            .getMetricGroup()
            .counter("myCounter");
        
        meter = getRuntimeContext()
            .getMetricGroup()
            .meter("myMeter", new MeterView(60));
    }
    
    @Override
    public String map(String value) {
        counter.inc();
        meter.markEvent();
        return value.toUpperCase();
    }
}
```

### Metric Reporters

**Prometheus**:
```yaml
metrics.reporter.prom.class: org.apache.flink.metrics.prometheus.PrometheusReporter
metrics.reporter.prom.port: 9249
```

**InfluxDB**:
```yaml
metrics.reporter.influx.class: org.apache.flink.metrics.influxdb.InfluxdbReporter
metrics.reporter.influx.host: localhost
metrics.reporter.influx.port: 8086
metrics.reporter.influx.db: flink
```

## Best Practices

1. **Use Application Mode** for production (isolation)
2. **Enable HA** with ZooKeeper or Kubernetes
3. **Configure appropriate memory** for state size
4. **Set max parallelism** for future rescaling
5. **Use managed memory** for RocksDB state
6. **Monitor checkpoint duration** and backpressure
7. **Use exactly-once** for Kafka sink when needed
8. **Configure resource limits** in Kubernetes

## Practice Questions

**Q1**: What is the difference between Session and Application mode?
**A**: Session mode shares cluster; Application mode has dedicated cluster per job

**Q2**: How do you enable exactly-once for Kafka sink?
**A**: Set DeliveryGuarantee.EXACTLY_ONCE and transactional ID prefix

**Q3**: What is the purpose of task slots?
**A**: Resource unit for parallel task execution

**Q4**: How do you configure high availability?
**A**: Set high-availability to zookeeper or kubernetes with storage directory

**Q5**: What is max parallelism used for?
**A**: Upper bound for rescaling (affects key distribution)

**Q6**: Can you change parallelism when restoring from savepoint?
**A**: Yes, if max parallelism allows it

**Q7**: What is the difference between managed and heap memory?
**A**: Managed memory for RocksDB/sorting; heap memory for objects

**Q8**: How do you monitor Flink jobs?
**A**: Web UI, REST API, metric reporters (Prometheus, InfluxDB)

**Q9**: What is the purpose of rolling policy in file sink?
**A**: Controls when to close current file and start new one

**Q10**: How do you deploy Flink on Kubernetes?
**A**: Use Flink Kubernetes Operator or native Kubernetes deployment
