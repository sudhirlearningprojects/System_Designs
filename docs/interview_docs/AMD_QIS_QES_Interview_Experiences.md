# AMD QIS/QES Interview Experiences

## 📚 Comprehensive Interview Preparation Guide

This repository contains **27 in-depth technical guides** covering Java, Spring Boot, Kafka, Databases, Distributed Systems, and Algorithms - all topics frequently asked in AMD QIS/QES interviews.

**Total Documentation**: ~550KB | **Average Guide Size**: ~20KB | **Last Updated**: January 29, 2024

---

## 📑 Table of Contents

1. [Core Java & Data Structures](#core-java--data-structures)
2. [Concurrency & Threading](#concurrency--threading)
3. [Spring Boot](#spring-boot)
4. [Kafka & Messaging](#kafka--messaging)
5. [Database & Indexing](#database--indexing)
6. [Distributed Systems](#distributed-systems)
7. [Caching & Performance](#caching--performance)
8. [Algorithms & Problem Solving](#algorithms--problem-solving)

---

## Core Java & Data Structures

### 1. [Custom HashMap Implementation](./Custom_HashMap_Implementation.md)
**Size**: 20KB | **Topics**: HashMap internals, collision handling, rehashing
- Build HashMap from scratch
- Understand internal array structure
- Implement put, get, remove operations
- Handle collisions with chaining
- Resize and rehashing logic

### 2. [HashMap to ConcurrentHashMap Design](./HashMap_ConcurrentHashMap_Design.md)
**Size**: 20KB | **Topics**: Thread-safe maps, CAS operations, segmentation
- HashMap vs ConcurrentHashMap internals
- Conversion strategies (4 methods)
- Performance benchmarks (3.4x faster!)
- Thread safety mechanisms
- Production use cases

### 3. [ArrayDeque vs Stack Deep Dive](./ArrayDeque_Stack_Deep_Dive.md)
**Size**: 18KB | **Topics**: LIFO/FIFO structures, circular arrays
- Stack internals (extends Vector)
- ArrayDeque circular array implementation
- Performance comparison (ArrayDeque 3.4x faster)
- Real-world examples (5 use cases)
- Best practices

### 4. [Java Records Deep Dive](./Java_Records_Deep_Dive.md)
**Size**: 24KB | **Topics**: Immutable data carriers, Java 16+ features
- What compiler generates automatically
- Compact constructor validation
- Pattern matching with records
- Sealed records (Java 17+)
- 7 real-world examples

### 5. [Java Optional Deep Dive](./Java_Optional_Deep_Dive.md)
**Size**: 17KB | **Topics**: Null safety, functional programming
- Optional API methods
- Best practices and anti-patterns
- Chaining operations
- Performance considerations
- Real-world use cases

### 6. [StringBuilder vs StringBuffer](./StringBuilder_vs_StringBuffer.md)
**Size**: 18KB | **Topics**: String manipulation, thread safety
- Internal implementation
- Performance benchmarks
- Thread safety comparison
- When to use which
- Memory optimization

### 7. [Thread-Safe Singleton Pattern](./Thread_Safe_Singleton_Pattern.md)
**Size**: 17KB | **Topics**: Design patterns, thread safety
- 6 implementation approaches
- Double-checked locking
- Enum singleton (best practice)
- Bill Pugh solution
- Performance comparison

---

## Concurrency & Threading

### 8. [Runnable vs Callable](./Runnable_vs_Callable.md)
**Size**: 16KB | **Topics**: Thread execution, return values
- Runnable vs Callable comparison
- Future and FutureTask
- Exception handling
- Real-world examples
- Best practices

### 9. [ExecutorService Framework Deep Dive](./ExecutorService_Framework_Deep_Dive.md)
**Size**: 21KB | **Topics**: Thread pools, task scheduling
- Thread pool types (Fixed, Cached, Scheduled)
- Custom ThreadPoolExecutor
- Rejection policies
- Performance tuning
- Production examples

### 10. [Java Virtual Threads Deep Dive](./Java_Virtual_Threads_Deep_Dive.md)
**Size**: 23KB | **Topics**: Project Loom, lightweight threads
- Virtual threads vs Platform threads
- Structured concurrency
- Performance benchmarks (10,000 threads!)
- Migration guide
- Real-world use cases

### 11. [Java Garbage Collection](./Java_Garbage_Collection.md)
**Size**: 17KB | **Topics**: Memory management, GC algorithms
- GC algorithms (Serial, Parallel, G1, ZGC)
- Heap structure
- GC tuning parameters
- Memory leak detection
- Best practices

---

## Spring Boot

### 12. [Spring Boot Startup Time Optimization](./Spring_Boot_Startup_Time_Optimization.md)
**Size**: 19KB | **Topics**: Performance optimization, lazy loading
- Startup time analysis
- Lazy initialization
- Component scanning optimization
- AOT compilation
- 10+ optimization techniques

### 13. [Spring Boot Exception Handling](./Spring_Boot_Exception_Handling.md)
**Size**: 28KB | **Topics**: @ControllerAdvice, ResponseEntity
- @ControllerAdvice vs ResponseEntity
- Global exception handling
- Custom error responses
- Validation errors
- 4 real-world examples

### 14. [Spring Boot Containerization Deep Dive](./Spring_Boot_Containerization_Deep_Dive.md)
**Size**: 17KB | **Topics**: Docker, Kubernetes, microservices
- Dockerfile strategies (3 approaches)
- Multi-stage builds
- Docker Compose for microservices
- Kubernetes deployment
- Production best practices

---

## Kafka & Messaging

### 15. [Kafka Architecture and Parallelism](./Kafka_Architecture_And_Parallelism.md)
**Size**: 21KB | **Topics**: Kafka internals, partitions, consumer groups
- Kafka architecture deep dive
- Partition and replication
- Consumer group coordination
- Parallelism strategies
- Performance tuning

### 16. [Kafka Consumer Lag Handling](./Kafka_Consumer_Lag_Handling.md)
**Size**: 21KB | **Topics**: Consumer lag, monitoring, optimization
- What is consumer lag
- Monitoring strategies
- 8 solutions to reduce lag
- Auto-scaling consumers
- Production examples

### 17. [CompletableFuture with Kafka Integration](./CompletableFuture_Kafka_Integration.md)
**Size**: 24KB | **Topics**: Async processing, reactive programming
- CompletableFuture internals
- Kafka producer async operations
- Kafka consumer parallel processing
- Error handling and retry
- 3 real-world examples

---

## Database & Indexing

### 18. [Database Partitioning and Sharding](./Database_Partitioning_And_Sharding.md)
**Size**: 22KB | **Topics**: Horizontal scaling, data distribution
- Partitioning strategies (Range, Hash, List)
- Sharding techniques
- Consistent hashing
- Rebalancing strategies
- Production examples

### 19. [Database Indexing Deep Dive](./Database_Indexing_Deep_Dive.md)
**Size**: 18KB | **Topics**: B+ Tree, indexing algorithms
- B+ Tree structure and operations
- 6 indexing algorithms
- Hash, Bitmap, R-Tree, Inverted Index
- Performance analysis (250,000x faster!)
- Real-world examples

### 20. [PostgreSQL vs MySQL vs MS SQL Server](./Database_Comparison_Deep_Dive.md)
**Size**: 21KB | **Topics**: Database comparison, SQL dialects
- Architecture comparison
- Feature comparison (MVCC, JSON, Full-text)
- Performance benchmarks
- SQL syntax differences
- Cost analysis

---

## Distributed Systems

### 21. [Load Balancers: ALB vs NLB](./Load_Balancers_ALB_vs_NLB.md)
**Size**: 22KB | **Topics**: AWS load balancers, traffic distribution
- ALB vs NLB comparison
- Layer 7 vs Layer 4
- Use cases and scenarios
- Performance benchmarks
- Configuration examples

### 22. [Rate Limiter with In-Memory Cache](./Rate_Limiter_In_Memory_Cache.md)
**Size**: 24KB | **Topics**: Rate limiting algorithms, API protection
- 4 rate limiting algorithms
- Token bucket, Sliding window
- Redis implementation
- Distributed rate limiting
- Production examples

### 23. [Inter-Service Communication Slow Response](./Inter_Service_Communication_Slow_Response.md)
**Size**: 21KB | **Topics**: Microservices, performance optimization
- Identify bottlenecks
- Circuit breaker pattern
- Timeout and retry strategies
- Async communication
- 8 optimization techniques

### 24. [Nested API Calls Async Handling](./Nested_API_Calls_Async_Handling.md)
**Size**: 19KB | **Topics**: Async programming, parallel execution
- Sequential vs Parallel execution
- CompletableFuture chaining
- Error handling
- Performance comparison
- Real-world examples

---

## Caching & Performance

### 25. [Redis Deep Dive with Spring Boot](./Redis_Deep_Dive_Spring_Boot.md)
**Size**: 24KB | **Topics**: Caching, Redis data structures
- Redis data structures (5 types)
- Spring Boot integration
- Cache strategies (Cache-Aside, Write-Through)
- Distributed caching
- Production examples

---

## Algorithms & Problem Solving

### 26. [Transaction Stream API Solutions](./Transaction_Stream_API_Solutions.md)
**Size**: 32KB | **Topics**: Java Stream API, functional programming
- Filter and transform operations
- Group by and aggregation
- 5 complete solutions
- Performance optimization
- Real-world examples

### 27. [Find Minimum Positive Integer](./Find_Minimum_Positive_Integer.md)
**Size**: 11KB | **Topics**: Array algorithms, optimization
- 3 solution approaches
- First missing positive (LeetCode 41)
- Time/space complexity analysis
- 10 edge cases
- Related problems

---

## 📊 Statistics

| Category | Documents | Total Size |
|----------|-----------|------------|
| Core Java & Data Structures | 7 | ~135KB |
| Concurrency & Threading | 4 | ~77KB |
| Spring Boot | 3 | ~64KB |
| Kafka & Messaging | 3 | ~66KB |
| Database & Indexing | 3 | ~61KB |
| Distributed Systems | 4 | ~86KB |
| Caching & Performance | 1 | ~24KB |
| Algorithms & Problem Solving | 2 | ~43KB |
| **Total** | **27** | **~550KB** |

---

## 🎯 Interview Preparation Tips

### For AMD QIS/QES Interviews

**Round 1: Technical Screening (1 hour)**
- Focus on: Core Java, Data Structures, Algorithms
- Practice: HashMap, ConcurrentHashMap, ArrayDeque
- Coding: LeetCode Medium level problems

**Round 2: System Design (1-1.5 hours)**
- Focus on: Distributed Systems, Microservices, Databases
- Topics: Load Balancing, Caching, Sharding, Rate Limiting
- Practice: Design URL shortener, Design cache system

**Round 3: Deep Dive Technical (1 hour)**
- Focus on: Spring Boot, Kafka, Redis, Docker
- Topics: Exception handling, Async processing, Containerization
- Be ready for: Production scenarios, debugging, optimization

**Round 4: Managerial (30-45 minutes)**
- Behavioral questions
- Past project experiences
- Problem-solving approach

---

## 🚀 How to Use This Repository

### Study Plan (2 Weeks)

**Week 1: Fundamentals**
- Day 1-2: Core Java (HashMap, Records, Optional)
- Day 3-4: Concurrency (ExecutorService, Virtual Threads)
- Day 5-6: Spring Boot (Exception Handling, Optimization)
- Day 7: Review and practice coding problems

**Week 2: Advanced Topics**
- Day 8-9: Kafka (Architecture, Consumer Lag)
- Day 10-11: Databases (Indexing, Sharding, Comparison)
- Day 12-13: Distributed Systems (Load Balancers, Rate Limiter)
- Day 14: Mock interviews and revision

### Quick Reference

**Most Important Topics** (Must Read):
1. HashMap Implementation ⭐⭐⭐
2. ConcurrentHashMap Design ⭐⭐⭐
3. Spring Boot Exception Handling ⭐⭐⭐
4. Kafka Architecture ⭐⭐⭐
5. Database Indexing ⭐⭐⭐
6. Rate Limiter ⭐⭐⭐

**Advanced Topics** (Good to Know):
1. Virtual Threads
2. Containerization
3. Database Comparison
4. CompletableFuture with Kafka

---

## 📝 Document Features

Each guide includes:
- ✅ **Deep dive explanations** with diagrams
- ✅ **Production-ready code** examples
- ✅ **Real-world use cases** and scenarios
- ✅ **Performance analysis** and benchmarks
- ✅ **Best practices** and anti-patterns
- ✅ **Interview questions** and answers

---

## 🔗 Additional Resources

### Official Documentation
- [Java Documentation](https://docs.oracle.com/en/java/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### Practice Platforms
- [LeetCode](https://leetcode.com/) - Coding problems
- [System Design Primer](https://github.com/donnemartin/system-design-primer)
- [Educative.io](https://www.educative.io/) - System design courses

---

## 💡 Contributing

Found an error or want to add more content? Feel free to:
1. Open an issue
2. Submit a pull request
3. Suggest new topics

---

## 📧 Contact

For questions or feedback, reach out via:
- GitHub Issues
- Email: [your-email@example.com]

---

## ⭐ Star This Repository

If you find these guides helpful for your interview preparation, please star this repository!

---

**Good luck with your AMD QIS/QES interviews! 🚀**

*Last Updated: January 29, 2024*
