# Job Scheduler - API Documentation

## Base URL
```
https://api.jobscheduler.com/v1
```

## Authentication
All API endpoints require JWT authentication via the Authorization header:
```
Authorization: Bearer <jwt_token>
```

## Job Management API

### 1. Submit Job

**Endpoint:** `POST /jobs`

**Description:** Submit a new job for scheduling

**Request:**
```http
POST /jobs
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "Daily Report Generation",
  "type": "email",
  "scheduleType": "CRON",
  "scheduleValue": "0 0 9 * * *",
  "payload": {
    "recipients": ["admin@company.com", "manager@company.com"],
    "template": "daily-report",
    "parameters": {
      "date": "{{current_date}}",
      "department": "sales"
    }
  },
  "priority": 3,
  "maxRetries": 3,
  "timeoutSeconds": 300,
  "tags": {
    "department": "sales",
    "environment": "production"
  }
}
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Daily Report Generation",
  "type": "email",
  "scheduleType": "CRON",
  "scheduleValue": "0 0 9 * * *",
  "status": "SCHEDULED",
  "priority": 3,
  "maxRetries": 3,
  "currentRetries": 0,
  "createdAt": "2024-01-15T10:30:00Z",
  "scheduledAt": "2024-01-16T09:00:00Z",
  "nextExecutionAt": "2024-01-16T09:00:00Z",
  "createdBy": "user@company.com"
}
```

### 2. Get Job Details

**Endpoint:** `GET /jobs/{jobId}`

**Description:** Retrieve job details and current status

**Request:**
```http
GET /jobs/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Daily Report Generation",
  "type": "email",
  "scheduleType": "CRON",
  "scheduleValue": "0 0 9 * * *",
  "status": "COMPLETED",
  "priority": 3,
  "maxRetries": 3,
  "currentRetries": 0,
  "createdAt": "2024-01-15T10:30:00Z",
  "scheduledAt": "2024-01-16T09:00:00Z",
  "nextExecutionAt": "2024-01-17T09:00:00Z",
  "lastExecutionAt": "2024-01-16T09:00:02Z",
  "createdBy": "user@company.com"
}
```

### 3. List Jobs

**Endpoint:** `GET /jobs`

**Description:** List jobs with filtering and pagination

**Query Parameters:**
- `status` (optional): Filter by status (SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED, PAUSED)
- `type` (optional): Filter by job type
- `priority` (optional): Filter by priority level
- `createdBy` (optional): Filter by creator
- `limit` (optional): Number of results (default: 50, max: 100)
- `offset` (optional): Pagination offset (default: 0)

**Request:**
```http
GET /jobs?status=SCHEDULED&type=email&limit=20&offset=0
Authorization: Bearer <token>
```

**Response:**
```json
{
  "jobs": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Daily Report Generation",
      "type": "email",
      "status": "SCHEDULED",
      "priority": 3,
      "nextExecutionAt": "2024-01-16T09:00:00Z",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "pagination": {
    "limit": 20,
    "offset": 0,
    "total": 150,
    "hasMore": true
  }
}
```

### 4. Cancel Job

**Endpoint:** `POST /jobs/{jobId}/cancel`

**Description:** Cancel a scheduled job

**Request:**
```http
POST /jobs/550e8400-e29b-41d4-a716-446655440000/cancel
Authorization: Bearer <token>
```

**Response:**
```http
HTTP/1.1 200 OK
```

### 5. Pause Job

**Endpoint:** `POST /jobs/{jobId}/pause`

**Description:** Pause a recurring job (prevents future executions)

**Request:**
```http
POST /jobs/550e8400-e29b-41d4-a716-446655440000/pause
Authorization: Bearer <token>
```

**Response:**
```http
HTTP/1.1 200 OK
```

### 6. Resume Job

**Endpoint:** `POST /jobs/{jobId}/resume`

**Description:** Resume a paused job

**Request:**
```http
POST /jobs/550e8400-e29b-41d4-a716-446655440000/resume
Authorization: Bearer <token>
```

**Response:**
```http
HTTP/1.1 200 OK
```

### 7. Get Job Executions

**Endpoint:** `GET /jobs/{jobId}/executions`

**Description:** Get execution history for a job

**Query Parameters:**
- `status` (optional): Filter by execution status
- `limit` (optional): Number of results (default: 50)
- `offset` (optional): Pagination offset (default: 0)

**Request:**
```http
GET /jobs/550e8400-e29b-41d4-a716-446655440000/executions?limit=10
Authorization: Bearer <token>
```

**Response:**
```json
{
  "executions": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "jobId": "550e8400-e29b-41d4-a716-446655440000",
      "executionId": "550e8400-e29b-41d4-a716-446655440000-1642248600-0",
      "status": "COMPLETED",
      "startedAt": "2024-01-16T09:00:00Z",
      "completedAt": "2024-01-16T09:00:02Z",
      "durationMs": 2000,
      "result": {
        "emailsSent": 2,
        "reportGenerated": true
      },
      "retryCount": 0,
      "executorNode": "executor-node-1"
    }
  ],
  "pagination": {
    "limit": 10,
    "offset": 0,
    "total": 25,
    "hasMore": true
  }
}
```

## Job Types and Handlers

### 1. Email Job
```json
{
  "type": "email",
  "payload": {
    "recipients": ["user@example.com"],
    "subject": "Daily Report",
    "template": "daily-report",
    "parameters": {
      "date": "2024-01-16",
      "data": {...}
    }
  }
}
```

### 2. Data Processing Job
```json
{
  "type": "data_processing",
  "payload": {
    "inputPath": "s3://bucket/input/data.csv",
    "outputPath": "s3://bucket/output/processed.csv",
    "processingType": "aggregation",
    "parameters": {
      "groupBy": "department",
      "metrics": ["sum", "avg"]
    }
  }
}
```

### 3. Cleanup Job
```json
{
  "type": "cleanup",
  "payload": {
    "targetPath": "/tmp/logs",
    "olderThan": "7d",
    "filePattern": "*.log",
    "action": "delete"
  }
}
```

### 4. HTTP Webhook Job
```json
{
  "type": "webhook",
  "payload": {
    "url": "https://api.external.com/webhook",
    "method": "POST",
    "headers": {
      "Content-Type": "application/json",
      "Authorization": "Bearer token"
    },
    "body": {
      "event": "scheduled_task",
      "timestamp": "{{current_timestamp}}"
    }
  }
}
```

## Schedule Types

### 1. One-time Execution
```json
{
  "scheduleType": "ONCE",
  "scheduledAt": "2024-01-16T15:30:00Z"
}
```

### 2. Cron Expression
```json
{
  "scheduleType": "CRON",
  "scheduleValue": "0 0 9 * * MON-FRI"
}
```

**Common Cron Examples:**
- `0 0 * * *` - Daily at midnight
- `0 0 9 * * MON-FRI` - Weekdays at 9 AM
- `*/15 * * * *` - Every 15 minutes
- `0 0 1 * *` - First day of every month
- `0 0 9 * * SUN` - Every Sunday at 9 AM

### 3. Interval-based
```json
{
  "scheduleType": "INTERVAL",
  "scheduleValue": "300000"
}
```

**Interval Examples:**
- `60000` - Every minute (60,000 ms)
- `300000` - Every 5 minutes (300,000 ms)
- `3600000` - Every hour (3,600,000 ms)
- `86400000` - Every day (86,400,000 ms)

## Bulk Operations

### 1. Bulk Job Submission

**Endpoint:** `POST /jobs/bulk`

**Request:**
```json
{
  "jobs": [
    {
      "name": "Report 1",
      "type": "email",
      "scheduleType": "ONCE",
      "scheduledAt": "2024-01-16T10:00:00Z",
      "payload": {...}
    },
    {
      "name": "Report 2",
      "type": "email",
      "scheduleType": "ONCE",
      "scheduledAt": "2024-01-16T11:00:00Z",
      "payload": {...}
    }
  ]
}
```

**Response:**
```json
{
  "submitted": 2,
  "failed": 0,
  "jobs": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Report 1",
      "status": "SCHEDULED"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Report 2",
      "status": "SCHEDULED"
    }
  ]
}
```

### 2. Bulk Job Cancellation

**Endpoint:** `POST /jobs/bulk/cancel`

**Request:**
```json
{
  "jobIds": [
    "550e8400-e29b-41d4-a716-446655440000",
    "550e8400-e29b-41d4-a716-446655440001"
  ]
}
```

**Response:**
```json
{
  "cancelled": 2,
  "failed": 0,
  "results": [
    {
      "jobId": "550e8400-e29b-41d4-a716-446655440000",
      "status": "CANCELLED"
    },
    {
      "jobId": "550e8400-e29b-41d4-a716-446655440001",
      "status": "CANCELLED"
    }
  ]
}
```

## System Management API

### 1. Health Check

**Endpoint:** `GET /health`

**Description:** Service health status

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00Z",
  "components": {
    "database": {
      "status": "UP",
      "responseTime": "15ms",
      "connections": {
        "active": 5,
        "idle": 10,
        "max": 20
      }
    },
    "kafka": {
      "status": "UP",
      "responseTime": "5ms",
      "topics": {
        "job-ready-queue": "UP",
        "job-retry-queue": "UP",
        "job-dead-letter-queue": "UP"
      }
    },
    "scheduler": {
      "status": "UP",
      "scheduledJobs": 1250,
      "activeExecutions": 45,
      "leaseStatus": "HELD"
    }
  }
}
```

### 2. Metrics

**Endpoint:** `GET /metrics`

**Description:** Service metrics in Prometheus format

**Response:**
```
# HELP jobs_scheduled_total Total number of jobs scheduled
# TYPE jobs_scheduled_total counter
jobs_scheduled_total 12500

# HELP jobs_executed_total Total number of jobs executed
# TYPE jobs_executed_total counter
jobs_executed_total{status="completed"} 11800
jobs_executed_total{status="failed"} 200

# HELP job_execution_duration_seconds Job execution duration
# TYPE job_execution_duration_seconds histogram
job_execution_duration_seconds_bucket{le="1.0"} 8500
job_execution_duration_seconds_bucket{le="5.0"} 11200
job_execution_duration_seconds_bucket{le="10.0"} 11800

# HELP scheduler_queue_depth Current queue depth
# TYPE scheduler_queue_depth gauge
scheduler_queue_depth{queue="ready"} 25
scheduler_queue_depth{queue="retry"} 5
scheduler_queue_depth{queue="dead_letter"} 2
```

### 3. System Statistics

**Endpoint:** `GET /stats`

**Description:** Detailed system statistics

**Response:**
```json
{
  "jobs": {
    "total": 12500,
    "byStatus": {
      "SCHEDULED": 1250,
      "RUNNING": 45,
      "COMPLETED": 11000,
      "FAILED": 200,
      "CANCELLED": 5
    },
    "byType": {
      "email": 8000,
      "data_processing": 3000,
      "cleanup": 1000,
      "webhook": 500
    }
  },
  "executions": {
    "totalExecutions": 15000,
    "successRate": 94.5,
    "averageExecutionTime": 2.3,
    "executionsLast24h": 2400
  },
  "scheduler": {
    "activeNodes": 3,
    "scheduledJobs": 1250,
    "timingWheelSize": 3600,
    "leaseStatus": "ACTIVE"
  }
}
```

## Error Responses

### Standard Error Format
```json
{
  "error": {
    "code": "JOB_NOT_FOUND",
    "message": "Job with ID 550e8400-e29b-41d4-a716-446655440000 not found",
    "timestamp": "2024-01-15T10:30:00Z",
    "details": {
      "jobId": "550e8400-e29b-41d4-a716-446655440000"
    }
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_SCHEDULE` | 400 | Invalid cron expression or schedule format |
| `JOB_NOT_FOUND` | 404 | Job ID does not exist |
| `INVALID_JOB_TYPE` | 400 | Unsupported job type |
| `INVALID_STATE_TRANSITION` | 400 | Cannot perform operation in current job state |
| `SCHEDULE_CONFLICT` | 409 | Job with same name already scheduled |
| `PAYLOAD_TOO_LARGE` | 413 | Job payload exceeds size limit |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `SCHEDULER_UNAVAILABLE` | 503 | Scheduler service is unavailable |
| `INTERNAL_ERROR` | 500 | Internal server error |

## Rate Limiting

API endpoints are rate limited per user:

- **Job Submission**: 100 requests per minute
- **Job Queries**: 1000 requests per minute
- **Job Management**: 200 requests per minute
- **Bulk Operations**: 10 requests per minute

Rate limit headers:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642248660
```

## SDK Examples

### JavaScript/Node.js
```javascript
const JobSchedulerClient = require('@company/job-scheduler-client');

const client = new JobSchedulerClient({
  apiKey: 'your_api_key',
  baseUrl: 'https://api.jobscheduler.com/v1'
});

// Submit a job
const job = await client.submitJob({
  name: 'Daily Report',
  type: 'email',
  scheduleType: 'CRON',
  scheduleValue: '0 0 9 * * *',
  payload: {
    recipients: ['admin@company.com'],
    template: 'daily-report'
  }
});

console.log('Job scheduled:', job.id);
```

### Python
```python
from job_scheduler_client import JobSchedulerClient

client = JobSchedulerClient(
    api_key='your_api_key',
    base_url='https://api.jobscheduler.com/v1'
)

# Submit a job
job = client.submit_job(
    name='Daily Report',
    type='email',
    schedule_type='CRON',
    schedule_value='0 0 9 * * *',
    payload={
        'recipients': ['admin@company.com'],
        'template': 'daily-report'
    }
)

print(f'Job scheduled: {job.id}')
```

### Java
```java
JobSchedulerClient client = JobSchedulerClient.builder()
    .apiKey("your_api_key")
    .baseUrl("https://api.jobscheduler.com/v1")
    .build();

JobRequest request = JobRequest.builder()
    .name("Daily Report")
    .type("email")
    .scheduleType(ScheduleType.CRON)
    .scheduleValue("0 0 9 * * *")
    .payload(Map.of(
        "recipients", List.of("admin@company.com"),
        "template", "daily-report"
    ))
    .build();

JobResponse job = client.submitJob(request);
System.out.println("Job scheduled: " + job.getId());
```

### cURL Examples

```bash
# Submit a job
curl -X POST https://api.jobscheduler.com/v1/jobs \
  -H "Authorization: Bearer your_token" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Daily Cleanup",
    "type": "cleanup",
    "scheduleType": "CRON",
    "scheduleValue": "0 0 2 * * *",
    "payload": {
      "targetPath": "/tmp/logs",
      "olderThan": "7d"
    }
  }'

# Get job status
curl -X GET https://api.jobscheduler.com/v1/jobs/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer your_token"

# Cancel a job
curl -X POST https://api.jobscheduler.com/v1/jobs/550e8400-e29b-41d4-a716-446655440000/cancel \
  -H "Authorization: Bearer your_token"
```