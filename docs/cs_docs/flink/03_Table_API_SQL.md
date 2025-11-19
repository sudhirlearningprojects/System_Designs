# Flink Table API & SQL - Complete Study Guide

## Overview

**Table API**: Declarative API for stream and batch processing
**Flink SQL**: ANSI SQL support for stream and batch queries

**Benefits**:
- Unified batch and streaming
- Automatic optimization
- No manual state management
- Easy integration with catalogs

## Environment Setup

```java
// Create environments
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

// Or batch environment
ExecutionEnvironment batchEnv = ExecutionEnvironment.getExecutionEnvironment();
BatchTableEnvironment batchTableEnv = BatchTableEnvironment.create(batchEnv);
```

## Creating Tables

### From DataStream

```java
DataStream<Tuple2<String, Integer>> stream = env.fromElements(
    Tuple2.of("Alice", 25),
    Tuple2.of("Bob", 30)
);

// Convert to Table
Table table = tableEnv.fromDataStream(stream, $("name"), $("age"));

// With schema
Table table = tableEnv.fromDataStream(
    stream,
    Schema.newBuilder()
        .column("name", DataTypes.STRING())
        .column("age", DataTypes.INT())
        .build()
);
```

### From Collection

```java
Table table = tableEnv.fromValues(
    DataTypes.ROW(
        DataTypes.FIELD("name", DataTypes.STRING()),
        DataTypes.FIELD("age", DataTypes.INT())
    ),
    Row.of("Alice", 25),
    Row.of("Bob", 30)
);
```

### From Connector

```java
// Kafka source
tableEnv.executeSql(
    "CREATE TABLE orders (" +
    "  order_id STRING," +
    "  user_id STRING," +
    "  amount DECIMAL(10, 2)," +
    "  order_time TIMESTAMP(3)," +
    "  WATERMARK FOR order_time AS order_time - INTERVAL '5' SECOND" +
    ") WITH (" +
    "  'connector' = 'kafka'," +
    "  'topic' = 'orders'," +
    "  'properties.bootstrap.servers' = 'localhost:9092'," +
    "  'properties.group.id' = 'flink-consumer'," +
    "  'format' = 'json'," +
    "  'scan.startup.mode' = 'earliest-offset'" +
    ")"
);
```

### From File

```java
tableEnv.executeSql(
    "CREATE TABLE users (" +
    "  user_id STRING," +
    "  name STRING," +
    "  age INT" +
    ") WITH (" +
    "  'connector' = 'filesystem'," +
    "  'path' = 'file:///path/to/users.csv'," +
    "  'format' = 'csv'" +
    ")"
);
```

## Table API Operations

### Select

```java
Table result = table.select($("name"), $("age"));

// With expressions
Table result = table.select(
    $("name"),
    $("age").plus(1).as("next_age")
);
```

### Where (Filter)

```java
Table result = table.where($("age").isGreater(25));

// Multiple conditions
Table result = table.where(
    $("age").isGreater(25).and($("name").isNotEqual("Alice"))
);
```

### GroupBy & Aggregate

```java
Table result = table
    .groupBy($("name"))
    .select($("name"), $("age").sum().as("total_age"));

// Multiple aggregations
Table result = table
    .groupBy($("name"))
    .select(
        $("name"),
        $("age").count().as("count"),
        $("age").avg().as("avg_age"),
        $("age").min().as("min_age"),
        $("age").max().as("max_age")
    );
```

### Join

```java
Table orders = ...;
Table users = ...;

// Inner join
Table result = orders
    .join(users)
    .where($("orders.user_id").isEqual($("users.user_id")))
    .select($("orders.order_id"), $("users.name"), $("orders.amount"));

// Left join
Table result = orders
    .leftOuterJoin(users, $("orders.user_id").isEqual($("users.user_id")))
    .select($("orders.order_id"), $("users.name"), $("orders.amount"));
```

### Window Aggregations

**Tumbling Window**:
```java
Table result = table
    .window(Tumble.over(lit(10).minutes()).on($("order_time")).as("w"))
    .groupBy($("user_id"), $("w"))
    .select(
        $("user_id"),
        $("w").start().as("window_start"),
        $("w").end().as("window_end"),
        $("amount").sum().as("total_amount")
    );
```

**Sliding Window**:
```java
Table result = table
    .window(Slide.over(lit(10).minutes()).every(lit(5).minutes()).on($("order_time")).as("w"))
    .groupBy($("user_id"), $("w"))
    .select($("user_id"), $("amount").sum().as("total_amount"));
```

**Session Window**:
```java
Table result = table
    .window(Session.withGap(lit(10).minutes()).on($("order_time")).as("w"))
    .groupBy($("user_id"), $("w"))
    .select($("user_id"), $("amount").sum().as("total_amount"));
```

## SQL Queries

### Basic SELECT

```java
Table result = tableEnv.sqlQuery(
    "SELECT name, age FROM users WHERE age > 25"
);
```

### Aggregations

```java
Table result = tableEnv.sqlQuery(
    "SELECT " +
    "  user_id, " +
    "  COUNT(*) as order_count, " +
    "  SUM(amount) as total_amount, " +
    "  AVG(amount) as avg_amount " +
    "FROM orders " +
    "GROUP BY user_id"
);
```

### Joins

```java
Table result = tableEnv.sqlQuery(
    "SELECT " +
    "  o.order_id, " +
    "  u.name, " +
    "  o.amount " +
    "FROM orders o " +
    "JOIN users u ON o.user_id = u.user_id"
);
```

### Window Aggregations

**Tumbling Window**:
```java
Table result = tableEnv.sqlQuery(
    "SELECT " +
    "  user_id, " +
    "  TUMBLE_START(order_time, INTERVAL '10' MINUTE) as window_start, " +
    "  TUMBLE_END(order_time, INTERVAL '10' MINUTE) as window_end, " +
    "  SUM(amount) as total_amount " +
    "FROM orders " +
    "GROUP BY user_id, TUMBLE(order_time, INTERVAL '10' MINUTE)"
);
```

**Sliding Window**:
```java
Table result = tableEnv.sqlQuery(
    "SELECT " +
    "  user_id, " +
    "  HOP_START(order_time, INTERVAL '5' MINUTE, INTERVAL '10' MINUTE) as window_start, " +
    "  SUM(amount) as total_amount " +
    "FROM orders " +
    "GROUP BY user_id, HOP(order_time, INTERVAL '5' MINUTE, INTERVAL '10' MINUTE)"
);
```

**Session Window**:
```java
Table result = tableEnv.sqlQuery(
    "SELECT " +
    "  user_id, " +
    "  SESSION_START(order_time, INTERVAL '10' MINUTE) as window_start, " +
    "  SUM(amount) as total_amount " +
    "FROM orders " +
    "GROUP BY user_id, SESSION(order_time, INTERVAL '10' MINUTE)"
);
```

### Over Window (Unbounded)

```java
Table result = tableEnv.sqlQuery(
    "SELECT " +
    "  order_id, " +
    "  user_id, " +
    "  amount, " +
    "  SUM(amount) OVER (" +
    "    PARTITION BY user_id " +
    "    ORDER BY order_time " +
    "    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW" +
    "  ) as running_total " +
    "FROM orders"
);
```

### Top-N

```java
Table result = tableEnv.sqlQuery(
    "SELECT * FROM (" +
    "  SELECT " +
    "    user_id, " +
    "    order_id, " +
    "    amount, " +
    "    ROW_NUMBER() OVER (" +
    "      PARTITION BY user_id " +
    "      ORDER BY amount DESC" +
    "    ) as row_num " +
    "  FROM orders" +
    ") WHERE row_num <= 3"
);
```

### Deduplication

```java
Table result = tableEnv.sqlQuery(
    "SELECT user_id, order_id, amount, order_time FROM (" +
    "  SELECT *, " +
    "    ROW_NUMBER() OVER (" +
    "      PARTITION BY user_id " +
    "      ORDER BY order_time DESC" +
    "    ) as row_num " +
    "  FROM orders" +
    ") WHERE row_num = 1"
);
```

## Time Attributes

### Event Time

```java
// In DDL
tableEnv.executeSql(
    "CREATE TABLE orders (" +
    "  order_id STRING," +
    "  amount DECIMAL(10, 2)," +
    "  order_time TIMESTAMP(3)," +
    "  WATERMARK FOR order_time AS order_time - INTERVAL '5' SECOND" +
    ") WITH (...)"
);

// From DataStream
DataStream<Event> stream = ...;
Table table = tableEnv.fromDataStream(
    stream,
    Schema.newBuilder()
        .column("order_id", DataTypes.STRING())
        .column("amount", DataTypes.DECIMAL(10, 2))
        .columnByExpression("order_time", "CAST(event_time AS TIMESTAMP(3))")
        .watermark("order_time", "order_time - INTERVAL '5' SECOND")
        .build()
);
```

### Processing Time

```java
// In DDL
tableEnv.executeSql(
    "CREATE TABLE orders (" +
    "  order_id STRING," +
    "  amount DECIMAL(10, 2)," +
    "  proc_time AS PROCTIME()" +
    ") WITH (...)"
);

// From DataStream
Table table = tableEnv.fromDataStream(
    stream,
    Schema.newBuilder()
        .column("order_id", DataTypes.STRING())
        .columnByExpression("proc_time", "PROCTIME()")
        .build()
);
```

## User-Defined Functions

### Scalar Function

```java
public class AddFunction extends ScalarFunction {
    public Integer eval(Integer a, Integer b) {
        return a + b;
    }
}

// Register
tableEnv.createTemporarySystemFunction("add", AddFunction.class);

// Use in SQL
Table result = tableEnv.sqlQuery("SELECT add(age, 1) FROM users");
```

### Table Function

```java
@FunctionHint(output = @DataTypeHint("ROW<word STRING, length INT>"))
public class SplitFunction extends TableFunction<Row> {
    public void eval(String str) {
        for (String word : str.split(" ")) {
            collect(Row.of(word, word.length()));
        }
    }
}

// Register
tableEnv.createTemporarySystemFunction("split", SplitFunction.class);

// Use in SQL
Table result = tableEnv.sqlQuery(
    "SELECT word, length FROM users, LATERAL TABLE(split(name))"
);
```

### Aggregate Function

```java
public class AvgAccumulator {
    public long sum = 0;
    public int count = 0;
}

public class AvgFunction extends AggregateFunction<Double, AvgAccumulator> {
    @Override
    public AvgAccumulator createAccumulator() {
        return new AvgAccumulator();
    }
    
    public void accumulate(AvgAccumulator acc, Integer value) {
        acc.sum += value;
        acc.count++;
    }
    
    @Override
    public Double getValue(AvgAccumulator acc) {
        return acc.count == 0 ? null : (double) acc.sum / acc.count;
    }
}

// Register and use
tableEnv.createTemporarySystemFunction("my_avg", AvgFunction.class);
```

## Catalogs

### In-Memory Catalog

```java
// Default catalog
Catalog catalog = tableEnv.getCatalog("default_catalog").get();
```

### Hive Catalog

```java
String catalogName = "myhive";
String defaultDatabase = "default";
String hiveConfDir = "/path/to/hive/conf";

HiveCatalog hive = new HiveCatalog(catalogName, defaultDatabase, hiveConfDir);
tableEnv.registerCatalog(catalogName, hive);
tableEnv.useCatalog(catalogName);
```

### JDBC Catalog

```java
JdbcCatalog catalog = new JdbcCatalog(
    "my_catalog",
    "default_database",
    "username",
    "password",
    "jdbc:postgresql://localhost:5432/mydb"
);
tableEnv.registerCatalog("my_catalog", catalog);
```

## Conversion Between Table and DataStream

### Table to DataStream

```java
// Append mode (insert-only)
DataStream<Row> stream = tableEnv.toDataStream(table);

// Changelog mode (insert/update/delete)
DataStream<Row> stream = tableEnv.toChangelogStream(table);

// With type
DataStream<Tuple2<String, Integer>> stream = 
    tableEnv.toDataStream(table, Types.TUPLE(Types.STRING, Types.INT));
```

### DataStream to Table

```java
DataStream<Tuple2<String, Integer>> stream = ...;

Table table = tableEnv.fromDataStream(stream);

// With schema
Table table = tableEnv.fromDataStream(
    stream,
    Schema.newBuilder()
        .column("f0", DataTypes.STRING())
        .column("f1", DataTypes.INT())
        .build()
);
```

## Sinks

### Print Sink

```java
table.execute().print();
```

### Kafka Sink

```java
tableEnv.executeSql(
    "CREATE TABLE kafka_sink (" +
    "  order_id STRING," +
    "  amount DECIMAL(10, 2)" +
    ") WITH (" +
    "  'connector' = 'kafka'," +
    "  'topic' = 'output-topic'," +
    "  'properties.bootstrap.servers' = 'localhost:9092'," +
    "  'format' = 'json'" +
    ")"
);

table.executeInsert("kafka_sink");
```

### JDBC Sink

```java
tableEnv.executeSql(
    "CREATE TABLE jdbc_sink (" +
    "  user_id STRING," +
    "  total_amount DECIMAL(10, 2)," +
    "  PRIMARY KEY (user_id) NOT ENFORCED" +
    ") WITH (" +
    "  'connector' = 'jdbc'," +
    "  'url' = 'jdbc:postgresql://localhost:5432/mydb'," +
    "  'table-name' = 'user_totals'," +
    "  'username' = 'user'," +
    "  'password' = 'password'" +
    ")"
);
```

## Best Practices

1. **Use SQL for simple queries**, Table API for complex logic
2. **Define watermarks** in source table DDL
3. **Use catalogs** for metadata management
4. **Leverage UDFs** for custom logic
5. **Use changelog streams** for updates/deletes
6. **Set appropriate parallelism** in table config
7. **Use primary keys** for upsert sinks
8. **Monitor query plans** with EXPLAIN

## Practice Questions

**Q1**: What is the difference between Table API and SQL?
**A**: Table API is programmatic; SQL is declarative. Both compile to same execution plan

**Q2**: How do you define event time in Table API?
**A**: Use WATERMARK clause in CREATE TABLE or Schema.watermark() in fromDataStream

**Q3**: What is the difference between toDataStream() and toChangelogStream()?
**A**: toDataStream() for append-only; toChangelogStream() for updates/deletes

**Q4**: Can you use window aggregations without event time?
**A**: Yes, with processing time using PROCTIME()

**Q5**: What is a catalog in Flink?
**A**: Metadata repository for tables, views, functions, and databases

**Q6**: How do you implement Top-N in SQL?
**A**: Use ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...) with WHERE row_num <= N

**Q7**: What is the purpose of LATERAL TABLE?
**A**: Unnest table function results (similar to CROSS JOIN UNNEST)

**Q8**: Can you join streaming tables?
**A**: Yes, with time constraints (window join or interval join)

**Q9**: What is the difference between tumbling and hopping windows?
**A**: Tumbling windows don't overlap; hopping windows overlap based on slide

**Q10**: How do you handle late data in SQL?
**A**: Define watermark with allowed lateness in WATERMARK clause
