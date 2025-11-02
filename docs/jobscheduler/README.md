# Job Scheduler - Distributed Job Scheduling System

A reliable, scalable, and fault-tolerant distributed job scheduling system that allows users to schedule one-off or recurring tasks with precise timing and high availability.

## 📋 Overview

This job scheduler addresses critical challenges in distributed task scheduling:
- **Reliable Scheduling** with no missed or duplicate executions
- **Fault Tolerance** with automatic failover and recovery
- **Scalability** to handle millions of scheduled jobs
- **Precise Timing** with sub-second execution accuracy
- **Distributed Coordination** using lease-based partitioning

## 🎯 Key Features

### ✅ **Job Scheduling**
- One-off jobs with specific execution times
- Recurring jobs with cron expressions
- Interval-based recurring jobs
- Priority-based job execution
- Timezone-aware scheduling

### ✅ **Job Management**
- Submit, cancel, pause, and resume jobs
- Real-time job status tracking
- Job execution history and logs
- Bulk job operations
- Job metadata and tagging

### ✅ **Fault Tolerance**
- Exactly-once execution guarantee
- Automatic retry with exponential backoff
- Dead letter queue for failed jobs
- Orphaned job recovery
- Node failure handling

### ✅ **Scalability & Performance**
- Timing wheel for O(1) scheduling operations
- Horizontal scaling with lease management
- Load balancing across executor nodes
- Thundering herd prevention
- Auto-scaling based on load

### ✅ **Monitoring & Observability**
- Real-time metrics and dashboards
- Job execution tracking
- Performance monitoring
- Health checks and alerts
- Comprehensive audit logging

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Client Applications                          │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              Job Management Service                              │
├─────────────────────┼───────────────────────────────────────────┤
│ Job Submitter │ Job Metadata │ Job Lifecycle │ Status Tracking │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│                Job Scheduler Cluster                            │
├─────────────────────┼───────────────────────────────────────────┤
│ Timing Wheel │ Cron Engine │ Priority Queue │ Lease Manager    │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              Apache Kafka                                       │
├─────────────────────┼───────────────────────────────────────────┤
│ Ready Queue │ Retry Queue │ Dead Letter │ Status Updates       │
└─────────────────────┬───────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌──────▼──────┐
│Job Executor  │ │Job Store│ │ Monitoring  │
│   Cluster    │ │(Database)│ │ & Logging   │
└──────────────┘ └─────────┘ └─────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Apache Kafka 3.0+

### Configuration
```bash
# Database
export DB_URL=jdbc:postgresql://localhost:5432/jobscheduler_db
export DB_USERNAME=scheduler_user
export DB_PASSWORD=scheduler_pass

# Kafka
export KAFKA_SERVERS=localhost:9092

# Scheduler
export SCHEDULER_LEASE_DURATION=30
export SCHEDULER_HEARTBEAT_INTERVAL=10
```

### Run the Service
```bash
mvn clean install
mvn spring-boot:run -Dspring-boot.run.main-class=org.sudhir512kj.jobscheduler.JobSchedulerApplication
```

### Submit a Job
```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Daily Report",
    "type": "email",
    "scheduleType": "CRON",
    "scheduleValue": "0 0 9 * * *",
    "payload": {
      "recipients": ["admin@company.com"],
      "template": "daily-report"
    },
    "priority": 3,
    "maxRetries": 3
  }'
```

## 📊 Performance & Scale

### Scale Targets
- **Scheduled Jobs**: Millions of jobs in the system
- **Execution Rate**: 100K+ job executions per second
- **Latency**: Sub-second execution from scheduled time
- **Availability**: 99.99% uptime with automatic failover

### Key Metrics
- **Scheduling Accuracy**: 99.9% of jobs execute within 1 second of scheduled time
- **Throughput**: 100K+ concurrent job executions
- **Recovery Time**: <30 seconds for node failures
- **Storage Efficiency**: Optimized job metadata storage

## 🔧 Core Components

### 1. **Timing Wheel**
Hierarchical timing wheel for efficient O(1) job scheduling operations.

### 2. **Job Scheduler**
Core scheduling engine with cron parsing and interval calculation.

### 3. **Lease Manager**
Distributed coordination using database-based lease management.

### 4. **Job Executor**
Scalable job execution engine with thread pool management.

### 5. **Retry Service**
Exponential backoff retry mechanism with dead letter queue.

### 6. **Job Management Service**
Complete job lifecycle management with status tracking.

## 📚 Documentation

- [System Design](System_Design.md) - Complete HLD and LLD with implementation details
- [Architecture Diagrams](Architecture_Diagrams.md) - Visual system architecture
- [API Documentation](API_Documentation.md) - Complete REST API reference
- [Scale Calculations](Scale_Calculations.md) - Performance analysis and scaling strategies

## 🎯 Critical Design Decisions

### 1. **Timing Wheel vs Priority Queue**
- **Choice**: Hierarchical Timing Wheel
- **Reason**: O(1) insertion/deletion vs O(log n) for priority queue
- **Trade-off**: Memory usage vs CPU efficiency

### 2. **Lease-based Coordination**
- **Choice**: Database-based leases with heartbeat
- **Reason**: Simpler than ZooKeeper, more reliable than Redis
- **Trade-off**: Database load vs operational complexity

### 3. **Message Queue Selection**
- **Choice**: Apache Kafka
- **Reason**: High throughput, durability, ordering guarantees
- **Trade-off**: Complexity vs reliability

### 4. **Job State Management**
- **Choice**: Database-first with event sourcing
- **Reason**: Strong consistency for job state
- **Trade-off**: Latency vs consistency

## 🔒 Security Features

### Authentication & Authorization
- JWT-based API authentication
- Role-based access control for job operations
- API key management for service accounts

### Data Protection
- Encrypted job payloads for sensitive data
- Audit logging for all job operations
- Secure communication between components

## 🧪 Testing Strategy

### Unit Tests
- Timing wheel operations
- Cron expression parsing
- Lease management logic
- Job execution flows

### Integration Tests
- End-to-end job scheduling
- Failure recovery scenarios
- Multi-node coordination
- Database consistency

### Load Tests
- High-volume job scheduling
- Concurrent execution limits
- Memory usage under load
- Failover performance

## 📈 Monitoring & Alerting

### Key Metrics
- **Jobs Scheduled**: Rate of new job submissions
- **Jobs Executed**: Successful execution rate
- **Execution Latency**: Time from scheduled to actual execution
- **Failure Rate**: Percentage of failed job executions
- **Queue Depth**: Number of pending jobs

### Alerts
- Job execution failure rate >5%
- Execution latency >5 seconds
- Queue depth >10,000 jobs
- Node lease failures
- Database connection issues

## 🔄 Deployment Strategy

### Multi-Node Setup
- Scheduler nodes with lease-based coordination
- Executor nodes for job processing
- Database cluster for metadata storage
- Kafka cluster for message queuing

### Auto-scaling
- Horizontal scaling based on queue depth
- CPU and memory-based scaling
- Automatic node registration/deregistration

---

**Built for enterprise-scale job scheduling with zero job loss guarantee and sub-second execution accuracy.**