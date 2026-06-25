# Part 3: Edge Computing

## 3.1 Lambda on Snowball Edge

Snowball Edge can run AWS Lambda functions **locally, completely disconnected from the cloud**. Functions are pre-loaded during job creation and triggered by S3 events or invoked directly.

### How It Works

```
┌─────────────────────────────────────────────────┐
│            Snowball Edge Device                   │
│                                                   │
│  [S3 PUT event] ──► [Lambda Trigger] ──► [Function]
│                                              │
│                                              ▼
│                                     [Processed Data]
│                                         written to
│                                        local S3
└─────────────────────────────────────────────────┘
```

### Constraints
- **Runtime**: Python 3.8, Node.js 12.x (limited runtimes)
- **Memory**: Up to 3008 MB per function
- **Timeout**: Up to 15 minutes (same as cloud Lambda)
- **Concurrency**: Up to 8 concurrent executions
- **Trigger**: S3 PUT events only (no API Gateway, SQS, etc.)
- **Layers**: Not supported on Snowball Edge

### Creating a Lambda Function for Edge

```python
# edge_processor.py
"""
Lambda function that runs on Snowball Edge.
Triggered on S3 PUT events - validates and enriches sensor data.
"""
import json
import boto3
import time

# Local S3 client (on-device)
s3 = boto3.client('s3', endpoint_url='http://localhost:8443')

def handler(event, context):
    """Process incoming sensor readings"""
    
    # Extract S3 event details
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = event['Records'][0]['s3']['object']['key']
    
    # Read the uploaded object
    response = s3.get_object(Bucket=bucket, Key=key)
    reading = json.loads(response['Body'].read().decode('utf-8'))
    
    # Validate data
    if not validate_reading(reading):
        # Move to quarantine
        s3.put_object(
            Bucket=bucket,
            Key=f"quarantine/{key}",
            Body=json.dumps({"original": reading, "reason": "validation_failed"})
        )
        return {'statusCode': 400, 'body': 'Invalid reading'}
    
    # Enrich data
    enriched = enrich_reading(reading)
    
    # Anomaly detection
    if is_anomaly(reading):
        s3.put_object(
            Bucket=bucket,
            Key=f"alerts/{key}",
            Body=json.dumps({"reading": enriched, "alert_type": "anomaly"})
        )
    
    # Write processed data
    s3.put_object(
        Bucket=bucket,
        Key=f"processed/{key}",
        Body=json.dumps(enriched),
        ContentType='application/json'
    )
    
    return {'statusCode': 200, 'body': f'Processed: {key}'}


def validate_reading(reading):
    """Validate sensor reading schema and ranges"""
    required_fields = ['device_id', 'timestamp', 'temperature_c', 'pressure_psi']
    
    for field in required_fields:
        if field not in reading:
            return False
    
    # Physical impossibility checks
    if reading['temperature_c'] < -273.15 or reading['temperature_c'] > 1000:
        return False
    if reading['pressure_psi'] < 0 or reading['pressure_psi'] > 100:
        return False
    
    return True


def enrich_reading(reading):
    """Add metadata and computed fields"""
    reading['processed_at'] = time.time()
    reading['edge_device'] = 'snowball-factory-alpha'
    
    # Compute derived metrics
    if 'temperature_c' in reading:
        reading['temperature_f'] = reading['temperature_c'] * 9/5 + 32
    
    # Classify severity
    temp = reading.get('temperature_c', 0)
    if temp > 80:
        reading['severity'] = 'CRITICAL'
    elif temp > 60:
        reading['severity'] = 'WARNING'
    else:
        reading['severity'] = 'NORMAL'
    
    return reading


def is_anomaly(reading):
    """Simple anomaly detection based on thresholds"""
    return (
        reading.get('temperature_c', 0) > 85 or
        reading.get('pressure_psi', 0) > 22 or
        reading.get('vibration_hz', 0) > 400
    )
```

### Deploying Lambda to Snowball Edge

```bash
# Package the function
zip edge_processor.zip edge_processor.py

# Upload to S3 (cloud) before creating the job
aws s3 cp edge_processor.zip s3://my-lambda-code/edge_processor.zip

# When creating the Snowball Edge job, include Lambda resources:
aws snowball create-job \
  --job-type LOCAL_USE \
  --resources '{
    "S3Resources": [{"BucketArn": "arn:aws:s3:::factory-sensor-data"}],
    "LambdaResources": [{
      "LambdaArn": "arn:aws:lambda:us-east-1:123456789012:function:edge-processor",
      "EventTriggers": [{
        "EventResourceARN": "arn:aws:s3:::factory-sensor-data"
      }]
    }]
  }' \
  --snowball-type EDGE
```

### Invoking Lambda Directly (Without S3 Trigger)

```bash
# Get Lambda endpoint
snowballEdge list-services --endpoint https://192.168.1.100

# Invoke function directly
aws lambda invoke \
  --endpoint-url http://192.168.1.100:8085 \
  --function-name edge-processor \
  --payload '{"test": true, "Records": [{"s3": {"bucket": {"name": "factory-sensor-data"}, "object": {"key": "test/reading.json"}}}]}' \
  --profile snowball \
  output.json

cat output.json
```

---

## 3.2 EC2 Instances on Snowball Edge

Run full EC2 instances locally for heavier compute workloads like ML inference, databases, or custom applications.

### Available Instance Types

| Instance Type | vCPUs | RAM | Use Case |
|---------------|-------|-----|----------|
| sbe1.small | 1 | 2 GB | Lightweight services |
| sbe1.medium | 2 | 4 GB | Web servers, APIs |
| sbe1.large | 4 | 8 GB | Data processing |
| sbe1.xlarge | 8 | 16 GB | ML inference |
| sbe1.2xlarge | 16 | 32 GB | Heavy compute |
| sbe1.4xlarge | 32 | 64 GB | Database servers |
| sbe-c.large | 4 | 8 GB | Compute optimized |
| sbe-c.xlarge | 8 | 16 GB | Compute optimized |
| sbe-c.2xlarge | 16 | 32 GB | Compute optimized |
| sbe-c.4xlarge | 32 | 64 GB | Compute optimized |
| sbe-g.4xlarge | 32 | 64 GB + GPU | ML/AI with GPU |

### Pre-Loading AMIs

AMIs must be pre-loaded during job creation. You can use:
- Standard AWS AMIs (Amazon Linux 2, Ubuntu)
- Custom AMIs from your account

```bash
# Include EC2 AMIs in job creation
aws snowball create-job \
  --resources '{
    "Ec2AmiResources": [
      {
        "AmiId": "ami-0abcdef1234567890",
        "SnowballAmiId": "s.ami-0abcdef1234567890"
      }
    ]
  }' \
  --snowball-type EDGE_CG  # Compute Optimized with GPU
```

### Launching EC2 on Snowball Edge

```bash
# Set endpoint
export EC2_ENDPOINT=http://192.168.1.100:8008

# List available AMIs on the device
aws ec2 describe-images \
  --endpoint-url $EC2_ENDPOINT \
  --profile snowball

# Create a key pair
aws ec2 create-key-pair \
  --key-name edge-key \
  --endpoint-url $EC2_ENDPOINT \
  --profile snowball \
  --query 'KeyMaterial' --output text > edge-key.pem
chmod 400 edge-key.pem

# Launch instance
aws ec2 run-instances \
  --endpoint-url $EC2_ENDPOINT \
  --profile snowball \
  --image-id s.ami-0abcdef1234567890 \
  --instance-type sbe1.xlarge \
  --key-name edge-key \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=ml-inference-node}]'

# Check instance status
aws ec2 describe-instances \
  --endpoint-url $EC2_ENDPOINT \
  --profile snowball

# Create a virtual network interface for the instance
snowballEdge create-virtual-network-interface \
  --endpoint https://192.168.1.100 \
  --physical-network-interface-id s.ni-12345 \
  --ip-address-assignment STATIC \
  --static-ip-address-configuration '{
    "IpAddress": "192.168.1.110",
    "Netmask": "255.255.255.0"
  }'

# Associate VNI with instance
aws ec2 associate-address \
  --endpoint-url $EC2_ENDPOINT \
  --profile snowball \
  --instance-id s.i-01234567890abcdef \
  --public-ip 192.168.1.110

# SSH into the instance
ssh -i edge-key.pem ec2-user@192.168.1.110
```

### ML Inference Application on EC2

```python
"""
ML inference service running on EC2 instance on Snowball Edge.
Reads sensor data from local S3, runs prediction, writes results back.
"""
import boto3
import json
import time
import numpy as np
from flask import Flask, request, jsonify

app = Flask(__name__)

# Connect to local S3 on Snowball Edge
s3 = boto3.client(
    's3',
    endpoint_url='https://192.168.1.100:8443',
    aws_access_key_id='AKIA1234567890EXAMPLE',
    aws_secret_access_key='wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY',
    verify=False
)

BUCKET = 'factory-sensor-data'

# Simulated ML model (in production, load a real model)
class PredictiveMaintenanceModel:
    def __init__(self):
        # Load pre-trained model weights
        # In production: self.model = load_model('/models/predictive_maintenance.onnx')
        self.threshold_temp = 75.0
        self.threshold_vibration = 350.0
        self.threshold_pressure = 20.0
    
    def predict(self, features):
        """Predict remaining useful life (RUL) and failure probability"""
        temp = features.get('temperature_c', 0)
        vibration = features.get('vibration_hz', 0)
        pressure = features.get('pressure_psi', 0)
        
        # Simple rule-based model (replace with real ML model)
        risk_score = (
            (temp / 100) * 0.4 +
            (vibration / 500) * 0.35 +
            (pressure / 25) * 0.25
        )
        
        failure_probability = min(risk_score, 1.0)
        remaining_hours = max(0, int((1 - failure_probability) * 720))  # Max 30 days
        
        return {
            'failure_probability': round(failure_probability, 4),
            'remaining_useful_life_hours': remaining_hours,
            'recommended_action': self._get_action(failure_probability),
            'risk_level': self._get_risk_level(failure_probability)
        }
    
    def _get_action(self, prob):
        if prob > 0.8:
            return 'IMMEDIATE_SHUTDOWN'
        elif prob > 0.6:
            return 'SCHEDULE_MAINTENANCE_24H'
        elif prob > 0.4:
            return 'SCHEDULE_MAINTENANCE_7D'
        return 'MONITOR'
    
    def _get_risk_level(self, prob):
        if prob > 0.8: return 'CRITICAL'
        if prob > 0.6: return 'HIGH'
        if prob > 0.4: return 'MEDIUM'
        return 'LOW'


model = PredictiveMaintenanceModel()


@app.route('/predict', methods=['POST'])
def predict():
    """Real-time prediction endpoint"""
    data = request.json
    prediction = model.predict(data)
    
    # Store prediction
    s3.put_object(
        Bucket=BUCKET,
        Key=f"predictions/{data.get('device_id', 'unknown')}/{int(time.time())}.json",
        Body=json.dumps({**data, **prediction}),
        ContentType='application/json'
    )
    
    return jsonify(prediction)


@app.route('/batch-predict', methods=['POST'])
def batch_predict():
    """Process all unprocessed readings from S3"""
    prefix = request.json.get('prefix', 'processed/')
    
    # List objects to process
    response = s3.list_objects_v2(Bucket=BUCKET, Prefix=prefix, MaxKeys=100)
    results = []
    
    for obj in response.get('Contents', []):
        reading_response = s3.get_object(Bucket=BUCKET, Key=obj['Key'])
        reading = json.loads(reading_response['Body'].read())
        prediction = model.predict(reading)
        results.append({'key': obj['Key'], 'prediction': prediction})
    
    return jsonify({'processed': len(results), 'results': results})


@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'healthy', 'model_loaded': True})


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)
```

---

## 3.3 EKS Anywhere on Snowball Edge

Run Kubernetes workloads on Snowball Edge using Amazon EKS Anywhere.

### Overview

```
┌────────────────────────────────────────────────┐
│         Snowball Edge (Compute Optimized)        │
│                                                  │
│  ┌────────────────────────────────────────────┐ │
│  │          EKS Anywhere Cluster               │ │
│  │                                              │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐ │ │
│  │  │  Pod 1   │  │  Pod 2   │  │  Pod 3   │ │ │
│  │  │ Data     │  │ ML       │  │ API      │ │ │
│  │  │ Ingester │  │ Inference│  │ Gateway  │ │ │
│  │  └──────────┘  └──────────┘  └──────────┘ │ │
│  │                                              │ │
│  │  ┌──────────────────────────────────────┐   │ │
│  │  │  Persistent Volume (local S3/EBS)    │   │ │
│  │  └──────────────────────────────────────┘   │ │
│  └────────────────────────────────────────────┘ │
└────────────────────────────────────────────────┘
```

### Setup EKS Anywhere on Snowball Edge

```bash
# Prerequisites: Device must be Compute Optimized with EKS enabled during job creation

# 1. Get Kubernetes credentials
snowballEdge describe-service \
  --endpoint https://192.168.1.100 \
  --service-id eks

# 2. Configure kubectl
snowballEdge get-eks-credentials \
  --endpoint https://192.168.1.100 \
  --cluster-name edge-cluster > kubeconfig.yaml

export KUBECONFIG=./kubeconfig.yaml

# 3. Verify cluster
kubectl get nodes
kubectl get pods -A
```

### Deploying Workloads

```yaml
# data-pipeline.yaml - Edge data processing pipeline
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sensor-ingester
  labels:
    app: data-pipeline
spec:
  replicas: 2
  selector:
    matchLabels:
      app: sensor-ingester
  template:
    metadata:
      labels:
        app: sensor-ingester
    spec:
      containers:
      - name: ingester
        image: local-registry/sensor-ingester:latest
        resources:
          requests:
            cpu: "500m"
            memory: "512Mi"
          limits:
            cpu: "1"
            memory: "1Gi"
        env:
        - name: S3_ENDPOINT
          value: "https://192.168.1.100:8443"
        - name: BUCKET_NAME
          value: "factory-sensor-data"
        ports:
        - containerPort: 8080
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ml-inference
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ml-inference
  template:
    metadata:
      labels:
        app: ml-inference
    spec:
      containers:
      - name: inference
        image: local-registry/ml-inference:latest
        resources:
          requests:
            cpu: "2"
            memory: "4Gi"
            nvidia.com/gpu: 1  # If GPU available
          limits:
            cpu: "4"
            memory: "8Gi"
            nvidia.com/gpu: 1
        ports:
        - containerPort: 8000
---
apiVersion: v1
kind: Service
metadata:
  name: ml-inference-svc
spec:
  selector:
    app: ml-inference
  ports:
  - port: 8000
    targetPort: 8000
  type: NodePort
```

```bash
# Deploy
kubectl apply -f data-pipeline.yaml

# Check status
kubectl get pods -w
kubectl logs -f deployment/sensor-ingester
```

---

## 3.4 IoT Greengrass on Snowball Edge

AWS IoT Greengrass runs on Snowball Edge to manage IoT devices and run local inference.

### Architecture with IoT Greengrass

```
┌─────────────────────────────────────────────────────┐
│                   Factory Floor                       │
│                                                       │
│  [Sensor 1] ──┐                                     │
│  [Sensor 2] ──┼──► [IoT Greengrass on Snowball]    │
│  [Sensor 3] ──┘         │                           │
│  [Camera 1] ────────────┘                           │
│                                                       │
│  Greengrass Components:                              │
│  ├── MQTT Broker (local messaging)                   │
│  ├── Stream Manager (buffered upload)                │
│  ├── ML Inference Component                          │
│  └── Lambda Components (data processing)             │
└─────────────────────────────────────────────────────┘
```

### Greengrass Setup

```bash
# IoT Greengrass is pre-installed on Snowball Edge (Compute Optimized)

# 1. Configure Greengrass Core
snowballEdge describe-service \
  --endpoint https://192.168.1.100 \
  --service-id greengrass

# 2. Deploy Greengrass components from AWS console (during job creation)
# Or use the Greengrass CLI on the device:

# List deployed components
greengrass-cli component list

# Check component status
greengrass-cli component details --name com.example.SensorProcessor
```

### Greengrass Component for Sensor Processing

```python
# artifacts/com.example.SensorProcessor/1.0.0/sensor_processor.py
"""
Greengrass component that processes MQTT messages from factory sensors.
"""
import json
import time
import awsiot.greengrasscoreipc as ipc
from awsiot.greengrasscoreipc.model import (
    SubscribeToTopicRequest,
    PublishToTopicRequest,
    PublishMessage,
    JsonMessage
)

SUBSCRIBE_TOPIC = "factory/sensors/+"
PUBLISH_TOPIC = "factory/processed"
ALERT_TOPIC = "factory/alerts"

ipc_client = ipc.connect()


class SensorStreamHandler(ipc.client.SubscribeToTopicStreamHandler):
    def on_stream_event(self, event):
        message = json.loads(event.json_message.message)
        process_sensor_message(message)

    def on_stream_error(self, error):
        print(f"Stream error: {error}")

    def on_stream_closed(self):
        print("Stream closed")


def process_sensor_message(message):
    """Process incoming sensor data"""
    device_id = message.get('device_id')
    temp = message.get('temperature_c', 0)
    
    # Anomaly detection
    if temp > 85:
        publish_alert({
            'device_id': device_id,
            'alert_type': 'HIGH_TEMPERATURE',
            'value': temp,
            'timestamp': time.time()
        })
    
    # Publish processed data
    enriched = {
        **message,
        'processed_at': time.time(),
        'edge_node': 'snowball-factory-01'
    }
    publish_message(PUBLISH_TOPIC, enriched)


def publish_message(topic, payload):
    request = PublishToTopicRequest(topic=topic)
    request.publish_message = PublishMessage(
        json_message=JsonMessage(message=json.dumps(payload))
    )
    ipc_client.new_publish_to_topic().activate(request)


def publish_alert(alert):
    publish_message(ALERT_TOPIC, alert)


def main():
    # Subscribe to sensor topics
    request = SubscribeToTopicRequest(topic=SUBSCRIBE_TOPIC)
    handler = SensorStreamHandler()
    operation = ipc_client.new_subscribe_to_topic(handler)
    operation.activate(request)
    
    print(f"Subscribed to {SUBSCRIBE_TOPIC}")
    
    # Keep running
    while True:
        time.sleep(1)


if __name__ == '__main__':
    main()
```

---

## 3.5 SageMaker Neo for Edge ML

Deploy optimized ML models using SageMaker Neo compilation for Snowball Edge.

### Model Compilation Pipeline

```
┌──────────────┐     ┌───────────────┐     ┌──────────────────┐
│  Train Model │────►│  Neo Compile  │────►│  Deploy to Edge  │
│  (Cloud)     │     │  (Optimize)   │     │  (Snowball Edge) │
└──────────────┘     └───────────────┘     └──────────────────┘
```

### Compile Model with Neo

```python
import boto3

sagemaker = boto3.client('sagemaker')

# Compile model for Snowball Edge (x86_64 or GPU)
response = sagemaker.create_compilation_job(
    CompilationJobName='factory-anomaly-model-edge',
    RoleArn='arn:aws:iam::123456789012:role/SageMakerRole',
    InputConfig={
        'S3Uri': 's3://my-models/anomaly-detection/model.tar.gz',
        'DataInputConfig': '{"input": [1, 13]}',  # Input shape
        'Framework': 'PYTORCH'
    },
    OutputConfig={
        'S3OutputLocation': 's3://my-models/compiled/',
        'TargetDevice': 'ml_c5'  # x86_64 for Snowball Edge
        # Use 'ml_g4dn' for GPU-enabled Snowball Edge
    },
    StoppingCondition={
        'MaxRuntimeInSeconds': 900
    }
)
```

### Running Inference on Snowball Edge

```python
"""
Run Neo-compiled model on Snowball Edge EC2 instance.
"""
import numpy as np
import dlr  # Deep Learning Runtime (Neo runtime)
import json
import time

# Load Neo-compiled model
model = dlr.DLRModel(
    '/opt/models/anomaly-detection',  # Path to compiled model
    'cpu'  # or 'gpu' for GPU-enabled device
)

def predict_anomaly(sensor_data):
    """Run inference on sensor reading"""
    # Prepare input features
    features = np.array([[
        sensor_data['temperature_c'],
        sensor_data['pressure_psi'],
        sensor_data['vibration_hz'],
        sensor_data['humidity_pct'],
        sensor_data['power_watts'],
        sensor_data.get('rpm', 0),
        sensor_data.get('flow_rate', 0),
        sensor_data.get('voltage', 0),
        sensor_data.get('current', 0),
        sensor_data.get('noise_db', 0),
        sensor_data.get('ph_level', 7.0),
        sensor_data.get('dissolved_oxygen', 0),
        sensor_data.get('turbidity', 0)
    ]], dtype=np.float32)
    
    # Run inference
    start = time.time()
    result = model.run(features)
    latency_ms = (time.time() - start) * 1000
    
    probability = float(result[0][0])
    
    return {
        'anomaly_probability': probability,
        'is_anomaly': probability > 0.7,
        'inference_latency_ms': round(latency_ms, 2),
        'model_version': '1.0.0'
    }


# Benchmark
if __name__ == '__main__':
    test_data = {
        'temperature_c': 78.5,
        'pressure_psi': 18.2,
        'vibration_hz': 320.0,
        'humidity_pct': 65.0,
        'power_watts': 3200.0
    }
    
    # Warmup
    for _ in range(10):
        predict_anomaly(test_data)
    
    # Benchmark
    latencies = []
    for _ in range(1000):
        result = predict_anomaly(test_data)
        latencies.append(result['inference_latency_ms'])
    
    print(f"P50 latency: {np.percentile(latencies, 50):.2f} ms")
    print(f"P95 latency: {np.percentile(latencies, 95):.2f} ms")
    print(f"P99 latency: {np.percentile(latencies, 99):.2f} ms")
    print(f"Throughput: {1000 / np.mean(latencies):.0f} inferences/sec")
```

---

## 3.6 Edge Computing Summary

### What You Can Run on Snowball Edge

| Capability | Storage Optimized | Compute Optimized | Compute + GPU |
|------------|:-----------------:|:-----------------:|:-------------:|
| Lambda functions | ✅ | ✅ | ✅ |
| EC2 instances | ✅ (limited) | ✅ (full) | ✅ (full) |
| EKS Anywhere | ❌ | ✅ | ✅ |
| IoT Greengrass | ✅ | ✅ | ✅ |
| SageMaker Neo | ✅ (CPU) | ✅ (CPU) | ✅ (GPU) |
| GPU workloads | ❌ | ❌ | ✅ (V100) |

### Resource Planning

```
Example: Factory with 100 sensors, data every second

Data Rate: 100 sensors × 1 reading/sec × 500 bytes = 50 KB/s = 4.3 GB/day

Lambda needs:
- 8 concurrent invocations covers 100 readings/sec easily
- 512 MB memory per function sufficient

EC2 for ML inference:
- sbe1.xlarge (8 vCPU, 16 GB RAM) handles ~500 inferences/sec
- GPU instance (sbe-g.4xlarge) handles ~5000 inferences/sec

Storage (30-day on-site):
- Raw data: 4.3 GB/day × 30 = 129 GB
- Processed + predictions: ~200 GB total
- Well within 210 TB capacity
```

---

## Next: [Part 4 - Advanced Features →](Part4_Advanced_Features.md)
