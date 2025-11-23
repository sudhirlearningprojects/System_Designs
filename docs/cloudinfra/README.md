# Cloud Infrastructure Management Platform

A comprehensive cloud infrastructure management platform similar to AWS Console/Azure Portal.

## Features

### Compute Services
- **Virtual Machines**: Create, start, stop, terminate VMs
- **Instance Types**: t2.micro, t2.small, t2.medium, t2.large
- **Auto-scaling**: Horizontal scaling based on metrics
- **Load Balancers**: Application, Network, Gateway LBs

### Storage Services
- **Object Storage**: S3-like bucket storage
- **Storage Classes**: Standard, Infrequent Access, Glacier, Deep Archive
- **Versioning**: Track object versions
- **Encryption**: At-rest encryption

### Networking
- **VPC**: Virtual Private Cloud isolation
- **Subnets**: Public and private subnets
- **Security Groups**: Firewall rules
- **Load Balancers**: Traffic distribution

### Database Services
- **Managed Databases**: PostgreSQL, MySQL, MongoDB, Redis, Cassandra
- **Multi-AZ**: High availability deployments
- **Automated Backups**: Point-in-time recovery

## Production Features

### ✅ Finite State Machine (FSM)
- Resource lifecycle: PROVISIONING → RUNNING → STOPPED → DELETED
- Idempotent state transitions
- Error state handling

### ✅ Async Task Processing
- Task queue with retry logic (max 3 retries)
- Dead-letter queue for failed tasks
- Exponential backoff
- Idempotent operations

### ✅ Quota Management
- Per-project resource limits
- Atomic quota enforcement
- Prevents resource exhaustion

### ✅ Placement Algorithm
- Best-fit bin-packing for host selection
- Resource allocation tracking
- Affinity/anti-affinity support

### ✅ Monitoring & Metrics
- Time-series metrics collection
- Resource utilization tracking
- Time-window queries

### ✅ Multi-Tenancy
- Project-based isolation
- Account-level resource organization

### ✅ Host Management
- Heartbeat monitoring
- Capacity tracking
- Quarantine for unhealthy hosts

## Quick Start

### 1. Start infrastructure
```bash
docker-compose up -d postgres redis
```

### 2. Start the service
```bash
./run-systems.sh cloudinfra  # Port 8096
```

### 3. Create a Virtual Machine
```bash
curl -X POST http://localhost:8096/api/v1/compute/vms \
  -H "Content-Type: application/json" \
  -H "X-Account-Id: acc-123" \
  -d '{
    "name": "web-server-1",
    "region": "us-east-1",
    "instanceType": "t2.medium",
    "imageId": "ami-ubuntu-20.04",
    "vpcId": "vpc-123",
    "subnetId": "subnet-456",
    "securityGroupId": "sg-789",
    "diskGb": 50
  }'
```

### 4. Create Storage Bucket
```bash
curl -X POST http://localhost:8096/api/v1/storage/buckets \
  -H "Content-Type: application/json" \
  -H "X-Account-Id: acc-123" \
  -d '{
    "bucketName": "my-app-data",
    "region": "us-east-1",
    "storageClass": "STANDARD",
    "accessLevel": "PRIVATE",
    "versioningEnabled": true,
    "encryptionEnabled": true
  }'
```

### 5. Upload Object
```bash
curl -X POST http://localhost:8096/api/v1/storage/buckets/my-app-data/objects/file.txt \
  -F "file=@/path/to/file.txt"
```

## Architecture

### High-Level Design
```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
┌──────▼──────────────────────────────────────┐
│         API Gateway / Load Balancer         │
└──────┬──────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────┐
│         Cloud Infrastructure Service        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ Compute  │  │ Storage  │  │ Network  │  │
│  │ Service  │  │ Service  │  │ Service  │  │
│  └──────────┘  └──────────┘  └──────────┘  │
└──────┬──────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────┐
│         Resource Provisioning Layer         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │   VM     │  │  Bucket  │  │    VPC   │  │
│  │Provision │  │ Creation │  │  Setup   │  │
│  └──────────┘  └──────────┘  └──────────┘  │
└──────┬──────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────┐
│         Infrastructure Layer                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │Hypervisor│  │  Object  │  │ Network  │  │
│  │  (KVM)   │  │ Storage  │  │  Stack   │  │
│  └──────────┘  └──────────┘  └──────────┘  │
└─────────────────────────────────────────────┘
```

## Scale

- **VMs**: 1M+ concurrent instances
- **Storage**: 100PB+ object storage
- **Requests**: 100K requests/sec
- **Regions**: Multi-region deployment
- **Availability**: 99.99% uptime SLA

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.2
- **Database**: PostgreSQL (metadata)
- **Storage**: Local filesystem / S3
- **Async**: Spring @Async for provisioning
- **API**: REST with JSON
