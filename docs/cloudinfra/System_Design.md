# Cloud Infrastructure Platform - System Design

## 1. Requirements

### Functional Requirements
1. **Compute**: Create, manage, and terminate virtual machines
2. **Storage**: Object storage with buckets and versioning
3. **Networking**: VPC, subnets, security groups, load balancers
4. **Database**: Managed database services
5. **Monitoring**: Resource metrics and health checks
6. **IAM**: Identity and access management

### Non-Functional Requirements
1. **Scalability**: Support millions of resources
2. **Availability**: 99.99% uptime SLA
3. **Performance**: Sub-second API response times
4. **Security**: Encryption, isolation, access control
5. **Cost**: Pay-per-use pricing model

## 2. Capacity Estimation

### Traffic
- **Active Users**: 100K organizations
- **Resources per Org**: 100 VMs, 50 buckets
- **API Requests**: 100K req/sec peak
- **Storage**: 100PB total

### Storage
- **Metadata DB**: 1TB (resource metadata)
- **Object Storage**: 100PB (user data)
- **Logs**: 10TB/day

### Bandwidth
- **Ingress**: 10 Gbps
- **Egress**: 50 Gbps

## 3. API Design

### Compute APIs
```
POST   /api/v1/compute/vms
GET    /api/v1/compute/vms
GET    /api/v1/compute/vms/{vmId}
POST   /api/v1/compute/vms/{vmId}/start
POST   /api/v1/compute/vms/{vmId}/stop
DELETE /api/v1/compute/vms/{vmId}
```

### Storage APIs
```
POST   /api/v1/storage/buckets
GET    /api/v1/storage/buckets
POST   /api/v1/storage/buckets/{bucket}/objects/{key}
GET    /api/v1/storage/buckets/{bucket}/objects/{key}
DELETE /api/v1/storage/buckets/{bucket}
```

## 4. Database Schema

### resources
```sql
CREATE TABLE resources (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255),
    region VARCHAR(50),
    account_id VARCHAR(50),
    type VARCHAR(50),
    state VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    tags TEXT
);
```

### virtual_machines
```sql
CREATE TABLE virtual_machines (
    id VARCHAR(50) PRIMARY KEY,
    instance_type VARCHAR(50),
    vcpus INT,
    memory_gb INT,
    disk_gb INT,
    image_id VARCHAR(100),
    public_ip VARCHAR(50),
    private_ip VARCHAR(50),
    vpc_id VARCHAR(50),
    subnet_id VARCHAR(50),
    security_group_id VARCHAR(50),
    FOREIGN KEY (id) REFERENCES resources(id)
);
```

### storage_buckets
```sql
CREATE TABLE storage_buckets (
    id VARCHAR(50) PRIMARY KEY,
    bucket_name VARCHAR(255) UNIQUE,
    size_bytes BIGINT,
    object_count BIGINT,
    storage_class VARCHAR(50),
    access_level VARCHAR(50),
    versioning_enabled BOOLEAN,
    encryption_enabled BOOLEAN,
    FOREIGN KEY (id) REFERENCES resources(id)
);
```

## 5. High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Client Layer                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │   Web    │  │   CLI    │  │   SDK    │             │
│  │ Console  │  │  Tools   │  │ (Python) │             │
│  └──────────┘  └──────────┘  └──────────┘             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  API Gateway                            │
│  - Authentication  - Rate Limiting  - Routing           │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              Service Layer (Spring Boot)                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Compute  │  │ Storage  │  │ Network  │             │
│  │ Service  │  │ Service  │  │ Service  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Database │  │   IAM    │  │Monitoring│             │
│  │ Service  │  │ Service  │  │ Service  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│           Resource Provisioning Layer                   │
│  - Async provisioning  - State management               │
│  - Health checks       - Auto-recovery                  │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              Infrastructure Layer                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │Hypervisor│  │  Object  │  │ Network  │             │
│  │  (KVM)   │  │ Storage  │  │  Stack   │             │
│  └──────────┘  └──────────┘  └──────────┘             │
└─────────────────────────────────────────────────────────┘
```

## 6. Key Components

### Compute Service
- VM lifecycle management with FSM
- Instance type selection (t2.micro to t2.large)
- Quota enforcement per project
- Async provisioning with task queue

### Storage Service
- S3-like bucket management
- Object upload/download with versioning
- Storage classes (Standard, Glacier, etc.)
- Encryption at rest

### Provisioning Worker Service
- Async task processing with retry logic
- Exponential backoff (max 3 retries)
- Dead-letter queue for failed tasks
- Idempotent operations

### Placement Service
- Best-fit bin-packing algorithm
- Host selection based on CPU/memory
- Resource allocation tracking
- Affinity/anti-affinity rules

### Quota Service
- Per-project resource limits
- Atomic quota checks and increments
- Prevents resource exhaustion

### Metrics Collector Service
- Time-series data ingestion
- Async batch processing
- Time-window queries
- Resource utilization tracking

### Hypervisor Agent Service
- VM boot/shutdown/delete operations
- Host communication via RPC
- Heartbeat monitoring

## 7. Resource Lifecycle FSM

```
PROVISIONING → RUNNING → STOPPING → STOPPED
       ↓           ↓          ↓         ↓
     ERROR ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
       ↓
   DELETING → DELETED
```

### State Transitions
- All transitions are idempotent and durable
- Logged for observability
- Atomic updates in database

## 8. Async Task Processing

### Task Queue Architecture
```
RMS → Task Queue → Provisioning Workers
         ↓
    Dead Letter Queue (after 3 retries)
```

### Features
- At-least-once delivery
- Deduplication via task UUID
- Exponential backoff retry
- Task TTL to prevent zombie tasks

## 9. Placement Algorithm

### Best-Fit Bin Packing
```java
select host where:
  - available_cpu >= required_cpu
  - available_memory >= required_memory
  - minimize(available_cpu - required_cpu)
```

### Benefits
- Efficient resource utilization
- Reduces fragmentation
- Supports affinity rules

## 10. Scalability

### Horizontal Scaling
- Stateless API servers (scale to 1000+)
- Provisioning workers (scale based on queue depth)
- Database read replicas (up to 15)
- Distributed object storage

### Caching
- Resource metadata in Redis
- Host availability cache
- API response caching

## 11. Security

### Authentication
- API keys and tokens
- IAM roles and policies
- Multi-factor authentication

### Encryption
- TLS for data in transit
- AES-256 for data at rest
- Key management service

### Isolation
- VPC network isolation
- Security groups
- Resource tagging

## 12. Monitoring

### Metrics
- Resource utilization (CPU, memory, disk)
- API latency and throughput
- Error rates
- Cost tracking

### Alerting
- Resource state changes
- Capacity thresholds
- Security events
- Service health

## 13. Cost Model

### Compute Pricing
- **t2.micro**: $0.01/hour
- **t2.small**: $0.02/hour
- **t2.medium**: $0.04/hour
- **t2.large**: $0.08/hour

### Storage Pricing
- **Standard**: $0.023/GB/month
- **Infrequent Access**: $0.0125/GB/month
- **Glacier**: $0.004/GB/month

### Data Transfer
- **Ingress**: Free
- **Egress**: $0.09/GB
