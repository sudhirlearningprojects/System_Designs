# Alert Manager API Documentation

## Base URL
```
Production: https://api.alertmanager.company.com
Development: http://localhost:8100
```

## Authentication
All API requests require authentication via API key:
```http
Authorization: Bearer <api-key>
```

---

## Table of Contents
1. [Webhook Endpoints](#webhook-endpoints)
2. [Alert Rule Management](#alert-rule-management)
3. [Notification Channel Management](#notification-channel-management)
4. [Alert History](#alert-history)
5. [Error Codes](#error-codes)

---

## Webhook Endpoints

### 1. Receive Jira Webhook

Endpoint for Jira to send ticket events.

**Endpoint**: `POST /api/v1/webhooks/jira`

**Request Body**:
```json
{
  "webhookEvent": "jira:issue_updated",
  "issue": {
    "key": "PROJ-123",
    "fields": {
      "summary": "Production API down",
      "description": "Users unable to login",
      "project": {
        "key": "PROJ"
      },
      "assignee": {
        "displayName": "John Doe"
      },
      "status": {
        "name": "In Progress"
      },
      "priority": {
        "name": "Critical"
      }
    }
  }
}
```

**Response**: `200 OK`
```json
{
  "message": "Webhook received"
}
```

**Supported Jira Events**:
- `jira:issue_created` → CREATED
- `jira:issue_updated` → UPDATED
- `jira:issue_assigned` → ASSIGNED
- `comment_created` → COMMENTED

---

### 2. Generic Ticket Event Webhook

Universal endpoint for any ticket management system.

**Endpoint**: `POST /api/v1/alerts/webhook`

**Request Body**:
```json
{
  "ticketId": "INC0012345",
  "projectKey": "INCIDENT",
  "eventType": "CREATED",
  "summary": "Database connection timeout",
  "description": "Production DB unreachable",
  "metadata": {
    "priority": "Critical",
    "assignee": "DBA Team",
    "status": "Open",
    "category": "Database"
  }
}
```

**Event Types**:
- `CREATED`
- `UPDATED`
- `ASSIGNED`
- `STATUS_CHANGED`
- `PRIORITY_CHANGED`
- `COMMENTED`
- `RESOLVED`
- `CLOSED`
- `REOPENED`

**Response**: `200 OK`
```json
{
  "message": "Event processed"
}
```

---

## Alert Rule Management

### 1. Create Alert Rule

**Endpoint**: `POST /api/v1/rules`

**Request Body**:
```json
{
  "name": "Critical Production Alerts",
  "description": "Notify on-call team for critical production issues",
  "projectKey": "PROD",
  "triggerEvents": ["CREATED", "PRIORITY_CHANGED"],
  "channelIds": ["channel-slack-1", "channel-pagerduty-1"],
  "enabled": true,
  "filterCondition": "priority == 'Critical' || priority == 'Blocker'"
}
```

**Response**: `200 OK`
```json
{
  "id": "rule-a1b2c3d4",
  "name": "Critical Production Alerts",
  "description": "Notify on-call team for critical production issues",
  "projectKey": "PROD",
  "triggerEvents": ["CREATED", "PRIORITY_CHANGED"],
  "channelIds": ["channel-slack-1", "channel-pagerduty-1"],
  "enabled": true,
  "filterCondition": "priority == 'Critical' || priority == 'Blocker'",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": null
}
```

---

### 2. Get All Alert Rules

**Endpoint**: `GET /api/v1/rules`

**Response**: `200 OK`
```json
[
  {
    "id": "rule-a1b2c3d4",
    "name": "Critical Production Alerts",
    "projectKey": "PROD",
    "triggerEvents": ["CREATED", "PRIORITY_CHANGED"],
    "channelIds": ["channel-slack-1", "channel-pagerduty-1"],
    "enabled": true,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "id": "rule-e5f6g7h8",
    "name": "Support Ticket Assignments",
    "projectKey": "SUPPORT",
    "triggerEvents": ["ASSIGNED"],
    "channelIds": ["channel-email-1"],
    "enabled": true,
    "createdAt": "2024-01-14T09:15:00Z"
  }
]
```

---

### 3. Get Alert Rule by ID

**Endpoint**: `GET /api/v1/rules/{ruleId}`

**Response**: `200 OK`
```json
{
  "id": "rule-a1b2c3d4",
  "name": "Critical Production Alerts",
  "description": "Notify on-call team for critical production issues",
  "projectKey": "PROD",
  "triggerEvents": ["CREATED", "PRIORITY_CHANGED"],
  "channelIds": ["channel-slack-1", "channel-pagerduty-1"],
  "enabled": true,
  "filterCondition": "priority == 'Critical'",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": null
}
```

---

### 4. Update Alert Rule

**Endpoint**: `PUT /api/v1/rules/{ruleId}`

**Request Body**:
```json
{
  "name": "Updated Rule Name",
  "description": "Updated description",
  "triggerEvents": ["CREATED", "UPDATED", "PRIORITY_CHANGED"],
  "channelIds": ["channel-slack-1", "channel-teams-1"],
  "enabled": false
}
```

**Response**: `200 OK`
```json
{
  "id": "rule-a1b2c3d4",
  "name": "Updated Rule Name",
  "description": "Updated description",
  "projectKey": "PROD",
  "triggerEvents": ["CREATED", "UPDATED", "PRIORITY_CHANGED"],
  "channelIds": ["channel-slack-1", "channel-teams-1"],
  "enabled": false,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-16T14:20:00Z"
}
```

---

### 5. Delete Alert Rule

**Endpoint**: `DELETE /api/v1/rules/{ruleId}`

**Response**: `204 No Content`

---

## Notification Channel Management

### 1. Create Notification Channel

**Endpoint**: `POST /api/v1/channels`

#### Slack Channel
```json
{
  "name": "Engineering Slack",
  "type": "SLACK",
  "configuration": {
    "webhookUrl": "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX"
  },
  "enabled": true
}
```

#### MS Teams Channel
```json
{
  "name": "DevOps Teams",
  "type": "MS_TEAMS",
  "configuration": {
    "webhookUrl": "https://outlook.office.com/webhook/a1b2c3d4@tenant.com/IncomingWebhook/e5f6g7h8/i9j0k1l2"
  },
  "enabled": true
}
```

#### Discord Channel
```json
{
  "name": "Operations Discord",
  "type": "DISCORD",
  "configuration": {
    "webhookUrl": "https://discord.com/api/webhooks/123456789/abcdefghijklmnop"
  },
  "enabled": true
}
```

#### Email Channel
```json
{
  "name": "On-Call Email",
  "type": "EMAIL",
  "configuration": {
    "recipients": "oncall@company.com,manager@company.com",
    "apiKey": "SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "fromEmail": "alerts@company.com",
    "fromName": "Alert Manager"
  },
  "enabled": true
}
```

#### SMS Channel
```json
{
  "name": "Emergency SMS",
  "type": "SMS",
  "configuration": {
    "phoneNumbers": "+1234567890,+0987654321",
    "accountSid": "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "authToken": "your_auth_token",
    "fromNumber": "+1555000000"
  },
  "enabled": true
}
```

#### PagerDuty Channel
```json
{
  "name": "PagerDuty Incidents",
  "type": "PAGERDUTY",
  "configuration": {
    "integrationKey": "R0XXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
  },
  "enabled": true
}
```

#### OpsGenie Channel
```json
{
  "name": "OpsGenie Alerts",
  "type": "OPSGENIE",
  "configuration": {
    "apiKey": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
  },
  "enabled": true
}
```

#### Generic Webhook Channel
```json
{
  "name": "Custom Webhook",
  "type": "WEBHOOK",
  "configuration": {
    "webhookUrl": "https://api.yourservice.com/alerts",
    "authHeader": "Bearer your-api-key"
  },
  "enabled": true
}
```

**Response**: `200 OK`
```json
{
  "id": "channel-a1b2c3d4",
  "name": "Engineering Slack",
  "type": "SLACK",
  "configuration": {
    "webhookUrl": "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX"
  },
  "enabled": true,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": null
}
```

---

### 2. Get All Notification Channels

**Endpoint**: `GET /api/v1/channels`

**Response**: `200 OK`
```json
[
  {
    "id": "channel-a1b2c3d4",
    "name": "Engineering Slack",
    "type": "SLACK",
    "enabled": true,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "id": "channel-e5f6g7h8",
    "name": "DevOps Teams",
    "type": "MS_TEAMS",
    "enabled": true,
    "createdAt": "2024-01-14T09:15:00Z"
  }
]
```

---

### 3. Get Notification Channel by ID

**Endpoint**: `GET /api/v1/channels/{channelId}`

**Response**: `200 OK`
```json
{
  "id": "channel-a1b2c3d4",
  "name": "Engineering Slack",
  "type": "SLACK",
  "configuration": {
    "webhookUrl": "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX"
  },
  "enabled": true,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": null
}
```

---

### 4. Update Notification Channel

**Endpoint**: `PUT /api/v1/channels/{channelId}`

**Request Body**:
```json
{
  "name": "Updated Slack Channel",
  "configuration": {
    "webhookUrl": "https://hooks.slack.com/services/NEW/WEBHOOK/URL"
  },
  "enabled": false
}
```

**Response**: `200 OK`
```json
{
  "id": "channel-a1b2c3d4",
  "name": "Updated Slack Channel",
  "type": "SLACK",
  "configuration": {
    "webhookUrl": "https://hooks.slack.com/services/NEW/WEBHOOK/URL"
  },
  "enabled": false,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-16T14:20:00Z"
}
```

---

### 5. Delete Notification Channel

**Endpoint**: `DELETE /api/v1/channels/{channelId}`

**Response**: `204 No Content`

---

## Alert History

### Get Alert Deliveries

**Endpoint**: `GET /api/v1/alerts/{alertId}/deliveries`

**Response**: `200 OK`
```json
[
  {
    "id": "delivery-a1b2c3d4",
    "alertId": "alert-x1y2z3",
    "channelId": "channel-slack-1",
    "channelType": "SLACK",
    "status": "DELIVERED",
    "retryCount": 0,
    "createdAt": "2024-01-15T10:30:00Z",
    "deliveredAt": "2024-01-15T10:30:02Z"
  },
  {
    "id": "delivery-e5f6g7h8",
    "alertId": "alert-x1y2z3",
    "channelId": "channel-email-1",
    "channelType": "EMAIL",
    "status": "FAILED",
    "retryCount": 3,
    "errorMessage": "SMTP connection timeout",
    "createdAt": "2024-01-15T10:30:00Z",
    "deliveredAt": null
  }
]
```

---

## Error Codes

### HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created |
| 204 | No Content (successful deletion) |
| 400 | Bad Request (invalid input) |
| 401 | Unauthorized (missing/invalid API key) |
| 404 | Not Found (resource doesn't exist) |
| 409 | Conflict (duplicate resource) |
| 429 | Too Many Requests (rate limit exceeded) |
| 500 | Internal Server Error |
| 503 | Service Unavailable |

### Error Response Format

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid channel configuration: webhookUrl is required for SLACK channel",
  "path": "/api/v1/channels"
}
```

---

## Rate Limits

| Endpoint | Limit |
|----------|-------|
| Webhook endpoints | 1000 requests/minute per project |
| Rule management | 100 requests/minute |
| Channel management | 100 requests/minute |

**Rate Limit Headers**:
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1642248600
```

---

## Integration Examples

### cURL Examples

#### Create Slack Channel
```bash
curl -X POST https://api.alertmanager.company.com/api/v1/channels \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Engineering Slack",
    "type": "SLACK",
    "configuration": {
      "webhookUrl": "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
    },
    "enabled": true
  }'
```

#### Create Alert Rule
```bash
curl -X POST https://api.alertmanager.company.com/api/v1/rules \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Critical Alerts",
    "projectKey": "PROD",
    "triggerEvents": ["CREATED", "PRIORITY_CHANGED"],
    "channelIds": ["channel-slack-1"],
    "enabled": true
  }'
```

### Python Example

```python
import requests

API_BASE = "https://api.alertmanager.company.com"
API_KEY = "your-api-key"

headers = {
    "Authorization": f"Bearer {API_KEY}",
    "Content-Type": "application/json"
}

# Create Slack channel
channel_data = {
    "name": "Engineering Slack",
    "type": "SLACK",
    "configuration": {
        "webhookUrl": "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
    },
    "enabled": True
}

response = requests.post(
    f"{API_BASE}/api/v1/channels",
    headers=headers,
    json=channel_data
)

channel = response.json()
print(f"Created channel: {channel['id']}")

# Create alert rule
rule_data = {
    "name": "Critical Alerts",
    "projectKey": "PROD",
    "triggerEvents": ["CREATED", "PRIORITY_CHANGED"],
    "channelIds": [channel['id']],
    "enabled": True
}

response = requests.post(
    f"{API_BASE}/api/v1/rules",
    headers=headers,
    json=rule_data
)

rule = response.json()
print(f"Created rule: {rule['id']}")
```

### JavaScript Example

```javascript
const API_BASE = 'https://api.alertmanager.company.com';
const API_KEY = 'your-api-key';

const headers = {
  'Authorization': `Bearer ${API_KEY}`,
  'Content-Type': 'application/json'
};

// Create MS Teams channel
const channelData = {
  name: 'DevOps Teams',
  type: 'MS_TEAMS',
  configuration: {
    webhookUrl: 'https://outlook.office.com/webhook/YOUR/WEBHOOK/URL'
  },
  enabled: true
};

fetch(`${API_BASE}/api/v1/channels`, {
  method: 'POST',
  headers: headers,
  body: JSON.stringify(channelData)
})
.then(response => response.json())
.then(channel => {
  console.log(`Created channel: ${channel.id}`);
  
  // Create alert rule
  const ruleData = {
    name: 'Support Tickets',
    projectKey: 'SUPPORT',
    triggerEvents: ['ASSIGNED', 'COMMENTED'],
    channelIds: [channel.id],
    enabled: true
  };
  
  return fetch(`${API_BASE}/api/v1/rules`, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify(ruleData)
  });
})
.then(response => response.json())
.then(rule => console.log(`Created rule: ${rule.id}`));
```
