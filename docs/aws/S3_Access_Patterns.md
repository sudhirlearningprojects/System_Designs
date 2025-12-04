# S3 Access Patterns from AWS Services

## Overview
This document explains multiple ways to access Amazon S3 buckets and objects from various AWS services across different scenarios.

## Table of Contents
1. [EC2 Instance Access](#1-ec2-instance-access)
2. [Lambda Function Access](#2-lambda-function-access)
3. [ECS/EKS Access](#3-ecseks-access)
4. [RDS Access](#4-rds-access)
5. [DynamoDB Integration](#5-dynamodb-integration)
6. [Streaming Services](#6-streaming-services)
7. [Analytics Services](#7-analytics-services)
8. [Machine Learning Services](#8-machine-learning-services)
9. [Database Services](#9-database-services)
10. [Application Services](#10-application-services)
11. [Security Best Practices](#11-security-best-practices)

---

## 1. EC2 Instance Access

### Method 1: IAM Role (Recommended)
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": [
      "s3:GetObject",
      "s3:PutObject",
      "s3:ListBucket"
    ],
    "Resource": [
      "arn:aws:s3:::my-bucket",
      "arn:aws:s3:::my-bucket/*"
    ]
  }]
}
```

**Usage in EC2:**
```bash
# AWS CLI (uses instance profile automatically)
aws s3 cp s3://my-bucket/file.txt .

# Python SDK
import boto3
s3 = boto3.client('s3')
s3.download_file('my-bucket', 'file.txt', '/tmp/file.txt')
```

### Method 2: Access Keys (Not Recommended)
```bash
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
aws s3 ls s3://my-bucket/
```

### Method 3: S3 VPC Endpoint (Private Access)
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:*",
    "Resource": [
      "arn:aws:s3:::my-bucket",
      "arn:aws:s3:::my-bucket/*"
    ],
    "Condition": {
      "StringEquals": {
        "aws:SourceVpce": "vpce-1a2b3c4d"
      }
    }
  }]
}
```

### Scenarios:
- **Web Server**: Upload user files, serve static content
- **Batch Processing**: Read input files, write results
- **Backup**: Automated backup scripts
- **Log Aggregation**: Collect and store logs

---

## 2. Lambda Function Access

### Method 1: Execution Role
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": [
      "s3:GetObject",
      "s3:PutObject"
    ],
    "Resource": "arn:aws:s3:::my-bucket/*"
  }]
}
```

**Lambda Function:**
```python
import boto3
import json

s3 = boto3.client('s3')

def lambda_handler(event, context):
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = event['Records'][0]['s3']['object']['key']
    
    # Read object
    response = s3.get_object(Bucket=bucket, Key=key)
    content = response['Body'].read()
    
    # Process and write
    s3.put_object(
        Bucket='output-bucket',
        Key=f'processed/{key}',
        Body=content
    )
    
    return {'statusCode': 200}
```

### Method 2: S3 Event Trigger
```json
{
  "LambdaFunctionConfigurations": [{
    "LambdaFunctionArn": "arn:aws:lambda:us-east-1:123456789012:function:ProcessS3",
    "Events": ["s3:ObjectCreated:*"],
    "Filter": {
      "Key": {
        "FilterRules": [{
          "Name": "prefix",
          "Value": "uploads/"
        }]
      }
    }
  }]
}
```

### Method 3: Presigned URLs
```python
def generate_presigned_url(bucket, key, expiration=3600):
    s3 = boto3.client('s3')
    url = s3.generate_presigned_url(
        'get_object',
        Params={'Bucket': bucket, 'Key': key},
        ExpiresIn=expiration
    )
    return url
```

### Scenarios:
- **Image Processing**: Resize, compress, format conversion
- **ETL Pipeline**: Extract, transform, load data
- **Real-time Processing**: Process files as they arrive
- **Serverless APIs**: Generate presigned URLs for uploads

---

## 3. ECS/EKS Access

### Method 1: Task Role (ECS)
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": [
      "s3:GetObject",
      "s3:PutObject",
      "s3:ListBucket"
    ],
    "Resource": [
      "arn:aws:s3:::my-bucket",
      "arn:aws:s3:::my-bucket/*"
    ]
  }]
}
```

**ECS Task Definition:**
```json
{
  "family": "my-app",
  "taskRoleArn": "arn:aws:iam::123456789012:role/ecsTaskRole",
  "containerDefinitions": [{
    "name": "app",
    "image": "my-app:latest",
    "environment": [
      {"name": "S3_BUCKET", "value": "my-bucket"}
    ]
  }]
}
```

### Method 2: IRSA (EKS - IAM Roles for Service Accounts)
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: s3-access-sa
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/s3-access-role
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  template:
    spec:
      serviceAccountName: s3-access-sa
      containers:
      - name: app
        image: my-app:latest
        env:
        - name: S3_BUCKET
          value: my-bucket
```

**Application Code:**
```java
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

S3Client s3 = S3Client.builder().build();
GetObjectRequest request = GetObjectRequest.builder()
    .bucket("my-bucket")
    .key("data.json")
    .build();
s3.getObject(request);
```

### Method 3: Node IAM Role (Not Recommended)
Attach IAM role to EC2 nodes - all pods inherit permissions.

### Scenarios:
- **Microservices**: Store/retrieve configuration, assets
- **Data Processing**: Batch jobs reading from S3
- **ML Training**: Load training data from S3
- **Logging**: Ship container logs to S3

---

## 4. RDS Access

### Method 1: S3 Integration (Aurora/PostgreSQL/MySQL)
```sql
-- Enable S3 integration
SELECT aws_s3.table_import_from_s3(
   'my_table',
   '',
   '(format csv, header true)',
   'my-bucket',
   'data/input.csv',
   'us-east-1'
);

-- Export to S3
SELECT * FROM aws_s3.query_export_to_s3(
   'SELECT * FROM my_table',
   'my-bucket',
   'data/output.csv',
   'us-east-1'
);
```

**IAM Role for RDS:**
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": [
      "s3:GetObject",
      "s3:PutObject",
      "s3:ListBucket"
    ],
    "Resource": [
      "arn:aws:s3:::my-bucket",
      "arn:aws:s3:::my-bucket/*"
    ]
  }]
}
```

### Method 2: Aurora S3 Export
```bash
aws rds start-export-task \
  --export-task-identifier my-export \
  --source-arn arn:aws:rds:us-east-1:123456789012:snapshot:my-snapshot \
  --s3-bucket-name my-bucket \
  --iam-role-arn arn:aws:iam::123456789012:role/rds-s3-export-role \
  --kms-key-id arn:aws:kms:us-east-1:123456789012:key/abcd1234
```

### Scenarios:
- **Data Import**: Bulk load CSV/JSON from S3
- **Data Export**: Backup tables to S3
- **ETL**: Export for analytics processing
- **Disaster Recovery**: Automated backups to S3

---

## 5. DynamoDB Integration

### Method 1: DynamoDB Streams + Lambda + S3
```python
import boto3
import json

dynamodb = boto3.resource('dynamodb')
s3 = boto3.client('s3')

def lambda_handler(event, context):
    for record in event['Records']:
        if record['eventName'] == 'INSERT':
            item = record['dynamodb']['NewImage']
            
            # Archive to S3
            s3.put_object(
                Bucket='dynamodb-archive',
                Key=f"items/{item['id']['S']}.json",
                Body=json.dumps(item)
            )
```

### Method 2: Export to S3
```bash
aws dynamodb export-table-to-point-in-time \
  --table-arn arn:aws:dynamodb:us-east-1:123456789012:table/MyTable \
  --s3-bucket my-bucket \
  --s3-prefix exports/ \
  --export-format DYNAMODB_JSON
```

### Method 3: Import from S3
```bash
aws dynamodb import-table \
  --s3-bucket-source S3Bucket=my-bucket,S3KeyPrefix=data/ \
  --input-format CSV \
  --table-creation-parameters TableName=MyTable,KeySchema=[...],AttributeDefinitions=[...]
```

### Scenarios:
- **Cold Storage**: Archive old records to S3
- **Analytics**: Export for Athena/Redshift analysis
- **Backup**: Point-in-time backups
- **Data Migration**: Import bulk data

---

## 6. Streaming Services

### 6.1 Kinesis Data Firehose
```json
{
  "DeliveryStreamName": "s3-delivery-stream",
  "S3DestinationConfiguration": {
    "RoleARN": "arn:aws:iam::123456789012:role/firehose-role",
    "BucketARN": "arn:aws:s3:::my-bucket",
    "Prefix": "logs/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/",
    "BufferingHints": {
      "SizeInMBs": 5,
      "IntervalInSeconds": 300
    },
    "CompressionFormat": "GZIP"
  }
}
```

**Producer:**
```python
import boto3

firehose = boto3.client('firehose')

firehose.put_record(
    DeliveryStreamName='s3-delivery-stream',
    Record={'Data': b'log message\n'}
)
```

### 6.2 Kinesis Data Streams + Lambda
```python
def lambda_handler(event, context):
    s3 = boto3.client('s3')
    
    for record in event['Records']:
        payload = base64.b64decode(record['kinesis']['data'])
        
        s3.put_object(
            Bucket='stream-archive',
            Key=f"data/{record['kinesis']['sequenceNumber']}.json",
            Body=payload
        )
```

### 6.3 MSK (Managed Kafka) + S3 Sink Connector
```json
{
  "name": "s3-sink-connector",
  "config": {
    "connector.class": "io.confluent.connect.s3.S3SinkConnector",
    "tasks.max": "1",
    "topics": "my-topic",
    "s3.bucket.name": "my-bucket",
    "s3.region": "us-east-1",
    "flush.size": "1000",
    "storage.class": "io.confluent.connect.s3.storage.S3Storage",
    "format.class": "io.confluent.connect.s3.format.json.JsonFormat"
  }
}
```

### Scenarios:
- **Log Aggregation**: Stream logs to S3
- **Real-time Analytics**: Archive streaming data
- **Data Lake**: Build data lake from streams
- **Compliance**: Long-term storage of events

---

## 7. Analytics Services

### 7.1 Amazon Athena
```sql
-- Create external table
CREATE EXTERNAL TABLE logs (
  timestamp STRING,
  level STRING,
  message STRING
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe'
LOCATION 's3://my-bucket/logs/';

-- Query S3 data
SELECT level, COUNT(*) as count
FROM logs
WHERE timestamp > '2024-01-01'
GROUP BY level;
```

### 7.2 Amazon Redshift
```sql
-- Load from S3
COPY sales
FROM 's3://my-bucket/data/sales.csv'
IAM_ROLE 'arn:aws:iam::123456789012:role/RedshiftS3Role'
CSV
IGNOREHEADER 1;

-- Unload to S3
UNLOAD ('SELECT * FROM sales WHERE year = 2024')
TO 's3://my-bucket/exports/sales_2024_'
IAM_ROLE 'arn:aws:iam::123456789012:role/RedshiftS3Role'
PARALLEL OFF
GZIP;
```

### 7.3 AWS Glue
```python
import sys
from awsglue.transforms import *
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext

glueContext = GlueContext(SparkContext.getOrCreate())

# Read from S3
datasource = glueContext.create_dynamic_frame.from_options(
    connection_type="s3",
    connection_options={"paths": ["s3://my-bucket/input/"]},
    format="json"
)

# Transform and write to S3
glueContext.write_dynamic_frame.from_options(
    frame=datasource,
    connection_type="s3",
    connection_options={"path": "s3://my-bucket/output/"},
    format="parquet"
)
```

### 7.4 EMR (Elastic MapReduce)
```python
# PySpark on EMR
from pyspark.sql import SparkSession

spark = SparkSession.builder.appName("S3Access").getOrCreate()

# Read from S3
df = spark.read.json("s3://my-bucket/data/*.json")

# Process
result = df.groupBy("category").count()

# Write to S3
result.write.parquet("s3://my-bucket/results/")
```

### Scenarios:
- **Data Warehousing**: Load data into Redshift
- **Ad-hoc Queries**: Query S3 with Athena
- **ETL Jobs**: Transform data with Glue
- **Big Data Processing**: Spark jobs on EMR

---

## 8. Machine Learning Services

### 8.1 SageMaker
```python
import sagemaker
from sagemaker.estimator import Estimator

# Training data from S3
estimator = Estimator(
    image_uri='my-training-image',
    role='arn:aws:iam::123456789012:role/SageMakerRole',
    instance_count=1,
    instance_type='ml.m5.xlarge'
)

estimator.fit({
    'training': 's3://my-bucket/training-data/',
    'validation': 's3://my-bucket/validation-data/'
})

# Model artifacts saved to S3
model_data = estimator.model_data
```

### 8.2 Rekognition
```python
import boto3

rekognition = boto3.client('rekognition')

response = rekognition.detect_labels(
    Image={
        'S3Object': {
            'Bucket': 'my-bucket',
            'Name': 'photos/image.jpg'
        }
    },
    MaxLabels=10
)
```

### 8.3 Comprehend
```python
comprehend = boto3.client('comprehend')

# Batch analysis from S3
response = comprehend.start_sentiment_detection_job(
    InputDataConfig={
        'S3Uri': 's3://my-bucket/input/',
        'InputFormat': 'ONE_DOC_PER_LINE'
    },
    OutputDataConfig={
        'S3Uri': 's3://my-bucket/output/'
    },
    DataAccessRoleArn='arn:aws:iam::123456789012:role/ComprehendRole',
    LanguageCode='en'
)
```

### 8.4 Textract
```python
textract = boto3.client('textract')

response = textract.start_document_analysis(
    DocumentLocation={
        'S3Object': {
            'Bucket': 'my-bucket',
            'Name': 'documents/invoice.pdf'
        }
    },
    FeatureTypes=['TABLES', 'FORMS']
)
```

### Scenarios:
- **Model Training**: Load datasets from S3
- **Inference**: Process images/documents from S3
- **Feature Store**: Store features in S3
- **Model Registry**: Store model artifacts

---

## 9. Database Services

### 9.1 DocumentDB
```javascript
// Backup to S3
const { MongoClient } = require('mongodb');
const AWS = require('aws-sdk');
const s3 = new AWS.S3();

async function backupToS3() {
  const client = await MongoClient.connect(process.env.DOCDB_URI);
  const db = client.db('mydb');
  const collection = db.collection('mycollection');
  
  const data = await collection.find({}).toArray();
  
  await s3.putObject({
    Bucket: 'my-bucket',
    Key: `backups/${Date.now()}.json`,
    Body: JSON.stringify(data)
  }).promise();
}
```

### 9.2 Neptune
```python
# Export Neptune graph to S3
import boto3

neptune = boto3.client('neptune')

response = neptune.start_export_task(
    ClusterIdentifier='my-cluster',
    S3BucketName='my-bucket',
    S3Prefix='graph-exports/',
    IamRoleArn='arn:aws:iam::123456789012:role/NeptuneS3Role',
    ExportFormat='NTRIPLES'
)
```

### 9.3 ElastiCache
```python
# Backup Redis to S3 via Lambda
import boto3
import redis

def backup_redis_to_s3():
    r = redis.Redis(host='my-cluster.cache.amazonaws.com')
    s3 = boto3.client('s3')
    
    # Get all keys
    keys = r.keys('*')
    backup_data = {}
    
    for key in keys:
        backup_data[key.decode()] = r.get(key).decode()
    
    s3.put_object(
        Bucket='my-bucket',
        Key=f'redis-backups/{datetime.now().isoformat()}.json',
        Body=json.dumps(backup_data)
    )
```

### Scenarios:
- **Backup**: Automated database backups
- **Migration**: Export/import data
- **Analytics**: Export for analysis
- **Disaster Recovery**: Cross-region backups

---

## 10. Application Services

### 10.1 API Gateway
```yaml
# OpenAPI definition
paths:
  /upload:
    post:
      x-amazon-apigateway-integration:
        type: aws
        uri: arn:aws:apigateway:us-east-1:s3:path/{bucket}/{key}
        credentials: arn:aws:iam::123456789012:role/APIGatewayS3Role
        httpMethod: PUT
        requestParameters:
          integration.request.path.bucket: method.request.header.bucket
          integration.request.path.key: method.request.header.key
```

### 10.2 CloudFront + S3
```json
{
  "Origins": [{
    "Id": "S3-my-bucket",
    "DomainName": "my-bucket.s3.amazonaws.com",
    "S3OriginConfig": {
      "OriginAccessIdentity": "origin-access-identity/cloudfront/ABCDEFG1234567"
    }
  }],
  "DefaultCacheBehavior": {
    "TargetOriginId": "S3-my-bucket",
    "ViewerProtocolPolicy": "redirect-to-https"
  }
}
```

### 10.3 AppSync
```graphql
type Query {
  getDocument(key: String!): Document
    @aws_iam
    @aws_api_key
}

# Resolver
{
  "version": "2018-05-29",
  "operation": "Invoke",
  "payload": {
    "field": "getDocument",
    "arguments": $util.toJson($context.arguments)
  }
}
```

**Lambda Resolver:**
```python
def lambda_handler(event, context):
    s3 = boto3.client('s3')
    key = event['arguments']['key']
    
    response = s3.get_object(Bucket='my-bucket', Key=key)
    content = response['Body'].read().decode('utf-8')
    
    return {'key': key, 'content': content}
```

### 10.4 Step Functions
```json
{
  "StartAt": "ProcessFile",
  "States": {
    "ProcessFile": {
      "Type": "Task",
      "Resource": "arn:aws:states:::lambda:invoke",
      "Parameters": {
        "FunctionName": "ProcessS3File",
        "Payload": {
          "bucket.$": "$.bucket",
          "key.$": "$.key"
        }
      },
      "Next": "SaveToS3"
    },
    "SaveToS3": {
      "Type": "Task",
      "Resource": "arn:aws:states:::aws-sdk:s3:putObject",
      "Parameters": {
        "Bucket": "output-bucket",
        "Key.$": "$.processedKey",
        "Body.$": "$.result"
      },
      "End": true
    }
  }
}
```

### 10.5 EventBridge
```json
{
  "source": ["aws.s3"],
  "detail-type": ["Object Created"],
  "detail": {
    "bucket": {
      "name": ["my-bucket"]
    },
    "object": {
      "key": [{
        "prefix": "uploads/"
      }]
    }
  }
}
```

### Scenarios:
- **Static Website**: Host via CloudFront + S3
- **File Upload API**: Direct upload via API Gateway
- **Workflow Orchestration**: Step Functions processing
- **Event-Driven**: EventBridge triggers

---

## 11. Security Best Practices

### 11.1 Bucket Policies
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EnforceSSL",
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:*",
      "Resource": [
        "arn:aws:s3:::my-bucket",
        "arn:aws:s3:::my-bucket/*"
      ],
      "Condition": {
        "Bool": {
          "aws:SecureTransport": "false"
        }
      }
    },
    {
      "Sid": "AllowVPCEOnly",
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:*",
      "Resource": [
        "arn:aws:s3:::my-bucket",
        "arn:aws:s3:::my-bucket/*"
      ],
      "Condition": {
        "StringNotEquals": {
          "aws:SourceVpce": "vpce-1a2b3c4d"
        }
      }
    }
  ]
}
```

### 11.2 Encryption
```bash
# Server-side encryption (SSE-S3)
aws s3 cp file.txt s3://my-bucket/ --sse AES256

# SSE-KMS
aws s3 cp file.txt s3://my-bucket/ \
  --sse aws:kms \
  --sse-kms-key-id arn:aws:kms:us-east-1:123456789012:key/abcd1234

# Client-side encryption
aws s3 cp file.txt s3://my-bucket/ \
  --sse-c AES256 \
  --sse-c-key fileb://encryption-key.bin
```

### 11.3 Access Logging
```json
{
  "LoggingEnabled": {
    "TargetBucket": "my-logs-bucket",
    "TargetPrefix": "s3-access-logs/"
  }
}
```

### 11.4 Cross-Account Access
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "AWS": "arn:aws:iam::999999999999:role/CrossAccountRole"
    },
    "Action": [
      "s3:GetObject",
      "s3:ListBucket"
    ],
    "Resource": [
      "arn:aws:s3:::my-bucket",
      "arn:aws:s3:::my-bucket/*"
    ]
  }]
}
```

### 11.5 S3 Block Public Access
```bash
aws s3api put-public-access-block \
  --bucket my-bucket \
  --public-access-block-configuration \
    BlockPublicAcls=true,\
    IgnorePublicAcls=true,\
    BlockPublicPolicy=true,\
    RestrictPublicBuckets=true
```

---

## Summary Table

| Service | Access Method | Use Case | IAM Required |
|---------|--------------|----------|--------------|
| EC2 | Instance Profile | General compute | Yes |
| Lambda | Execution Role | Serverless processing | Yes |
| ECS | Task Role | Containerized apps | Yes |
| EKS | IRSA | Kubernetes workloads | Yes |
| RDS | DB Role | Data import/export | Yes |
| DynamoDB | Export/Import | Archival, analytics | Yes |
| Kinesis | Firehose | Stream to S3 | Yes |
| Athena | Direct | Query S3 data | Yes |
| Redshift | COPY/UNLOAD | Data warehouse | Yes |
| Glue | ETL Jobs | Data transformation | Yes |
| SageMaker | Training Jobs | ML workflows | Yes |
| API Gateway | Integration | Direct upload | Yes |
| CloudFront | OAI | CDN delivery | Yes |
| Step Functions | SDK Integration | Orchestration | Yes |

---

## Cost Optimization Tips

1. **Use S3 Intelligent-Tiering** for automatic cost optimization
2. **Enable S3 Transfer Acceleration** for faster uploads
3. **Use S3 Select** to retrieve subset of data
4. **Implement lifecycle policies** to move to cheaper storage classes
5. **Use VPC Endpoints** to avoid data transfer costs
6. **Enable compression** in Kinesis Firehose
7. **Use S3 Batch Operations** for bulk processing
8. **Monitor with S3 Storage Lens** for insights

---

## Monitoring and Observability

### CloudWatch Metrics
```python
cloudwatch = boto3.client('cloudwatch')

cloudwatch.put_metric_data(
    Namespace='S3Access',
    MetricData=[{
        'MetricName': 'DownloadLatency',
        'Value': latency_ms,
        'Unit': 'Milliseconds',
        'Dimensions': [
            {'Name': 'Bucket', 'Value': 'my-bucket'},
            {'Name': 'Service', 'Value': 'Lambda'}
        ]
    }]
)
```

### CloudTrail Logging
```json
{
  "eventVersion": "1.08",
  "eventSource": "s3.amazonaws.com",
  "eventName": "GetObject",
  "requestParameters": {
    "bucketName": "my-bucket",
    "key": "data/file.txt"
  },
  "responseElements": null,
  "userIdentity": {
    "type": "AssumedRole",
    "principalId": "AIDAI...",
    "arn": "arn:aws:sts::123456789012:assumed-role/MyRole/session"
  }
}
```

---

## Conclusion

S3 provides flexible access patterns for virtually every AWS service. Choose the appropriate method based on:
- **Security requirements**: IAM roles vs access keys
- **Network topology**: Public vs VPC endpoints
- **Performance needs**: Direct access vs caching
- **Cost constraints**: Data transfer and request costs
- **Compliance**: Encryption, logging, auditing

Always follow the principle of least privilege and enable encryption at rest and in transit.
