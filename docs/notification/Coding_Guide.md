# Notification System - Complete Coding Guide

## System Design Overview

**Problem**: Send notifications via multiple channels with retry

**Core Features**:
1. Multi-channel (Email, SMS, Push)
2. Priority-based delivery
3. Retry with exponential backoff
4. User preferences

## SOLID Principles

- **SRP**: Each channel handler separate
- **OCP**: Add new channels without modifying
- **Strategy**: Different retry strategies

## Design Patterns

1. **Strategy Pattern**: Channel handlers
2. **Template Method**: Retry logic
3. **Observer Pattern**: Delivery status updates

## Complete Implementation

```java
import java.util.*;
import java.util.concurrent.*;

enum Channel { EMAIL, SMS, PUSH }
enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
enum Status { PENDING, SENT, FAILED }

class Notification {
    String id, userId, message;
    Set<Channel> channels;
    Priority priority;
    Status status;
    int retryCount = 0;
    
    Notification(String userId, String message, Set<Channel> channels, Priority priority) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.userId = userId;
        this.message = message;
        this.channels = channels;
        this.priority = priority;
        this.status = Status.PENDING;
    }
}

interface ChannelHandler {
    boolean send(Notification notification) throws Exception;
    Channel getChannel();
}

class EmailHandler implements ChannelHandler {
    public boolean send(Notification notification) throws Exception {
        System.out.println("  [EMAIL] Sending to user " + notification.userId);
        if (Math.random() > 0.7) throw new Exception("SMTP timeout");
        return true;
    }
    public Channel getChannel() { return Channel.EMAIL; }
}

class SMSHandler implements ChannelHandler {
    public boolean send(Notification notification) throws Exception {
        System.out.println("  [SMS] Sending to user " + notification.userId);
        if (Math.random() > 0.8) throw new Exception("Twilio error");
        return true;
    }
    public Channel getChannel() { return Channel.SMS; }
}

class PushHandler implements ChannelHandler {
    public boolean send(Notification notification) throws Exception {
        System.out.println("  [PUSH] Sending to user " + notification.userId);
        if (Math.random() > 0.9) throw new Exception("FCM error");
        return true;
    }
    public Channel getChannel() { return Channel.PUSH; }
}

class RetryPolicy {
    private static final int MAX_RETRIES = 3;
    
    public boolean shouldRetry(int retryCount) {
        return retryCount < MAX_RETRIES;
    }
    
    public long getBackoffMs(int retryCount) {
        return (long) Math.pow(2, retryCount) * 1000; // 1s, 2s, 4s
    }
}

class NotificationService {
    private Map<Channel, ChannelHandler> handlers = new HashMap<>();
    private RetryPolicy retryPolicy = new RetryPolicy();
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    private Queue<Notification> dlq = new LinkedList<>();
    
    public NotificationService() {
        registerHandler(new EmailHandler());
        registerHandler(new SMSHandler());
        registerHandler(new PushHandler());
    }
    
    private void registerHandler(ChannelHandler handler) {
        handlers.put(handler.getChannel(), handler);
    }
    
    public void send(Notification notification) {
        System.out.println("\n=== Sending Notification ===");
        System.out.println("ID: " + notification.id);
        System.out.println("Priority: " + notification.priority);
        System.out.println("Channels: " + notification.channels);
        
        for (Channel channel : notification.channels) {
            executor.submit(() -> sendWithRetry(notification, channel));
        }
    }
    
    private void sendWithRetry(Notification notification, Channel channel) {
        ChannelHandler handler = handlers.get(channel);
        
        while (retryPolicy.shouldRetry(notification.retryCount)) {
            try {
                if (handler.send(notification)) {
                    notification.status = Status.SENT;
                    System.out.println("  ✓ Success via " + channel);
                    return;
                }
            } catch (Exception e) {
                notification.retryCount++;
                System.out.println("  ✗ Failed via " + channel + " (attempt " + notification.retryCount + "): " + e.getMessage());
                
                if (retryPolicy.shouldRetry(notification.retryCount)) {
                    try {
                        long backoff = retryPolicy.getBackoffMs(notification.retryCount);
                        System.out.println("  ⏳ Retrying in " + backoff + "ms");
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // Move to DLQ
        notification.status = Status.FAILED;
        dlq.offer(notification);
        System.out.println("  ⚠ Moved to DLQ after " + notification.retryCount + " retries");
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}

public class NotificationDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Notification System ===");
        
        NotificationService service = new NotificationService();
        
        // Send notifications
        Notification n1 = new Notification("user1", "Your order is confirmed", 
            Set.of(Channel.EMAIL, Channel.SMS), Priority.HIGH);
        service.send(n1);
        
        Thread.sleep(1000);
        
        Notification n2 = new Notification("user2", "New message from Alice", 
            Set.of(Channel.PUSH), Priority.MEDIUM);
        service.send(n2);
        
        Thread.sleep(5000);
        service.shutdown();
    }
}
```

## Key Concepts

**Retry Strategy**:
- Exponential backoff: 1s, 2s, 4s, 8s
- Max retries: 3-5 attempts
- Dead Letter Queue for failures

**Priority Handling**:
- CRITICAL: Immediate delivery
- HIGH: <1 second
- MEDIUM: <5 seconds
- LOW: Best effort

**Scalability**:
- Kafka for message queue
- Redis for deduplication
- Worker pools per channel

## Interview Questions

**Q: Handle 10M notifications/min?**
A: Kafka partitions, worker pools, async processing

**Q: Guarantee delivery?**
A: At-least-once with idempotency, DLQ for failures

**Q: User preferences (quiet hours)?**
A: Check preferences before sending, schedule for later

**Q: Rate limiting per channel?**
A: Token bucket per provider, queue overflow handling

Run: https://www.jdoodle.com/online-java-compiler
