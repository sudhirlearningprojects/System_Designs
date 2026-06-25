# Part 1: Overview & Architecture

## 1.1 What is AWS Snowball Edge?

AWS Snowball Edge is a **physical data transport and edge computing device** provided by AWS. It's a ruggedized, tamper-evident appliance that you can use to:

1. **Migrate large datasets** to AWS without using the network
2. **Run compute workloads** at remote/disconnected locations
3. **Collect and process IoT data** at the edge

### Physical Specs
- **Weight**: ~50 lbs (22.7 kg)
- **Dimensions**: 28.3" × 10.6" × 15.5"
- **Power**: 110V/220V AC
- **Networking**: 10GbE (RJ45, SFP+), 25GbE (SFP28), 40GbE (QSFP+)
- **Operating Temperature**: 0°C to 45°C
- **Ruggedization**: ISTA-3A certified, IP44 rated

---

## 1.2 Device Variants

| Variant | Storage | Compute | GPU | Best For |
|---------|---------|---------|-----|----------|
| **Storage Optimized** | 210 TB NVMe | 40 vCPUs, 80 GB RAM | ❌ | Large data migrations |
| **Compute Optimized** | 28 TB NVMe | 104 vCPUs, 416 GB RAM | ❌ | Heavy edge compute |
| **Compute Optimized with GPU** | 28 TB NVMe | 104 vCPUs, 416 GB RAM | ✅ NVIDIA V100 | ML inference at edge |

### Storage Breakdown (Storage Optimized - 210TB)
```
┌─────────────────────────────────────┐
│  Total Raw: 210 TB NVMe             │
├─────────────────────────────────────┤
│  S3-compatible storage: ~210 TB     │
│  EBS volumes for EC2: configurable  │
│  Block storage (iSCSI): supported   │
└─────────────────────────────────────┘
```

### Compute Breakdown (Compute Optimized)
```
┌─────────────────────────────────────┐
│  vCPUs: 104                         │
│  RAM: 416 GB                        │
│  EC2 instance types: sbe-c, sbe-g   │
│  Lambda: 8 concurrent functions     │
│  EKS Anywhere: supported            │
└─────────────────────────────────────┘
```

---

## 1.3 Use Cases Deep Dive

### Use Case 1: Large-Scale Data Migration
**Scenario**: Migrate 100TB from on-premises data center to S3.

```
Network transfer at 1 Gbps:
- 100 TB ÷ 1 Gbps = ~9.3 days (theoretical)
- With overhead: ~12-15 days
- Cost: ~$9,000 in data transfer fees

Snowball Edge:
- Copy time: ~2.5 days (at 10 Gbps local network)
- Shipping: 5-7 days
- Total: ~10 days
- Cost: ~$600
```

**Break-even**: Snowball Edge becomes cost-effective at ~10TB+ when you factor in network bandwidth consumption and transfer time.

### Use Case 2: Edge Computing (Disconnected)
**Scenario**: Oil rig 200 miles offshore with satellite-only connectivity.

```
┌──────────────────────────────────────────┐
│              Oil Rig (Offshore)            │
│                                            │
│  [Sensors] ──► [Snowball Edge Cluster]    │
│                  │                         │
│                  ├── Lambda: Data filtering │
│                  ├── EC2: ML inference      │
│                  └── S3: Local storage      │
│                                            │
│  [Alerts] ◄── [Local Processing]          │
└──────────────────────────────────────────┘
         │ (Monthly device swap)
         ▼
┌──────────────────────────────────────────┐
│              AWS Cloud                     │
│  S3 → Athena → QuickSight (Analytics)    │
└──────────────────────────────────────────┘
```

### Use Case 3: Content Distribution
**Scenario**: Deploy ML models and media content to remote locations.

- **EXPORT job**: AWS loads data onto Snowball Edge from S3
- Ship device to remote location
- Device serves content locally via S3/NFS endpoints

### Use Case 4: IoT Data Collection
**Scenario**: Autonomous vehicle fleet generating 20TB/day of sensor data.

```
[Vehicle 1] ──┐
[Vehicle 2] ──┼──► [Snowball Edge at Depot]
[Vehicle 3] ──┘         │
                         ├── Filter noise (Lambda)
                         ├── Extract features (EC2/GPU)
                         └── Store raw + processed (S3)
```

---

## 1.4 Project Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    REMOTE SITE (FACTORY)                          │
│                                                                   │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐                    │
│  │ Temp     │   │ Pressure │   │ Vibration│                    │
│  │ Sensors  │   │ Sensors  │   │ Sensors  │                    │
│  └────┬─────┘   └────┬─────┘   └────┬─────┘                    │
│       │               │               │                          │
│       └───────────────┼───────────────┘                          │
│                       │                                           │
│                       ▼                                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              SNOWBALL EDGE DEVICE                         │    │
│  │                                                           │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌────────────────┐  │    │
│  │  │ S3 Endpoint │  │ NFS Gateway │  │ iSCSI Block    │  │    │
│  │  │ (Port 8443) │  │ (Port 2049) │  │ (Port 3260)    │  │    │
│  │  └──────┬──────┘  └──────┬──────┘  └────────┬───────┘  │    │
│  │         │                 │                   │           │    │
│  │         ▼                 ▼                   ▼           │    │
│  │  ┌───────────────────────────────────────────────────┐   │    │
│  │  │              LOCAL STORAGE (210 TB)                │   │    │
│  │  └───────────────────────────────────────────────────┘   │    │
│  │                                                           │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌────────────────┐  │    │
│  │  │ Lambda      │  │ EC2 (sbe1)  │  │ EKS Anywhere   │  │    │
│  │  │ Functions   │  │ Instances   │  │ Kubernetes     │  │    │
│  │  └─────────────┘  └─────────────┘  └────────────────┘  │    │
│  │                                                           │    │
│  │  ┌─────────────┐  ┌─────────────┐                       │    │
│  │  │ IoT         │  │ SageMaker   │                       │    │
│  │  │ Greengrass  │  │ Neo Model   │                       │    │
│  │  └─────────────┘  └─────────────┘                       │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                          │
                          │ (Ship device back to AWS)
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                         AWS CLOUD                                 │
│                                                                   │
│  ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌────────────────┐   │
│  │   S3    │  │ Glacier │  │  Athena  │  │  QuickSight    │   │
│  │ (Hot)   │  │ (Cold)  │  │ (Query)  │  │ (Dashboards)   │   │
│  └─────────┘  └─────────┘  └──────────┘  └────────────────┘   │
│                                                                   │
│  ┌─────────┐  ┌─────────┐  ┌──────────┐                        │
│  │ Redshift│  │   EMR   │  │SageMaker │                        │
│  │ (DW)    │  │ (Spark) │  │(Training)│                        │
│  └─────────┘  └─────────┘  └──────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
Step 1: IoT sensors generate data (JSON, binary, images)
         │
         ▼
Step 2: Data lands on Snowball Edge via S3 PUT or NFS write
         │
         ▼
Step 3: Lambda function triggers on S3 event
         ├── Validates data format
         ├── Filters noise/duplicates
         └── Routes anomalies to alert bucket
         │
         ▼
Step 4: EC2 instance runs ML inference
         ├── Predictive maintenance model
         ├── Anomaly detection
         └── Stores predictions locally
         │
         ▼
Step 5: Device shipped back to AWS
         │
         ▼
Step 6: AWS ingests data into S3
         │
         ▼
Step 7: Cloud analytics pipeline processes data
         ├── Athena for ad-hoc queries
         ├── EMR for batch processing
         └── SageMaker for model retraining
```

---

## 1.5 Security Architecture

### Encryption Layers

```
┌─────────────────────────────────────┐
│  Layer 1: Physical Security         │
│  - Tamper-evident enclosure         │
│  - E-ink shipping label (no data)   │
│  - Automatic wipe on tamper detect  │
├─────────────────────────────────────┤
│  Layer 2: Encryption at Rest        │
│  - 256-bit AES encryption           │
│  - Keys managed by AWS KMS          │
│  - Keys NEVER stored on device      │
├─────────────────────────────────────┤
│  Layer 3: Network Security          │
│  - TLS 1.2+ for all endpoints      │
│  - Self-signed certificates         │
│  - No public internet access needed │
├─────────────────────────────────────┤
│  Layer 4: Access Control            │
│  - IAM credentials for S3 access    │
│  - Unlock code (separate channel)   │
│  - Manifest file (separate channel) │
└─────────────────────────────────────┘
```

### Unlock Process
```
To access a Snowball Edge device, you need ALL THREE:

1. Device itself (physical possession)
2. Manifest file (downloaded from AWS console)
3. Unlock code (displayed in AWS console, never on device)

Without all three → device is a paperweight
```

---

## 1.6 Networking on Snowball Edge

### Available Network Interfaces

| Interface | Speed | Connector | Use Case |
|-----------|-------|-----------|----------|
| RJ45 | 1/10 GbE | Standard Ethernet | General connectivity |
| SFP+ | 10 GbE | Fiber optic | High-speed data transfer |
| SFP28 | 25 GbE | Fiber optic | Maximum throughput |
| QSFP+ | 40 GbE | Fiber optic | Cluster interconnect |

### IP Configuration
```bash
# Static IP assignment
snowballEdge configure-network \
  --physical-network-interface-id s.ni-12345 \
  --static-ip-address-configuration '{
    "IpAddress": "192.168.1.100",
    "Netmask": "255.255.255.0"
  }'

# DHCP (default)
snowballEdge configure-network \
  --physical-network-interface-id s.ni-12345 \
  --dhcp
```

### Virtual Network Interfaces (VNIs)
```
Physical NIC → Multiple VNIs
  ├── VNI for S3 endpoint (192.168.1.101)
  ├── VNI for EC2 instance (192.168.1.102)
  ├── VNI for NFS endpoint (192.168.1.103)
  └── VNI for management (192.168.1.100)
```

---

## 1.7 Snowball Edge Job Types

| Job Type | Direction | Description |
|----------|-----------|-------------|
| **IMPORT** | On-prem → AWS | Transfer data to S3/Glacier |
| **EXPORT** | AWS → On-prem | Copy data from S3 to device |
| **LOCAL_USE** | On-site only | Edge compute, no data transfer |

### Job Lifecycle
```
ORDER → PREPARING → IN_TRANSIT_TO_YOU → DELIVERED → 
IN_USE → IN_TRANSIT_TO_AWS → AT_AWS → COMPLETE
```

---

## Next: [Part 2 - Setup & Data Transfer →](Part2_Setup_and_Data_Transfer.md)
