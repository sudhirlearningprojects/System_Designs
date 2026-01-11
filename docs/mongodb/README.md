# MongoDB Complete Guide for Developers (5-6 Years Experience)

A comprehensive, production-ready MongoDB tutorial covering fundamentals to advanced topics with Spring Boot integration.

---

## 📚 Tutorial Structure

### [Part 1: Fundamentals & CRUD Operations](./01_MongoDB_Fundamentals.md)
- Installation & Setup (Docker, Homebrew)
- Database & Collections
- CRUD Operations (Insert, Read, Update, Delete)
- Query Operators (Comparison, Logical, Array, String)
- Update Operators (Field, Array)
- Practical E-commerce Examples

**Key Topics**: Document model, BSON, insertOne/Many, find, updateOne/Many, deleteOne/Many, $set, $inc, $push, $pull

---

### [Part 2: Indexing & Performance](./02_MongoDB_Indexing_Performance.md)
- Index Fundamentals
- Index Types (Single, Compound, Multikey, Text, Geospatial, Hashed, Wildcard)
- Query Optimization (ESR Rule, Covered Queries, Index Intersection)
- Explain Plans (COLLSCAN vs IXSCAN)
- Performance Best Practices
- Real-World Optimization Example (2000ms → 5ms)

**Key Topics**: createIndex, getIndexes, explain(), ESR rule, covered queries, performance tuning

---

### [Part 3: Aggregation Framework](./03_MongoDB_Aggregation.md)
- Aggregation Basics & Pipeline Stages
- $match, $project, $group, $sort, $limit, $skip
- $lookup (Joins), $unwind, $addFields, $bucket, $facet
- Aggregation Operators (Arithmetic, String, Date, Array, Conditional)
- Real-World Examples (Sales Analytics, Product Recommendations, User Segmentation, Inventory Report)
- Performance Tips (Filter Early, Use Indexes, Limit Early)

**Key Topics**: aggregate(), $group, $lookup, $unwind, $sum, $avg, $max, $min, allowDiskUse

---

### [Part 4: Transactions, Replication & Sharding](./04_MongoDB_Transactions_Replication.md)
- ACID Transactions (Single & Multi-Document)
- Replication (Replica Sets, Automatic Failover)
- Read Preferences & Write Concerns
- Sharding (Horizontal Scaling, Shard Key Selection)
- Data Modeling (Embedded vs Referenced, Design Patterns)
- Production Best Practices (Connection Pooling, Monitoring, Backup, Security)

**Key Topics**: startSession, commitTransaction, replica sets, sharding, shard keys, data modeling patterns

---

### [Part 5: Spring Boot Integration](./05_MongoDB_Spring_Boot.md)
- Project Setup (Maven Dependencies, Configuration)
- Entity Modeling (@Document, @Id, @Indexed, @TextIndexed)
- Repository Layer (MongoRepository, Custom Queries, Aggregation)
- Service Layer (Business Logic, Transactions, Caching)
- REST Controllers (CRUD APIs, Pagination, Search)
- Advanced Queries (MongoTemplate, Criteria, Bulk Operations)

**Key Topics**: MongoRepository, @Query, @Aggregation, MongoTemplate, @Transactional

---

## 🎯 Who Is This For?

Developers with **5-6 years of experience** who:
- Have worked with relational databases (MySQL, PostgreSQL)
- Understand REST APIs and microservices
- Want to learn MongoDB for production use
- Need practical, real-world examples
- Are building scalable applications

---

## 🚀 Quick Start

### 1. Install MongoDB with Docker
```bash
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=password \
  mongo:7.0

docker exec -it mongodb mongosh -u admin -p password
```

### 2. Basic Operations
```javascript
// Create database
use ecommerce

// Insert document
db.users.insertOne({
  name: "John Doe",
  email: "john@example.com",
  age: 30
})

// Query document
db.users.find({ email: "john@example.com" })

// Update document
db.users.updateOne(
  { email: "john@example.com" },
  { $set: { age: 31 } }
)

// Create index
db.users.createIndex({ email: 1 }, { unique: true })
```

### 3. Spring Boot Setup
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/ecommerce
```

```java
@Document(collection = "users")
@Data
public class User {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    private String name;
    private Integer age;
}

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
}
```

---

## 📖 Learning Path

### Week 1: Fundamentals
- Part 1: CRUD Operations
- Practice: Build a simple blog API

### Week 2: Performance
- Part 2: Indexing & Performance
- Practice: Optimize slow queries

### Week 3: Advanced Queries
- Part 3: Aggregation Framework
- Practice: Build analytics dashboard

### Week 4: Production
- Part 4: Transactions, Replication, Sharding
- Part 5: Spring Boot Integration
- Practice: Build e-commerce backend

---

## 🛠️ Real-World Use Cases

### 1. E-commerce Platform
- Product catalog with flexible schema
- User profiles with nested addresses
- Order management with transactions
- Real-time inventory updates

### 2. Social Media Application
- User posts with comments and likes
- News feed aggregation
- Follower/following relationships
- Real-time notifications

### 3. Analytics Dashboard
- Time-series data with bucket pattern
- Aggregation pipelines for metrics
- Real-time data processing
- Historical data analysis

### 4. Content Management System
- Flexible document structure
- Full-text search
- Media metadata storage
- Version control

---

## 💡 Key Concepts

### When to Use MongoDB
✅ Flexible schema requirements  
✅ Rapid development and iteration  
✅ Horizontal scaling needs  
✅ Real-time analytics  
✅ Content management systems  
✅ Catalog/product data  
✅ User profiles and preferences  

### When NOT to Use MongoDB
❌ Complex multi-table JOINs  
❌ Strong ACID requirements across collections  
❌ Heavy relational data  
❌ Financial transactions (use PostgreSQL)  

---

## 🔥 Best Practices

### 1. Schema Design
- Embed for one-to-few relationships
- Reference for one-to-many relationships
- Use design patterns (Subset, Bucket, Computed)
- Keep documents under 16MB

### 2. Indexing
- Create indexes for frequent queries
- Use compound indexes (ESR rule)
- Avoid too many indexes (max 5-10)
- Monitor index usage

### 3. Queries
- Filter early with $match
- Use projections to reduce data transfer
- Leverage indexes in aggregations
- Avoid large skip values

### 4. Performance
- Use connection pooling
- Enable profiling for slow queries
- Monitor with serverStatus()
- Use explain() to analyze queries

### 5. Production
- Use replica sets for high availability
- Implement proper backup strategy
- Enable authentication and TLS
- Monitor disk space and memory

---

## 📊 Performance Benchmarks

### Query Performance
- **Without Index**: 2000ms (1M documents scanned)
- **With Index**: 5ms (1 document scanned)
- **Improvement**: 400x faster

### Aggregation Performance
- **Filter Early**: 100ms
- **Filter Late**: 5000ms
- **Improvement**: 50x faster

### Write Performance
- **Single Insert**: 1ms
- **Bulk Insert (1000 docs)**: 50ms
- **Throughput**: 20,000 inserts/sec

---

## 🔗 Additional Resources

### Official Documentation
- [MongoDB Manual](https://docs.mongodb.com/manual/)
- [MongoDB University](https://university.mongodb.com/)
- [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb)

### Tools
- **MongoDB Compass**: GUI for MongoDB
- **Studio 3T**: Advanced MongoDB IDE
- **Robo 3T**: Lightweight MongoDB client
- **mongosh**: MongoDB Shell

### Monitoring
- **MongoDB Atlas**: Cloud-hosted MongoDB with monitoring
- **Prometheus + Grafana**: Self-hosted monitoring
- **MongoDB Cloud Manager**: Enterprise monitoring

---

## 🎓 Certification

Consider MongoDB certifications:
- **MongoDB Certified Developer Associate**
- **MongoDB Certified DBA Associate**

---

## 📝 Practice Projects

### Beginner
1. Blog API with posts and comments
2. Todo list with user authentication
3. Product catalog with search

### Intermediate
4. E-commerce platform with orders
5. Social media feed with aggregations
6. Analytics dashboard with time-series data

### Advanced
7. Multi-tenant SaaS application
8. Real-time chat application
9. Distributed system with sharding

---

## 🤝 Contributing

Found an error or want to improve the tutorial? Contributions welcome!

---

## 📄 License

This tutorial is part of the System Designs Collection.

---

**Happy Learning! 🚀**

Start with [Part 1: Fundamentals & CRUD Operations](./01_MongoDB_Fundamentals.md)
