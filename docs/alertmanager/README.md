# Alert Manager - Ticket Management Integration & Multi-Channel Notifications

A highly scalable, extensible alert management system that integrates with ticket management platforms (Jira, ServiceNow, GitHub) and sends real-time notifications through multiple channels (Slack, MS Teams, Discord, Email, SMS, PagerDuty, OpsGenie).

## 🎯 Overview

Alert Manager acts as a central notification hub for your ticket management workflow. When tickets are created, updated, assigned, or commented on, Alert Manager automatically sends notifications to configured channels based on customizable rules.

### Key Features

- **🔗 Ticket System Integration**: Native support for Jira, ServiceNow, GitHub Issues
- **📢 Multi-Channel Notifications**: Slack, MS Teams, Discord, Email, SMS, Webhook, PagerDuty, OpsGenie
- **⚙️ Flexible Rule Engine**: Configure alerts based on events, projects, priorities, and custom filters
- **🔌 Extensible Architecture**: Add new notification channels via plugin pattern
- **⚡ Real-time Processing**: Sub-500ms latency from event to notification
- **🔄 Reliable Delivery**: Retry mechanism with exponential backoff
- **📊 Delivery Tracking**: Complete audit trail of all notifications
- **🚀 High Performance**: Handle 10K+ events per second

---

## 🏗️ Architecture

```
Ticket Systems (Jira/ServiceNow) 
        ↓ Webhook
Alert Manager API
        ↓ Rule Matching
Notification Dispatcher (Async)
        ↓ Fan-out
Multiple Channels (Slack/Teams/Email/SMS/etc.)
```

**Components**:
- **Webhook Controller**: Receives ticket events
- **Alert Service**: Matches events against rules, creates alerts
- **Channel Handlers**: Channel-specific delivery logic (Strategy Pattern)
- **Delivery Tracker**: Monitors and retries failed deliveries

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Redis 6+ (optional, for caching)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/sudhir512kj/system-designs.git
cd system-designs
```

2. **Configure database**
```bash
# Create database
createdb alertmanager

# Update application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/alertmanager
spring.datasource.username=postgres
spring.datasource.password=password
```

3. **Build and run**
```bash
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=alertmanager
```

The service will start on `http://localhost:8100`

---

## 📖 Usage Guide

### Step 1: Create Notification Channels

#### Create Slack Channel
```bash
curl -X POST http://localhost:8100/api/v1/channels \
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

**Response**:
```json
{
  "id": "channel-abc123",
  "name": "Engineering Slack",
  "type": "SLACK",
  "enabled": true
}
```

#### Create MS Teams Channel
```bash
curl -X POST http://localhost:8100/api/v1/channels \
  -H "Content-Type: application/json" \
  -d '{
    "name": "DevOps Teams",
    "type": "MS_TEAMS",
    "configuration": {
      "webhookUrl": "https://outlook.office.com/webhook/YOUR/WEBHOOK/URL"
    },
    "enabled": true
  }'
```

#### Create Email Channel
```bash
curl -X POST http://localhost:8100/api/v1/channels \
  -H "Content-Type: application/json" \
  -d '{
    "name": "On-Call Email",
    "type": "EMAIL",
    "configuration": {
      "recipients": "oncall@company.com,manager@company.com",
      "apiKey": "sendgrid-api-key"
    },
    "enabled": true
  }'
```

#### Create PagerDuty Channel
```bash
curl -X POST http://localhost:8100/api/v1/channels \
  -H "Content-Type: application/json" \
  -d '{
    "name": "PagerDuty Incidents",
    "type": "PAGERDUTY",
    "configuration": {
      "integrationKey": "R0XXXXXXXXXXXXX"
    },
    "enabled": true
  }'
```

### Step 2: Create Alert Rules

```bash
curl -X POST http://localhost:8100/api/v1/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Critical Production Alerts",
    "description": "Notify on-call team for critical issues",
    "projectKey": "PROD",
    "triggerEvents": ["CREATED", "PRIORITY_CHANGED"],
    "channelIds": ["channel-abc123", "channel-def456"],
    "enabled": true,
    "filterCondition": "priority == '\''Critical'\''"
  }'
```

**Response**:
```json
{
  "id": "rule-xyz789",
  "name": "Critical Production Alerts",
  "projectKey": "PROD",
  "triggerEvents": ["CREATED", "PRIORITY_CHANGED"],
  "channelIds": ["channel-abc123", "channel-def456"],
  "enabled": true
}
```

### Step 3: Configure Jira Webhook

1. Go to **Jira Settings** → **System** → **WebHooks**
2. Click **Create a WebHook**
3. Configure:
   - **URL**: `http://your-server:8100/api/v1/webhooks/jira`
   - **Events**: Issue Created, Issue Updated, Issue Assigned, Comment Created
4. Save

### Step 4: Test the Integration

Create or update a Jira ticket in the configured project. Alert Manager will:
1. Receive the webhook event
2. Match against configured rules
3. Send notifications to all configured channels
4. Track delivery status

---

## 🔧 Supported Channels

| Channel | Type | Configuration |
|---------|------|---------------|
| **Slack** | `SLACK` | `webhookUrl` |
| **MS Teams** | `MS_TEAMS` | `webhookUrl` |
| **Discord** | `DISCORD` | `webhookUrl` |
| **Email** | `EMAIL` | `recipients`, `apiKey` |
| **SMS** | `SMS` | `phoneNumbers`, `accountSid`, `authToken` |
| **Webhook** | `WEBHOOK` | `webhookUrl`, `authHeader` |
| **PagerDuty** | `PAGERDUTY` | `integrationKey` |
| **OpsGenie** | `OPSGENIE` | `apiKey` |

---

## 📊 Supported Ticket Events

- `CREATED` - New ticket created
- `UPDATED` - Ticket fields updated
- `ASSIGNED` - Ticket assigned to user
- `STATUS_CHANGED` - Status changed (Open → In Progress)
- `PRIORITY_CHANGED` - Priority changed (Medium → Critical)
- `COMMENTED` - New comment added
- `RESOLVED` - Ticket marked as resolved
- `CLOSED` - Ticket closed
- `REOPENED` - Closed ticket reopened

---

## 🔌 Integration Examples

### Jira Integration

Alert Manager automatically maps Jira webhook events:

```
Jira Event              →  Alert Manager Event
─────────────────────────────────────────────────
jira:issue_created      →  CREATED
jira:issue_updated      →  UPDATED
jira:issue_assigned     →  ASSIGNED
comment_created         →  COMMENTED
```

### ServiceNow Integration

```bash
# Configure ServiceNow Business Rule to call webhook
curl -X POST http://your-server:8100/api/v1/alerts/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "ticketId": "INC0012345",
    "projectKey": "INCIDENT",
    "eventType": "CREATED",
    "summary": "Database connection timeout",
    "description": "Production DB unreachable",
    "metadata": {
      "priority": "1 - Critical",
      "assignee": "DBA Team"
    }
  }'
```

### GitHub Issues Integration

```bash
# Configure GitHub webhook
curl -X POST http://your-server:8100/api/v1/alerts/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "ticketId": "repo#123",
    "projectKey": "GITHUB",
    "eventType": "CREATED",
    "summary": "Bug: Memory leak in API",
    "description": "Memory usage increases over time",
    "metadata": {
      "labels": "bug,priority-high",
      "assignee": "developer1"
    }
  }'
```

---

## 🎨 Notification Examples

### Slack Notification
```
*CREATED*
[PROD] Production API down - Users unable to login
Ticket: PROD-123
```

### MS Teams Notification
```
Title: PRIORITY_CHANGED
Text: [PROD] Database performance degraded - Query timeout
Facts:
  - Ticket ID: PROD-456
  - Project: PROD
```

### Email Notification
```
Subject: [PROD-789] Critical: Payment gateway failure

Body:
Event: CREATED
Project: PROD
Ticket: PROD-789
Summary: Payment gateway failure
Description: Users unable to complete purchases

Priority: Critical
Assignee: Payment Team
```

---

## 🔒 Security

### API Key Management
- Store API keys encrypted in database
- Use environment variables for sensitive configuration
- Rotate keys regularly

### Webhook Authentication
```java
// Verify Jira webhook signature
@PostMapping("/webhooks/jira")
public ResponseEntity<String> handleJiraWebhook(
    @RequestHeader("X-Jira-Signature") String signature,
    @RequestBody String payload) {
    
    if (!verifySignature(signature, payload)) {
        return ResponseEntity.status(401).build();
    }
    // Process webhook
}
```

### HTTPS Only
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
```

---

## 📈 Performance & Scalability

### Performance Metrics
- **Throughput**: 10,000 events/second
- **Latency**: <500ms end-to-end
- **Availability**: 99.9% uptime
- **Concurrent Channels**: 100+ per alert

### Scaling Strategy
```
Load Balancer
    ├─ Alert Manager Instance 1
    ├─ Alert Manager Instance 2
    └─ Alert Manager Instance N

Shared:
- PostgreSQL (Primary + Read Replicas)
- Redis Cluster (Caching)
```

### Async Processing
```java
@Async("asyncExecutor")
public void sendNotifications(Alert alert, List<String> channelIds) {
    // Non-blocking parallel delivery to all channels
}
```

---

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8100/actuator/health
```

### Metrics (Prometheus)
```
# Alert processing time
alert_processing_time_seconds

# Delivery success/failure
alert_delivery_success_total
alert_delivery_failure_total

# Channel latency
notification_channel_latency_seconds
```

### Logging
```
2024-01-15 10:30:00 INFO  Alert created: alertId=abc123, ticketId=PROD-456, channels=[slack, teams]
2024-01-15 10:30:01 INFO  Sent Slack notification for alert abc123
2024-01-15 10:30:01 INFO  Sent MS Teams notification for alert abc123
```

---

## 🛠️ Configuration

### application.yml
```yaml
spring:
  application:
    name: alert-manager
  datasource:
    url: jdbc:postgresql://localhost:5432/alertmanager
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: 8100

async:
  core-pool-size: 10
  max-pool-size: 50
  queue-capacity: 500
```

---

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Manual Testing
```bash
# Test Slack notification
curl -X POST http://localhost:8100/api/v1/alerts/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "ticketId": "TEST-123",
    "projectKey": "TEST",
    "eventType": "CREATED",
    "summary": "Test alert",
    "description": "Testing notification delivery"
  }'
```

---

## 📚 Documentation

- [System Design](docs/alertmanager/System_Design.md) - Complete HLD/LLD
- [API Documentation](docs/alertmanager/API_Documentation.md) - REST API reference
- [Integration Guide](docs/alertmanager/Integration_Guide.md) - Jira/ServiceNow setup

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/new-channel`)
3. Commit changes (`git commit -am 'Add Discord channel support'`)
4. Push to branch (`git push origin feature/new-channel`)
5. Create Pull Request

---

## 📝 License

This project is licensed under the MIT License.

---

## 🙋 Support

For issues and questions:
- GitHub Issues: https://github.com/sudhir512kj/system-designs/issues
- Email: support@alertmanager.com

---

## 🎯 Roadmap

- [ ] Advanced filtering with SpEL expressions
- [ ] Message template customization
- [ ] Delivery scheduling (quiet hours)
- [ ] Alert aggregation and batching
- [ ] Analytics dashboard
- [ ] Mobile push notifications
- [ ] Voice call integration (Twilio)
- [ ] AI-powered channel routing
- [ ] Multi-tenancy support
