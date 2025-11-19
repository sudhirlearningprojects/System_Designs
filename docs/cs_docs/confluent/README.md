# Confluent Certified Developer for Apache Kafka (CCDAK) - Study Guide

Complete study materials for the Confluent Kafka certification exam.

## 📚 Study Modules

### [01. Kafka Fundamentals](01_Kafka_Fundamentals.md)
- Core concepts (Producer, Consumer, Broker, Topic, Partition)
- Message structure and offsets
- Replication and ISR
- Consumer groups and rebalancing
- Delivery semantics
- Topic configuration

### [02. Producer API](02_Producer_API.md)
- Producer configuration
- Acknowledgment modes (acks=0, 1, all)
- Idempotence and transactions
- Serialization (String, Avro, custom)
- Partitioning strategies
- Performance tuning (batching, compression)
- Error handling and retries

### [03. Consumer API](03_Consumer_API.md)
- Consumer configuration
- Consumer groups and coordination
- Subscribing to topics
- Offset management (auto-commit, manual commit)
- Rebalancing and listeners
- Partition assignment strategies
- Deserialization
- Performance tuning

### [04. Kafka Streams](04_Kafka_Streams.md)
- KStream vs KTable vs GlobalKTable
- Stateless operations (filter, map, flatMap)
- Stateful operations (aggregate, count, reduce)
- Joins (stream-stream, stream-table, table-table)
- Windowing (tumbling, hopping, sliding, session)
- State stores and interactive queries
- Processor API
- Testing with TopologyTestDriver

### [05. Schema Registry](05_Schema_Registry.md)
- Avro schema basics
- Schema Registry API
- Compatibility modes (BACKWARD, FORWARD, FULL)
- Schema evolution
- Producer/Consumer with Avro
- Kafka Streams with Schema Registry
- Subject naming strategies

### [06. Kafka Connect](06_Kafka_Connect.md)
- Standalone vs Distributed mode
- REST API operations
- Common source connectors (JDBC, Debezium, File)
- Common sink connectors (JDBC, Elasticsearch, S3)
- Converters (JSON, Avro, String)
- Single Message Transforms (SMT)
- Error handling and Dead Letter Queue
- Custom connector development

## 🎯 Exam Details

**Exam Name**: Confluent Certified Developer for Apache Kafka (CCDAK)

**Format**:
- 60 multiple-choice questions
- 90 minutes duration
- Passing score: 70%
- Online proctored exam

**Topics Covered**:
1. Application Design (25%)
2. Development (30%)
3. Deployment and Testing (15%)
4. Monitoring and Troubleshooting (15%)
5. Kafka Streams (15%)

## 📖 Study Plan

### Week 1-2: Fundamentals
- [ ] Kafka architecture and core concepts
- [ ] Producer API basics
- [ ] Consumer API basics
- [ ] Practice: Write simple producer/consumer applications

### Week 3-4: Advanced APIs
- [ ] Producer idempotence and transactions
- [ ] Consumer offset management
- [ ] Rebalancing strategies
- [ ] Practice: Implement exactly-once semantics

### Week 5-6: Kafka Streams
- [ ] Stream processing concepts
- [ ] Stateless and stateful operations
- [ ] Joins and windowing
- [ ] Practice: Build stream processing applications

### Week 7: Schema Registry
- [ ] Avro schema design
- [ ] Compatibility modes
- [ ] Schema evolution
- [ ] Practice: Implement schema evolution scenarios

### Week 8: Kafka Connect
- [ ] Connector configuration
- [ ] Source and sink connectors
- [ ] Transforms and error handling
- [ ] Practice: Set up connectors for common use cases

### Week 9-10: Review and Practice
- [ ] Review all modules
- [ ] Practice exam questions
- [ ] Hands-on labs
- [ ] Troubleshooting scenarios

## 🛠️ Hands-On Practice

### Setup Local Environment

```bash
# Download Confluent Platform
wget https://packages.confluent.io/archive/7.5/confluent-7.5.0.tar.gz
tar -xzf confluent-7.5.0.tar.gz
cd confluent-7.5.0

# Start services
confluent local services start

# Verify
confluent local services status
```

### Practice Exercises

**Exercise 1: Producer with Idempotence**
```java
// Implement producer with exactly-once semantics
// Test retry behavior
// Verify no duplicates
```

**Exercise 2: Consumer with Manual Commit**
```java
// Implement at-least-once processing
// Handle rebalancing
// Commit offsets per partition
```

**Exercise 3: Kafka Streams Word Count**
```java
// Build word count application
// Use windowing
// Query state stores
```

**Exercise 4: Schema Evolution**
```
// Create initial schema
// Add field with default
// Test backward compatibility
// Remove field
```

**Exercise 5: Kafka Connect Pipeline**
```
// Set up JDBC source connector
// Transform data with SMT
// Write to Elasticsearch sink
```

## 📝 Key Concepts to Master

### Producer
- [ ] acks configuration and implications
- [ ] Idempotence requirements
- [ ] Transaction API usage
- [ ] Partitioning strategies
- [ ] Serialization options

### Consumer
- [ ] Consumer group coordination
- [ ] Offset management strategies
- [ ] Rebalancing protocols
- [ ] Partition assignment strategies
- [ ] Deserialization handling

### Kafka Streams
- [ ] KStream vs KTable semantics
- [ ] Join types and requirements
- [ ] Window types and use cases
- [ ] State store types
- [ ] Processing guarantees

### Schema Registry
- [ ] Compatibility modes
- [ ] Schema evolution rules
- [ ] Subject naming strategies
- [ ] Avro schema design

### Kafka Connect
- [ ] Standalone vs distributed mode
- [ ] Connector configuration
- [ ] Converter types
- [ ] SMT usage
- [ ] Error handling

## 🔍 Common Exam Topics

### Configuration
- Producer: acks, retries, idempotence, batch.size, linger.ms
- Consumer: auto.offset.reset, enable.auto.commit, max.poll.records
- Streams: processing.guarantee, num.stream.threads
- Connect: tasks.max, errors.tolerance

### Troubleshooting
- Consumer lag
- Rebalancing issues
- Serialization errors
- Schema compatibility errors
- Connector failures

### Best Practices
- Choosing appropriate acks setting
- Offset commit strategies
- Partition count and replication factor
- Schema evolution guidelines
- Error handling patterns

## 📚 Additional Resources

**Official Documentation**:
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Documentation](https://docs.confluent.io/)
- [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)

**Books**:
- "Kafka: The Definitive Guide" by Neha Narkhede
- "Kafka Streams in Action" by William Bejeck

**Online Courses**:
- Confluent Developer Skills for Building Apache Kafka
- Udemy: Apache Kafka Series

**Practice**:
- [Confluent Examples](https://github.com/confluentinc/examples)
- [Kafka Tutorials](https://kafka-tutorials.confluent.io/)

## ✅ Pre-Exam Checklist

- [ ] Understand all configuration parameters
- [ ] Can explain delivery semantics
- [ ] Know when to use each join type
- [ ] Understand compatibility modes
- [ ] Can troubleshoot common issues
- [ ] Practiced with TopologyTestDriver
- [ ] Familiar with REST APIs
- [ ] Understand rebalancing protocols
- [ ] Know SMT types and usage
- [ ] Can design schema evolution strategy

## 💡 Exam Tips

1. **Read questions carefully** - Look for keywords like "best", "most", "least"
2. **Eliminate wrong answers** - Narrow down to 2-3 options
3. **Time management** - 90 seconds per question average
4. **Flag uncertain questions** - Review at the end
5. **Understand scenarios** - Many questions are scenario-based
6. **Know defaults** - Default values are frequently tested
7. **Practice hands-on** - Theory + practice = success

## 🎓 After Certification

**Next Steps**:
- Confluent Certified Administrator for Apache Kafka (CCAAK)
- Confluent Certified Operator for Apache Kafka (CCOAK)
- Build real-world projects
- Contribute to open source
- Share knowledge with community

---

**Good luck with your certification! 🚀**

For questions or clarifications, refer to the official [Confluent Certification page](https://www.confluent.io/certification/).
