# Part 2: Setup & Data Transfer

## 2.1 Ordering a Snowball Edge Device

### Via AWS Console
1. Navigate to **AWS Snow Family** in the console
2. Click **Create Job**
3. Select job type: IMPORT / EXPORT / LOCAL_USE
4. Choose device: Snowball Edge Storage Optimized / Compute Optimized
5. Configure S3 buckets, Lambda functions, EC2 AMIs
6. Select shipping address and speed
7. Set IAM role and KMS key
8. Review and submit

### Via CLI

```bash
# Step 1: Create an address
aws snowball create-address --address '{
  "Name": "Factory Site Alpha",
  "Company": "Acme Corp",
  "Street1": "123 Industrial Blvd",
  "City": "Houston",
  "StateOrProvince": "TX",
  "PostalCode": "77001",
  "Country": "US",
  "PhoneNumber": "+1-555-0100"
}'

# Step 2: Create the job
aws snowball create-job \
  --job-type IMPORT \
  --resources '{
    "S3Resources": [
      {
        "BucketArn": "arn:aws:s3:::factory-sensor-data",
        "KeyRange": {}
      }
    ],
    "LambdaResources": [
      {
        "LambdaArn": "arn:aws:lambda:us-east-1:123456789:function:edge-processor"
      }
    ]
  }' \
  --address-id ADID1234567890 \
  --role-arn arn:aws:iam::123456789012:role/SnowballEdgeRole \
  --kms-key-arn arn:aws:kms:us-east-1:123456789012:key/abcd-1234 \
  --snowball-type EDGE \
  --shipping-option SECOND_DAY \
  --description "Factory Alpha monthly data collection"

# Step 3: Check job status
aws snowball describe-job --job-id JID1234567890
```

### IAM Role for Snowball Edge

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "importexport.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:GetBucketLocation",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::factory-sensor-data",
        "arn:aws:s3:::factory-sensor-data/*"
      ]
    }
  ]
}
```

---

## 2.2 Device Unlock & Initial Setup

### Step 1: Physical Connection
```
1. Unbox device, connect power cable
2. Connect network cable (RJ45 or SFP+)
3. Power on (front panel button)
4. Wait for boot sequence (~10 minutes)
5. Device gets IP via DHCP or use LCD to set static IP
```

### Step 2: Install Snowball Edge Client

```bash
# Download client
# Linux
wget https://snowball-client.s3.amazonaws.com/latest/snowball-client-linux.tar.gz
tar xzf snowball-client-linux.tar.gz

# macOS
brew install snowball-edge-client

# Verify installation
snowballEdge version
```

### Step 3: Get Credentials from AWS Console

```bash
# Download manifest file (JSON) from AWS Console → Snow Family → Job Details
# Note the unlock code displayed in the console

# These two items + physical device = complete access
```

### Step 4: Unlock Device

```bash
# Unlock the device
snowballEdge unlock-device \
  --endpoint https://192.168.1.100 \
  --manifest-file /path/to/manifest.bin \
  --unlock-code ABCDE-12345-FGHIJ-67890-KLMNO

# Verify device status
snowballEdge describe-device --endpoint https://192.168.1.100

# Expected output:
# {
#   "DeviceId": "JID1234567890",
#   "UnlockStatus": {
#     "State": "UNLOCKED"
#   },
#   "ActiveNetworkInterface": {
#     "IpAddress": "192.168.1.100"
#   }
# }
```

### Step 5: Get Local S3 Credentials

```bash
# List available S3 access keys
snowballEdge list-access-keys --endpoint https://192.168.1.100

# Get the secret key for the access key
snowballEdge get-secret-access-key \
  --endpoint https://192.168.1.100 \
  --access-key-id AKIA1234567890EXAMPLE

# Output:
# [snowballEdge]
# aws_access_key_id = AKIA1234567890EXAMPLE
# aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

# Configure AWS CLI profile
aws configure --profile snowball
# AWS Access Key ID: AKIA1234567890EXAMPLE
# AWS Secret Access Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
# Default region: snow
# Default output: json
```

---

## 2.3 S3-Compatible Data Transfer

### Basic S3 Operations

```bash
# Set endpoint variable
export SNOW_ENDPOINT=https://192.168.1.100:8443

# List buckets
aws s3 ls --profile snowball --endpoint-url $SNOW_ENDPOINT

# Create bucket (if LOCAL_USE job)
aws s3 mb s3://sensor-data --profile snowball --endpoint-url $SNOW_ENDPOINT

# Upload single file
aws s3 cp ./data.json s3://factory-sensor-data/raw/ \
  --profile snowball --endpoint-url $SNOW_ENDPOINT

# Upload directory (recursive)
aws s3 sync ./sensor-output/ s3://factory-sensor-data/2024/01/ \
  --profile snowball --endpoint-url $SNOW_ENDPOINT

# Download file from device
aws s3 cp s3://factory-sensor-data/processed/report.csv ./local/ \
  --profile snowball --endpoint-url $SNOW_ENDPOINT
```

### High-Performance Transfer with S3 Adapter

```bash
# Parallel upload with multipart (for large files)
aws s3 cp ./large-dataset.tar.gz s3://factory-sensor-data/ \
  --profile snowball \
  --endpoint-url $SNOW_ENDPOINT \
  --storage-class STANDARD \
  --metadata '{"source":"factory-alpha","timestamp":"2024-01-15"}' \
  --expected-size 107374182400

# Multi-part configuration for optimal throughput
aws configure set default.s3.multipart_threshold 64MB --profile snowball
aws configure set default.s3.multipart_chunksize 64MB --profile snowball
aws configure set default.s3.max_concurrent_requests 30 --profile snowball
```

### Python SDK for Programmatic Transfer

```python
import boto3
import json
import time
import os
from concurrent.futures import ThreadPoolExecutor
from botocore.config import Config

# Configure client with connection pooling
config = Config(
    max_pool_connections=50,
    retries={'max_attempts': 3, 'mode': 'adaptive'}
)

s3_client = boto3.client(
    's3',
    endpoint_url='https://192.168.1.100:8443',
    aws_access_key_id='AKIA1234567890EXAMPLE',
    aws_secret_access_key='wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY',
    verify=False,  # Self-signed cert on device
    config=config
)

BUCKET = 'factory-sensor-data'


def generate_sensor_reading(device_id, reading_id):
    """Simulate IoT sensor data"""
    import random
    return {
        "device_id": device_id,
        "reading_id": reading_id,
        "timestamp": time.time(),
        "temperature_c": round(random.uniform(20.0, 95.0), 2),
        "pressure_psi": round(random.uniform(10.0, 25.0), 2),
        "vibration_hz": round(random.uniform(0.0, 500.0), 2),
        "humidity_pct": round(random.uniform(30.0, 90.0), 1),
        "power_watts": round(random.uniform(100.0, 5000.0), 1)
    }


def upload_reading(args):
    """Upload a single sensor reading"""
    device_id, reading_id = args
    reading = generate_sensor_reading(device_id, reading_id)
    key = f"raw/{device_id}/{time.strftime('%Y/%m/%d')}/reading-{reading_id}.json"
    
    s3_client.put_object(
        Bucket=BUCKET,
        Key=key,
        Body=json.dumps(reading),
        ContentType='application/json',
        Metadata={
            'device-id': device_id,
            'ingestion-time': str(int(time.time()))
        }
    )
    return key


def bulk_ingest(num_devices=10, readings_per_device=1000):
    """Parallel bulk ingestion of sensor data"""
    tasks = [
        (f"device-{d:03d}", r)
        for d in range(num_devices)
        for r in range(readings_per_device)
    ]
    
    start = time.time()
    with ThreadPoolExecutor(max_workers=20) as executor:
        results = list(executor.map(upload_reading, tasks))
    
    elapsed = time.time() - start
    total = len(results)
    print(f"Uploaded {total} readings in {elapsed:.1f}s ({total/elapsed:.0f} objects/sec)")


def upload_large_file(filepath, key):
    """Multipart upload for large files (video, binary data)"""
    file_size = os.path.getsize(filepath)
    chunk_size = 64 * 1024 * 1024  # 64 MB chunks
    
    # Initiate multipart upload
    mpu = s3_client.create_multipart_upload(Bucket=BUCKET, Key=key)
    upload_id = mpu['UploadId']
    
    parts = []
    part_number = 1
    
    with open(filepath, 'rb') as f:
        while True:
            data = f.read(chunk_size)
            if not data:
                break
            
            response = s3_client.upload_part(
                Bucket=BUCKET,
                Key=key,
                PartNumber=part_number,
                UploadId=upload_id,
                Body=data
            )
            parts.append({'PartNumber': part_number, 'ETag': response['ETag']})
            
            progress = (part_number * chunk_size / file_size) * 100
            print(f"  Part {part_number}: {min(progress, 100):.1f}% complete")
            part_number += 1
    
    # Complete multipart upload
    s3_client.complete_multipart_upload(
        Bucket=BUCKET,
        Key=key,
        UploadId=upload_id,
        MultipartUpload={'Parts': parts}
    )
    print(f"Upload complete: {key} ({file_size / (1024**3):.2f} GB)")


if __name__ == '__main__':
    # Bulk ingest sensor data
    bulk_ingest(num_devices=10, readings_per_device=1000)
    
    # Upload large binary file
    # upload_large_file('/data/camera-feed-2024-01-15.mp4', 'video/cam01/2024-01-15.mp4')
```

---

## 2.4 NFS Gateway for File-Based Access

### Enable NFS on Snowball Edge

```bash
# Start the NFS service on the device
snowballEdge start-service \
  --endpoint https://192.168.1.100 \
  --service-id nfs

# Create a virtual network interface for NFS
snowballEdge create-virtual-network-interface \
  --endpoint https://192.168.1.100 \
  --physical-network-interface-id s.ni-12345 \
  --ip-address-assignment STATIC \
  --static-ip-address-configuration '{
    "IpAddress": "192.168.1.103",
    "Netmask": "255.255.255.0"
  }'
```

### Mount NFS Share

```bash
# On Linux client
sudo mkdir -p /mnt/snowball
sudo mount -t nfs 192.168.1.103:/buckets/factory-sensor-data /mnt/snowball

# Verify mount
df -h /mnt/snowball
ls /mnt/snowball/

# On macOS
sudo mount -t nfs -o vers=3,tcp 192.168.1.103:/buckets/factory-sensor-data /mnt/snowball
```

### NFS Configuration (exports)

```bash
# Restrict access to specific subnet
snowballEdge describe-service --endpoint https://192.168.1.100 --service-id nfs

# Allowed clients are configured during mount
# Default: all clients on the local network
```

### NFS vs S3: When to Use Which

| Criteria | NFS | S3 API |
|----------|-----|--------|
| **Legacy apps** | ✅ No code changes needed | ❌ Requires S3 SDK |
| **Performance** | Good for sequential I/O | Better for parallel ops |
| **POSIX compliance** | ✅ Full filesystem semantics | ❌ Object storage only |
| **Metadata** | Standard file attributes | Custom S3 metadata |
| **Concurrent access** | File locking supported | Eventual consistency |
| **Large files** | Simple `cp` command | Multipart upload needed |

---

## 2.5 Optimizing Transfer Performance

### Network Configuration for Maximum Throughput

```bash
# Check current network speed
snowballEdge describe-device --endpoint https://192.168.1.100

# Recommended: Use 10GbE or higher for bulk transfers
# Theoretical maximums:
#   1 GbE  = ~100 MB/s = 8.6 TB/day
#   10 GbE = ~1 GB/s   = 86 TB/day
#   25 GbE = ~2.5 GB/s = 216 TB/day
```

### Transfer Optimization Script

```python
"""
Optimized data transfer to Snowball Edge.
Uses multiprocessing, chunked uploads, and connection pooling.
"""
import boto3
import os
import sys
from concurrent.futures import ProcessPoolExecutor, as_completed
from botocore.config import Config
from pathlib import Path

# Tuning parameters
WORKERS = os.cpu_count() * 2  # Parallel workers
CHUNK_SIZE = 128 * 1024 * 1024  # 128 MB multipart chunks
MAX_CONNECTIONS = 100  # Connection pool size

config = Config(
    max_pool_connections=MAX_CONNECTIONS,
    retries={'max_attempts': 5, 'mode': 'adaptive'},
    tcp_keepalive=True
)


def get_client():
    return boto3.client(
        's3',
        endpoint_url='https://192.168.1.100:8443',
        aws_access_key_id='AKIA1234567890EXAMPLE',
        aws_secret_access_key='wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY',
        verify=False,
        config=config
    )


def upload_file(args):
    """Upload a single file to Snowball Edge"""
    local_path, s3_key, bucket = args
    client = get_client()
    file_size = os.path.getsize(local_path)
    
    transfer_config = boto3.s3.transfer.TransferConfig(
        multipart_threshold=64 * 1024 * 1024,
        multipart_chunksize=CHUNK_SIZE,
        max_concurrency=10,
        use_threads=True
    )
    
    client.upload_file(
        local_path, bucket, s3_key,
        Config=transfer_config
    )
    return local_path, file_size


def sync_directory(local_dir, bucket, prefix=''):
    """Sync entire directory to Snowball Edge with maximum parallelism"""
    local_path = Path(local_dir)
    tasks = []
    
    for file_path in local_path.rglob('*'):
        if file_path.is_file():
            relative = file_path.relative_to(local_path)
            s3_key = f"{prefix}/{relative}" if prefix else str(relative)
            tasks.append((str(file_path), s3_key, bucket))
    
    total_size = 0
    completed = 0
    
    with ProcessPoolExecutor(max_workers=WORKERS) as executor:
        futures = {executor.submit(upload_file, task): task for task in tasks}
        
        for future in as_completed(futures):
            path, size = future.result()
            total_size += size
            completed += 1
            if completed % 100 == 0:
                print(f"Progress: {completed}/{len(tasks)} files, "
                      f"{total_size / (1024**3):.2f} GB transferred")
    
    print(f"\nComplete: {completed} files, {total_size / (1024**3):.2f} GB total")


if __name__ == '__main__':
    sync_directory(
        local_dir='/data/factory-output/',
        bucket='factory-sensor-data',
        prefix='raw/2024/01'
    )
```

### Performance Benchmarks

```
┌──────────────────────────────────────────────────────┐
│  Transfer Speed Benchmarks (Snowball Edge)            │
├──────────────────────────────────────────────────────┤
│  Interface   │ Small Files  │ Large Files │ Mixed    │
│              │ (<1MB each)  │ (>1GB each) │          │
├──────────────┼──────────────┼─────────────┼──────────┤
│  1 GbE       │  40 MB/s     │  110 MB/s   │  60 MB/s │
│  10 GbE      │  200 MB/s    │  900 MB/s   │  500 MB/s│
│  25 GbE      │  400 MB/s    │  2.2 GB/s   │  1.2 GB/s│
└──────────────┴──────────────┴─────────────┴──────────┘

Tips for maximum throughput:
1. Use 10GbE or 25GbE connections
2. Batch small files into tarballs (reduces S3 API overhead)
3. Use parallel multi-part uploads for large files
4. Keep 20-30 concurrent connections
5. Avoid file listings during bulk transfers
```

---

## 2.6 Data Validation & Integrity

```python
import hashlib
import json

def validate_transfer(local_dir, bucket, s3_client):
    """Verify all files transferred correctly using checksums"""
    errors = []
    local_path = Path(local_dir)
    
    for file_path in local_path.rglob('*'):
        if not file_path.is_file():
            continue
        
        # Calculate local checksum
        with open(file_path, 'rb') as f:
            local_md5 = hashlib.md5(f.read()).hexdigest()
        
        # Get S3 ETag (MD5 for non-multipart uploads)
        s3_key = str(file_path.relative_to(local_path))
        try:
            response = s3_client.head_object(Bucket=bucket, Key=s3_key)
            s3_etag = response['ETag'].strip('"')
            
            # Note: multipart uploads have composite ETags
            if '-' not in s3_etag and local_md5 != s3_etag:
                errors.append(f"MISMATCH: {s3_key}")
        except Exception as e:
            errors.append(f"MISSING: {s3_key} - {e}")
    
    if errors:
        print(f"❌ {len(errors)} validation errors:")
        for err in errors[:10]:
            print(f"  {err}")
    else:
        print("✅ All files validated successfully")
    
    return len(errors) == 0
```

---

## 2.7 Monitoring Transfer Progress

```bash
# Check device storage usage
snowballEdge describe-device --endpoint https://192.168.1.100

# List objects and count
aws s3 ls s3://factory-sensor-data/ --recursive --summarize \
  --profile snowball --endpoint-url https://192.168.1.100:8443

# Watch real-time object count
watch -n 5 'aws s3 ls s3://factory-sensor-data/ --recursive --summarize \
  --profile snowball --endpoint-url https://192.168.1.100:8443 2>/dev/null | tail -2'
```

---

## Next: [Part 3 - Edge Computing →](Part3_Edge_Computing.md)
