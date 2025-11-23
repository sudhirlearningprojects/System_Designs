# Cloud Infrastructure API Documentation

## Authentication

All API requests require authentication via `X-Account-Id` header.

```bash
X-Account-Id: acc-123
```

## Compute APIs

### Create Virtual Machine

**Endpoint**: `POST /api/v1/compute/vms`

**Request**:
```json
{
  "name": "web-server-1",
  "region": "us-east-1",
  "instanceType": "t2.medium",
  "imageId": "ami-ubuntu-20.04",
  "vpcId": "vpc-123",
  "subnetId": "subnet-456",
  "securityGroupId": "sg-789",
  "diskGb": 50
}
```

**Response**:
```json
{
  "id": "vm-a1b2c3d4",
  "name": "web-server-1",
  "state": "CREATING",
  "instanceType": "t2.medium",
  "vcpus": 2,
  "memoryGb": 4,
  "diskGb": 50,
  "region": "us-east-1",
  "createdAt": "2024-01-15T10:30:00"
}
```

### List Virtual Machines

**Endpoint**: `GET /api/v1/compute/vms`

**Response**:
```json
[
  {
    "id": "vm-a1b2c3d4",
    "name": "web-server-1",
    "state": "RUNNING",
    "publicIp": "54.123.45.67",
    "privateIp": "10.0.1.10"
  }
]
```

### Start/Stop/Terminate VM

**Endpoints**:
- `POST /api/v1/compute/vms/{vmId}/start`
- `POST /api/v1/compute/vms/{vmId}/stop`
- `DELETE /api/v1/compute/vms/{vmId}`

## Storage APIs

### Create Bucket

**Endpoint**: `POST /api/v1/storage/buckets`

**Request**:
```json
{
  "bucketName": "my-app-data",
  "region": "us-east-1",
  "storageClass": "STANDARD",
  "accessLevel": "PRIVATE",
  "versioningEnabled": true,
  "encryptionEnabled": true
}
```

**Response**:
```json
{
  "id": "bucket-x1y2z3",
  "bucketName": "my-app-data",
  "state": "RUNNING",
  "storageClass": "STANDARD",
  "sizeBytes": 0,
  "objectCount": 0
}
```

### Upload Object

**Endpoint**: `POST /api/v1/storage/buckets/{bucketName}/objects/{objectKey}`

**Request**: Multipart form data with file

**Response**: 200 OK

### Download Object

**Endpoint**: `GET /api/v1/storage/buckets/{bucketName}/objects/{objectKey}`

**Response**: Binary file data

## Instance Types

| Type | vCPUs | Memory | Price/Hour |
|------|-------|--------|------------|
| t2.micro | 1 | 1 GB | $0.01 |
| t2.small | 1 | 2 GB | $0.02 |
| t2.medium | 2 | 4 GB | $0.04 |
| t2.large | 2 | 8 GB | $0.08 |

## Storage Classes

| Class | Use Case | Price/GB/Month |
|-------|----------|----------------|
| STANDARD | Frequent access | $0.023 |
| INFREQUENT_ACCESS | Backup | $0.0125 |
| GLACIER | Archive | $0.004 |
| DEEP_ARCHIVE | Long-term | $0.001 |
