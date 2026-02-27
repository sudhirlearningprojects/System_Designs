# WhatsApp Messenger - Complete Coding Guide

## System Design Overview

**Problem**: Real-time messaging with delivery status tracking

**Core Features**:
1. Send/receive messages
2. Message status (sent ✓, delivered ✓✓, read ✓✓)
3. Group chats
4. Online presence

## SOLID Principles

- **SRP**: Message, Chat, User separate concerns
- **OCP**: Add new message types without modifying
- **Observer**: Notify users of new messages

## Design Patterns

1. **Observer Pattern**: Real-time message delivery
2. **State Pattern**: Message status transitions
3. **Factory Pattern**: Create different message types

## Complete Implementation

```java
import java.util.*;
import java.time.LocalDateTime;

enum MessageStatus { SENT, DELIVERED, READ }
enum MessageType { TEXT, IMAGE, VIDEO }

class User {
    String id, name;
    boolean online;
    LocalDateTime lastSeen;
    
    User(String id, String name) {
        this.id = id;
        this.name = name;
        this.online = false;
        this.lastSeen = LocalDateTime.now();
    }
}

class Message {
    String id, chatId, senderId, content;
    MessageType type;
    MessageStatus status;
    LocalDateTime timestamp;
    Map<String, LocalDateTime> readBy = new HashMap<>();
    
    Message(String chatId, String senderId, String content, MessageType type) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = content;
        this.type = type;
        this.status = MessageStatus.SENT;
        this.timestamp = LocalDateTime.now();
    }
}

class Chat {
    String id;
    List<String> participants;
    List<Message> messages = new ArrayList<>();
    boolean isGroup;
    
    Chat(List<String> participants, boolean isGroup) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.participants = participants;
        this.isGroup = isGroup;
    }
}

interface MessageObserver {
    void onMessageReceived(Message message);
}

class WhatsAppService {
    private Map<String, User> users = new HashMap<>();
    private Map<String, Chat> chats = new HashMap<>();
    private Map<String, List<MessageObserver>> observers = new HashMap<>();
    
    public User createUser(String name) {
        User user = new User(UUID.randomUUID().toString().substring(0, 8), name);
        users.put(user.id, user);
        System.out.println("User created: " + name);
        return user;
    }
    
    public Chat createChat(List<String> participantIds, boolean isGroup) {
        Chat chat = new Chat(participantIds, isGroup);
        chats.put(chat.id, chat);
        
        if (isGroup) {
            System.out.println("Group chat created with " + participantIds.size() + " members");
        } else {
            System.out.println("Chat created between " + 
                users.get(participantIds.get(0)).name + " and " + 
                users.get(participantIds.get(1)).name);
        }
        return chat;
    }
    
    public void sendMessage(String chatId, String senderId, String content, MessageType type) {
        Chat chat = chats.get(chatId);
        Message message = new Message(chatId, senderId, content, type);
        chat.messages.add(message);
        
        User sender = users.get(senderId);
        System.out.println("\n" + sender.name + ": " + content);
        
        // Deliver to online users
        for (String participantId : chat.participants) {
            if (!participantId.equals(senderId)) {
                User recipient = users.get(participantId);
                if (recipient.online) {
                    message.status = MessageStatus.DELIVERED;
                    System.out.println("  ✓✓ Delivered to " + recipient.name);
                } else {
                    System.out.println("  ✓ Sent (offline: " + recipient.name + ")");
                }
            }
        }
    }
    
    public void markAsRead(String chatId, String userId) {
        Chat chat = chats.get(chatId);
        for (Message msg : chat.messages) {
            if (!msg.senderId.equals(userId) && msg.status != MessageStatus.READ) {
                msg.status = MessageStatus.READ;
                msg.readBy.put(userId, LocalDateTime.now());
            }
        }
        System.out.println(users.get(userId).name + " read messages ✓✓ (blue)");
    }
    
    public void setOnline(String userId, boolean online) {
        User user = users.get(userId);
        user.online = online;
        if (!online) {
            user.lastSeen = LocalDateTime.now();
        }
        System.out.println(user.name + " is " + (online ? "online" : "offline"));
    }
    
    public void printChat(String chatId) {
        Chat chat = chats.get(chatId);
        System.out.println("\n=== Chat History ===");
        for (Message msg : chat.messages) {
            User sender = users.get(msg.senderId);
            String status = switch (msg.status) {
                case SENT -> "✓";
                case DELIVERED -> "✓✓";
                case READ -> "✓✓ (blue)";
            };
            System.out.println(sender.name + ": " + msg.content + " " + status);
        }
    }
}

public class WhatsAppDemo {
    public static void main(String[] args) {
        System.out.println("=== WhatsApp Messenger ===\n");
        
        WhatsAppService whatsapp = new WhatsAppService();
        
        // Create users
        User alice = whatsapp.createUser("Alice");
        User bob = whatsapp.createUser("Bob");
        User charlie = whatsapp.createUser("Charlie");
        
        // Set online status
        System.out.println();
        whatsapp.setOnline(alice.id, true);
        whatsapp.setOnline(bob.id, true);
        
        // Create 1-on-1 chat
        System.out.println();
        Chat chat1 = whatsapp.createChat(List.of(alice.id, bob.id), false);
        
        // Send messages
        whatsapp.sendMessage(chat1.id, alice.id, "Hey Bob!", MessageType.TEXT);
        whatsapp.sendMessage(chat1.id, bob.id, "Hi Alice! How are you?", MessageType.TEXT);
        
        // Mark as read
        System.out.println();
        whatsapp.markAsRead(chat1.id, alice.id);
        
        // Create group chat
        System.out.println();
        Chat group = whatsapp.createChat(List.of(alice.id, bob.id, charlie.id), true);
        whatsapp.sendMessage(group.id, alice.id, "Hello everyone!", MessageType.TEXT);
        
        // Print chat
        whatsapp.printChat(chat1.id);
    }
}
```

## Key Concepts

**Message Delivery**:
- WebSocket for real-time
- Queue for offline users
- ACK for delivery confirmation

**Status Tracking**:
- Sent: Message left sender
- Delivered: Reached recipient device
- Read: Recipient opened chat

**Scalability**:
- Cassandra for message storage
- Redis for online presence
- Kafka for message queue

## Interview Questions

**Q: Handle 100B messages/day?**
A: Cassandra partitioned by userId + timestamp, Kafka for delivery

**Q: Real-time delivery?**
A: WebSocket connections, fallback to push notifications

**Q: Group message delivery?**
A: Fan-out to all members, parallel delivery

**Q: Store messages forever?**
A: Hot storage (recent), cold storage (archived), compression

Run: https://www.jdoodle.com/online-java-compiler
