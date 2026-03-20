# AWS EC2 (Elastic Compute Cloud) - Deep Dive

## Table of Contents
1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Instance Types](#instance-types)
4. [Pricing Models](#pricing-models)
5. [Storage Options](#storage-options)
6. [Networking](#networking)
7. [Security](#security)
8. [High Availability & Scaling](#high-availability--scaling)
9. [Monitoring & Management](#monitoring--management)
10. [Best Practices](#best-practices)
11. [Real-World Use Cases](#real-world-use-cases)

---

## Overview

**AWS EC2** is a web service that provides resizable compute capacity in the cloud. It allows you to launch virtual servers (instances) on-demand, paying only for what you use.

### Key Benefits
- **Elasticity**: Scale up/down based on demand
- **Pay-as-you-go**: No upfront hardware costs
- **Global Infrastructure**: Deploy across 33+ regions worldwide
- **Flexibility**: Choose OS, instance type, storage, and networking
- **Integration**: Seamless integration with AWS services

### Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                        AWS Region                            │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Availability Zone A                       │  │
│  │  ┌──────────────────────────────────────────────┐    │  │
│  │  │  VPC Subnet (10.0.1.0/24)                    │    │  │
│  │  │  ┌────────────┐  ┌────────────┐              │    │  │
│  │  │  │ EC2        │  │ EC2        │              │    │  │
│  │  │  │ Instance 1 │  │ Instance 2 │              │    │  │
│  │  │  │ (t3.medium)│  │ (t3.medium)│              │    │  │
│  │  │  └────────────┘  └────────────┘              │    │  │
│  │  └──────────────────────────────────────────────┘    │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Availability Zone B                       │  │
│  │  ┌──────────────────────────────────────────────┐    │  │
│  │  │  VPC Subnet (10.0.2.0/24)                    │    │  │
│  │  │  ┌────────────┐  ┌────────────┐              │    │  │
│  │  │  │ EC2        │  │ EC2        │              │    │  │
│  │  │  │ Instance 3 │  │ Instance 4 │              │    │  │
│  │  │  │ (t3.medium)│  │ (t3.medium)│              │    │  │
│  │  │  └────────────┘  └────────────┘              │    │  │
│  │  └──────────────────────────────────────────────┘    │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Core Concepts

### 1. Amazon Machine Image (AMI)
A template containing the OS, application server, and applications needed to launch an instance.

**Types of AMIs**:
- **AWS-provided AMIs**: Amazon Linux 2, Ubuntu, Windows Server, Red Hat
- **Marketplace AMIs**: Pre-configured software from vendors
- **Community AMIs**: Shared by AWS community
- **Custom AMIs**: Your own images

**AMI Components**:
```
AMI
├── Root Volume Template (OS + Apps)
├── Launch Permissions (Public/Private/Specific Accounts)
├── Block Device Mapping (EBS volumes)
└── Instance Type Recommendations
```

### 2. Instance Lifecycle

```
┌──────────┐
│ Pending  │ ← Launch instance
└────┬─────┘
     │
     ▼
┌──────────┐     Stop      ┌──────────┐
│ Running  │ ────────────→ │ Stopped  │
└────┬─────┘               └────┬─────┘
     │                          │
     │ Reboot                   │ Start
     │ ←────────────────────────┘
     │
     │ Terminate
     ▼
┌──────────┐
│Terminated│
└──────────┘
```

**States Explained**:
- **Pending**: Instance is launching (billed once running)
- **Running**: Instance is active (billed per second)
- **Stopping**: Instance is shutting down
- **Stopped**: Instance is shut down (EBS storage still billed)
- **Terminated**: Instance is deleted (cannot be restarted)

### 3. Instance Metadata
Access instance information from within the instance:
```bash
# Get instance ID
curl http://169.254.169.254/latest/meta-data/instance-id

# Get instance type
curl http://169.254.169.254/latest/meta-data/instance-type

# Get public IP
curl http://169.254.169.254/latest/meta-data/public-ipv4

# Get IAM role credentials
curl http://169.254.169.254/latest/meta-data/iam/security-credentials/role-name
```

---

## Instance Types

AWS offers 500+ instance types optimized for different workloads.

### Instance Naming Convention
```
t3.medium
│ │  │
│ │  └─ Size (nano, micro, small, medium, large, xlarge, 2xlarge, etc.)
│ └──── Generation (3 = 3rd generation)
└────── Family (t = burstable performance)
```

### Instance Families

#### 1. General Purpose (T, M, Mac)
**Use Case**: Web servers, development environments, small databases

**T3/T3a (Burstable)**:
- **vCPUs**: 2-8
- **Memory**: 0.5-32 GB
- **Network**: Up to 5 Gbps
- **CPU Credits**: Accumulate credits when idle, burst when needed
- **Example**: `t3.medium` - 2 vCPU, 4 GB RAM, $0.0416/hr

**M6i (Balanced)**:
- **vCPUs**: 2-128
- **Memory**: 8-512 GB
- **Network**: Up to 50 Gbps
- **Example**: `m6i.xlarge` - 4 vCPU, 16 GB RAM, $0.192/hr

#### 2. Compute Optimized (C)
**Use Case**: High-performance web servers, batch processing, gaming servers

**C6i**:
- **vCPUs**: 2-128
- **Memory**: 4-256 GB
- **Network**: Up to 50 Gbps
- **CPU**: Intel Xeon 3rd Gen (3.5 GHz)
- **Example**: `c6i.2xlarge` - 8 vCPU, 16 GB RAM, $0.34/hr

#### 3. Memory Optimized (R, X, z)
**Use Case**: In-memory databases (Redis, Memcached), big data analytics

**R6i**:
- **vCPUs**: 2-128
- **Memory**: 16-1024 GB (8 GB per vCPU)
- **Network**: Up to 50 Gbps
- **Example**: `r6i.xlarge` - 4 vCPU, 32 GB RAM, $0.252/hr

**X2iedn (Extreme Memory)**:
- **vCPUs**: 2-128
- **Memory**: 256-4096 GB (32 GB per vCPU)
- **Example**: `x2iedn.32xlarge` - 128 vCPU, 4 TB RAM

#### 4. Storage Optimized (I, D, H)
**Use Case**: NoSQL databases, data warehousing, Elasticsearch

**I4i (NVMe SSD)**:
- **vCPUs**: 2-128
- **Memory**: 16-1024 GB
- **Storage**: Up to 30 TB NVMe SSD
- **IOPS**: Up to 2 million
- **Example**: `i4i.xlarge` - 4 vCPU, 32 GB RAM, 937 GB NVMe

#### 5. Accelerated Computing (P, G, F)
**Use Case**: Machine learning, video encoding, graphics workloads

**P4d (GPU - ML Training)**:
- **GPUs**: 8x NVIDIA A100 (40 GB each)
- **vCPUs**: 96
- **Memory**: 1152 GB
- **Network**: 400 Gbps
- **Example**: `p4d.24xlarge` - $32.77/hr

**G5 (GPU - Graphics/Inference)**:
- **GPUs**: 1-8x NVIDIA A10G
- **vCPUs**: 4-96
- **Memory**: 16-768 GB
- **Example**: `g5.xlarge` - 1 GPU, 4 vCPU, 16 GB RAM, $1.006/hr

### Instance Type Selection Matrix

| Workload | Instance Family | Example Type | Key Feature |
|----------|----------------|--------------|-------------|
| Web Server | T3, M6i | t3.medium | Balanced CPU/Memory |
| API Server | C6i | c6i.xlarge | High CPU |
| Redis Cache | R6i | r6i.large | High Memory |
| MongoDB | I4i | i4i.xlarge | High IOPS |
| ML Training | P4d | p4d.24xlarge | GPU |
| Video Encoding | G5 | g5.2xlarge | GPU |

---

## Pricing Models

### 1. On-Demand Instances
**Pay per second** (minimum 60 seconds) with no commitment.

**Pricing Example** (us-east-1):
- `t3.medium`: $0.0416/hr = $30.37/month
- `m6i.xlarge`: $0.192/hr = $140.16/month
- `c6i.2xlarge`: $0.34/hr = $248.20/month

**Use Case**: 
- Short-term workloads
- Unpredictable usage
- Development/testing

### 2. Reserved Instances (RI)
**Save up to 72%** by committing to 1 or 3 years.

**Types**:
- **Standard RI**: Highest discount (up to 72%), cannot change instance type
- **Convertible RI**: Lower discount (up to 54%), can change instance family
- **Scheduled RI**: Reserve for specific time windows

**Payment Options**:
- **All Upfront**: Highest discount
- **Partial Upfront**: Moderate discount
- **No Upfront**: Lowest discount

**Example** (`m6i.xlarge` in us-east-1):
| Payment | 1-Year | 3-Year | Savings |
|---------|--------|--------|---------|
| On-Demand | $1,681/yr | $5,046/3yr | 0% |
| No Upfront RI | $1,095/yr | $2,628/3yr | 35-48% |
| All Upfront RI | $1,013/yr | $2,190/3yr | 40-57% |

### 3. Savings Plans
**Flexible pricing** with commitment to consistent usage ($/hour).

**Types**:
- **Compute Savings Plans**: Up to 66% savings, any instance family/region/OS
- **EC2 Instance Savings Plans**: Up to 72% savings, specific instance family/region

**Example**:
- Commit to $10/hour for 1 year
- Use any combination of EC2, Fargate, Lambda
- Automatic discount applied

### 4. Spot Instances
**Save up to 90%** by using spare AWS capacity.

**How it works**:
1. Set maximum price you're willing to pay
2. Instance runs when spot price < your max price
3. AWS can terminate with 2-minute warning if capacity needed

**Spot Price Example** (`m6i.xlarge`):
- On-Demand: $0.192/hr
- Spot Price: $0.0576/hr (70% savings)

**Use Case**:
- Batch processing
- Big data analytics
- CI/CD pipelines
- Fault-tolerant workloads

**Spot Fleet**: Request multiple instance types across AZs for better availability.

### 5. Dedicated Hosts
**Physical server** dedicated to your use.

**Pricing**: $2-$5 per hour per host (varies by instance family)

**Use Case**:
- Compliance requirements
- Bring Your Own License (BYOL)
- Server-bound software licenses

### 6. Dedicated Instances
**Instances on hardware** dedicated to your account (but not a specific physical server).

**Pricing**: On-Demand + $2/hour per region

---

## Storage Options

### 1. Amazon EBS (Elastic Block Store)
**Network-attached block storage** that persists independently of instance lifecycle.

#### EBS Volume Types

**General Purpose SSD (gp3)** - Default choice
- **Size**: 1 GB - 16 TB
- **IOPS**: 3,000-16,000 (baseline 3,000)
- **Throughput**: 125-1,000 MB/s (baseline 125 MB/s)
- **Price**: $0.08/GB-month + $0.005/provisioned IOPS + $0.04/MB/s throughput
- **Use Case**: Boot volumes, dev/test, low-latency apps

**Provisioned IOPS SSD (io2)** - High performance
- **Size**: 4 GB - 16 TB
- **IOPS**: 100-64,000 (up to 1,000 IOPS per GB)
- **Throughput**: Up to 1,000 MB/s
- **Durability**: 99.999% (vs 99.8-99.9% for gp3)
- **Price**: $0.125/GB-month + $0.065/provisioned IOPS
- **Use Case**: Databases (MySQL, PostgreSQL, MongoDB)

**Throughput Optimized HDD (st1)** - Big data
- **Size**: 125 GB - 16 TB
- **Throughput**: 40-500 MB/s
- **IOPS**: Up to 500
- **Price**: $0.045/GB-month
- **Use Case**: Big data, data warehouses, log processing

**Cold HDD (sc1)** - Infrequent access
- **Size**: 125 GB - 16 TB
- **Throughput**: 12-250 MB/s
- **Price**: $0.015/GB-month
- **Use Case**: Archival data, infrequent access

#### EBS Features

**Snapshots**:
```bash
# Create snapshot
aws ec2 create-snapshot --volume-id vol-1234567890abcdef0 --description "Daily backup"

# Restore from snapshot
aws ec2 create-volume --snapshot-id snap-1234567890abcdef0 --availability-zone us-east-1a
```

**Encryption**:
- AES-256 encryption at rest
- Encrypted data in transit between instance and volume
- Minimal performance impact
- Snapshots automatically encrypted

**Multi-Attach** (io2 only):
- Attach single volume to up to 16 instances
- Use case: Clustered applications (Oracle RAC)

### 2. Instance Store
**Physically attached storage** (ephemeral - data lost on stop/terminate).

**Characteristics**:
- **Performance**: Very high IOPS (millions) and throughput
- **Cost**: Included in instance price
- **Durability**: Data lost on instance stop/terminate/failure
- **Size**: Varies by instance type (up to 30 TB for i4i.32xlarge)

**Use Case**:
- Temporary data (cache, buffers, scratch data)
- Data replicated across instances (Cassandra, HDFS)

### 3. Amazon EFS (Elastic File System)
**Managed NFS** that can be mounted by multiple EC2 instances.

**Features**:
- **Scalability**: Petabyte-scale, auto-scaling
- **Performance**: Up to 10 GB/s throughput, 500K IOPS
- **Availability**: Multi-AZ by default
- **Price**: $0.30/GB-month (Standard), $0.043/GB-month (Infrequent Access)

**Use Case**:
- Shared file storage
- Content management
- Web serving

### 4. Amazon FSx
**Managed file systems** for Windows and Lustre.

**FSx for Windows File Server**:
- SMB protocol
- Active Directory integration
- Price: $0.013-$0.65/GB-month

**FSx for Lustre**:
- High-performance computing
- ML training
- Price: $0.145-$0.575/GB-month

---

## Networking

### 1. Elastic Network Interface (ENI)
**Virtual network card** attached to EC2 instance.

**Attributes**:
- Primary private IPv4 address
- One or more secondary private IPv4 addresses
- One Elastic IP per private IPv4
- One public IPv4 address
- One or more security groups
- MAC address

**Use Case**:
- Create management network
- Dual-homed instances (multiple subnets)
- Low-cost failover (move ENI to standby instance)

### 2. Elastic IP (EIP)
**Static public IPv4 address** that can be remapped.

**Characteristics**:
- Persists until you release it
- Can be moved between instances
- Charged $0.005/hr when NOT associated with running instance
- Limited to 5 per region (can request increase)

### 3. Enhanced Networking
**High performance networking** using SR-IOV.

**Types**:
- **ENA (Elastic Network Adapter)**: Up to 100 Gbps
- **Intel 82599 VF**: Up to 10 Gbps (older instances)

**Benefits**:
- Higher bandwidth
- Higher PPS (packets per second)
- Lower latency
- Lower jitter

**Enabled by default** on modern instance types (M5, C5, R5, etc.)

### 4. Placement Groups
**Control instance placement** for performance or availability.

**Cluster Placement Group**:
- Instances in single AZ, close proximity
- Low latency (10 Gbps between instances)
- Use case: HPC, big data

**Spread Placement Group**:
- Instances on distinct hardware
- Max 7 instances per AZ per group
- Use case: Critical applications requiring isolation

**Partition Placement Group**:
- Instances in logical partitions (different racks)
- Up to 7 partitions per AZ
- Use case: Distributed systems (Hadoop, Cassandra, Kafka)

---

## Security

### 1. Security Groups
**Virtual firewall** controlling inbound/outbound traffic.

**Characteristics**:
- **Stateful**: Return traffic automatically allowed
- **Default**: All inbound denied, all outbound allowed
- **Rules**: Allow only (no deny rules)
- **Evaluation**: All rules evaluated (not ordered)

**Example**:
```json
{
  "SecurityGroupRules": [
    {
      "IpProtocol": "tcp",
      "FromPort": 80,
      "ToPort": 80,
      "IpRanges": [{"CidrIp": "0.0.0.0/0"}],
      "Description": "Allow HTTP from anywhere"
    },
    {
      "IpProtocol": "tcp",
      "FromPort": 443,
      "ToPort": 443,
      "IpRanges": [{"CidrIp": "0.0.0.0/0"}],
      "Description": "Allow HTTPS from anywhere"
    },
    {
      "IpProtocol": "tcp",
      "FromPort": 22,
      "ToPort": 22,
      "IpRanges": [{"CidrIp": "10.0.0.0/16"}],
      "Description": "Allow SSH from VPC only"
    }
  ]
}
```

### 2. IAM Roles for EC2
**Grant permissions** to applications running on EC2 without storing credentials.

**Example**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::my-bucket/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem"
      ],
      "Resource": "arn:aws:dynamodb:us-east-1:123456789012:table/MyTable"
    }
  ]
}
```

**Best Practice**: Use IAM roles instead of storing AWS credentials on instances.

### 3. Key Pairs
**SSH key pairs** for secure instance access.

**Create key pair**:
```bash
aws ec2 create-key-pair --key-name my-key --query 'KeyMaterial' --output text > my-key.pem
chmod 400 my-key.pem
```

**Connect to instance**:
```bash
ssh -i my-key.pem ec2-user@ec2-54-123-45-67.compute-1.amazonaws.com
```

### 4. Systems Manager Session Manager
**Browser-based shell** without SSH keys or bastion hosts.

**Benefits**:
- No open inbound ports
- Centralized access control via IAM
- Audit logging to CloudWatch/S3
- No key management

---

## High Availability & Scaling

### 1. Multi-AZ Deployment
Deploy instances across multiple Availability Zones for fault tolerance.

```
┌─────────────────────────────────────────────┐
│          Application Load Balancer          │
│         (Multi-AZ by default)               │
└──────────────┬──────────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
┌──────▼──────┐  ┌──────▼──────┐
│    AZ-A     │  │    AZ-B     │
│  ┌────────┐ │  │  ┌────────┐ │
│  │  EC2   │ │  │  │  EC2   │ │
│  │Instance│ │  │  │Instance│ │
│  └────────┘ │  │  └────────┘ │
└─────────────┘  └─────────────┘
```

### 2. Load Balancing
Distribute traffic across multiple instances.

**Application Load Balancer (ALB)**:
- Layer 7 (HTTP/HTTPS)
- Path-based routing
- Host-based routing
- WebSocket support

**Network Load Balancer (NLB)**:
- Layer 4 (TCP/UDP)
- Ultra-low latency
- Static IP support
- Millions of requests per second

### 3. Auto Scaling (See Auto Scaling Deep Dive)
Automatically adjust capacity based on demand.

---

## Monitoring & Management

### 1. CloudWatch Metrics
**Default metrics** (5-minute intervals, free):
- CPUUtilization
- NetworkIn/NetworkOut
- DiskReadOps/DiskWriteOps
- StatusCheckFailed

**Detailed monitoring** (1-minute intervals, $2.10/instance/month):
```bash
aws ec2 monitor-instances --instance-ids i-1234567890abcdef0
```

### 2. CloudWatch Alarms
**Alert on metric thresholds**:
```bash
aws cloudwatch put-metric-alarm \
  --alarm-name cpu-high \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2
```

### 3. CloudWatch Logs
**Collect logs** from applications:
```bash
# Install CloudWatch agent
sudo yum install amazon-cloudwatch-agent

# Configure agent
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/config.json
```

### 4. AWS Systems Manager
**Manage instances at scale**:
- **Run Command**: Execute commands on multiple instances
- **Patch Manager**: Automate OS patching
- **Session Manager**: Browser-based shell access
- **Parameter Store**: Store configuration and secrets

---

## Best Practices

### 1. Right-Sizing
- Start with smaller instance, scale up as needed
- Use CloudWatch metrics to identify underutilized instances
- Consider burstable instances (T3) for variable workloads

### 2. Cost Optimization
- Use Reserved Instances or Savings Plans for steady-state workloads
- Use Spot Instances for fault-tolerant workloads
- Stop instances when not in use (dev/test environments)
- Delete unused EBS volumes and snapshots

### 3. Security
- Never store credentials on instances (use IAM roles)
- Use Security Groups as firewalls
- Enable encryption for EBS volumes
- Regularly patch OS and applications
- Use Systems Manager Session Manager instead of SSH

### 4. High Availability
- Deploy across multiple AZs
- Use Auto Scaling for automatic recovery
- Use ELB for traffic distribution
- Regular backups (EBS snapshots, AMIs)

### 5. Performance
- Choose appropriate instance type for workload
- Use Enhanced Networking
- Use Placement Groups for low-latency workloads
- Use EBS-optimized instances
- Monitor CloudWatch metrics

---

## Real-World Use Cases

### 1. Web Application (3-Tier Architecture)
```
Internet
   │
   ▼
┌─────────────────────┐
│  Application LB     │ (Multi-AZ)
└──────────┬──────────┘
           │
    ┌──────┴──────┐
    │             │
┌───▼───┐     ┌───▼───┐
│ Web   │     │ Web   │ (t3.medium, Auto Scaling)
│ Tier  │     │ Tier  │
└───┬───┘     └───┬───┘
    │             │
    └──────┬──────┘
           │
    ┌──────┴──────┐
    │             │
┌───▼───┐     ┌───▼───┐
│ App   │     │ App   │ (c6i.xlarge, Auto Scaling)
│ Tier  │     │ Tier  │
└───┬───┘     └───┬───┘
    │             │
    └──────┬──────┘
           │
    ┌──────▼──────┐
    │   RDS       │ (Multi-AZ)
    │  (Primary)  │
    └─────────────┘
```

**Configuration**:
- Web Tier: 2-10 t3.medium instances (Auto Scaling)
- App Tier: 2-10 c6i.xlarge instances (Auto Scaling)
- Database: RDS Multi-AZ (not EC2)
- Cost: ~$500-$2,000/month

### 2. Big Data Processing (Spot Fleet)
```
S3 Bucket (Input Data)
   │
   ▼
┌─────────────────────┐
│  Spot Fleet         │
│  ┌────┐ ┌────┐      │
│  │ c6i│ │ c6i│ ...  │ (50-100 instances)
│  │.4xl│ │.4xl│      │
│  └────┘ └────┘      │
└──────────┬──────────┘
           │
           ▼
S3 Bucket (Output Data)
```

**Configuration**:
- Instance Type: c6i.4xlarge (16 vCPU, 32 GB RAM)
- Pricing: Spot ($0.272/hr vs $0.68/hr On-Demand)
- Fleet Size: 50-100 instances
- Cost Savings: 60-70% vs On-Demand

### 3. Machine Learning Training (GPU)
```
S3 (Training Data)
   │
   ▼
┌─────────────────────┐
│  p4d.24xlarge       │
│  8x NVIDIA A100     │
│  96 vCPU, 1152 GB   │
└──────────┬──────────┘
           │
           ▼
S3 (Model Artifacts)
```

**Configuration**:
- Instance Type: p4d.24xlarge
- Pricing: $32.77/hr On-Demand, $19.66/hr Spot (40% savings)
- Training Time: 10 hours
- Cost: $327.70 On-Demand, $196.60 Spot

### 4. High-Performance Database (io2 + R6i)
```
┌─────────────────────┐
│  r6i.4xlarge        │
│  16 vCPU, 128 GB    │
│  ┌────────────────┐ │
│  │ EBS io2        │ │
│  │ 1 TB           │ │
│  │ 32,000 IOPS    │ │
│  └────────────────┘ │
└─────────────────────┘
```

**Configuration**:
- Instance: r6i.4xlarge ($1.008/hr)
- Storage: 1 TB io2 ($125/month) + 32,000 IOPS ($2,080/month)
- Total: ~$2,950/month
- Performance: 32,000 IOPS, <1ms latency

---

## Summary

**EC2 is the foundation of AWS compute**, offering:
- **500+ instance types** for any workload
- **Multiple pricing models** (On-Demand, Reserved, Spot, Savings Plans)
- **Flexible storage** (EBS, Instance Store, EFS, FSx)
- **High availability** (Multi-AZ, Auto Scaling, Load Balancing)
- **Enterprise security** (IAM, Security Groups, Encryption)

**Key Takeaways**:
1. Choose the right instance type for your workload
2. Use Reserved Instances/Savings Plans for steady-state workloads
3. Use Spot Instances for fault-tolerant workloads (up to 90% savings)
4. Deploy across multiple AZs for high availability
5. Use IAM roles instead of storing credentials
6. Monitor with CloudWatch and set up alarms
7. Automate with Auto Scaling and Systems Manager

**Next Steps**: Learn about VPC, Auto Scaling, ECS, and EKS to build complete cloud architectures.
