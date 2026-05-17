# Cron Job System - API Documentation

## Base URL
```
http://localhost:8102/api/v1/cron
```

---

## Jobs API

### Create Cron Job
```http
POST /jobs
Content-Type: application/json

{
  "name": "cleanup-logs",
  "namespace": "infra",
  "cronExpression": "0 0 3 * * *",
  "timezone": "UTC",
  "taskType": "SHELL_COMMAND",
  "taskConfig": {
    "command": "find /var/log -name '*.log' -mtime +7 -delete"
  },
  "timeoutSeconds": 60,
  "maxRetries": 2,
  "concurrencyPolicy": "FORBID",
  "description": "Delete logs older than 7 days"
}
```

**Response** (200):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "cleanup-logs",
  "namespace": "infra",
  "cronExpression": "0 0 3 * * *",
  "timezone": "UTC",
  "status": "ACTIVE",
  "taskType": "SHELL_COMMAND",
  "concurrencyPolicy": "FORBID",
  "nextRunAt": "2024-01-16T03:00:00",
  "successCount": 0,
  "failureCount": 0
}
```

### Get Job
```http
GET /jobs/{id}
```

### List Jobs by Namespace
```http
GET /jobs?namespace=infra
```

### Pause Job
```http
POST /jobs/{id}/pause
```

### Resume Job
```http
POST /jobs/{id}/resume
```

### Delete Job
```http
DELETE /jobs/{id}
```

### Trigger Manual Execution
```http
POST /jobs/{id}/trigger
```

### Get Execution History
```http
GET /jobs/{id}/executions
```

**Response**:
```json
[
  {
    "id": "...",
    "jobId": "...",
    "status": "SUCCESS",
    "attemptNumber": 1,
    "startedAt": "2024-01-15T03:00:00",
    "completedAt": "2024-01-15T03:00:02",
    "durationMs": 2100,
    "executorNode": "node-1",
    "triggeredBy": "SCHEDULER"
  }
]
```

---

## DAGs API

### Create DAG
```http
POST /dags
Content-Type: application/json

{
  "name": "etl-pipeline",
  "namespace": "data-eng",
  "cronExpression": "0 0 2 * * *",
  "timezone": "America/Chicago",
  "maxParallelTasks": 3,
  "dagTimeoutSeconds": 7200,
  "description": "Nightly ETL pipeline"
}
```

---

## Task Types

### HTTP_WEBHOOK
```json
{
  "taskType": "HTTP_WEBHOOK",
  "taskConfig": {
    "url": "https://api.example.com/webhook",
    "method": "POST",
    "headers": {"X-API-Key": "<key>"},
    "body": {"event": "cron_trigger"}
  }
}
```

### SHELL_COMMAND
```json
{
  "taskType": "SHELL_COMMAND",
  "taskConfig": {
    "command": "python /opt/scripts/process.py --date=$(date +%Y-%m-%d)"
  }
}
```

### KAFKA_PUBLISH
```json
{
  "taskType": "KAFKA_PUBLISH",
  "taskConfig": {
    "topic": "cron-events",
    "key": "daily-trigger",
    "value": {"timestamp": "{{now}}"}
  }
}
```

---

## Cron Expression Format

Standard 6-field Spring cron format:
```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12)
│ │ │ │ │ ┌───────────── day of week (MON-SUN)
│ │ │ │ │ │
* * * * * *
```

**Examples**:
| Expression | Description |
|-----------|-------------|
| `0 0 * * * *` | Every hour |
| `0 0 9 * * MON-FRI` | 9 AM weekdays |
| `0 */15 * * * *` | Every 15 minutes |
| `0 0 2 * * *` | Daily at 2 AM |
| `0 0 0 1 * *` | First of every month |
