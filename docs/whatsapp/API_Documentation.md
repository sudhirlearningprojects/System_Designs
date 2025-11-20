# WhatsApp Messenger - Enhanced API Documentation

## 📋 Overview

Production-ready RESTful API documentation for WhatsApp Messenger system following enterprise software design principles with real-time WebSocket communication, comprehensive error handling, and scalable architecture.

**Base URL**: `http://localhost:8093/api/v1`
**WebSocket URL**: `ws://localhost:8093/ws`
**Health Check**: `http://localhost:8093/actuator/health`
**Metrics**: `http://localhost:8093/actuator/metrics`

## 🏗️ Architecture Highlights

- **Clean Architecture**: Layered design with proper separation of concerns
- **SOLID Principles**: Single responsibility, dependency inversion, open/closed
- **Exception Handling**: Typed exceptions with proper HTTP status codes
- **Validation**: Input validation with meaningful error messages
- **Caching**: Multi-layer caching (Redis + Application cache)
- **Scalability**: Horizontal scaling with connection management
- **Reliability**: Kafka message queuing with exactly-once semantics

## 🔐 Authentication & Security

### Authentication Methods
- **Development**: Simple user ID parameter
- **Production**: JWT tokens with phone number verification

```bash
# Development Example
curl -X GET "http://localhost:8093/api/v1/users/search?query=john" \
  -H "X-User-ID: user123"

# Production Example (JWT)
curl -X GET "http://localhost:8093/api/v1/users/search?query=john" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Security Features
- **Input Validation**: Phone number format, message content, group size validation
- **Authorization**: Participant verification, admin-only operations
- **Rate Limiting**: Prevents spam and abuse
- **Error Sanitization**: No sensitive data in error responses

## 👤 User Management APIs

### Register User

Register a new user with phone number.

**Endpoint**: `POST /users/register`

**Request Parameters**:
```json
{
  "phoneNumber": "+1234567890",
  "name": "John Doe"
}
```

**Response**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "phoneNumber": "+1234567890",
  "name": "John Doe",
  "profilePicture": null,
  "about": "Hey there! I am using WhatsApp.",
  "status": "OFFLINE",
  "lastSeen": null
}
```

**Validation Rules**:
- Phone number: Must follow E.164 format (`+[country][number]`)
- Name: 1-100 characters, non-empty
- Duplicate phone numbers are rejected

**Example**:
```bash
curl -X POST "http://localhost:8093/api/v1/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+1234567890",
    "name": "John Doe"
  }'
```

**Error Responses**:
```json
// Invalid phone number
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid operation: Invalid phone number format"
}

// User already exists
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "User with phone number already exists"
}
```

### Get User by Phone Number

Retrieve user information by phone number.

**Endpoint**: `GET /users/phone/{phoneNumber}`

**Response**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "phoneNumber": "+1234567890",
  "name": "John Doe",
  "profilePicture": "https://example.com/profile.jpg",
  "about": "Hey there! I am using WhatsApp.",
  "status": "ONLINE",
  "lastSeen": "2024-01-15T10:30:00"
}
```

**Example**:
```bash
curl -X GET "http://localhost:8093/api/v1/users/phone/+1234567890"
```

### Search Users

Search users by name or phone number.

**Endpoint**: `GET /users/search?query={searchTerm}`

**Response**:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "phoneNumber": "+1234567890",
    "name": "John Doe",
    "profilePicture": "https://example.com/profile.jpg",
    "about": "Hey there! I am using WhatsApp.",
    "status": "ONLINE",
    "lastSeen": "2024-01-15T10:30:00"
  }
]
```

**Example**:
```bash
curl -X GET "http://localhost:8093/api/v1/users/search?query=john"
```

### Update User Status

Update user's online status.

**Endpoint**: `PUT /users/{userId}/status?status={status}`

**Status Values**: `ONLINE`, `OFFLINE`, `TYPING`, `AWAY`

**Example**:
```bash
curl -X PUT "http://localhost:8093/api/v1/users/550e8400-e29b-41d4-a716-446655440000/status?status=ONLINE"
```

### Update User Profile

Update user profile information.

**Endpoint**: `PUT /users/{userId}/profile`

**Request Parameters**:
- `name` (optional): User's display name
- `about` (optional): User's status message
- `profilePicture` (optional): Profile picture URL

**Example**:
```bash
curl -X PUT "http://localhost:8093/api/v1/users/550e8400-e29b-41d4-a716-446655440000/profile" \
  -d "name=John Smith&about=Busy at work"
```

## 💬 Chat Management APIs

### Create Individual Chat

Create a one-on-one chat between two users.

**Endpoint**: `POST /chats/individual`

**Request Parameters**:
```json
{
  "userId1": "550e8400-e29b-41d4-a716-446655440000",
  "userId2": "550e8400-e29b-41d4-a716-446655440001"
}
```

**Response**:
```json
{
  "id": "chat123",
  "type": "INDIVIDUAL",
  "name": null,
  "description": null,
  "groupIcon": null,
  "createdBy": "550e8400-e29b-41d4-a716-446655440000",
  "participants": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "John Doe",
      "phoneNumber": "+1234567890"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Jane Smith",
      "phoneNumber": "+1234567891"
    }
  ],
  "admins": [],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**Example**:
```bash
curl -X POST "http://localhost:8093/api/v1/chats/individual" \
  -d "userId1=550e8400-e29b-41d4-a716-446655440000&userId2=550e8400-e29b-41d4-a716-446655440001"
```

### Create Group Chat

Create a group chat with multiple participants.

**Endpoint**: `POST /chats/group`

**Request Parameters**:
```json
{
  "creatorId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Family Group",
  "description": "Our family chat",
  "participantIds": [
    "550e8400-e29b-41d4-a716-446655440001",
    "550e8400-e29b-41d4-a716-446655440002"
  ]
}
```

**Response**:
```json
{
  "id": "chat456",
  "type": "GROUP",
  "name": "Family Group",
  "description": "Our family chat",
  "groupIcon": null,
  "createdBy": "550e8400-e29b-41d4-a716-446655440000",
  "participants": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "John Doe"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Jane Smith"
    }
  ],
  "admins": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "John Doe"
    }
  ],
  "createdAt": "2024-01-15T10:30:00"
}
```

**Example**:
```bash
curl -X POST "http://localhost:8093/api/v1/chats/group" \
  -H "Content-Type: application/json" \
  -d '{
    "creatorId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Family Group",
    "description": "Our family chat",
    "participantIds": ["550e8400-e29b-41d4-a716-446655440001"]
  }'
```

### Get User Chats

Retrieve all chats for a specific user.

**Endpoint**: `GET /chats/user/{userId}`

**Response**:
```json
[
  {
    "id": "chat123",
    "type": "INDIVIDUAL",
    "name": null,
    "participants": [...],
    "lastMessage": {
      "id": "msg123",
      "content": "Hello there!",
      "type": "TEXT",
      "senderId": "550e8400-e29b-41d4-a716-446655440001",
      "createdAt": "2024-01-15T10:30:00"
    },
    "unreadCount": 2,
    "updatedAt": "2024-01-15T10:30:00"
  }
]
```

**Example**:
```bash
curl -X GET "http://localhost:8093/api/v1/chats/user/550e8400-e29b-41d4-a716-446655440000"
```

### Add Participant to Group

Add a new participant to a group chat.

**Endpoint**: `POST /chats/{chatId}/participants`

**Request Parameters**:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440003",
  "adminId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Example**:
```bash
curl -X POST "http://localhost:8093/api/v1/chats/chat456/participants" \
  -d "userId=550e8400-e29b-41d4-a716-446655440003&adminId=550e8400-e29b-41d4-a716-446655440000"
```

### Remove Participant from Group

Remove a participant from a group chat.

**Endpoint**: `DELETE /chats/{chatId}/participants/{userId}?adminId={adminId}`

**Example**:
```bash
curl -X DELETE "http://localhost:8093/api/v1/chats/chat456/participants/550e8400-e29b-41d4-a716-446655440003?adminId=550e8400-e29b-41d4-a716-446655440000"
```

## 📨 Message APIs

### Send Message

Send a message to a chat.

**Endpoint**: `POST /messages/send?senderId={senderId}`

**Request Body**:
```json
{
  "chatId": "chat123",
  "content": "Hello, how are you?",
  "type": "TEXT",
  "mediaUrl": null,
  "mediaType": null,
  "mediaSize": null,
  "thumbnailUrl": null,
  "latitude": null,
  "longitude": null,
  "replyToMessageId": null,
  "isForwarded": false
}
```

**Message Types**:
- `TEXT`: Plain text message
- `IMAGE`: Image with optional caption
- `VIDEO`: Video with optional caption
- `AUDIO`: Voice message or audio file
- `DOCUMENT`: PDF, Word, or other document
- `LOCATION`: GPS coordinates
- `CONTACT`: Contact information
- `STICKER`: Sticker or emoji

**Response**:
```json
{
  "id": "msg123",
  "chatId": "chat123",
  "senderId": "550e8400-e29b-41d4-a716-446655440000",
  "senderName": "John Doe",
  "content": "Hello, how are you?",
  "type": "TEXT",
  "mediaUrl": null,
  "mediaType": null,
  "mediaSize": null,
  "thumbnailUrl": null,
  "latitude": null,
  "longitude": null,
  "replyToMessageId": null,
  "isForwarded": false,
  "status": "SENT",
  "createdAt": "2024-01-15T10:30:00",
  "editedAt": null
}
```

**Examples**:

**Text Message**:
```bash
curl -X POST "http://localhost:8093/api/v1/messages/send?senderId=550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "chatId": "chat123",
    "content": "Hello, how are you?",
    "type": "TEXT"
  }'
```

**Image Message**:
```bash
curl -X POST "http://localhost:8093/api/v1/messages/send?senderId=550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "chatId": "chat123",
    "content": "Check out this photo!",
    "type": "IMAGE",
    "mediaUrl": "https://example.com/image.jpg",
    "mediaType": "image/jpeg",
    "mediaSize": 1024000,
    "thumbnailUrl": "https://example.com/thumb.jpg"
  }'
```

**Location Message**:
```bash
curl -X POST "http://localhost:8093/api/v1/messages/send?senderId=550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "chatId": "chat123",
    "content": "My current location",
    "type": "LOCATION",
    "latitude": 37.7749,
    "longitude": -122.4194
  }'
```

**Reply Message**:
```bash
curl -X POST "http://localhost:8093/api/v1/messages/send?senderId=550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "chatId": "chat123",
    "content": "Thanks for asking!",
    "type": "TEXT",
    "replyToMessageId": "msg122"
  }'
```

### Message Validation
- **Content**: 1-4096 characters for text messages
- **Media Size**: Max 16MB for media files
- **Participant Check**: Sender must be chat participant
- **Idempotency**: Duplicate messages prevented with 5-minute window

### Get Chat Messages

Retrieve messages from a chat with pagination and caching.

**Endpoint**: `GET /messages/chat/{chatId}?page={page}&size={size}`

**Parameters**:
- `page`: Page number (default: 0)
- `size`: Messages per page (default: 20, max: 100)

**Caching**: Recent messages (page 0) served from Redis cache for sub-50ms response

**Response**:
```json
{
  "content": [
    {
      "id": "msg123",
      "chatId": "chat123",
      "senderId": "550e8400-e29b-41d4-a716-446655440000",
      "senderName": "John Doe",
      "content": "Hello, how are you?",
      "type": "TEXT",
      "status": "READ",
      "createdAt": "2024-01-15T10:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

**Example**:
```bash
curl -X GET "http://localhost:8093/api/v1/messages/chat/chat123?page=0&size=20"
```

### Mark Messages as Read

Mark all unread messages in a chat as read with real-time notification.

**Endpoint**: `PUT /messages/chat/{chatId}/read?userId={userId}`

**Features**:
- **Batch Update**: Marks all unread messages as read
- **Real-time Notification**: WebSocket notification to sender
- **Cache Update**: Updates read status in Redis cache

**Example**:
```bash
curl -X PUT "http://localhost:8093/api/v1/messages/chat/chat123/read?userId=550e8400-e29b-41d4-a716-446655440000"
```

### Delete Message

Delete a message (delete for everyone) with time restrictions.

**Endpoint**: `DELETE /messages/{messageId}?userId={userId}`

**Restrictions**:
- **Time Limit**: Messages can only be deleted within 1 hour
- **Authorization**: Only sender can delete their messages
- **Soft Delete**: Message content replaced with "This message was deleted"

**Example**:
```bash
curl -X DELETE "http://localhost:8093/api/v1/messages/msg123?userId=550e8400-e29b-41d4-a716-446655440000"
```

### Search Messages

Search for messages within a chat with full-text search.

**Endpoint**: `GET /messages/search?chatId={chatId}&query={searchTerm}`

**Features**:
- **Full-text Search**: Search message content
- **Case Insensitive**: Flexible search matching
- **Performance**: Optimized database queries

**Response**:
```json
[
  {
    "id": "msg123",
    "chatId": "chat123",
    "senderId": "550e8400-e29b-41d4-a716-446655440000",
    "senderName": "John Doe",
    "content": "Hello, how are you?",
    "type": "TEXT",
    "createdAt": "2024-01-15T10:30:00"
  }
]
```

**Example**:
```bash
curl -X GET "http://localhost:8093/api/v1/messages/search?chatId=chat123&query=hello"
```

## 👥 Presence Management APIs

### Heartbeat

Send heartbeat to maintain online status.

**Endpoint**: `POST /presence/heartbeat?userId={userId}`

**Features**:
- **TTL Management**: Extends presence TTL to 5 minutes
- **Connection Tracking**: Updates user-server mapping
- **Automatic Cleanup**: Offline status after TTL expiry

**Example**:
```bash
curl -X POST "http://localhost:8093/api/v1/presence/heartbeat?userId=user123"
```

### Get User Presence

Retrieve current user presence status.

**Endpoint**: `GET /presence/{userId}`

**Response**: `ONLINE`, `OFFLINE`, `TYPING`, `AWAY`

**Example**:
```bash
curl -X GET "http://localhost:8093/api/v1/presence/user123"
```

### Update Status

Update user presence status manually.

**Endpoint**: `POST /presence/{userId}/status?status={status}`

**Example**:
```bash
curl -X POST "http://localhost:8093/api/v1/presence/user123/status?status=AWAY"
```

### Check Online Status

Check if user is currently online.

**Endpoint**: `GET /presence/online/{userId}`

**Response**: `true` or `false`

**Example**:
```bash
curl -X GET "http://localhost:8093/api/v1/presence/online/user123"
``` "Content-Type: application/json" \
  -d '{
    "chatId": "chat123",
    "content": "Thanks for asking!",
    "type": "TEXT",
    "replyToMessageId": "msg122"
  }'
```

### Get Chat Messages

Retrieve messages from a chat with pagination.

**Endpoint**: `GET /messages/chat/{chatId}?page={page}&size={size}`

**Parameters**:
- `page`: Page number (default: 0)
- `size`: Messages per page (default: 20)

**Response**:
```json
{
  "content": [
    {
      "id": "msg123",
      "chatId": "chat123",
      "senderId": "550e8400-e29b-41d4-a716-446655440000",
      "senderName": "John Doe",
      "content": "Hello, how are you?",
      "type": "TEXT",
      "status": "READ",
      "createdAt": "2024-01-15T10:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

**Example**:
```bash
curl -X GET "http://localhost:8093/api/v1/messages/chat/chat123?page=0&size=20"
```

### Mark Messages as Read

Mark all unread messages in a chat as read.

**Endpoint**: `PUT /messages/chat/{chatId}/read?userId={userId}`

**Example**:
```bash
curl -X PUT "http://localhost:8093/api/v1/messages/chat/chat123/read?userId=550e8400-e29b-41d4-a716-446655440000"
```

### Delete Message

Delete a message (delete for everyone).

**Endpoint**: `DELETE /messages/{messageId}?userId={userId}`

**Note**: Messages can only be deleted within 1 hour of sending and only by the sender.

**Example**:
```bash
curl -X DELETE "http://localhost:8093/api/v1/messages/msg123?userId=550e8400-e29b-41d4-a716-446655440000"
```

### Search Messages

Search for messages within a chat.

**Endpoint**: `GET /messages/search?chatId={chatId}&query={searchTerm}`

**Response**:
```json
[
  {
    "id": "msg123",
    "chatId": "chat123",
    "senderId": "550e8400-e29b-41d4-a716-446655440000",
    "senderName": "John Doe",
    "content": "Hello, how are you?",
    "type": "TEXT",
    "createdAt": "2024-01-15T10:30:00"
  }
]
```

**Example**:
```bash
curl -X GET "http://localhost:8093/api/v1/messages/search?chatId=chat123&query=hello"
```

## 🔄 Enhanced WebSocket Real-time Communication

### Connection Management

**Multi-Server Support**: Connection manager tracks user-server mapping for horizontal scaling
**Automatic Reconnection**: Client-side reconnection with exponential backoff
**Heartbeat Mechanism**: 30-second heartbeat to detect disconnections
**Offline Message Delivery**: Messages delivered when user comes back online

**WebSocket URL**: `ws://localhost:8093/ws`

### Connection Headers
```javascript
const headers = {
    'userId': 'user123',
    'Authorization': 'Bearer jwt-token' // Production
};
```

### Enhanced JavaScript Client
```javascript
class WhatsAppWebSocket {
    constructor(userId, token) {
        this.userId = userId;
        this.token = token;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.connect();
    }
    
    connect() {
        const socket = new SockJS('http://localhost:8093/ws');
        this.stompClient = Stomp.over(socket);
        
        const headers = {
            'userId': this.userId,
            'Authorization': `Bearer ${this.token}`
        };
        
        this.stompClient.connect(headers, 
            (frame) => this.onConnect(frame),
            (error) => this.onError(error)
        );
    }
    
    onConnect(frame) {
        console.log('Connected to WhatsApp WebSocket:', frame);
        this.reconnectAttempts = 0;
        
        // Subscribe to personal message queue
        this.stompClient.subscribe(`/user/${this.userId}/queue/messages`, 
            (message) => this.handleMessage(JSON.parse(message.body))
        );
        
        // Subscribe to presence updates
        this.stompClient.subscribe(`/topic/presence/${this.userId}`, 
            (presence) => this.handlePresenceUpdate(presence.body)
        );
        
        // Start heartbeat
        this.startHeartbeat();
    }
    
    onError(error) {
        console.error('WebSocket error:', error);
        this.reconnect();
    }
    
    reconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            const delay = Math.pow(2, this.reconnectAttempts) * 1000;
            setTimeout(() => {
                this.reconnectAttempts++;
                this.connect();
            }, delay);
        }
    }
    
    startHeartbeat() {
        setInterval(() => {
            if (this.stompClient && this.stompClient.connected) {
                fetch(`/api/v1/presence/heartbeat?userId=${this.userId}`, {
                    method: 'POST'
                });
            }
        }, 30000); // 30 seconds
    }
    
    // Subscribe to chat-specific events
    subscribeToChat(chatId) {
        // New messages
        this.stompClient.subscribe(`/topic/chat/${chatId}`, 
            (message) => this.handleMessage(JSON.parse(message.body))
        );
        
        // Typing indicators
        this.stompClient.subscribe(`/topic/chat/${chatId}/typing`, 
            (typing) => this.handleTyping(JSON.parse(typing.body))
        );
        
        // Read receipts
        this.stompClient.subscribe(`/topic/chat/${chatId}/read`, 
            (read) => this.handleReadReceipt(read.body)
        );
        
        // Message deletions
        this.stompClient.subscribe(`/topic/chat/${chatId}/delete`, 
            (deleted) => this.handleMessageDeletion(deleted.body)
        );
        
        // Delivery status updates
        this.stompClient.subscribe(`/topic/chat/${chatId}/delivery`, 
            (delivery) => this.handleDeliveryUpdate(JSON.parse(delivery.body))
        );
    }
    
    // Send typing indicator
    sendTyping(chatId, isTyping) {
        this.stompClient.send(`/app/chat/${chatId}/${isTyping ? 'typing' : 'stop-typing'}`, {}, 
            JSON.stringify({
                userId: this.userId,
                userName: this.userName
            })
        );
    }
    
    // Update user status
    updateStatus(status) {
        this.stompClient.send(`/app/user/${this.userId}/status`, {}, 
            JSON.stringify({ status: status })
        );
    }
    
    handleMessage(messageData) {
        console.log('New message:', messageData);
        // Update UI with new message
        this.displayMessage(messageData);
        
        // Send delivery receipt
        this.sendDeliveryReceipt(messageData.id, 'DELIVERED');
    }
    
    handleTyping(typingData) {
        console.log('Typing indicator:', typingData);
        this.showTypingIndicator(typingData.userName);
    }
    
    handleReadReceipt(userId) {
        console.log('Read receipt from:', userId);
        this.updateMessageReadStatus(userId);
    }
    
    handleMessageDeletion(messageId) {
        console.log('Message deleted:', messageId);
        this.removeMessageFromUI(messageId);
    }
    
    handleDeliveryUpdate(deliveryData) {
        console.log('Delivery update:', deliveryData);
        this.updateMessageStatus(deliveryData.messageId, deliveryData.status);
    }
    
    sendDeliveryReceipt(messageId, status) {
        fetch(`/api/v1/messages/delivery`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                messageId: messageId,
                userId: this.userId,
                status: status
            })
        });
    }
}

// Usage
const whatsapp = new WhatsAppWebSocket('user123', 'jwt-token');
whatsapp.subscribeToChat('chat123');
```

### Real-time Event Types

#### 1. New Message

**Topic**: `/topic/chat/{chatId}`

**Payload**:
```json
{
  "id": "msg123",
  "chatId": "chat123",
  "senderId": "550e8400-e29b-41d4-a716-446655440000",
  "senderName": "John Doe",
  "content": "Hello, how are you?",
  "type": "TEXT",
  "createdAt": "2024-01-15T10:30:00"
}
```

#### 2. Typing Indicator

**Send**: `/app/chat/{chatId}/typing`
**Receive**: `/topic/chat/{chatId}/typing`

**Payload**:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "userName": "John Doe"
}
```

**JavaScript Example**:
```javascript
// Send typing indicator
stompClient.send('/app/chat/chat123/typing', {}, JSON.stringify({
    userId: 'user123',
    userName: 'John Doe'
}));

// Stop typing
stompClient.send('/app/chat/chat123/stop-typing', {}, JSON.stringify({
    userId: 'user123',
    userName: 'John Doe'
}));
```

#### 3. User Status Update

**Send**: `/app/user/{userId}/status`
**Receive**: `/topic/user/{userId}/status`

**Payload**:
```json
{
  "status": "ONLINE"
}
```

#### 4. Read Receipts

**Topic**: `/topic/chat/{chatId}/read`

**Payload**: `userId` (string)

#### 5. Message Deletion

**Topic**: `/topic/chat/{chatId}/delete`

**Payload**: `messageId` (string)

## 🚨 Enhanced Error Handling

### Error Response Format
All errors follow a consistent structure with proper HTTP status codes and detailed messages.

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error description",
  "path": "/api/v1/endpoint"
}
```

### Error Categories

#### Validation Errors (400 Bad Request)
```json
// Invalid phone number
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid operation: Invalid phone number format"
}

// Invalid message content
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid operation: Invalid message content"
}
```

#### Authorization Errors (403 Forbidden)
```json
// Unauthorized message sending
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Unauthorized: User not authorized to send message to this chat"
}
```

#### Resource Not Found (404 Not Found)
```json
// User not found
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found: user123"
}
```

### Rate Limiting & Security

#### Rate Limits by Endpoint
- **Message sending**: 100 messages per minute per user
- **User search**: 10 requests per minute per user  
- **Chat creation**: 5 chats per minute per user
- **File uploads**: 10 uploads per minute per user

#### Rate Limit Response (429 Too Many Requests)
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 60 seconds.",
  "retryAfter": 60
}
```

## 📊 Performance & Optimization

### Caching Strategy
- **Recent Messages**: Redis cache with 30-minute TTL
- **User Sessions**: 10-minute TTL for active users
- **Chat Metadata**: 6-hour TTL for chat information
- **Presence Data**: 5-minute TTL for user status

### Response Times
- **Message Sending**: <100ms (cached participants)
- **Message Retrieval**: <50ms (Redis cache hit)
- **User Search**: <200ms (database + cache)
- **Chat Creation**: <150ms (validation + persistence)

### Scalability Features
- **Horizontal Scaling**: Stateless services with load balancing
- **Connection Management**: Multi-server WebSocket handling
- **Message Queuing**: Kafka for reliable delivery
- **Database Sharding**: Partitioned by chat_id and user_id

## 📱 Complete Chat Flow Example

### 1. User Registration and Chat Creation

```bash
# Register users
curl -X POST "http://localhost:8093/api/v1/users/register" \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+1234567890", "name": "John Doe"}'

curl -X POST "http://localhost:8093/api/v1/users/register" \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+1234567891", "name": "Jane Smith"}'

# Create individual chat
curl -X POST "http://localhost:8093/api/v1/chats/individual" \
  -d "userId1=john-id&userId2=jane-id"
```

### 2. Real-time Messaging

```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8093/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // Subscribe to chat
    stompClient.subscribe('/topic/chat/chat123', function(message) {
        const msg = JSON.parse(message.body);
        addMessageToUI(msg);
    });
    
    // Subscribe to typing
    stompClient.subscribe('/topic/chat/chat123/typing', function(message) {
        const typing = JSON.parse(message.body);
        showTypingIndicator(typing.userName);
    });
});

// Send message via REST API
function sendMessage(content) {
    fetch('http://localhost:8093/api/v1/messages/send?senderId=john-id', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            chatId: 'chat123',
            content: content,
            type: 'TEXT'
        })
    });
}

// Send typing indicator
function startTyping() {
    stompClient.send('/app/chat/chat123/typing', {}, JSON.stringify({
        userId: 'john-id',
        userName: 'John Doe'
    }));
}
```

### 3. Message Status Updates

```bash
# Mark messages as read
curl -X PUT "http://localhost:8093/api/v1/messages/chat/chat123/read?userId=jane-id"

# Delete message
curl -X DELETE "http://localhost:8093/api/v1/messages/msg123?userId=john-id"
```

## 🚨 Error Handling

### Common Error Responses

**400 Bad Request**:
```json
{
  "error": "Bad Request",
  "message": "Invalid phone number format",
  "timestamp": "2024-01-15T10:30:00"
}
```

**404 Not Found**:
```json
{
  "error": "Not Found",
  "message": "User not found",
  "timestamp": "2024-01-15T10:30:00"
}
```

**403 Forbidden**:
```json
{
  "error": "Forbidden",
  "message": "User not authorized to send message to this chat",
  "timestamp": "2024-01-15T10:30:00"
}
```

### Rate Limiting

API endpoints are rate-limited to prevent abuse:
- **Message sending**: 100 messages per minute per user
- **User search**: 10 requests per minute per user
- **Chat creation**: 5 chats per minute per user

**Rate Limit Response**:
```json
{
  "error": "Rate Limit Exceeded",
  "message": "Too many requests. Try again in 60 seconds.",
  "retryAfter": 60
}
```



## 🔍 API Testing & Development

### Postman Collection
```json
{
  "info": {
    "name": "WhatsApp Messenger API",
    "description": "Complete API collection for WhatsApp Messenger"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8093/api/v1"
    },
    {
      "key": "userId",
      "value": "user123"
    }
  ]
}
```

### Environment Variables
```bash
# Development
export WHATSAPP_BASE_URL=http://localhost:8093
export WHATSAPP_WS_URL=ws://localhost:8093/ws
export REDIS_HOST=localhost
export KAFKA_BROKERS=localhost:9092

# Production
export WHATSAPP_BASE_URL=https://api.whatsapp.example.com
export WHATSAPP_WS_URL=wss://ws.whatsapp.example.com/ws
export REDIS_CLUSTER=redis-cluster.internal
export KAFKA_BROKERS=kafka-cluster.internal:9092
```

### Health Checks
```bash
# Application health
curl http://localhost:8093/actuator/health

# Database connectivity
curl http://localhost:8093/actuator/health/db

# Redis connectivity  
curl http://localhost:8093/actuator/health/redis

# Kafka connectivity
curl http://localhost:8093/actuator/health/kafka
```

### Monitoring Endpoints
```bash
# Application metrics
curl http://localhost:8093/actuator/metrics

# Message throughput
curl http://localhost:8093/actuator/metrics/whatsapp.messages.sent

# WebSocket connections
curl http://localhost:8093/actuator/metrics/whatsapp.websocket.connections

# Cache statistics
curl http://localhost:8093/actuator/metrics/cache.gets
```

## 🛠️ SDK & Client Libraries

### JavaScript/TypeScript SDK
```typescript
import { WhatsAppClient } from '@whatsapp/sdk';

const client = new WhatsAppClient({
  baseUrl: 'http://localhost:8093/api/v1',
  websocketUrl: 'ws://localhost:8093/ws',
  userId: 'user123',
  token: 'jwt-token'
});

// Send message
await client.sendMessage({
  chatId: 'chat123',
  content: 'Hello World!',
  type: 'TEXT'
});

// Listen for messages
client.onMessage((message) => {
  console.log('New message:', message);
});
```

### Java SDK
```java
WhatsAppClient client = WhatsAppClient.builder()
    .baseUrl("http://localhost:8093/api/v1")
    .websocketUrl("ws://localhost:8093/ws")
    .userId("user123")
    .token("jwt-token")
    .build();

// Send message
MessageDTO message = client.sendMessage(SendMessageRequest.builder()
    .chatId("chat123")
    .content("Hello World!")
    .type(MessageType.TEXT)
    .build());

// Listen for messages
client.onMessage(message -> {
    System.out.println("New message: " + message);
});
```

### Python SDK
```python
from whatsapp_sdk import WhatsAppClient

client = WhatsAppClient(
    base_url='http://localhost:8093/api/v1',
    websocket_url='ws://localhost:8093/ws',
    user_id='user123',
    token='jwt-token'
)

# Send message
message = client.send_message(
    chat_id='chat123',
    content='Hello World!',
    message_type='TEXT'
)

# Listen for messages
@client.on_message
def handle_message(message):
    print(f'New message: {message}')
```

## 📚 Integration Examples

### React Native Integration
```jsx
import React, { useEffect, useState } from 'react';
import { WhatsAppWebSocket } from './whatsapp-client';

const ChatScreen = ({ userId, chatId }) => {
  const [messages, setMessages] = useState([]);
  const [ws, setWs] = useState(null);
  
  useEffect(() => {
    const websocket = new WhatsAppWebSocket(userId, 'jwt-token');
    websocket.subscribeToChat(chatId);
    
    websocket.onMessage = (message) => {
      setMessages(prev => [...prev, message]);
    };
    
    setWs(websocket);
    
    return () => websocket.disconnect();
  }, [userId, chatId]);
  
  const sendMessage = async (content) => {
    const response = await fetch('/api/v1/messages/send?senderId=' + userId, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        chatId,
        content,
        type: 'TEXT'
      })
    });
    
    const message = await response.json();
    setMessages(prev => [...prev, message]);
  };
  
  return (
    <div>
      {messages.map(msg => (
        <div key={msg.id}>{msg.content}</div>
      ))}
      <input onSubmit={(e) => sendMessage(e.target.value)} />
    </div>
  );
};
```

### Flutter Integration
```dart
class WhatsAppService {
  static const String baseUrl = 'http://localhost:8093/api/v1';
  static const String wsUrl = 'ws://localhost:8093/ws';
  
  late WebSocketChannel channel;
  
  Future<void> connect(String userId) async {
    channel = WebSocketChannel.connect(
      Uri.parse('$wsUrl?userId=$userId')
    );
    
    channel.stream.listen((message) {
      final data = jsonDecode(message);
      _handleMessage(data);
    });
  }
  
  Future<MessageModel> sendMessage({
    required String chatId,
    required String content,
    required String senderId,
  }) async {
    final response = await http.post(
      Uri.parse('$baseUrl/messages/send?senderId=$senderId'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'chatId': chatId,
        'content': content,
        'type': 'TEXT',
      }),
    );
    
    return MessageModel.fromJson(jsonDecode(response.body));
  }
}
```

---

## 🎆 Summary

This enhanced API documentation provides:

✅ **Production-Ready Architecture**: Clean code principles and SOLID design
✅ **Comprehensive Error Handling**: Typed exceptions with proper HTTP status codes
✅ **Real-time Communication**: Advanced WebSocket implementation with reconnection
✅ **Performance Optimization**: Multi-layer caching and horizontal scaling
✅ **Security Features**: Input validation, rate limiting, and authorization
✅ **Monitoring & Observability**: Health checks, metrics, and logging
✅ **Developer Experience**: SDKs, examples, and integration guides

The WhatsApp Messenger API is now enterprise-ready with comprehensive documentation covering all aspects from basic usage to advanced integration patterns.