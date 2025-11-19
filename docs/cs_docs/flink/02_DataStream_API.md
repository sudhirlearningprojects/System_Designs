# Flink DataStream API - Complete Study Guide

## Transformations

### Map Transformations

**map()** - 1:1 transformation:
```java
DataStream<Integer> input = env.fromElements(1, 2, 3, 4, 5);
DataStream<Integer> doubled = input.map(x -> x * 2);

// With MapFunction
DataStream<String> strings = input.map(new MapFunction<Integer, String>() {
    @Override
    public String map(Integer value) {
        return "Number: " + value;
    }
});
```

**flatMap()** - 1:N transformation:
```java
DataStream<String> sentences = env.fromElements("hello world", "flink streaming");

DataStream<String> words = sentences.flatMap(
    (String sentence, Collector<String> out) -> {
        for (String word : sentence.split(" ")) {
            out.collect(word);
        }
    }
).returns(Types.STRING);
```

### Filter & Selection

**filter()**:
```java
DataStream<Integer> filtered = input.filter(x -> x % 2 == 0);

// Complex filter
DataStream<Event> filtered = events.filter(new FilterFunction<Event>() {
    @Override
    public boolean filter(Event event) {
        return event.getValue() > 100 && event.getType().equals("ALERT");
    }
});
```

### Partitioning

**keyBy()** - Logical partitioning:
```java
// By field
DataStream<Tuple2<String, Integer>> input = ...;
KeyedStream<Tuple2<String, Integer>, String> keyed = input.keyBy(t -> t.f0);

// By POJO field
DataStream<Event> events = ...;
KeyedStream<Event, String> keyed = events.keyBy(Event::getUserId);

// Multiple keys
keyed = events.keyBy(e -> Tuple2.of(e.getUserId(), e.getType()));
```

**shuffle()** - Random partitioning:
```java
DataStream<String> shuffled = input.shuffle();
```

**rebalance()** - Round-robin:
```java
DataStream<String> rebalanced = input.rebalance();
```

**rescale()** - Local round-robin:
```java
DataStream<String> rescaled = input.rescale();
```

**broadcast()** - Send to all partitions:
```java
DataStream<String> broadcasted = input.broadcast();
```

**partitionCustom()** - Custom partitioning:
```java
DataStream<Tuple2<String, Integer>> partitioned = input.partitionCustom(
    (key, numPartitions) -> Math.abs(key.hashCode()) % numPartitions,
    t -> t.f0
);
```

## Rich Functions

### RichMapFunction

```java
public class MyRichMapper extends RichMapFunction<String, Tuple2<String, Integer>> {
    private transient ValueState<Integer> countState;
    
    @Override
    public void open(Configuration parameters) throws Exception {
        // Initialization - called once per parallel instance
        ValueStateDescriptor<Integer> descriptor = 
            new ValueStateDescriptor<>("count", Integer.class, 0);
        countState = getRuntimeContext().getState(descriptor);
        
        // Access runtime context
        int parallelism = getRuntimeContext().getNumberOfParallelSubtasks();
        int index = getRuntimeContext().getIndexOfThisSubtask();
    }
    
    @Override
    public Tuple2<String, Integer> map(String value) throws Exception {
        Integer count = countState.value();
        count++;
        countState.update(count);
        return Tuple2.of(value, count);
    }
    
    @Override
    public void close() throws Exception {
        // Cleanup - called once per parallel instance
    }
}
```

### RichFlatMapFunction

```java
public class Splitter extends RichFlatMapFunction<String, Tuple2<String, Integer>> {
    private transient MapState<String, Integer> wordCounts;
    
    @Override
    public void open(Configuration parameters) {
        MapStateDescriptor<String, Integer> descriptor = 
            new MapStateDescriptor<>("wordCounts", String.class, Integer.class);
        wordCounts = getRuntimeContext().getMapState(descriptor);
    }
    
    @Override
    public void flatMap(String sentence, Collector<Tuple2<String, Integer>> out) 
            throws Exception {
        for (String word : sentence.split(" ")) {
            Integer count = wordCounts.get(word);
            count = (count == null) ? 1 : count + 1;
            wordCounts.put(word, count);
            out.collect(Tuple2.of(word, count));
        }
    }
}
```

## Process Functions

### ProcessFunction

**Full control over events, state, and timers**:
```java
public class MyProcessFunction extends ProcessFunction<Event, Result> {
    private transient ValueState<Long> lastTimerState;
    
    @Override
    public void open(Configuration parameters) {
        ValueStateDescriptor<Long> descriptor = 
            new ValueStateDescriptor<>("lastTimer", Long.class);
        lastTimerState = getRuntimeContext().getState(descriptor);
    }
    
    @Override
    public void processElement(Event event, Context ctx, Collector<Result> out) 
            throws Exception {
        // Access event
        String key = event.getKey();
        long timestamp = ctx.timestamp();
        
        // Access watermark
        long watermark = ctx.timerService().currentWatermark();
        
        // Register timer
        long timerTime = timestamp + 60000; // 1 minute later
        ctx.timerService().registerEventTimeTimer(timerTime);
        lastTimerState.update(timerTime);
        
        // Output
        out.collect(new Result(key, event.getValue()));
    }
    
    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Result> out) 
            throws Exception {
        // Timer fired
        out.collect(new Result("TIMEOUT", timestamp));
    }
}
```

### KeyedProcessFunction

```java
public class AlertFunction extends KeyedProcessFunction<String, Event, Alert> {
    private transient ValueState<Integer> countState;
    private transient ValueState<Long> timerState;
    
    @Override
    public void open(Configuration parameters) {
        countState = getRuntimeContext().getState(
            new ValueStateDescriptor<>("count", Integer.class, 0));
        timerState = getRuntimeContext().getState(
            new ValueStateDescriptor<>("timer", Long.class));
    }
    
    @Override
    public void processElement(Event event, Context ctx, Collector<Alert> out) 
            throws Exception {
        // Update count
        Integer count = countState.value();
        count++;
        countState.update(count);
        
        // Set timer if not already set
        Long timer = timerState.value();
        if (timer == null) {
            long timerTime = ctx.timestamp() + 60000;
            ctx.timerService().registerEventTimeTimer(timerTime);
            timerState.update(timerTime);
        }
        
        // Check threshold
        if (count > 10) {
            out.collect(new Alert(ctx.getCurrentKey(), count));
            countState.clear();
            ctx.timerService().deleteEventTimeTimer(timer);
            timerState.clear();
        }
    }
    
    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Alert> out) 
            throws Exception {
        // Window expired, reset count
        countState.clear();
        timerState.clear();
    }
}
```

### CoProcessFunction

**Process two streams**:
```java
public class JoinFunction extends CoProcessFunction<Event1, Event2, Result> {
    private transient ValueState<Event1> event1State;
    private transient ValueState<Event2> event2State;
    
    @Override
    public void processElement1(Event1 event1, Context ctx, Collector<Result> out) 
            throws Exception {
        Event2 event2 = event2State.value();
        if (event2 != null) {
            out.collect(new Result(event1, event2));
            event1State.clear();
            event2State.clear();
        } else {
            event1State.update(event1);
            // Register cleanup timer
            ctx.timerService().registerEventTimeTimer(ctx.timestamp() + 60000);
        }
    }
    
    @Override
    public void processElement2(Event2 event2, Context ctx, Collector<Result> out) 
            throws Exception {
        Event1 event1 = event1State.value();
        if (event1 != null) {
            out.collect(new Result(event1, event2));
            event1State.clear();
            event2State.clear();
        } else {
            event2State.update(event2);
            ctx.timerService().registerEventTimeTimer(ctx.timestamp() + 60000);
        }
    }
    
    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Result> out) 
            throws Exception {
        // Cleanup expired state
        event1State.clear();
        event2State.clear();
    }
}
```

## Side Outputs

**Multiple output streams from single operator**:
```java
// Define output tags
final OutputTag<String> lateDataTag = new OutputTag<String>("late-data"){};
final OutputTag<String> errorTag = new OutputTag<String>("errors"){};

SingleOutputStreamOperator<String> mainStream = input.process(
    new ProcessFunction<String, String>() {
        @Override
        public void processElement(String value, Context ctx, Collector<String> out) {
            try {
                if (isLate(value)) {
                    ctx.output(lateDataTag, value);
                } else {
                    out.collect(value); // Main output
                }
            } catch (Exception e) {
                ctx.output(errorTag, value);
            }
        }
    }
);

// Access side outputs
DataStream<String> lateData = mainStream.getSideOutput(lateDataTag);
DataStream<String> errors = mainStream.getSideOutput(errorTag);
```

## Joins

### Window Join

```java
DataStream<Event1> stream1 = ...;
DataStream<Event2> stream2 = ...;

DataStream<Result> joined = stream1
    .join(stream2)
    .where(e1 -> e1.getKey())
    .equalTo(e2 -> e2.getKey())
    .window(TumblingEventTimeWindows.of(Time.minutes(5)))
    .apply((e1, e2) -> new Result(e1, e2));
```

### Interval Join

```java
KeyedStream<Event1, String> keyed1 = stream1.keyBy(Event1::getKey);
KeyedStream<Event2, String> keyed2 = stream2.keyBy(Event2::getKey);

DataStream<Result> joined = keyed1
    .intervalJoin(keyed2)
    .between(Time.minutes(-5), Time.minutes(5))
    .process(new ProcessJoinFunction<Event1, Event2, Result>() {
        @Override
        public void processElement(Event1 e1, Event2 e2, Context ctx, 
                                  Collector<Result> out) {
            out.collect(new Result(e1, e2));
        }
    });
```

### CoGroup

```java
stream1
    .coGroup(stream2)
    .where(e1 -> e1.getKey())
    .equalTo(e2 -> e2.getKey())
    .window(TumblingEventTimeWindows.of(Time.minutes(5)))
    .apply(new CoGroupFunction<Event1, Event2, Result>() {
        @Override
        public void coGroup(Iterable<Event1> first, Iterable<Event2> second, 
                           Collector<Result> out) {
            // Process all matching events in window
            for (Event1 e1 : first) {
                for (Event2 e2 : second) {
                    out.collect(new Result(e1, e2));
                }
            }
        }
    });
```

### Connect (Different Types)

```java
DataStream<String> control = ...;
DataStream<Event> data = ...;

DataStream<Result> connected = data
    .connect(control)
    .process(new CoProcessFunction<Event, String, Result>() {
        private String controlValue = "default";
        
        @Override
        public void processElement1(Event event, Context ctx, Collector<Result> out) {
            out.collect(new Result(event, controlValue));
        }
        
        @Override
        public void processElement2(String control, Context ctx, Collector<Result> out) {
            controlValue = control;
        }
    });
```

## Async I/O

**Non-blocking external calls**:
```java
public class AsyncDatabaseRequest extends RichAsyncFunction<String, Tuple2<String, String>> {
    private transient DatabaseClient client;
    
    @Override
    public void open(Configuration parameters) {
        client = new DatabaseClient();
    }
    
    @Override
    public void asyncInvoke(String key, ResultFuture<Tuple2<String, String>> resultFuture) {
        CompletableFuture<String> future = client.asyncQuery(key);
        
        future.whenComplete((result, error) -> {
            if (error != null) {
                resultFuture.completeExceptionally(error);
            } else {
                resultFuture.complete(Collections.singleton(Tuple2.of(key, result)));
            }
        });
    }
    
    @Override
    public void timeout(String key, ResultFuture<Tuple2<String, String>> resultFuture) {
        resultFuture.complete(Collections.singleton(Tuple2.of(key, "TIMEOUT")));
    }
}

// Apply async function
DataStream<Tuple2<String, String>> enriched = AsyncDataStream.unorderedWait(
    input,
    new AsyncDatabaseRequest(),
    5000,  // Timeout
    TimeUnit.MILLISECONDS,
    100    // Capacity
);
```

## Broadcast State

**Share state across all parallel instances**:
```java
// Define broadcast state descriptor
MapStateDescriptor<String, Rule> ruleStateDescriptor = 
    new MapStateDescriptor<>("rules", String.class, Rule.class);

// Broadcast stream
DataStream<Rule> ruleStream = ...;
BroadcastStream<Rule> ruleBroadcast = ruleStream.broadcast(ruleStateDescriptor);

// Connect with data stream
DataStream<Event> events = ...;
DataStream<Result> processed = events
    .connect(ruleBroadcast)
    .process(new BroadcastProcessFunction<Event, Rule, Result>() {
        @Override
        public void processElement(Event event, ReadOnlyContext ctx, 
                                  Collector<Result> out) throws Exception {
            // Read broadcast state
            ReadOnlyBroadcastState<String, Rule> rules = 
                ctx.getBroadcastState(ruleStateDescriptor);
            
            for (Map.Entry<String, Rule> entry : rules.immutableEntries()) {
                Rule rule = entry.getValue();
                if (rule.matches(event)) {
                    out.collect(new Result(event, rule));
                }
            }
        }
        
        @Override
        public void processBroadcastElement(Rule rule, Context ctx, 
                                           Collector<Result> out) throws Exception {
            // Update broadcast state
            BroadcastState<String, Rule> rules = 
                ctx.getBroadcastState(ruleStateDescriptor);
            rules.put(rule.getId(), rule);
        }
    });
```

## Iteration

**Feedback loops**:
```java
DataStream<Long> input = env.fromElements(1L, 2L, 3L);

IterativeStream<Long> iteration = input.iterate();

DataStream<Long> iterationBody = iteration
    .map(x -> x * 2)
    .filter(x -> x < 100);

DataStream<Long> feedback = iterationBody.filter(x -> x < 50);
DataStream<Long> output = iterationBody.filter(x -> x >= 50);

iteration.closeWith(feedback);

output.print();
```

## Best Practices

1. **Use KeyedProcessFunction** for complex event-time logic
2. **Leverage async I/O** for external lookups
3. **Use side outputs** instead of filter + union
4. **Prefer ProcessFunction** over RichFunction for timers
5. **Use broadcast state** for reference data
6. **Clean up state** with timers to prevent memory leaks
7. **Set appropriate parallelism** per operator
8. **Use interval joins** for time-based correlations

## Practice Questions

**Q1**: What is the difference between map() and flatMap()?
**A**: map() produces 1 output per input; flatMap() produces 0-N outputs per input

**Q2**: When should you use ProcessFunction over MapFunction?
**A**: When you need timers, side outputs, or access to watermarks

**Q3**: What is the purpose of keyBy()?
**A**: Partitions stream by key for stateful operations and parallel processing

**Q4**: Can you access state in a non-keyed stream?
**A**: No, keyed state requires keyBy(); use operator state for non-keyed streams

**Q5**: What is the difference between join() and intervalJoin()?
**A**: join() requires window; intervalJoin() joins based on time interval between events

**Q6**: What is broadcast state used for?
**A**: Sharing read-only state across all parallel instances (e.g., rules, configuration)

**Q7**: How do you handle late data?
**A**: Use side outputs with OutputTag to route late data separately

**Q8**: What is async I/O used for?
**A**: Non-blocking external calls (database, REST API) to improve throughput

**Q9**: Can you have multiple timers per key?
**A**: Yes, register multiple timers with different timestamps

**Q10**: What happens when you call clear() on state?
**A**: Removes state for current key, freeing memory
