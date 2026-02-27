# Alert Manager - Complete Coding Guide

## Table of Contents
1. [Introduction](#introduction)
2. [System Design Overview](#system-design-overview)
3. [SOLID Principles Applied](#solid-principles-applied)
4. [Design Patterns Used](#design-patterns-used)
5. [Authentication with Jira](#authentication-with-jira)
6. [Complete Single-File Implementation](#complete-single-file-implementation)
7. [How to Run](#how-to-run)
8. [Testing Guide](#testing-guide)

---

## Introduction

This guide explains how to build an Alert Manager system that integrates with ticket management tools like Jira. The system receives webhook events from Jira when tickets are created/updated and sends notifications to multiple channels (Slack, Email, etc.).

### Core Concepts

**Problem**: When a Jira ticket is created or updated, notify the team via Slack, Email, or other channels based on configurable rules.

**Solution**: Build a webhook receiver that:
1. Receives Jira webhook events
2. Matches events against configured alert rules
3. Sends notifications to configured channels
4. Tracks delivery status

---

## System Design Overview

### Architecture

```
Jira → Webhook (POST) → Alert Manager → Rule Engine → Channel Dispatcher → Slack/Email/SMS
```

### Components

1. **Webhook Receiver**: Receives HTTP POST from Jira
2. **Alert Rule Engine**: Matches events against rules
3. **Channel Handlers**: Send notifications (Strategy Pattern)
4. **Data Store**: Store rules, channels, alerts

---

## SOLID Principles Applied

### 1. Single Responsibility Principle (SRP)
Each class has ONE reason to change:
- `AlertRule`: Manages rule data only
- `SlackHandler`: Only handles Slack notifications
- `AlertService`: Only processes alerts

### 2. Open/Closed Principle (OCP)
System is open for extension, closed for modification:
- Add new channels by implementing `ChannelHandler` interface
- No need to modify existing code

### 3. Liskov Substitution Principle (LSP)
Any `ChannelHandler` can replace another:
- All handlers implement same interface
- Can swap Slack with Email without breaking code

### 4. Interface Segregation Principle (ISP)
Small, focused interfaces:
- `ChannelHandler`: Only `send()` method
- No fat interfaces with unused methods

### 5. Dependency Inversion Principle (DIP)
Depend on abstractions, not concrete classes:
- `AlertService` depends on `ChannelHandler` interface
- Not on `SlackHandler` or `EmailHandler` directly

---

## Design Patterns Used

### 1. Strategy Pattern
**Purpose**: Select notification channel at runtime

```java
interface ChannelHandler {
    void send(Alert alert);
}

class SlackHandler implements ChannelHandler { }
class EmailHandler implements ChannelHandler { }
```

### 2. Factory Pattern
**Purpose**: Create appropriate channel handler

```java
class ChannelFactory {
    ChannelHandler getHandler(String type) {
        return switch(type) {
            case "SLACK" -> new SlackHandler();
            case "EMAIL" -> new EmailHandler();
        };
    }
}
```

### 3. Observer Pattern
**Purpose**: Alert rules observe ticket events

```java
// Rules "observe" ticket events
// When event occurs, notify all matching rules
```

### 4. Repository Pattern
**Purpose**: Abstract data access

```java
interface AlertRuleRepository {
    List<AlertRule> findByProject(String project);
}
```

---

## Authentication with Jira

### How Jira Webhooks Work

1. **Configure Webhook in Jira**:
   - Go to: Settings → System → WebHooks
   - URL: `https://your-server.com/api/webhooks/jira`
   - Events: Issue Created, Issue Updated

2. **Jira sends HTTP POST**:
```json
POST /api/webhooks/jira
Content-Type: application/json
X-Atlassian-Webhook-Identifier: unique-id

{
  "webhookEvent": "jira:issue_created",
  "issue": {
    "key": "PROJ-123",
    "fields": { ... }
  }
}
```

### Authentication Methods

#### Method 1: Webhook Secret (Recommended)
```java
// Jira sends signature in header
String signature = request.getHeader("X-Hub-Signature");
String payload = request.getBody();

// Verify signature
String computed = HMAC_SHA256(payload, SECRET_KEY);
if (!computed.equals(signature)) {
    throw new UnauthorizedException();
}
```

#### Method 2: IP Whitelist
```java
String clientIp = request.getRemoteAddr();
List<String> allowedIps = List.of("52.1.2.3", "52.4.5.6"); // Jira IPs

if (!allowedIps.contains(clientIp)) {
    throw new UnauthorizedException();
}
```

#### Method 3: API Key
```java
String apiKey = request.getHeader("X-API-Key");
if (!apiKey.equals(CONFIGURED_API_KEY)) {
    throw new UnauthorizedException();
}
```

### Jira API Authentication (For Outbound Calls)

If you need to call Jira API:

```java
// Basic Auth
String auth = Base64.encode("email:api_token");
request.setHeader("Authorization", "Basic " + auth);

// OAuth 2.0
String token = getOAuthToken();
request.setHeader("Authorization", "Bearer " + token);
```

---

## Complete Single-File Implementation

Below is a complete, runnable implementation that demonstrates all concepts:

```java
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;

// ============================================
// DOMAIN MODELS
// ============================================

enum ChannelType {
    SLACK, EMAIL, SMS
}

enum EventType {
    CREATED, UPDATED, ASSIGNED, RESOLVED
}

class Alert {
    String id;
    String ticketId;
    String projectKey;
    EventType eventType;
    String message;
    Map<String, String> metadata;
    LocalDateTime createdAt;

    public Alert(String ticketId, String projectKey, EventType eventType, String message) {
        this.id = UUID.randomUUID().toString();
        this.ticketId = ticketId;
        this.projectKey = projectKey;
        this.eventType = eventType;
        this.message = message;
        this.metadata = new HashMap<>();
        this.createdAt = LocalDateTime.now();
    }
}

class AlertRule {
    String id;
    String name;
    String projectKey;
    List<EventType> triggerEvents;
    List<String> channelIds;
    boolean enabled;

    public AlertRule(String name, String projectKey, List<EventType> events, List<String> channels) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.projectKey = projectKey;
        this.triggerEvents = events;
        this.channelIds = channels;
        this.enabled = true;
    }

    public boolean matches(String project, EventType event) {
        return enabled && projectKey.equals(project) && triggerEvents.contains(event);
    }
}

class NotificationChannel {
    String id;
    String name;
    ChannelType type;
    Map<String, String> config;
    boolean enabled;

    public NotificationChannel(String name, ChannelType type, Map<String, String> config) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
        this.config = config;
        this.enabled = true;
    }
}

// ============================================
// STRATEGY PATTERN - Channel Handlers
// ============================================

interface ChannelHandler {
    ChannelType getType();
    void send(Alert alert, NotificationChannel channel);
}

class SlackHandler implements ChannelHandler {
    @Override
    public ChannelType getType() {
        return ChannelType.SLACK;
    }

    @Override
    public void send(Alert alert, NotificationChannel channel) {
        String webhookUrl = channel.config.get("webhookUrl");
        System.out.println("[SLACK] Sending to " + webhookUrl);
        System.out.println("  Message: " + alert.message);
        System.out.println("  Ticket: " + alert.ticketId);
        // In real implementation: HTTP POST to Slack webhook
    }
}

class EmailHandler implements ChannelHandler {
    @Override
    public ChannelType getType() {
        return ChannelType.EMAIL;
    }

    @Override
    public void send(Alert alert, NotificationChannel channel) {
        String recipients = channel.config.get("recipients");
        System.out.println("[EMAIL] Sending to " + recipients);
        System.out.println("  Subject: [" + alert.projectKey + "] " + alert.eventType);
        System.out.println("  Body: " + alert.message);
        // In real implementation: Send via SMTP or SendGrid API
    }
}

class SMSHandler implements ChannelHandler {
    @Override
    public ChannelType getType() {
        return ChannelType.SMS;
    }

    @Override
    public void send(Alert alert, NotificationChannel channel) {
        String phoneNumbers = channel.config.get("phoneNumbers");
        System.out.println("[SMS] Sending to " + phoneNumbers);
        System.out.println("  Message: " + alert.message);
        // In real implementation: Send via Twilio API
    }
}

// ============================================
// FACTORY PATTERN - Channel Factory
// ============================================

class ChannelHandlerFactory {
    private final Map<ChannelType, ChannelHandler> handlers = new HashMap<>();

    public ChannelHandlerFactory() {
        register(new SlackHandler());
        register(new EmailHandler());
        register(new SMSHandler());
    }

    private void register(ChannelHandler handler) {
        handlers.put(handler.getType(), handler);
    }

    public ChannelHandler getHandler(ChannelType type) {
        ChannelHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("No handler for type: " + type);
        }
        return handler;
    }
}

// ============================================
// REPOSITORY PATTERN - Data Access
// ============================================

interface AlertRuleRepository {
    void save(AlertRule rule);
    List<AlertRule> findByProject(String projectKey);
    List<AlertRule> findAll();
}

class InMemoryAlertRuleRepository implements AlertRuleRepository {
    private final Map<String, AlertRule> storage = new ConcurrentHashMap<>();

    @Override
    public void save(AlertRule rule) {
        storage.put(rule.id, rule);
    }

    @Override
    public List<AlertRule> findByProject(String projectKey) {
        return storage.values().stream()
            .filter(r -> r.projectKey.equals(projectKey) && r.enabled)
            .toList();
    }

    @Override
    public List<AlertRule> findAll() {
        return new ArrayList<>(storage.values());
    }
}

interface ChannelRepository {
    void save(NotificationChannel channel);
    NotificationChannel findById(String id);
    List<NotificationChannel> findByIds(List<String> ids);
}

class InMemoryChannelRepository implements ChannelRepository {
    private final Map<String, NotificationChannel> storage = new ConcurrentHashMap<>();

    @Override
    public void save(NotificationChannel channel) {
        storage.put(channel.id, channel);
    }

    @Override
    public NotificationChannel findById(String id) {
        return storage.get(id);
    }

    @Override
    public List<NotificationChannel> findByIds(List<String> ids) {
        return ids.stream()
            .map(storage::get)
            .filter(Objects::nonNull)
            .filter(c -> c.enabled)
            .toList();
    }
}

// ============================================
// SERVICE LAYER - Business Logic
// ============================================

class AlertService {
    private final AlertRuleRepository ruleRepository;
    private final ChannelRepository channelRepository;
    private final ChannelHandlerFactory handlerFactory;
    private final ExecutorService executor;

    public AlertService(AlertRuleRepository ruleRepo, ChannelRepository channelRepo) {
        this.ruleRepository = ruleRepo;
        this.channelRepository = channelRepo;
        this.handlerFactory = new ChannelHandlerFactory();
        this.executor = Executors.newFixedThreadPool(10);
    }

    public void processTicketEvent(String ticketId, String projectKey, EventType eventType, String message) {
        System.out.println("\n=== Processing Ticket Event ===");
        System.out.println("Ticket: " + ticketId);
        System.out.println("Project: " + projectKey);
        System.out.println("Event: " + eventType);

        // Create alert
        Alert alert = new Alert(ticketId, projectKey, eventType, message);

        // Find matching rules
        List<AlertRule> matchingRules = ruleRepository.findByProject(projectKey).stream()
            .filter(rule -> rule.matches(projectKey, eventType))
            .toList();

        System.out.println("Matching rules: " + matchingRules.size());

        // Send notifications for each rule
        for (AlertRule rule : matchingRules) {
            sendNotifications(alert, rule.channelIds);
        }
    }

    private void sendNotifications(Alert alert, List<String> channelIds) {
        List<NotificationChannel> channels = channelRepository.findByIds(channelIds);

        for (NotificationChannel channel : channels) {
            // Async delivery
            executor.submit(() -> {
                try {
                    ChannelHandler handler = handlerFactory.getHandler(channel.type);
                    handler.send(alert, channel);
                } catch (Exception e) {
                    System.err.println("Failed to send via " + channel.type + ": " + e.getMessage());
                }
            });
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}

// ============================================
// WEBHOOK AUTHENTICATION
// ============================================

class WebhookAuthenticator {
    private final String secretKey;
    private final Set<String> allowedIps;

    public WebhookAuthenticator(String secretKey, Set<String> allowedIps) {
        this.secretKey = secretKey;
        this.allowedIps = allowedIps;
    }

    public boolean verifySignature(String payload, String signature) {
        // In real implementation: HMAC-SHA256
        String computed = "hmac_sha256(" + payload + ", " + secretKey + ")";
        return computed.equals(signature);
    }

    public boolean verifyIpAddress(String clientIp) {
        return allowedIps.contains(clientIp);
    }

    public boolean verifyApiKey(String apiKey) {
        return secretKey.equals(apiKey);
    }
}

// ============================================
// JIRA WEBHOOK HANDLER
// ============================================

class JiraWebhookHandler {
    private final AlertService alertService;
    private final WebhookAuthenticator authenticator;

    public JiraWebhookHandler(AlertService alertService, WebhookAuthenticator authenticator) {
        this.alertService = alertService;
        this.authenticator = authenticator;
    }

    public void handleWebhook(Map<String, Object> payload, String signature, String clientIp) {
        // Authentication
        if (!authenticator.verifyIpAddress(clientIp)) {
            throw new SecurityException("Unauthorized IP: " + clientIp);
        }

        // Parse Jira payload
        String webhookEvent = (String) payload.get("webhookEvent");
        Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
        
        if (issue == null) return;

        String issueKey = (String) issue.get("key");
        Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
        Map<String, Object> project = (Map<String, Object>) fields.get("project");
        
        String projectKey = (String) project.get("key");
        String summary = (String) fields.get("summary");
        
        EventType eventType = mapJiraEvent(webhookEvent);
        
        // Process event
        alertService.processTicketEvent(issueKey, projectKey, eventType, summary);
    }

    private EventType mapJiraEvent(String jiraEvent) {
        return switch (jiraEvent) {
            case "jira:issue_created" -> EventType.CREATED;
            case "jira:issue_updated" -> EventType.UPDATED;
            default -> EventType.UPDATED;
        };
    }
}

// ============================================
// MAIN APPLICATION
// ============================================

public class AlertManagerDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Alert Manager System ===\n");

        // Initialize repositories
        AlertRuleRepository ruleRepo = new InMemoryAlertRuleRepository();
        ChannelRepository channelRepo = new InMemoryChannelRepository();

        // Initialize services
        AlertService alertService = new AlertService(ruleRepo, channelRepo);
        WebhookAuthenticator authenticator = new WebhookAuthenticator(
            "secret-key-123",
            Set.of("192.168.1.1", "10.0.0.1")
        );
        JiraWebhookHandler webhookHandler = new JiraWebhookHandler(alertService, authenticator);

        // Setup: Create notification channels
        System.out.println("=== Setup: Creating Channels ===");
        
        NotificationChannel slackChannel = new NotificationChannel(
            "Engineering Slack",
            ChannelType.SLACK,
            Map.of("webhookUrl", "https://hooks.slack.com/services/T00/B00/XXX")
        );
        channelRepo.save(slackChannel);
        System.out.println("Created Slack channel: " + slackChannel.id);

        NotificationChannel emailChannel = new NotificationChannel(
            "On-Call Email",
            ChannelType.EMAIL,
            Map.of("recipients", "oncall@company.com,manager@company.com")
        );
        channelRepo.save(emailChannel);
        System.out.println("Created Email channel: " + emailChannel.id);

        NotificationChannel smsChannel = new NotificationChannel(
            "Emergency SMS",
            ChannelType.SMS,
            Map.of("phoneNumbers", "+1234567890,+0987654321")
        );
        channelRepo.save(smsChannel);
        System.out.println("Created SMS channel: " + smsChannel.id);

        // Setup: Create alert rules
        System.out.println("\n=== Setup: Creating Alert Rules ===");
        
        AlertRule criticalRule = new AlertRule(
            "Critical Production Alerts",
            "PROD",
            List.of(EventType.CREATED, EventType.UPDATED),
            List.of(slackChannel.id, emailChannel.id, smsChannel.id)
        );
        ruleRepo.save(criticalRule);
        System.out.println("Created rule: " + criticalRule.name);

        AlertRule devRule = new AlertRule(
            "Development Alerts",
            "DEV",
            List.of(EventType.CREATED),
            List.of(slackChannel.id)
        );
        ruleRepo.save(devRule);
        System.out.println("Created rule: " + devRule.name);

        // Simulate Jira webhook events
        System.out.println("\n=== Simulating Jira Webhooks ===");

        // Event 1: Production issue created
        Map<String, Object> jiraPayload1 = Map.of(
            "webhookEvent", "jira:issue_created",
            "issue", Map.of(
                "key", "PROD-123",
                "fields", Map.of(
                    "summary", "Production API is down",
                    "project", Map.of("key", "PROD")
                )
            )
        );
        webhookHandler.handleWebhook(jiraPayload1, "signature", "192.168.1.1");

        Thread.sleep(1000);

        // Event 2: Development issue created
        Map<String, Object> jiraPayload2 = Map.of(
            "webhookEvent", "jira:issue_created",
            "issue", Map.of(
                "key", "DEV-456",
                "fields", Map.of(
                    "summary", "Add new feature",
                    "project", Map.of("key", "DEV")
                )
            )
        );
        webhookHandler.handleWebhook(jiraPayload2, "signature", "192.168.1.1");

        Thread.sleep(1000);

        // Event 3: Production issue updated
        Map<String, Object> jiraPayload3 = Map.of(
            "webhookEvent", "jira:issue_updated",
            "issue", Map.of(
                "key", "PROD-123",
                "fields", Map.of(
                    "summary", "Production API is down - URGENT",
                    "project", Map.of("key", "PROD")
                )
            )
        );
        webhookHandler.handleWebhook(jiraPayload3, "signature", "192.168.1.1");

        Thread.sleep(2000);

        // Cleanup
        alertService.shutdown();
        System.out.println("\n=== Demo Complete ===");
    }
}
```

---

## How to Run

### Option 1: Online Java Compiler

1. Go to: https://www.jdoodle.com/online-java-compiler
2. Copy the entire code above
3. Click "Execute"
4. See output showing notifications being sent

### Option 2: Local Machine

```bash
# Save as AlertManagerDemo.java
javac AlertManagerDemo.java
java AlertManagerDemo
```

### Expected Output

```
=== Alert Manager System ===

=== Setup: Creating Channels ===
Created Slack channel: abc-123
Created Email channel: def-456
Created SMS channel: ghi-789

=== Setup: Creating Alert Rules ===
Created rule: Critical Production Alerts
Created rule: Development Alerts

=== Simulating Jira Webhooks ===

=== Processing Ticket Event ===
Ticket: PROD-123
Project: PROD
Event: CREATED
Matching rules: 1
[SLACK] Sending to https://hooks.slack.com/services/T00/B00/XXX
  Message: Production API is down
  Ticket: PROD-123
[EMAIL] Sending to oncall@company.com,manager@company.com
  Subject: [PROD] CREATED
  Body: Production API is down
[SMS] Sending to +1234567890,+0987654321
  Message: Production API is down

=== Processing Ticket Event ===
Ticket: DEV-456
Project: DEV
Event: CREATED
Matching rules: 1
[SLACK] Sending to https://hooks.slack.com/services/T00/B00/XXX
  Message: Add new feature
  Ticket: DEV-456

=== Demo Complete ===
```

---

## Testing Guide

### Unit Tests

```java
// Test Strategy Pattern
@Test
void testSlackHandler() {
    ChannelHandler handler = new SlackHandler();
    Alert alert = new Alert("PROJ-1", "PROJ", EventType.CREATED, "Test");
    NotificationChannel channel = new NotificationChannel(
        "Test", ChannelType.SLACK, Map.of("webhookUrl", "http://test")
    );
    
    handler.send(alert, channel); // Should not throw
}

// Test Rule Matching
@Test
void testRuleMatching() {
    AlertRule rule = new AlertRule(
        "Test Rule", "PROD", 
        List.of(EventType.CREATED), 
        List.of("channel-1")
    );
    
    assertTrue(rule.matches("PROD", EventType.CREATED));
    assertFalse(rule.matches("DEV", EventType.CREATED));
    assertFalse(rule.matches("PROD", EventType.UPDATED));
}

// Test Authentication
@Test
void testWebhookAuth() {
    WebhookAuthenticator auth = new WebhookAuthenticator(
        "secret", Set.of("192.168.1.1")
    );
    
    assertTrue(auth.verifyIpAddress("192.168.1.1"));
    assertFalse(auth.verifyIpAddress("10.0.0.1"));
}
```

### Integration Test

```java
@Test
void testEndToEnd() {
    // Setup
    AlertRuleRepository ruleRepo = new InMemoryAlertRuleRepository();
    ChannelRepository channelRepo = new InMemoryChannelRepository();
    AlertService service = new AlertService(ruleRepo, channelRepo);
    
    // Create channel
    NotificationChannel channel = new NotificationChannel(
        "Test", ChannelType.SLACK, Map.of("webhookUrl", "http://test")
    );
    channelRepo.save(channel);
    
    // Create rule
    AlertRule rule = new AlertRule(
        "Test", "PROJ", List.of(EventType.CREATED), List.of(channel.id)
    );
    ruleRepo.save(rule);
    
    // Process event
    service.processTicketEvent("PROJ-1", "PROJ", EventType.CREATED, "Test message");
    
    // Verify notification sent (check logs or mock)
}
```

---

## Key Takeaways

### SOLID Principles
✅ **SRP**: Each class has one responsibility
✅ **OCP**: Add new channels without modifying existing code
✅ **LSP**: All handlers are interchangeable
✅ **ISP**: Small, focused interfaces
✅ **DIP**: Depend on abstractions (interfaces)

### Design Patterns
✅ **Strategy**: Different notification strategies
✅ **Factory**: Create handlers dynamically
✅ **Repository**: Abstract data access
✅ **Observer**: Rules observe events

### Authentication
✅ **Webhook Secret**: HMAC signature verification
✅ **IP Whitelist**: Restrict to known IPs
✅ **API Key**: Simple token-based auth

### Production Considerations
- Add retry mechanism for failed deliveries
- Implement circuit breaker for external APIs
- Add rate limiting to prevent abuse
- Store alerts in database for audit trail
- Add monitoring and alerting
- Implement proper error handling
- Use connection pooling for HTTP clients

---

## Next Steps

1. **Add Database**: Replace in-memory storage with PostgreSQL
2. **Add REST API**: Expose endpoints for CRUD operations
3. **Add Retry Logic**: Exponential backoff for failed deliveries
4. **Add Monitoring**: Prometheus metrics, health checks
5. **Add Security**: JWT authentication, HTTPS
6. **Add Testing**: Comprehensive unit and integration tests
7. **Add Documentation**: OpenAPI/Swagger specs

This implementation demonstrates all core concepts in a single, runnable file!
