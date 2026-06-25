# Part 4: Advanced Features

## 4.1 Snowball Edge Clustering

Combine multiple Snowball Edge devices into a cluster for increased storage capacity and high availability.

### Cluster Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    Snowball Edge Cluster                       │
│                    (3-16 devices)                              │
│                                                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │   Node 1     │  │   Node 2     │  │   Node 3     │       │
│  │  (Leader)    │  │  (Follower)  │  │  (Follower)  │       │
│  │              │  │              │  │              │       │
│  │  S3: 210TB   │  │  S3: 210TB   │  │  S3: 210TB   │       │
│  │  EC2: ✅     │  │  EC2: ✅     │  │  EC2: ✅     │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                  │                  │               │
│         └──────────────────┼──────────────────┘               │
│                            │                                   │
│                   ┌────────▼────────┐                         │
│                   │  Cluster S3     │                         │
│                   │  Endpoint       │                         │
│                   │  (Unified view) │                         │
│                   │  Total: 630TB   │                         │
│                   └─────────────────┘                         │
│                                                                │
│  Features:                                                     │
│  • Single S3 endpoint for all nodes                           │
│  • Data striped across nodes for performance                  │
│  • Quorum-based durability (survives 1 node failure)          │
│  • Leader election for coordination                           │
└──────────────────────────────────────────────────────────────┘
```

### Cluster Configuration

```bash
# Order a cluster job (5 devices)
aws snowball create-cluster \
  --job-type LOCAL_USE \
  --resources '{
    "S3Resources": [{"BucketArn": "arn:aws:s3:::cluster-storage"}]
  }' \
  --address-id ADID1234567890 \
  --role-arn arn:aws:iam::123456789012:role/SnowballClusterRole \
  --snowball-type EDGE \
  --shipping-option SECOND_DAY \
  --description "Factory Alpha - 5 node cluster"

# Cluster ships as 5 individual devices
# All must be unlocked and connected to same network
```

### Setting Up the Cluster

```bash
# Step 1: Unlock each device individually
for ip in 192.168.1.101 192.168.1.102 192.168.1.103 192.168.1.104 192.168.1.105; do
  snowballEdge unlock-device \
    --endpoint https://$ip \
    --manifest-file /path/to/manifest-$ip.bin \
    --unlock-code XXXXX-XXXXX-XXXXX-XXXXX
done

# Step 2: Associate devices into cluster
snowballEdge associate-device \
  --endpoint https://192.168.1.101 \
  --device-ip-addresses 192.168.1.102 192.168.1.103 192.168.1.104 192.168.1.105

# Step 3: Verify cluster status
snowballEdge describe-cluster --endpoint https://192.168.1.101

# Output shows:
# - Cluster state: ACTIVE
# - Number of nodes: 5
# - Available storage: ~1.05 PB
# - S3 endpoint: https://192.168.1.101:8443 (leader)
```

### Cluster S3 Operations

```python
import boto3

# Connect to cluster S3 (use leader node IP)
s3 = boto3.client(
    's3',
    endpoint_url='https://192.168.1.101:8443',
    aws_access_key_id='CLUSTER_ACCESS_KEY',
    aws_secret_access_key='CLUSTER_SECRET_KEY',
    verify=False
)

# Data is automatically distributed across nodes
# Read from any node, write through the leader
s3.put_object(
    Bucket='cluster-storage',
    Key='large-dataset/part-001.parquet',
    Body=open('/data/part-001.parquet', 'rb')
)

# Cluster handles replication internally
# Default: data written to quorum (n/2 + 1) nodes before ACK
```

### Cluster Failure Handling

```
Scenario: 5-node cluster, 1 node fails

┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐
│ N1  │ │ N2  │ │ N3  │ │ N4  │ │ N5  │
│ ✅  │ │ ✅  │ │ ❌  │ │ ✅  │ │ ✅  │
└─────┘ └─────┘ └─────┘ └─────┘ └─────┘

Result:
- Cluster continues operating (quorum = 3 of 5)
- Data on N3 reconstructed from parity/replicas
- Performance may degrade slightly
- No data loss (durability guaranteed with quorum)

If leader (N1) fails:
- Automatic leader election among remaining nodes
- New leader takes over S3 endpoint
- Brief interruption (~30 seconds)
```

---

## 4.2 AWS OpsHub Management

AWS OpsHub is a **GUI application** for managing Snowball Edge devices without CLI.

### OpsHub Capabilities

```
┌───────────────────────────────────────────────────────┐
│                   AWS OpsHub                            │
│                                                         │
│  ┌─────────────────┐  ┌─────────────────┐             │
│  │ Device Manager  │  │ File Manager    │             │
│  │ • Unlock device │  │ • Browse S3     │             │
│  │ • View status   │  │ • Upload files  │             │
│  │ • Network config│  │ • Download files│             │
│  └─────────────────┘  └─────────────────┘             │
│                                                         │
│  ┌─────────────────┐  ┌─────────────────┐             │
│  │ Compute Manager │  │ Monitoring      │             │
│  │ • Launch EC2    │  │ • CPU usage     │             │
│  │ • Manage Lambda │  │ • Storage used  │             │
│  │ • View logs     │  │ • Network I/O   │             │
│  └─────────────────┘  └─────────────────┘             │
│                                                         │
│  ┌─────────────────┐  ┌─────────────────┐             │
│  │ NFS Manager     │  │ Cluster View    │             │
│  │ • Start/stop NFS│  │ • Node status   │             │
│  │ • Mount config  │  │ • Add/remove    │             │
│  └─────────────────┘  └─────────────────┘             │
└───────────────────────────────────────────────────────┘
```

### Installation

```bash
# Download OpsHub
# Available for: Windows, macOS, Linux
# https://aws.amazon.com/snowball/resources/

# macOS
brew install --cask aws-opshub

# Linux (AppImage)
wget https://d2iqc2vn7bujks.cloudfront.net/snow-opshub-linux-amd64.AppImage
chmod +x snow-opshub-linux-amd64.AppImage
./snow-opshub-linux-amd64.AppImage
```

### Key OpsHub Workflows

| Task | CLI Equivalent | OpsHub Advantage |
|------|---------------|------------------|
| Unlock device | `snowballEdge unlock-device` | Visual progress, no typos |
| Browse files | `aws s3 ls` | Drag-and-drop upload |
| Launch EC2 | `aws ec2 run-instances` | Template-based, visual |
| Check storage | `describe-device` | Real-time dashboard |
| Transfer files | `aws s3 sync` | Progress bar, pause/resume |
| View logs | SSH + journalctl | Integrated log viewer |

---

## 4.3 Operational Patterns

### Pattern 1: Rotating Device Model

For continuous data collection at remote sites:

```
Week 1-2: Device A on-site collecting data
Week 3:   Device A shipped to AWS, Device B deployed
Week 4:   Device B collecting, Device A data ingested to S3
Week 5:   Device B shipped to AWS, Device A returned clean
...

Timeline:
┌───────────────────────────────────────────────┐
│ Device A: [Collect]──[Ship]──[Ingest]──[Return]──[Collect]...
│ Device B:           [Collect]──[Ship]──[Ingest]──[Return]...
└───────────────────────────────────────────────┘

Result: Continuous data collection with no gaps
```

### Pattern 2: Edge-Cloud Hybrid Processing

```python
"""
Hybrid processing: Edge handles real-time, cloud handles deep analytics.
"""

class HybridProcessor:
    """
    Edge processing tier:
    - Real-time anomaly detection (< 100ms latency)
    - Data filtering (reduce 90% noise)
    - Local alerting
    
    Cloud processing tier (after device ships):
    - Deep learning model training
    - Historical trend analysis
    - Cross-site correlation
    """
    
    def __init__(self):
        self.edge_model = load_lightweight_model()  # Small, fast model
        self.alert_threshold = 0.8
        self.noise_threshold = 0.1
    
    def process_at_edge(self, reading):
        """Real-time edge processing"""
        # Step 1: Filter noise
        if reading['signal_strength'] < self.noise_threshold:
            return None  # Discard noise
        
        # Step 2: Quick anomaly check
        score = self.edge_model.predict(reading)
        
        # Step 3: Categorize and route
        if score > self.alert_threshold:
            self.trigger_local_alert(reading, score)
            self.store_in_alerts_bucket(reading)
        
        # Step 4: Store for cloud processing
        self.store_in_raw_bucket(reading)
        
        return {'score': score, 'stored': True}
    
    def trigger_local_alert(self, reading, score):
        """Alert local operators immediately"""
        # Could light up a physical alarm, send local notification, etc.
        pass
```

### Pattern 3: Disconnected Operations Playbook

```
┌─────────────────────────────────────────────────────────────┐
│           Disconnected Operations Playbook                    │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  PRE-DEPLOYMENT (Connected):                                  │
│  □ Load latest ML models                                     │
│  □ Update Lambda function code                               │
│  □ Configure EC2 instances                                   │
│  □ Set up monitoring scripts                                 │
│  □ Test all services locally                                 │
│                                                               │
│  ON-SITE (Disconnected):                                      │
│  □ Connect to local network                                  │
│  □ Verify all services running (health checks)               │
│  □ Start data ingestion pipeline                             │
│  □ Monitor via OpsHub (local dashboard)                      │
│  □ Check storage capacity daily                              │
│  □ Rotate logs to prevent disk fill                          │
│                                                               │
│  RETURN SHIPPING:                                             │
│  □ Stop data ingestion gracefully                            │
│  □ Verify data integrity (checksums)                         │
│  □ Document any issues in metadata                           │
│  □ Power off device                                          │
│  □ Schedule shipping pickup                                  │
│                                                               │
│  POST-RETURN (Cloud):                                         │
│  □ Verify data in S3 (count objects, spot check)             │
│  □ Trigger cloud analytics pipeline                          │
│  □ Update ML models with new data                            │
│  □ Prepare next device for deployment                        │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Pattern 4: Multi-Site Fleet Management

```
┌─────────────────────────────────────────────────────────────┐
│                  Central Operations (AWS Cloud)                │
│                                                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │ S3 Data  │  │ Analytics│  │ Model    │                  │
│  │ Lake     │  │ Pipeline │  │ Training │                  │
│  └──────────┘  └──────────┘  └──────────┘                  │
└───────────────────────┬─────────────────────────────────────┘
                        │ (Physical shipping)
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│  Site Alpha   │ │  Site Beta    │ │  Site Gamma   │
│  (Factory)    │ │  (Mine)       │ │  (Oil Rig)    │
│               │ │               │ │               │
│  2× Snowball  │ │  3× Snowball  │ │  5× Snowball  │
│  Edge devices │ │  Edge cluster │ │  Edge cluster │
│               │ │               │ │               │
│  50TB/month   │ │  200TB/month  │ │  500TB/month  │
└───────────────┘ └───────────────┘ └───────────────┘
```

---

## 4.4 Monitoring and Alerting (On-Device)

Since the device may be disconnected, monitoring must be local.

### Health Check Script

```python
"""
Local health monitoring for Snowball Edge.
Runs as a systemd service on an EC2 instance on the device.
"""
import boto3
import json
import time
import subprocess
import logging
from datetime import datetime

logging.basicConfig(level=logging.INFO, filename='/var/log/edge-health.log')
logger = logging.getLogger(__name__)

S3_ENDPOINT = 'https://192.168.1.100:8443'
DEVICE_ENDPOINT = 'https://192.168.1.100'
HEALTH_BUCKET = 'edge-health-metrics'

s3 = boto3.client('s3', endpoint_url=S3_ENDPOINT,
                  aws_access_key_id='KEY', aws_secret_access_key='SECRET',
                  verify=False)


def check_storage_capacity():
    """Check remaining storage on device"""
    result = subprocess.run(
        ['snowballEdge', 'describe-device', '--endpoint', DEVICE_ENDPOINT],
        capture_output=True, text=True
    )
    device_info = json.loads(result.stdout)
    
    total_bytes = device_info.get('StorageCapacity', {}).get('Total', 0)
    used_bytes = device_info.get('StorageCapacity', {}).get('Used', 0)
    available_pct = ((total_bytes - used_bytes) / total_bytes) * 100 if total_bytes else 0
    
    return {
        'total_tb': total_bytes / (1024**4),
        'used_tb': used_bytes / (1024**4),
        'available_pct': round(available_pct, 2),
        'alert': available_pct < 10  # Alert if less than 10% free
    }


def check_services():
    """Check all Snowball Edge services are running"""
    services = ['s3', 'ec2', 'lambda', 'nfs']
    status = {}
    
    for service in services:
        try:
            result = subprocess.run(
                ['snowballEdge', 'describe-service',
                 '--endpoint', DEVICE_ENDPOINT,
                 '--service-id', service],
                capture_output=True, text=True, timeout=10
            )
            status[service] = 'ACTIVE' if result.returncode == 0 else 'FAILED'
        except subprocess.TimeoutExpired:
            status[service] = 'TIMEOUT'
    
    return status


def check_ec2_instances():
    """Verify EC2 instances are running"""
    ec2 = boto3.client('ec2', endpoint_url='http://192.168.1.100:8008',
                       aws_access_key_id='KEY', aws_secret_access_key='SECRET',
                       region_name='snow')
    
    response = ec2.describe_instances()
    instances = []
    for reservation in response.get('Reservations', []):
        for instance in reservation.get('Instances', []):
            instances.append({
                'id': instance['InstanceId'],
                'state': instance['State']['Name'],
                'type': instance['InstanceType']
            })
    return instances


def collect_and_store_metrics():
    """Collect all metrics and store locally"""
    metrics = {
        'timestamp': datetime.utcnow().isoformat(),
        'storage': check_storage_capacity(),
        'services': check_services(),
        'ec2_instances': check_ec2_instances()
    }
    
    # Store metrics in local S3
    key = f"metrics/{datetime.utcnow().strftime('%Y/%m/%d/%H-%M-%S')}.json"
    s3.put_object(
        Bucket=HEALTH_BUCKET,
        Key=key,
        Body=json.dumps(metrics, indent=2)
    )
    
    # Check for alerts
    if metrics['storage']['alert']:
        logger.critical(f"LOW STORAGE: {metrics['storage']['available_pct']}% remaining")
    
    for svc, state in metrics['services'].items():
        if state != 'ACTIVE':
            logger.error(f"SERVICE DOWN: {svc} is {state}")
    
    return metrics


if __name__ == '__main__':
    while True:
        try:
            metrics = collect_and_store_metrics()
            logger.info(f"Health check OK - Storage: {metrics['storage']['available_pct']}%")
        except Exception as e:
            logger.error(f"Health check failed: {e}")
        
        time.sleep(60)  # Check every minute
```

---

## 4.5 Data Lifecycle Management

### On-Device Data Tiering

```python
"""
Manage data lifecycle on Snowball Edge to optimize limited storage.
"""
import boto3
import time
from datetime import datetime, timedelta

s3 = boto3.client('s3', endpoint_url='https://192.168.1.100:8443',
                  aws_access_key_id='KEY', aws_secret_access_key='SECRET',
                  verify=False)

BUCKET = 'factory-sensor-data'

# Storage tiers on device
TIERS = {
    'hot': 'raw/',           # Recent data, full resolution
    'warm': 'aggregated/',    # Older data, downsampled
    'cold': 'compressed/',    # Oldest data, compressed archives
}


def downsample_old_data(days_threshold=7):
    """Aggregate data older than threshold to save space"""
    cutoff = datetime.utcnow() - timedelta(days=days_threshold)
    
    # List raw data older than threshold
    paginator = s3.get_paginator('list_objects_v2')
    for page in paginator.paginate(Bucket=BUCKET, Prefix='raw/'):
        for obj in page.get('Contents', []):
            if obj['LastModified'].replace(tzinfo=None) < cutoff:
                # Read, aggregate, and move
                aggregate_and_archive(obj['Key'])


def aggregate_and_archive(key):
    """Aggregate hourly readings into daily summaries"""
    response = s3.get_object(Bucket=BUCKET, Key=key)
    data = json.loads(response['Body'].read())
    
    # Create daily summary (avg, min, max)
    summary_key = key.replace('raw/', 'aggregated/').replace('.json', '-summary.json')
    summary = {
        'original_key': key,
        'temperature_avg': data.get('temperature_c'),
        'aggregated_at': time.time()
    }
    
    s3.put_object(Bucket=BUCKET, Key=summary_key, Body=json.dumps(summary))
    
    # Delete raw data to free space
    s3.delete_object(Bucket=BUCKET, Key=key)


def check_capacity_and_cleanup(threshold_pct=80):
    """If storage exceeds threshold, aggressively clean old data"""
    # This would check device storage and trigger cleanup
    pass
```

---

## 4.6 Returning the Device

### Pre-Return Checklist

```bash
# 1. Verify all data is on device
aws s3 ls s3://factory-sensor-data/ --recursive --summarize \
  --profile snowball --endpoint-url https://192.168.1.100:8443

# 2. Stop all EC2 instances
aws ec2 stop-instances \
  --endpoint-url http://192.168.1.100:8008 \
  --profile snowball \
  --instance-ids s.i-01234567890abcdef

# 3. Unmount NFS
sudo umount /mnt/snowball

# 4. Power off (graceful shutdown)
snowballEdge shutdown --endpoint https://192.168.1.100

# 5. Disconnect cables, package device
# 6. Use prepaid E-ink shipping label (auto-displayed on device)
# 7. Schedule carrier pickup or drop at UPS/FedEx
```

### Post-Return Verification

```bash
# After AWS receives and processes the device:

# Check job status
aws snowball describe-job --job-id JID1234567890

# Verify data in S3
aws s3 ls s3://factory-sensor-data/ --recursive --summarize

# Check import completion report
aws snowball get-job-manifest --job-id JID1234567890
```

---

## Next: [Part 5 - Comparison & Alternatives →](Part5_Comparison_and_Alternatives.md)
