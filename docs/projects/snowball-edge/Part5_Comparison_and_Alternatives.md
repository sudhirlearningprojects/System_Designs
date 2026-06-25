# Part 5: Comparison & Alternatives

## 5.1 Comprehensive Feature Comparison

### Data Transfer Appliances

| Feature | AWS Snowball Edge | Azure Data Box | Azure Data Box Heavy | GCP Transfer Appliance |
|---------|:-----------------:|:--------------:|:-------------------:|:---------------------:|
| **Raw Capacity** | 80TB / 210TB | 100TB | 1 PB | 300TB (TA480) |
| **Usable Capacity** | 80TB / 210TB | 80TB | 770TB | 200TB |
| **Edge Compute** | ✅ Full (Lambda, EC2, EKS) | ❌ None | ❌ None | ❌ None |
| **GPU Support** | ✅ NVIDIA V100 | ❌ | ❌ | ❌ |
| **Clustering** | ✅ 5-16 devices | ❌ | ❌ | ❌ |
| **Local S3 API** | ✅ Full S3 compatible | ❌ | ❌ | ❌ |
| **NFS/SMB** | ✅ NFS | ✅ SMB 3.0 + NFS | ✅ SMB + NFS | ✅ NFS |
| **Block Storage** | ✅ iSCSI | ❌ | ❌ | ❌ |
| **Encryption** | AES-256 (KMS managed) | AES-256 (BitLocker) | AES-256 | AES-256 |
| **Tamper Evidence** | ✅ TPM + E-ink | ✅ Tamper-evident | ✅ | ✅ |
| **Form Factor** | Ruggedized suitcase | Ruggedized box | Rack-mountable | Rack-mountable |
| **Weight** | 50 lbs | 50 lbs | 500 lbs | ~340 lbs |
| **Network Ports** | 10/25/40 GbE | 1/10 GbE | 4× 40 GbE | 10/40 GbE |
| **Shipping** | 5-7 days | 1-5 days | 1-5 days | Weeks (limited regions) |
| **Online Portal** | ✅ AWS Console | ✅ Azure Portal | ✅ Azure Portal | ✅ GCP Console |
| **GUI Management** | ✅ OpsHub | ✅ Local Web UI | ✅ Local Web UI | ❌ |
| **Data Wipe** | ✅ NIST 800-88 | ✅ NIST 800-88 | ✅ NIST 800-88 | ✅ |

---

## 5.2 Edge Compute Alternatives (Full Comparison)

When you need BOTH data transfer AND compute at the edge:

| Feature | AWS Snowball Edge | Azure Stack Edge | GCP Distributed Cloud | AWS Outposts |
|---------|:-----------------:|:----------------:|:--------------------:|:------------:|
| **Type** | Portable appliance | Fixed appliance | Software-defined | Fixed rack |
| **Portability** | ✅ Shippable | ❌ Fixed install | ❌ Fixed install | ❌ Fixed install |
| **Compute** | EC2, Lambda, EKS | VMs, Kubernetes, IoT | Kubernetes, Anthos | Full AWS APIs |
| **GPU** | ✅ V100 | ✅ T4 / FPGA | ❌ | ✅ Various |
| **Storage** | 80-210TB local | 1-12TB local | Varies | EBS, S3 |
| **ML/AI** | SageMaker Neo, Greengrass | Azure ML, Cognitive Services | AI Platform | SageMaker |
| **Kubernetes** | EKS Anywhere | AKS Edge | GKE Enterprise | EKS |
| **IoT** | IoT Greengrass | IoT Edge | Cloud IoT | IoT Core |
| **Connectivity** | Fully disconnected OK | Needs periodic connection | Needs connection | Needs connection |
| **Pricing Model** | Per-job + daily rate | Monthly subscription | Subscription | Monthly |
| **Min Commitment** | None (per job) | 3-year term typical | Annual | 3-year term |
| **Best For** | Remote/disconnected + data migration | Long-term edge site | Multi-cloud edge | AWS in your DC |

### Decision Matrix

```
                    Need Portability?
                    /              \
                  YES               NO
                  /                   \
        Need Compute?          Need Full Cloud APIs?
        /          \           /              \
      YES          NO        YES              NO
      /              \        /                \
  Snowball      Data Box   Outposts/     Azure Stack Edge/
  Edge          (Azure)    Stack Edge    GCP Distributed Cloud
                GCP TA
```

---

## 5.3 Cost Comparison Deep Dive

### Scenario 1: 100TB One-Time Migration

| Service | Service Fee | Shipping | Transfer Time | Total Cost |
|---------|-----------|----------|---------------|-----------|
| **AWS Snowball Edge** | $300 | ~$200 (round trip) | 7-10 days | ~$500 |
| **Azure Data Box** | $0 | ~$500 (round trip) | 5-7 days | ~$500 |
| **GCP Transfer Appliance** | Contact sales | Included | 2-4 weeks | ~$300-600 |
| **Network (1 Gbps dedicated)** | $0 | N/A | 9.3 days | $9,000+ (egress) |
| **Network (10 Gbps dedicated)** | $0 | N/A | 22 hours | $9,000+ (egress) |

**Note**: Network transfer costs assume AWS egress pricing (~$0.09/GB). For migration INTO cloud, ingress is free, but you need available bandwidth.

### Scenario 2: Monthly Recurring Collection (50TB/month)

| Approach | Monthly Cost | Annual Cost | Notes |
|----------|-------------|-------------|-------|
| **Snowball Edge (rotating)** | $600-900 | $7,200-10,800 | 2 devices rotating |
| **Azure Data Box (recurring)** | ~$1,000 | ~$12,000 | New device each cycle |
| **Direct Connect (1 Gbps)** | $2,700 | $32,400 | Port + data transfer |
| **Site-to-Site VPN** | ~$500 + egress | ~$15,000 | Limited bandwidth |
| **Azure Stack Edge** | ~$2,100 | ~$25,200 | Monthly subscription |

### Scenario 3: Edge Computing (LOCAL_USE - No data transfer)

| Service | Monthly Cost | Compute | Storage | GPU |
|---------|-------------|---------|---------|-----|
| **Snowball Edge (Local Use)** | ~$900 (30-day rental) | 104 vCPU | 210TB | Optional |
| **Azure Stack Edge Pro** | ~$2,100 | 20 vCPU | 12TB | T4 |
| **Azure Stack Edge Mini** | ~$1,200 | 16 vCPU | 2TB | ❌ |
| **AWS Outposts (1U)** | ~$5,000+ | Variable | Variable | Variable |
| **On-prem server** | $500-1000 (amortized) | Custom | Custom | Custom |

### Cost Calculator

```python
"""
Snowball Edge cost calculator for different scenarios.
"""

def calculate_snowball_edge_cost(
    data_tb: float,
    days_on_site: int = 10,
    compute_needed: bool = False,
    num_devices: int = 1,
    shipping_speed: str = 'SECOND_DAY'
):
    """Calculate total cost for a Snowball Edge job"""
    
    # Base service fee
    service_fee = 300  # Per job
    
    # Daily usage fee (beyond first 10 days)
    FREE_DAYS = 10
    daily_rate = 30  # Per device per day
    extra_days = max(0, days_on_site - FREE_DAYS)
    daily_charges = extra_days * daily_rate * num_devices
    
    # Shipping (varies by region and speed)
    shipping_costs = {
        'STANDARD': 100,      # ~5-7 days
        'SECOND_DAY': 200,    # 2 days
        'NEXT_DAY': 350       # Next business day
    }
    shipping = shipping_costs.get(shipping_speed, 200) * 2  # Round trip
    
    # S3 data transfer to cloud (free for import jobs)
    s3_transfer = 0
    
    # Compute optimized adds ~$100/job
    compute_surcharge = 100 if compute_needed else 0
    
    total = (service_fee + daily_charges + shipping + compute_surcharge) * num_devices
    
    # Cost per TB
    cost_per_tb = total / data_tb if data_tb > 0 else 0
    
    return {
        'total_cost': total,
        'cost_per_tb': round(cost_per_tb, 2),
        'breakdown': {
            'service_fee': service_fee * num_devices,
            'daily_charges': daily_charges,
            'shipping': shipping * num_devices,
            'compute_surcharge': compute_surcharge * num_devices
        }
    }


def calculate_network_transfer_cost(data_tb: float, bandwidth_gbps: float = 1.0):
    """Calculate cost of transferring same data over network"""
    
    # Transfer time
    data_gb = data_tb * 1024
    transfer_seconds = (data_gb * 8) / bandwidth_gbps  # seconds
    transfer_days = transfer_seconds / 86400
    
    # AWS data transfer pricing (into AWS is free, but bandwidth has opportunity cost)
    # If you're paying for Direct Connect:
    direct_connect_port = 220  # Per month for 1 Gbps
    direct_connect_transfer = data_gb * 0.02  # Per GB data transfer
    
    # Or if using internet:
    # Ingress is free, but you're consuming bandwidth
    bandwidth_cost_monthly = 1000  # Estimated ISP cost for dedicated 1Gbps
    bandwidth_fraction = transfer_days / 30
    
    return {
        'transfer_days': round(transfer_days, 1),
        'direct_connect_cost': round(direct_connect_port + direct_connect_transfer, 2),
        'internet_bandwidth_cost': round(bandwidth_cost_monthly * bandwidth_fraction, 2)
    }


# Examples
print("=== 100TB Migration ===")
snowball = calculate_snowball_edge_cost(100, days_on_site=10, num_devices=1)
network = calculate_network_transfer_cost(100, bandwidth_gbps=1.0)
print(f"Snowball Edge: ${snowball['total_cost']} ({snowball['cost_per_tb']}/TB)")
print(f"Network (1Gbps): {network['transfer_days']} days, ${network['direct_connect_cost']}")

print("\n=== 500TB Migration (cluster) ===")
snowball = calculate_snowball_edge_cost(500, days_on_site=14, num_devices=3)
network = calculate_network_transfer_cost(500, bandwidth_gbps=10.0)
print(f"Snowball Edge: ${snowball['total_cost']} ({snowball['cost_per_tb']}/TB)")
print(f"Network (10Gbps): {network['transfer_days']} days, ${network['direct_connect_cost']}")
```

---

## 5.4 Azure Deep Dive - Comparable Services

### Azure Data Box Family

```
┌─────────────────────────────────────────────────────────┐
│              Azure Data Box Family                        │
│                                                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │ Data Box    │  │ Data Box    │  │ Data Box Heavy  │ │
│  │ Disk        │  │ (Standard)  │  │                 │ │
│  │             │  │             │  │                 │ │
│  │ 8TB/disk    │  │ 100TB       │  │ 1 PB            │ │
│  │ Up to 5     │  │ Single unit │  │ Rack-mounted    │ │
│  │ disks (40TB)│  │ Portable    │  │ Heavy (500 lbs) │ │
│  │             │  │             │  │                 │ │
│  │ Best for:   │  │ Best for:   │  │ Best for:       │ │
│  │ <40TB       │  │ 40-100TB    │  │ 100TB-1PB       │ │
│  └─────────────┘  └─────────────┘  └─────────────────┘ │
│                                                           │
│  NONE have edge compute capability!                      │
│                                                           │
│  For edge compute: Azure Stack Edge (separate product)   │
└─────────────────────────────────────────────────────────┘
```

### Azure Stack Edge (Compute at Edge)

| Variant | Compute | GPU | Storage | Monthly Cost |
|---------|---------|-----|---------|-------------|
| **Pro - GPU** | 40 vCPU, 128 GB | 1-2× T4 | 4.27 TB | ~$3,500 |
| **Pro - FPGA** | 20 vCPU, 64 GB | Intel FPGA | 1 TB | ~$2,800 |
| **Pro R** (Ruggedized) | 20 vCPU, 64 GB | Optional | 0.5 TB | ~$2,100 |
| **Mini R** | 16 vCPU, 48 GB | ❌ | 0.5 TB | ~$1,200 |

**Key Differences from Snowball Edge:**
- Azure Stack Edge is a **subscription service** (installed permanently)
- **Not portable** - designed for fixed edge locations
- **Requires periodic internet** connectivity for billing/management
- Supports Azure VMs, AKS, Azure IoT Edge
- Lower storage capacity but designed for long-term use

### Azure Workflow Comparison

```bash
# Azure Data Box ordering (Azure CLI)
az databox job create \
  --resource-group myRG \
  --name "factory-data-transfer" \
  --location eastus \
  --sku DataBox \
  --contact-name "John Smith" \
  --phone "+1-555-0100" \
  --email "john@example.com" \
  --street-address-1 "123 Industrial Blvd" \
  --city "Houston" \
  --state-or-province "TX" \
  --postal-code "77001" \
  --country "US" \
  --storage-accounts "/subscriptions/.../storageAccounts/mystorage"

# Azure Data Box uses SMB/NFS for data copy (no S3 API!)
# Connect via SMB share:
net use \\10.126.76.172\mystorageaccount_BlockBob /u:mystorageaccount
# Then copy files like a network share
robocopy D:\data \\10.126.76.172\mystorageaccount_BlockBob\container1 /E
```

---

## 5.5 GCP Deep Dive - Comparable Services

### GCP Transfer Appliance

```
┌─────────────────────────────────────────────────────────┐
│              GCP Transfer Appliance                       │
│                                                           │
│  ┌──────────────────────┐  ┌──────────────────────────┐ │
│  │  TA40 (40TB)         │  │  TA300 (300TB)           │ │
│  │                      │  │                          │ │
│  │  • Rackable 1U       │  │  • Rackable              │ │
│  │  • 40TB usable       │  │  • 300TB usable          │ │
│  │  • AES-256           │  │  • AES-256               │ │
│  │  • No compute        │  │  • No compute            │ │
│  │                      │  │                          │ │
│  │  Best for:           │  │  Best for:               │ │
│  │  <40TB migrations    │  │  Large data center       │ │
│  │                      │  │  migrations              │ │
│  └──────────────────────┘  └──────────────────────────┘ │
│                                                           │
│  Limitations:                                            │
│  • No edge compute                                       │
│  • Limited regional availability                         │
│  • Longer lead times (weeks vs days)                     │
│  • Must contact Google sales for access                  │
│  • Linux-only data capture tool                          │
└─────────────────────────────────────────────────────────┘
```

### GCP Edge Options (For Compute)

| Service | Description | Compared to |
|---------|-------------|-------------|
| **Google Distributed Cloud Edge** | GKE at the edge (5G/telco) | AWS Wavelength |
| **Google Distributed Cloud Connected** | GKE on your hardware | AWS Outposts |
| **Coral (Edge TPU)** | Tiny ML inference device | AWS IoT Greengrass + SageMaker |

**Key Insight**: GCP doesn't have a direct Snowball Edge equivalent that combines portability + compute + large storage. You'd need separate solutions for transfer and edge compute.

### GCP Transfer Appliance Workflow

```bash
# GCP Transfer Appliance workflow (using gcloud)

# 1. Order through Google Cloud Console (must contact sales)

# 2. When device arrives, connect and capture data
# Install the data capture application
sudo apt-get install google-transfer-appliance

# 3. Mount the appliance
ta mount --ip 192.168.1.50

# 4. Copy data (NFS mount)
sudo mount -t nfs 192.168.1.50:/export /mnt/transfer-appliance
cp -r /data/factory-output/* /mnt/transfer-appliance/

# 5. Finalize and ship back
ta finalize
ta unmount
# Ship device back to Google
```

---

## 5.6 Decision Framework

### When to Choose AWS Snowball Edge

✅ **Choose Snowball Edge when:**
- You need edge compute AND data transfer in one device
- Operating in truly disconnected environments (offshore, remote)
- You need GPU for ML inference at the edge
- You want S3-compatible API at the edge
- You need clustering for HA or large capacity
- You're already in the AWS ecosystem
- Short-term or per-job basis (no long-term commitment)

❌ **Don't choose Snowball Edge when:**
- You only need pure data transfer (Data Box may be cheaper/faster)
- You need < 10TB transferred (use Direct Connect or internet)
- You need a permanent edge solution (consider Outposts or Stack Edge)
- You need 24/7 cloud connectivity at the edge

### When to Choose Azure Data Box

✅ **Choose Azure Data Box when:**
- Pure data migration to Azure (no compute needed)
- Faster regional shipping is critical
- You're in the Azure ecosystem
- Simple SMB/NFS copy workflow preferred
- Need up to 1PB in single job (Data Box Heavy)

❌ **Don't choose Azure Data Box when:**
- You need to process data on the device
- You need S3-compatible API
- You need the device for extended periods

### When to Choose Azure Stack Edge

✅ **Choose Azure Stack Edge when:**
- You need permanent edge compute at a fixed location
- You need Azure-consistent APIs at the edge
- You have reliable (periodic) internet connectivity
- Long-term deployment (3+ year commitment)
- You need Azure ML or Cognitive Services at edge

### When to Choose GCP Transfer Appliance

✅ **Choose GCP Transfer Appliance when:**
- Very large migrations (300TB+) to GCP
- GCS/BigQuery as destination
- You're already in the GCP ecosystem

❌ **Don't choose GCP Transfer Appliance when:**
- You need edge compute
- You need quick turnaround (lead times are longer)
- You're outside GCP's supported regions

### Quick Decision Flowchart

```
START: I need to handle large data at a remote/edge location
│
├── Do I need compute at the edge?
│   ├── YES → Is the site disconnected?
│   │         ├── YES → AWS Snowball Edge ✅
│   │         └── NO  → Is it permanent?
│   │                   ├── YES → AWS Outposts / Azure Stack Edge
│   │                   └── NO  → AWS Snowball Edge ✅
│   │
│   └── NO → Pure data transfer
│            ├── Target is AWS?
│            │   ├── <10TB → Use network (Direct Connect / S3 Transfer Acceleration)
│            │   ├── 10-80TB → AWS Snowball Edge (Storage Optimized)
│            │   └── >80TB → Snowball Edge cluster or AWS Snowcone fleet
│            │
│            ├── Target is Azure?
│            │   ├── <40TB → Azure Data Box Disk
│            │   ├── 40-100TB → Azure Data Box
│            │   └── >100TB → Azure Data Box Heavy
│            │
│            └── Target is GCP?
│                ├── <40TB → GCP TA40
│                └── >40TB → GCP TA300 (contact sales)
```

---

## 5.7 Multi-Cloud Strategy

If operating across multiple clouds, consider this architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                  Multi-Cloud Edge Strategy                    │
│                                                               │
│  Remote Site:                                                 │
│  ┌─────────────────────────────────────────────┐             │
│  │  AWS Snowball Edge                           │             │
│  │  (Edge compute + data collection)            │             │
│  │  ├── Process data locally                    │             │
│  │  ├── Run ML inference                        │             │
│  │  └── Store 210TB raw data                    │             │
│  └─────────────────────────────────────────────┘             │
│                                                               │
│  Cloud Tier (multi-cloud):                                    │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐   │
│  │  AWS S3       │  │  Azure Blob   │  │  GCS          │   │
│  │  (Primary)    │  │  (DR/Backup)  │  │  (Analytics)  │   │
│  │               │  │               │  │               │   │
│  │  • Raw data   │  │  • Replicated │  │  • BigQuery   │   │
│  │  • ML models  │  │  • Compliance │  │  • Vertex AI  │   │
│  │  • Archives   │  │  • Geo-redun  │  │  • Looker     │   │
│  └───────────────┘  └───────────────┘  └───────────────┘   │
│                                                               │
│  Data Flow:                                                   │
│  Snowball Edge → S3 → Cross-cloud replication                │
└─────────────────────────────────────────────────────────────┘
```

---

## 5.8 Summary Scorecard

| Criteria (1-5 scale) | AWS Snowball Edge | Azure Data Box | Azure Stack Edge | GCP Transfer Appliance |
|----------------------|:-----------------:|:--------------:|:----------------:|:---------------------:|
| **Storage Capacity** | 5 (210TB) | 4 (100TB) | 2 (12TB) | 5 (300TB) |
| **Edge Compute** | 5 | 1 | 4 | 1 |
| **Portability** | 5 | 5 | 2 | 3 |
| **Ease of Use** | 4 | 5 | 3 | 3 |
| **Shipping Speed** | 3 | 5 | N/A | 2 |
| **Cost (short-term)** | 4 | 5 | 2 | 3 |
| **Cost (long-term)** | 3 | 3 | 4 | 3 |
| **Disconnected Ops** | 5 | 2 | 3 | 1 |
| **ML/AI Support** | 5 | 1 | 4 | 1 |
| **Ecosystem Integration** | 5 (AWS) | 5 (Azure) | 5 (Azure) | 5 (GCP) |
| **Regional Availability** | 4 | 5 | 4 | 3 |

**Overall Winner by Use Case:**
- **Data Migration Only**: Azure Data Box (simplest, fastest shipping)
- **Edge Compute + Storage**: AWS Snowball Edge (only real option)
- **Permanent Edge Site**: Azure Stack Edge (subscription, long-term)
- **Massive Migration (>500TB)**: GCP Transfer Appliance or Snowball cluster

---

## Next: [Part 6 - Local Simulation & Testing →](Part6_Local_Simulation_and_Testing.md)
