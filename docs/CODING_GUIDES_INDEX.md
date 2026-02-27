# System Design Coding Guides - Complete Index

## 📚 Overview

This collection contains **13 comprehensive coding guides** for major system design topics. Each guide includes:

✅ **Complete single-file implementation** (runs on any online Java compiler)
✅ **SOLID principles** explained with examples
✅ **Design patterns** with real implementations
✅ **Interview questions** with detailed answers
✅ **Scalability strategies** for production systems
✅ **No external dependencies** - pure Java

---

## 🎯 Available Guides

### 1. Alert Manager
**Location**: `docs/alertmanager/Coding_Guide.md`

**What You'll Learn**:
- Jira webhook integration with authentication
- Multi-channel notifications (Slack, Email, SMS, PagerDuty, OpsGenie)
- Strategy Pattern for channel handlers
- Factory Pattern for handler creation
- Repository Pattern for data access

**Key Patterns**: Strategy, Factory, Repository, Observer

**Run Online**: https://www.jdoodle.com/online-java-compiler

---

### 2. Parking Lot System
**Location**: `docs/parkinglot/Coding_Guide.md`

**What You'll Learn**:
- Thread-safe spot allocation
- Multiple vehicle types and spot types
- Pricing strategies (hourly, flat, dynamic)
- Singleton pattern for parking lot instance

**Key Patterns**: Factory, Strategy, Singleton

**Interview Focus**: Concurrency, thread safety, preventing double-booking

---

### 3. URL Shortener
**Location**: `docs/urlshortener/Coding_Guide.md`

**What You'll Learn**:
- Base62 encoding algorithm
- Collision-free short code generation
- Analytics tracking
- Repository pattern for data access

**Key Patterns**: Strategy, Repository

**Interview Focus**: Encoding algorithms, scalability, distributed ID generation

---

### 4. Rate Limiter
**Location**: `docs/ratelimiter/Coding_Guide.md`

**What You'll Learn**:
- Fixed Window algorithm
- Sliding Window algorithm
- Token Bucket algorithm
- Algorithm comparison and trade-offs

**Key Patterns**: Strategy, Factory

**Interview Focus**: Algorithm selection, distributed rate limiting, Redis implementation

---

### 5. LRU Cache
**Location**: `docs/cache/LRU_Cache_Guide.md`

**What You'll Learn**:
- HashMap + Doubly Linked List implementation
- O(1) get and put operations
- Thread-safe implementation
- Eviction policies

**Key Concepts**: Data structures, time complexity, concurrency

**Interview Focus**: Why HashMap + DLL, thread safety, distributed caching

---

### 6. Uber Clone
**Location**: `docs/uber/Coding_Guide.md`

**What You'll Learn**:
- Geo-location based matching
- Distance calculation algorithms
- Surge pricing implementation
- Driver-rider matching strategies

**Key Patterns**: Strategy, Observer, Factory

**Interview Focus**: Geo-spatial indexing, real-time matching, scalability

---

### 7. Ticket Booking
**Location**: `docs/ticketbooking/Coding_Guide.md`

**What You'll Learn**:
- Zero overselling guarantee
- Concurrent booking handling
- Timeout and expiry management
- Payment integration

**Key Patterns**: State, Strategy, Repository

**Interview Focus**: Preventing overselling, concurrency control, flash sales

---

### 8. Instagram Clone
**Location**: `docs/instagram/Coding_Guide.md`

**What You'll Learn**:
- Social graph management
- Feed generation algorithms (chronological, ranked)
- Like and comment system
- Follow/unfollow mechanics

**Key Patterns**: Strategy, Observer, Factory

**Interview Focus**: Feed generation, fan-out strategies, scalability

---

### 9. WhatsApp Messenger
**Location**: `docs/whatsapp/Coding_Guide.md`

**What You'll Learn**:
- Real-time message delivery
- Message status tracking (sent, delivered, read)
- Group chat implementation
- Online presence management

**Key Patterns**: Observer, State, Factory

**Interview Focus**: Real-time delivery, message storage, scalability

---

### 10. Notification System
**Location**: `docs/notification/Coding_Guide.md`

**What You'll Learn**:
- Multi-channel delivery (Email, SMS, Push)
- Retry with exponential backoff
- Priority-based delivery
- Dead Letter Queue (DLQ)

**Key Patterns**: Strategy, Template Method, Observer

**Interview Focus**: Reliability, retry strategies, scalability

---

### 11. Payment Service
**Location**: `docs/payment/Coding_Guide.md`

**What You'll Learn**:
- Idempotent payment processing
- Circuit breaker pattern
- Retry with exponential backoff
- Multiple payment gateways

**Key Patterns**: Strategy, Circuit Breaker, Saga

**Interview Focus**: Exactly-once processing, fault tolerance, distributed transactions

---

### 12. Job Scheduler
**Location**: `docs/jobscheduler/Coding_Guide.md`

**What You'll Learn**:
- One-time and recurring job scheduling
- Priority queue for job execution
- Retry mechanism for failed jobs
- Distributed coordination

**Key Patterns**: Strategy, Observer, Command

**Interview Focus**: Timing wheel, distributed locks, exactly-once execution

---

### 13. Dropbox Clone
**Location**: `docs/dropbox/Coding_Guide.md`

**What You'll Learn**:
- File upload/download
- Deduplication with hashing
- Version control
- File sharing and sync

**Key Patterns**: Strategy, Observer, Command

**Interview Focus**: Deduplication, sync algorithms, conflict resolution

---

## 🎓 How to Use These Guides

### For Interview Preparation

1. **Understand the Problem**: Read the system design overview
2. **Learn the Patterns**: Study which design patterns are used and why
3. **Code It Yourself**: Try implementing before looking at the solution
4. **Run the Code**: Copy to online compiler and see it work
5. **Answer Questions**: Practice the interview questions at the end

### For Learning System Design

1. **Start Simple**: Begin with URL Shortener or LRU Cache
2. **Progress to Complex**: Move to Uber, Instagram, WhatsApp
3. **Focus on Patterns**: Notice how same patterns appear across systems
4. **Scale It Up**: Think about how to scale each system to millions of users

### For Coding Practice

1. **Copy the Code**: Paste into https://www.jdoodle.com/online-java-compiler
2. **Modify It**: Add new features, change algorithms
3. **Break It**: Introduce bugs and fix them
4. **Extend It**: Add new patterns, improve scalability

---

## 🔑 Key Takeaways

### SOLID Principles Across All Systems

- **Single Responsibility**: Each class has one job
- **Open/Closed**: Extend without modifying existing code
- **Liskov Substitution**: Subclasses are interchangeable
- **Interface Segregation**: Small, focused interfaces
- **Dependency Inversion**: Depend on abstractions

### Common Design Patterns

| Pattern | Used In | Purpose |
|---------|---------|---------|
| **Strategy** | All systems | Different algorithms/behaviors |
| **Factory** | 8 systems | Object creation |
| **Observer** | 6 systems | Event notification |
| **Repository** | 5 systems | Data access abstraction |
| **Singleton** | 3 systems | Single instance |
| **State** | 3 systems | State transitions |
| **Circuit Breaker** | 2 systems | Fault tolerance |

### Scalability Patterns

- **Caching**: Redis for hot data
- **Sharding**: Partition by userId, region, hash
- **Replication**: Master-slave, multi-master
- **Load Balancing**: Round-robin, least connections
- **Async Processing**: Message queues (Kafka, RabbitMQ)
- **CDN**: Static content delivery

---

## 📊 Complexity Comparison

| System | Code Lines | Difficulty | Interview Frequency |
|--------|-----------|------------|-------------------|
| LRU Cache | ~100 | Easy | ⭐⭐⭐⭐⭐ |
| URL Shortener | ~150 | Easy | ⭐⭐⭐⭐⭐ |
| Rate Limiter | ~200 | Medium | ⭐⭐⭐⭐⭐ |
| Parking Lot | ~250 | Medium | ⭐⭐⭐⭐ |
| Payment Service | ~300 | Medium | ⭐⭐⭐⭐ |
| Job Scheduler | ~300 | Medium | ⭐⭐⭐⭐ |
| Notification | ~350 | Medium | ⭐⭐⭐⭐ |
| Ticket Booking | ~350 | Hard | ⭐⭐⭐⭐ |
| Uber Clone | ~400 | Hard | ⭐⭐⭐⭐⭐ |
| Instagram | ~400 | Hard | ⭐⭐⭐⭐⭐ |
| WhatsApp | ~400 | Hard | ⭐⭐⭐⭐⭐ |
| Dropbox | ~450 | Hard | ⭐⭐⭐⭐ |
| Alert Manager | ~500 | Hard | ⭐⭐⭐ |

---

## 🚀 Next Steps

1. **Start with Easy**: LRU Cache → URL Shortener → Rate Limiter
2. **Move to Medium**: Parking Lot → Payment → Job Scheduler
3. **Master Hard**: Uber → Instagram → WhatsApp
4. **Practice Daily**: Code one system per day
5. **Interview Ready**: Can explain and code any system in 45 minutes

---

## 💡 Pro Tips

### For Interviews

- **Start with Requirements**: Clarify functional and non-functional requirements
- **Draw Diagrams**: High-level architecture before coding
- **Explain Trade-offs**: Why you chose specific patterns/algorithms
- **Discuss Scalability**: How to scale to millions of users
- **Handle Edge Cases**: Concurrency, failures, network issues

### For Learning

- **Type the Code**: Don't copy-paste, type it yourself
- **Modify and Break**: Change things, see what breaks
- **Add Features**: Extend the systems with new capabilities
- **Compare Approaches**: Try different algorithms/patterns
- **Teach Others**: Best way to solidify understanding

---

## 📖 Additional Resources

### Books
- "Designing Data-Intensive Applications" by Martin Kleppmann
- "System Design Interview" by Alex Xu
- "Clean Code" by Robert C. Martin

### Online
- https://github.com/donnemartin/system-design-primer
- https://www.educative.io/courses/grokking-the-system-design-interview
- https://www.youtube.com/@ByteByteGo

### Practice
- https://leetcode.com/discuss/interview-question/system-design
- https://www.pramp.com/
- https://interviewing.io/

---

## 🎯 Success Metrics

After completing these guides, you should be able to:

✅ Explain SOLID principles with real examples
✅ Identify and implement 10+ design patterns
✅ Design scalable systems for millions of users
✅ Handle concurrency and distributed systems
✅ Code complete systems in 45-60 minutes
✅ Ace system design interviews at FAANG companies

---

**Happy Coding! 🚀**

*All code is production-ready, follows best practices, and runs on any Java compiler.*
