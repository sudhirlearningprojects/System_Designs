# Apache Cassandra Complete Guide for Developers (5-6 Years Experience)

A comprehensive, production-ready Apache Cassandra tutorial covering fundamentals to advanced topics with Spring Boot integration.

---

## 📚 Tutorial Structure

### [Part 1: Fundamentals & Data Modeling](./01_Cassandra_Fundamentals.md)
- Installation & Setup (Docker, Multi-Node Cluster)
- Keyspaces & Tables
- Data Modeling Principles (Query-First Design, Partition Keys, Denormalization)
- CRUD Operations (INSERT, SELECT, UPDATE, DELETE)
- Data Types (Basic, Collections, UDT)
- Practical Examples (Time-Series, Activity Logs, E-commerce)

**Key Topics**: CREATE KEYSPACE, CREATE TABLE, PRIMARY KEY, partition keys, clustering columns, denormalization

---

### [Part 2: Advanced Queries & Indexing](./02_Cassandra_Queries_Indexing.md)
- Secondary Indexes (Regular, SASI)
- Materialized Views
- Advanced Query Patterns (Range, IN, Pagination, Aggregations, Batches)
- Consistency Levels (ONE, QUORUM, ALL, LOCAL_QUORUM)
- Performance Optimization (Partition Size, TTL, Compaction, Compression)
- Real-World Examples (Sessions, Product Catalog, Metrics, Message Queue)

**Key Topics**: CREATE INDEX, MATERIALIZED VIEW, consistency levels, batches, TTL, compaction strategies

---

### [Part 3: Cluster Management & Operations](./03_Cassandra_Cluster_Operations.md)
- Cluster Architecture (Ring, Token Ranges, Virtual Nodes)
- Replication Strategies (SimpleStrategy, NetworkTopologyStrategy)
- Node Operations (Add, Remove, Replace, Repair, Cleanup)
- Monitoring & Maintenance (nodetool, JMX, Logging)
- Backup & Recovery (Snapshots, Incremental Backups, Restore)
- Production Best Practices (Hardware, Configuration, Security, Capacity Planning)

**Key Topics**: replication, nodetool, cluster operations, monitoring, backup strategies

---

### [Part 4: Spring Boot Integration](./04_Cassandra_Spring_Boot.md)
- Project Setup (Maven Dependencies, Configuration)
- Entity Modeling (@Table, @PrimaryKey, @PrimaryKeyColumn, @UserDefinedType)
- Repository Layer (CassandraRepository, Custom Queries)
- Service Layer (Business Logic, Denormalization, Time-Series)
- REST Controllers (CRUD APIs)
- Advanced Features (Batch Operations, Async Operations)

**Key Topics**: Spring Data Cassandra, CassandraRepository, CassandraTemplate, entity mapping

---

## 🎯 Who Is This For?

Developers with **5-6 years of experience** who:
- Have worked with databases (MySQL, PostgreSQL, MongoDB)
- Understand distributed systems concepts
- Need to handle high write throughput
- Are building time-series or IoT applications
- Want to learn Cassandra for production use

---

## 🚀 Quick Start

### 1. Install Cassandra with Docker
```bash
docker run -d \
  --name cassandra \
  -p 9042:9042 \
  -e CASSANDRA_CLUSTER_NAME=MyCluster \
  cassandra:5.0

docker exec -it cassandra cqlsh
```

### 2. Basic Operations
```cql
-- Create keyspace
CREATE KEYSPACE ecommerce
WITH replication = {
    'class': 'SimpleStrategy',
    'replication_factor': 3
};

USE ecommerce;

-- Create table
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email TEXT,
    name TEXT,
    age INT,
    created_at TIMESTAMP
);

-- Insert data
INSERT INTO users (user_id, email, name, age, created_at)
VALUES (uuid(), 'john@example.com', 'John Doe', 30, toTimestamp(now()));

-- Query data
SELECT * FROM users WHERE user_id = 123e4567-e89b-12d3-a456-426614174000;

-- Create index
CREATE INDEX ON users (email);
```

### 3. Spring Boot Setup
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-cassandra</artifactId>
</dependency>
```

```yaml
spring:
  data:
    cassandra:
      keyspace-name: ecommerce
      contact-points: localhost
      port: 9042
      local-datacenter: datacenter1
```

```java
@Table("users")
@Data
public class User {
    @PrimaryKey
    private UUID userId;
    private String email;
    private String name;
    private Integer age;
}

@Repository
public interface UserRepository extends CassandraRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
```

---

## 📖 Learning Path

### Week 1: Fundamentals
- Part 1: Data Modeling & CRUD
- Practice: Build user management system

### Week 2: Advanced Queries
- Part 2: Indexing, Materialized Views, Consistency
- Practice: Build time-series data collector

### Week 3: Operations
- Part 3: Cluster Management, Monitoring
- Practice: Set up 3-node cluster

### Week 4: Production
- Part 4: Spring Boot Integration
- Practice: Build IoT sensor platform

---

## 🛠️ Real-World Use Cases

### 1. Time-Series Data (IoT, Metrics)
- Sensor readings from millions of devices
- Application metrics and logs
- Financial tick data
- Network monitoring

### 2. User Activity Tracking
- User behavior analytics
- Audit logs
- Session management
- Event tracking

### 3. Messaging & Queues
- Message queues
- Notification systems
- Event streaming
- Real-time feeds

### 4. Product Catalogs
- E-commerce product data
- Content management
- Inventory tracking
- Search results

---

## 💡 Key Concepts

### When to Use Cassandra
✅ High write throughput (millions/sec)  
✅ Time-series data  
✅ Large datasets (petabytes)  
✅ Geographic distribution  
✅ Always-on applications  
✅ Linear scalability  
✅ No single point of failure  

### When NOT to Use Cassandra
❌ Complex JOINs (use PostgreSQL)  
❌ ACID transactions across partitions  
❌ Ad-hoc queries (use Elasticsearch)  
❌ Small datasets (<100GB)  
❌ Strong consistency requirements  

---

## 🔥 Best Practices

### 1. Data Modeling
- Design tables based on queries (query-first)
- Choose partition keys for even distribution
- Denormalize data (duplicate is OK)
- Keep partitions under 100MB
- Use time-bucketing for time-series

### 2. Queries
- Always include partition key in WHERE
- Avoid ALLOW FILTERING in production
- Use token-based pagination
- Batch writes to same partition only
- Use appropriate consistency level

### 3. Performance
- Use TTL for auto-expiring data
- Choose right compaction strategy
- Enable compression
- Monitor partition sizes
- Repair regularly

### 4. Operations
- Use RF=3 for production
- Monitor with nodetool and JMX
- Take regular snapshots
- Plan capacity (storage + throughput)
- Use NetworkTopologyStrategy for multi-DC

### 5. Production
- Use SSD storage
- Separate commit log disk
- Tune JVM heap (50% RAM, max 32GB)
- Enable authentication and encryption
- Monitor GC pause times

---

## 📊 Performance Benchmarks

### Write Performance
- **Single Node**: 10K writes/sec
- **3-Node Cluster**: 30K writes/sec
- **10-Node Cluster**: 100K writes/sec
- **Linear Scalability**: 2x nodes = 2x throughput

### Read Performance
- **Single Partition**: <5ms p99
- **Multi-Partition**: <50ms p99
- **With Secondary Index**: <100ms p99

### Storage
- **Compression Ratio**: 3:1 (LZ4)
- **Replication Overhead**: RF * data size
- **Recommended**: 1-2TB per node

---

## 🔗 Additional Resources

### Official Documentation
- [Apache Cassandra Documentation](https://cassandra.apache.org/doc/)
- [DataStax Academy](https://academy.datastax.com/)
- [Spring Data Cassandra](https://spring.io/projects/spring-data-cassandra)

### Tools
- **cqlsh**: CQL shell
- **nodetool**: Cluster management
- **DataStax DevCenter**: GUI client
- **Cassandra Reaper**: Repair automation

### Monitoring
- **Prometheus + Grafana**: Metrics monitoring
- **DataStax OpsCenter**: Enterprise monitoring
- **JMX**: Java Management Extensions

---

## 🎓 Certification

Consider DataStax certifications:
- **DataStax Certified Administrator**
- **DataStax Certified Developer**

---

## 📝 Practice Projects

### Beginner
1. User management system
2. Product catalog with categories
3. Session store with TTL

### Intermediate
4. IoT sensor data platform
5. User activity tracking system
6. Message queue implementation

### Advanced
7. Multi-datacenter deployment
8. Time-series analytics platform
9. Real-time recommendation engine

---

## 🤝 Contributing

Found an error or want to improve the tutorial? Contributions welcome!

---

## 📄 License

This tutorial is part of the System Designs Collection.

---

**Happy Learning! 🚀**

Start with [Part 1: Fundamentals & Data Modeling](./01_Cassandra_Fundamentals.md)
