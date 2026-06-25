# Part 6: Local Simulation & Testing

## 6.1 Overview

You can test most Snowball Edge concepts **without ordering a physical device** using open-source tools.

```
┌──────────────────────────────────────────────────┐
│          Local Simulation Stack                    │
│                                                    │
│  MinIO ─────── Simulates Snowball S3 endpoint    │
│  LocalStack ── Simulates Lambda + S3 events      │
│  Docker EC2 ── Simulates sbe1 instances          │
│  SAM CLI ───── Local Lambda testing              │
│  K3s ────────── Simulates EKS Anywhere           │
└──────────────────────────────────────────────────┘
```

---

## 6.2 Docker Compose Environment

```yaml
# docker-compose.yml
version: '3.8'

services:
  # Simulates Snowball Edge S3 endpoint
  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"    # S3 API
      - "9001:9001"    # Console
    environment:
      MINIO_ROOT_USER: snowball-access-key
      MINIO_ROOT_PASSWORD: snowball-secret-key
    command: server /data --console-address ":9001"
    volumes:
      - minio-data:/data

  # Simulates Lambda + S3 event triggers
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"
    environment:
      SERVICES: s3,lambda,events
      LAMBDA_EXECUTOR: docker
      DOCKER_HOST: unix:///var/run/docker.sock
    volumes:
      - "/var/run/docker.sock:/var/run/docker.sock"

  # Simulates ML inference EC2 instance
  ml-inference:
    build:
      context: ./ml-inference
      dockerfile: Dockerfile
    ports:
      - "8000:8000"
    environment:
      S3_ENDPOINT: http://minio:9000
      S3_ACCESS_KEY: snowball-access-key
      S3_SECRET_KEY: snowball-secret-key
      BUCKET_NAME: factory-sensor-data

  # Simulates IoT sensor data generator
  sensor-simulator:
    build:
      context: ./sensor-simulator
      dockerfile: Dockerfile
    environment:
      S3_ENDPOINT: http://minio:9000
      S3_ACCESS_KEY: snowball-access-key
      S3_SECRET_KEY: snowball-secret-key
      BUCKET_NAME: factory-sensor-data
      SENSORS: 10
      INTERVAL_MS: 1000
    depends_on:
      - minio

volumes:
  minio-data:
```

---

## 6.3 Sensor Simulator

```python
# sensor-simulator/simulator.py
import boto3
import json
import time
import random
import os
from threading import Thread

S3_ENDPOINT = os.getenv('S3_ENDPOINT', 'http://localhost:9000')
BUCKET = os.getenv('BUCKET_NAME', 'factory-sensor-data')
NUM_SENSORS = int(os.getenv('SENSORS', '10'))
INTERVAL = int(os.getenv('INTERVAL_MS', '1000')) / 1000

s3 = boto3.client('s3',
    endpoint_url=S3_ENDPOINT,
    aws_access_key_id=os.getenv('S3_ACCESS_KEY', 'snowball-access-key'),
    aws_secret_access_key=os.getenv('S3_SECRET_KEY', 'snowball-secret-key'),
    region_name='us-east-1'
)

def ensure_bucket():
    try:
        s3.create_bucket(Bucket=BUCKET)
    except:
        pass

def generate_reading(device_id):
    return {
        "device_id": device_id,
        "timestamp": time.time(),
        "temperature_c": round(random.gauss(55, 15), 2),
        "pressure_psi": round(random.gauss(14.7, 3), 2),
        "vibration_hz": round(random.gauss(200, 80), 2),
        "humidity_pct": round(random.uniform(30, 90), 1),
        "power_watts": round(random.gauss(2500, 500), 1)
    }

def sensor_loop(device_id):
    while True:
        reading = generate_reading(device_id)
        key = f"raw/{device_id}/{int(time.time())}.json"
        s3.put_object(Bucket=BUCKET, Key=key, Body=json.dumps(reading))
        time.sleep(INTERVAL)

if __name__ == '__main__':
    ensure_bucket()
    print(f"Starting {NUM_SENSORS} sensors, interval={INTERVAL}s")
    threads = []
    for i in range(NUM_SENSORS):
        t = Thread(target=sensor_loop, args=(f"sensor-{i:03d}",), daemon=True)
        t.start()
        threads.append(t)
    
    # Keep main thread alive
    while True:
        time.sleep(60)
```

```dockerfile
# sensor-simulator/Dockerfile
FROM python:3.11-slim
WORKDIR /app
RUN pip install boto3
COPY simulator.py .
CMD ["python", "simulator.py"]
```

---

## 6.4 ML Inference Service (Simulates EC2 on Edge)

```python
# ml-inference/app.py
from flask import Flask, request, jsonify
import boto3
import json
import os
import time

app = Flask(__name__)

s3 = boto3.client('s3',
    endpoint_url=os.getenv('S3_ENDPOINT', 'http://localhost:9000'),
    aws_access_key_id=os.getenv('S3_ACCESS_KEY', 'snowball-access-key'),
    aws_secret_access_key=os.getenv('S3_SECRET_KEY', 'snowball-secret-key'),
    region_name='us-east-1'
)
BUCKET = os.getenv('BUCKET_NAME', 'factory-sensor-data')


def predict(features):
    """Simple anomaly model (replace with real ONNX/TF model)"""
    temp = features.get('temperature_c', 0)
    vibration = features.get('vibration_hz', 0)
    pressure = features.get('pressure_psi', 0)
    
    score = (temp/100)*0.4 + (vibration/500)*0.35 + (pressure/25)*0.25
    return {
        'anomaly_score': round(min(score, 1.0), 4),
        'is_anomaly': score > 0.7,
        'risk_level': 'CRITICAL' if score > 0.8 else 'HIGH' if score > 0.6 else 'LOW'
    }


@app.route('/predict', methods=['POST'])
def predict_endpoint():
    data = request.json
    result = predict(data)
    
    # Store prediction
    key = f"predictions/{data.get('device_id','unknown')}/{int(time.time())}.json"
    s3.put_object(Bucket=BUCKET, Key=key, Body=json.dumps({**data, **result}))
    
    return jsonify(result)


@app.route('/health')
def health():
    return jsonify({'status': 'healthy'})


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)
```

```dockerfile
# ml-inference/Dockerfile
FROM python:3.11-slim
WORKDIR /app
RUN pip install flask boto3
COPY app.py .
CMD ["python", "app.py"]
```

---

## 6.5 Lambda Edge Simulation

```bash
# Test Lambda locally using SAM CLI
sam init --runtime python3.9 --name edge-processor

# Create template
cat > template.yaml << 'EOF'
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31

Resources:
  EdgeProcessor:
    Type: AWS::Serverless::Function
    Properties:
      Handler: app.handler
      Runtime: python3.9
      Timeout: 60
      MemorySize: 512
      Events:
        S3Event:
          Type: S3
          Properties:
            Bucket: !Ref SensorBucket
            Events: s3:ObjectCreated:*
            Filter:
              S3Key:
                Rules:
                  - Name: prefix
                    Value: raw/

  SensorBucket:
    Type: AWS::S3::Bucket
    Properties:
      BucketName: factory-sensor-data
EOF

# Test invoke
sam local invoke EdgeProcessor --event events/s3_put.json
```

---

## 6.6 EKS Anywhere Simulation with K3s

```bash
# Install K3s (lightweight Kubernetes)
curl -sfL https://get.k3s.io | sh -

# Deploy the same workloads you'd run on EKS Anywhere
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: edge-inference
spec:
  replicas: 2
  selector:
    matchLabels:
      app: edge-inference
  template:
    metadata:
      labels:
        app: edge-inference
    spec:
      containers:
      - name: inference
        image: ml-inference:latest
        ports:
        - containerPort: 8000
        env:
        - name: S3_ENDPOINT
          value: "http://minio.default.svc:9000"
---
apiVersion: v1
kind: Service
metadata:
  name: edge-inference-svc
spec:
  selector:
    app: edge-inference
  ports:
  - port: 8000
  type: ClusterIP
EOF
```

---

## 6.7 Running the Full Simulation

```bash
# 1. Start the stack
docker-compose up -d

# 2. Wait for services
sleep 5

# 3. Create bucket in MinIO
aws --endpoint-url http://localhost:9000 s3 mb s3://factory-sensor-data \
  --profile minio 2>/dev/null || true

# 4. Verify sensors are writing data
aws --endpoint-url http://localhost:9000 s3 ls s3://factory-sensor-data/raw/ \
  --recursive --profile minio | head -5

# 5. Test ML inference
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{"device_id":"sensor-001","temperature_c":88.5,"pressure_psi":21.3,"vibration_hz":420}'

# 6. Check predictions stored
aws --endpoint-url http://localhost:9000 s3 ls s3://factory-sensor-data/predictions/ \
  --recursive --profile minio

# 7. Cleanup
docker-compose down -v
```

---

## 6.8 Performance Testing

```python
# benchmark.py - Test transfer throughput against MinIO (simulating Snowball)
import boto3
import time
import os
from concurrent.futures import ThreadPoolExecutor

s3 = boto3.client('s3',
    endpoint_url='http://localhost:9000',
    aws_access_key_id='snowball-access-key',
    aws_secret_access_key='snowball-secret-key',
    region_name='us-east-1'
)

BUCKET = 'factory-sensor-data'
PAYLOAD = b'x' * 1024 * 100  # 100KB per object

def upload_one(i):
    s3.put_object(Bucket=BUCKET, Key=f"bench/{i}.bin", Body=PAYLOAD)
    return len(PAYLOAD)

def benchmark(num_objects=1000, workers=20):
    start = time.time()
    total_bytes = 0
    
    with ThreadPoolExecutor(max_workers=workers) as ex:
        results = list(ex.map(upload_one, range(num_objects)))
    
    total_bytes = sum(results)
    elapsed = time.time() - start
    
    print(f"Objects: {num_objects}")
    print(f"Time: {elapsed:.1f}s")
    print(f"Throughput: {num_objects/elapsed:.0f} objects/sec")
    print(f"Bandwidth: {total_bytes/elapsed/1024/1024:.1f} MB/s")

if __name__ == '__main__':
    benchmark()
```

---

## 6.9 What Can't Be Simulated Locally

| Feature | Simulatable? | Reason |
|---------|:------------:|--------|
| S3 data transfer | ✅ (MinIO) | API compatible |
| Lambda execution | ✅ (SAM/LocalStack) | Same runtime |
| EC2 workloads | ✅ (Docker) | Same code |
| EKS workloads | ✅ (K3s) | Same K8s API |
| NFS gateway | ✅ (NFS server) | Standard protocol |
| **Device unlock flow** | ❌ | Requires physical device |
| **Cluster formation** | ❌ | Requires multiple devices |
| **True network isolation** | ⚠️ Partial | Can simulate with firewall rules |
| **210TB storage capacity** | ❌ | Hardware limitation |
| **GPU inference (V100)** | ⚠️ Partial | Need local GPU |
| **E-ink shipping label** | ❌ | Physical only |
| **Tamper detection** | ❌ | Hardware security module |
| **OpsHub management** | ❌ | Requires real device connection |

---

## 6.10 Next Steps After Simulation

1. **Order a Snowball Edge** for a real test (AWS Free Tier doesn't cover it, but costs ~$300 for a 10-day test)
2. **Start with LOCAL_USE job** to test compute without data migration commitment
3. **Use AWS Snow Family documentation** for production deployment guides
4. **Explore AWS Snowcone** (8TB, ultra-portable) for smaller edge deployments

---

## ← [Back to README](README.md)
