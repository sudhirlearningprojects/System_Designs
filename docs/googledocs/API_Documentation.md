# Google Docs - API Documentation

## Base URL
```
Production: https://api.googledocs.example.com/api/v1
Development: http://localhost:8091/api/v1
```

## Authentication
All API requests require JWT authentication via the `Authorization` header:
```
Authorization: Bearer <jwt_token>
```

---

## Document APIs

### 1. Create Document

Creates a new document.

**Endpoint:** `POST /documents`

**Request:**
```json
{
  "title": "My New Document",
  "userId": "user-123"
}
```

**Response:** `200 OK`
```json
{
  "id": "doc-456",
  "title": "My New Document",
  "content": "",
  "ownerId": "user-123",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z",
  "watermark": null,
  "tags": [],
  "version": 1,
  "permissions": [
    {
      "userId": "user-123",
      "type": "OWNER"
    }
  ],
  "activeUsers": []
}
```

---

### 2. Get Document

Retrieves a document by ID.

**Endpoint:** `GET /documents/{documentId}`

**Response:** `200 OK`
```json
{
  "id": "doc-456",
  "title": "My Document",
  "content": "This is the document content...",
  "ownerId": "user-123",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T11:45:00Z",
  "watermark": "CONFIDENTIAL",
  "tags": ["work", "important"],
  "version": 15,
  "permissions": [
    {
      "userId": "user-123",
      "type": "OWNER"
    },
    {
      "userId": "user-789",
      "type": "EDITOR"
    }
  ],
  "activeUsers": [
    {
      "userId": "user-123",
      "userName": "Alice Smith",
      "cursorPosition": 150
    },
    {
      "userId": "user-789",
      "userName": "Bob Johnson",
      "cursorPosition": 200
    }
  ]
}
```

---

### 3. Get User Documents

Retrieves all documents owned by or shared with a user.

**Endpoint:** `GET /documents/user/{userId}`

**Response:** `200 OK`
```json
[
  {
    "id": "doc-456",
    "title": "My Document",
    "ownerId": "user-123",
    "status": "ACTIVE",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T11:45:00Z",
    "version": 15
  },
  {
    "id": "doc-789",
    "title": "Shared Document",
    "ownerId": "user-999",
    "status": "ACTIVE",
    "createdAt": "2024-01-14T09:00:00Z",
    "updatedAt": "2024-01-15T10:00:00Z",
    "version": 8
  }
]
```

---

### 4. Share Document

Grants permission to a user for a document.

**Endpoint:** `POST /documents/{documentId}/share`

**Request:**
```json
{
  "userId": "user-789",
  "permissionType": "EDITOR",
  "grantedBy": "user-123"
}
```

**Permission Types:**
- `OWNER`: Full control (delete, share, edit)
- `EDITOR`: Can edit and comment
- `COMMENTER`: Can only comment
- `VIEWER`: Read-only access

**Response:** `200 OK`

---

### 5. Add Watermark

Adds a watermark to a document.

**Endpoint:** `POST /documents/{documentId}/watermark`

**Request:**
```json
{
  "watermark": "CONFIDENTIAL - Internal Use Only"
}
```

**Response:** `200 OK`

---

## Version APIs

### 6. Get Version History

Retrieves all versions of a document.

**Endpoint:** `GET /documents/{documentId}/versions`

**Response:** `200 OK`
```json
[
  {
    "id": "ver-1",
    "versionNumber": 15,
    "content": "Latest content...",
    "createdBy": "user-123",
    "createdAt": "2024-01-15T11:45:00Z",
    "description": "Added conclusion section"
  },
  {
    "id": "ver-2",
    "versionNumber": 14,
    "content": "Previous content...",
    "createdBy": "user-789",
    "createdAt": "2024-01-15T11:30:00Z",
    "description": "Fixed typos in introduction"
  },
  {
    "id": "ver-3",
    "versionNumber": 13,
    "content": "Older content...",
    "createdBy": "user-123",
    "createdAt": "2024-01-15T11:00:00Z",
    "description": "Initial draft"
  }
]
```

---

### 7. Save Version

Manually saves the current state as a new version.

**Endpoint:** `POST /documents/{documentId}/versions`

**Request:**
```json
{
  "userId": "user-123",
  "description": "Completed first draft"
}
```

**Response:** `200 OK`

---

### 8. Restore Version

Restores a document to a previous version.

**Endpoint:** `POST /documents/{documentId}/versions/{versionId}/restore`

**Request:**
```json
{
  "userId": "user-123"
}
```

**Response:** `200 OK`
```json
{
  "id": "doc-456",
  "title": "My Document",
  "content": "Restored content from version 10...",
  "version": 16,
  "updatedAt": "2024-01-15T12:00:00Z"
}
```

---

## Comment APIs

### 9. Add Comment

Adds a comment to a document.

**Endpoint:** `POST /comments`

**Request:**
```json
{
  "documentId": "doc-456",
  "userId": "user-789",
  "content": "This section needs more detail.",
  "startPosition": 100,
  "endPosition": 150
}
```

**Response:** `200 OK`
```json
{
  "id": "comment-123",
  "content": "This section needs more detail.",
  "userId": "user-789",
  "startPosition": 100,
  "endPosition": 150,
  "createdAt": "2024-01-15T12:00:00Z",
  "status": "OPEN",
  "replies": [],
  "reactions": {}
}
```

---

### 10. Add Reply

Adds a reply to a comment.

**Endpoint:** `POST /comments/{commentId}/replies`

**Request:**
```json
{
  "userId": "user-123",
  "content": "Good point! I'll expand on this."
}
```

**Response:** `200 OK`
```json
{
  "id": "comment-123",
  "content": "This section needs more detail.",
  "userId": "user-789",
  "startPosition": 100,
  "endPosition": 150,
  "createdAt": "2024-01-15T12:00:00Z",
  "status": "OPEN",
  "replies": [
    {
      "id": "reply-456",
      "content": "Good point! I'll expand on this.",
      "userId": "user-123",
      "createdAt": "2024-01-15T12:05:00Z"
    }
  ],
  "reactions": {}
}
```

---

### 11. Add Reaction

Adds an emoji reaction to a comment.

**Endpoint:** `POST /comments/{commentId}/reactions`

**Request:**
```json
{
  "userId": "user-123",
  "emoji": "👍"
}
```

**Supported Emojis:**
- 👍 (thumbs up)
- ❤️ (heart)
- 😂 (laugh)
- 😮 (surprised)
- 😢 (sad)
- 🎉 (celebrate)

**Response:** `200 OK`
```json
{
  "id": "comment-123",
  "content": "This section needs more detail.",
  "userId": "user-789",
  "reactions": {
    "user-123": "👍",
    "user-456": "❤️"
  }
}
```

---

### 12. Resolve Comment

Marks a comment as resolved.

**Endpoint:** `PUT /comments/{commentId}/resolve`

**Response:** `200 OK`
```json
{
  "id": "comment-123",
  "status": "RESOLVED"
}
```

---

### 13. Get Document Comments

Retrieves all comments for a document.

**Endpoint:** `GET /comments/document/{documentId}`

**Response:** `200 OK`
```json
[
  {
    "id": "comment-123",
    "content": "This section needs more detail.",
    "userId": "user-789",
    "startPosition": 100,
    "endPosition": 150,
    "createdAt": "2024-01-15T12:00:00Z",
    "status": "OPEN",
    "replies": [
      {
        "id": "reply-456",
        "content": "Good point! I'll expand on this.",
        "userId": "user-123",
        "createdAt": "2024-01-15T12:05:00Z"
      }
    ],
    "reactions": {
      "user-123": "👍"
    }
  }
]
```

---

## Suggestion APIs

### 14. Create Suggestion

Creates a suggestion for text changes (suggesting mode).

**Endpoint:** `POST /suggestions`

**Request:**
```json
{
  "documentId": "doc-456",
  "userId": "user-789",
  "startPosition": 50,
  "endPosition": 60,
  "originalText": "teh quick",
  "suggestedText": "the quick"
}
```

**Response:** `200 OK`
```json
{
  "id": "sug-123",
  "userId": "user-789",
  "startPosition": 50,
  "endPosition": 60,
  "originalText": "teh quick",
  "suggestedText": "the quick",
  "status": "PENDING",
  "createdAt": "2024-01-15T12:10:00Z"
}
```

---

### 15. Accept Suggestion

Accepts a suggestion and applies the change.

**Endpoint:** `PUT /suggestions/{suggestionId}/accept`

**Request:**
```json
{
  "userId": "user-123"
}
```

**Response:** `200 OK`
```json
{
  "id": "sug-123",
  "status": "ACCEPTED",
  "resolvedAt": "2024-01-15T12:15:00Z",
  "resolvedBy": "user-123"
}
```

---

### 16. Reject Suggestion

Rejects a suggestion without applying the change.

**Endpoint:** `PUT /suggestions/{suggestionId}/reject`

**Request:**
```json
{
  "userId": "user-123"
}
```

**Response:** `200 OK`
```json
{
  "id": "sug-123",
  "status": "REJECTED",
  "resolvedAt": "2024-01-15T12:15:00Z",
  "resolvedBy": "user-123"
}
```

---

### 17. Get Document Suggestions

Retrieves all suggestions for a document.

**Endpoint:** `GET /suggestions/document/{documentId}`

**Response:** `200 OK`
```json
[
  {
    "id": "sug-123",
    "userId": "user-789",
    "startPosition": 50,
    "endPosition": 60,
    "originalText": "teh quick",
    "suggestedText": "the quick",
    "status": "PENDING",
    "createdAt": "2024-01-15T12:10:00Z"
  },
  {
    "id": "sug-456",
    "userId": "user-789",
    "startPosition": 200,
    "endPosition": 210,
    "originalText": "recieve",
    "suggestedText": "receive",
    "status": "ACCEPTED",
    "createdAt": "2024-01-15T11:50:00Z"
  }
]
```

---

## WebSocket APIs

### Connection

Connect to WebSocket endpoint:
```javascript
const socket = new SockJS('http://localhost:8091/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
});
```

---

### 18. Join Document Session

Join a document editing session.

**Topic:** `/app/document/{documentId}/join`

**Message:**
```json
{
  "userId": "user-123",
  "userName": "Alice Smith"
}
```

**Broadcast:** `/topic/document/{documentId}/users`

**Response:**
```json
[
  {
    "userId": "user-123",
    "userName": "Alice Smith",
    "cursorPosition": 0
  },
  {
    "userId": "user-789",
    "userName": "Bob Johnson",
    "cursorPosition": 150
  }
]
```

---

### 19. Send Edit Operation

Send a document edit operation.

**Topic:** `/app/document/{documentId}/edit`

**Message:**
```json
{
  "type": "INSERT",
  "position": 10,
  "text": "hello world",
  "userId": "user-123",
  "timestamp": 1705320000000,
  "version": 15
}
```

**Operation Types:**
- `INSERT`: Insert text at position
- `DELETE`: Delete text from position
- `RETAIN`: Keep text unchanged (for OT)

**Broadcast:** `/topic/document/{documentId}`

**Response:** Same operation broadcasted to all connected users

---

### 20. Update Cursor Position

Update cursor position for real-time tracking.

**Topic:** `/app/document/{documentId}/cursor`

**Message:**
```json
{
  "userId": "user-123",
  "userName": "Alice Smith",
  "position": 150
}
```

**Broadcast:** `/topic/document/{documentId}/cursors`

**Response:**
```json
{
  "userId": "user-123",
  "userName": "Alice Smith",
  "position": 150
}
```

---

### 21. Leave Document Session

Leave a document editing session.

**Topic:** `/app/document/{documentId}/leave`

**Message:**
```json
{
  "userId": "user-123"
}
```

**Broadcast:** `/topic/document/{documentId}/users`

**Response:** Updated list of active users (without the leaving user)

---

## Error Responses

### 400 Bad Request
```json
{
  "error": "Bad Request",
  "message": "Invalid request parameters",
  "timestamp": "2024-01-15T12:00:00Z"
}
```

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired JWT token",
  "timestamp": "2024-01-15T12:00:00Z"
}
```

### 403 Forbidden
```json
{
  "error": "Forbidden",
  "message": "You don't have permission to access this document",
  "timestamp": "2024-01-15T12:00:00Z"
}
```

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "Document not found",
  "timestamp": "2024-01-15T12:00:00Z"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "timestamp": "2024-01-15T12:00:00Z"
}
```

---

## Rate Limiting

- **Rate Limit:** 1000 requests per minute per user
- **Headers:**
  - `X-RateLimit-Limit`: Maximum requests allowed
  - `X-RateLimit-Remaining`: Remaining requests
  - `X-RateLimit-Reset`: Time when limit resets (Unix timestamp)

**Example:**
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1705320060
```

---

## Pagination

For endpoints returning lists, use query parameters:

**Parameters:**
- `page`: Page number (default: 0)
- `size`: Items per page (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

**Example:**
```
GET /documents/user/user-123?page=0&size=20&sort=updatedAt,desc
```

**Response Headers:**
```
X-Total-Count: 150
X-Page-Number: 0
X-Page-Size: 20
X-Total-Pages: 8
```

---

## Webhooks

Subscribe to document events via webhooks.

### Event Types
- `document.created`
- `document.updated`
- `document.deleted`
- `document.shared`
- `comment.added`
- `suggestion.created`
- `suggestion.accepted`

### Webhook Payload
```json
{
  "event": "document.updated",
  "timestamp": "2024-01-15T12:00:00Z",
  "data": {
    "documentId": "doc-456",
    "userId": "user-123",
    "version": 16
  }
}
```

---

## SDK Examples

### JavaScript/TypeScript

```javascript
import { GoogleDocsClient } from '@googledocs/sdk';

const client = new GoogleDocsClient({
  apiKey: 'your-api-key',
  baseUrl: 'https://api.googledocs.example.com/api/v1'
});

// Create document
const doc = await client.documents.create({
  title: 'My Document',
  userId: 'user-123'
});

// Get document
const document = await client.documents.get('doc-456');

// Add comment
const comment = await client.comments.add({
  documentId: 'doc-456',
  userId: 'user-123',
  content: 'Great work!',
  startPosition: 100,
  endPosition: 150
});

// Connect to WebSocket
client.realtime.connect('doc-456', {
  onEdit: (operation) => {
    console.log('Edit received:', operation);
  },
  onCursorMove: (cursor) => {
    console.log('Cursor moved:', cursor);
  }
});
```

### Python

```python
from googledocs import GoogleDocsClient

client = GoogleDocsClient(
    api_key='your-api-key',
    base_url='https://api.googledocs.example.com/api/v1'
)

# Create document
doc = client.documents.create(
    title='My Document',
    user_id='user-123'
)

# Get document
document = client.documents.get('doc-456')

# Add comment
comment = client.comments.add(
    document_id='doc-456',
    user_id='user-123',
    content='Great work!',
    start_position=100,
    end_position=150
)
```

---

## Testing

### Postman Collection

Import the Postman collection for easy API testing:
```
https://api.googledocs.example.com/postman/collection.json
```

### cURL Examples

**Create Document:**
```bash
curl -X POST https://api.googledocs.example.com/api/v1/documents \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My Document",
    "userId": "user-123"
  }'
```

**Get Document:**
```bash
curl -X GET https://api.googledocs.example.com/api/v1/documents/doc-456 \
  -H "Authorization: Bearer <token>"
```

**Add Comment:**
```bash
curl -X POST https://api.googledocs.example.com/api/v1/comments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "doc-456",
    "userId": "user-123",
    "content": "Great work!",
    "startPosition": 100,
    "endPosition": 150
  }'
```

---

## Support

For API support, contact:
- **Email:** api-support@googledocs.example.com
- **Documentation:** https://docs.googledocs.example.com
- **Status Page:** https://status.googledocs.example.com
