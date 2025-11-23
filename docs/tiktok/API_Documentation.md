# TikTok API Documentation

## Base URL
```
Production: https://api.tiktok.com
Staging: https://api-staging.tiktok.com
```

## Authentication
All API requests require JWT authentication via `Authorization` header:
```
Authorization: Bearer <jwt_token>
```

---

## 1. Video APIs

### 1.1 Upload Video
Upload a short video (15-60 seconds).

**Endpoint**: `POST /api/v1/videos/upload`

**Request**:
```http
POST /api/v1/videos/upload?userId=123
Content-Type: multipart/form-data
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Form Data:
- file: video.mp4 (max 100MB)
- caption: "Amazing dance moves! #fyp #dance"
- isPublic: true
- allowComments: true
- allowDuet: true
- allowStitch: true
```

**Response**: `200 OK`
```json
{
  "videoId": 123456789,
  "videoUrl": "https://cdn.tiktok.com/videos/abc123.mp4",
  "thumbnailUrl": "https://cdn.tiktok.com/thumbnails/abc123.jpg",
  "status": "PROCESSING",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Error Responses**:
- `400 Bad Request`: Invalid file format or size
- `401 Unauthorized`: Missing or invalid JWT token
- `413 Payload Too Large`: File exceeds 100MB

---

### 1.2 Get For You Feed
Get personalized video recommendations.

**Endpoint**: `GET /api/v1/videos/feed/foryou`

**Request**:
```http
GET /api/v1/videos/feed/foryou?userId=123&page=0&size=20
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**: `200 OK`
```json
{
  "videos": [
    {
      "videoId": 456789,
      "userId": 789,
      "username": "john_doe",
      "profilePictureUrl": "https://cdn.tiktok.com/avatars/john.jpg",
      "videoUrl": "https://cdn.tiktok.com/videos/xyz.mp4",
      "thumbnailUrl": "https://cdn.tiktok.com/thumbnails/xyz.jpg",
      "caption": "Check out this cool trick! #magic",
      "durationSeconds": 30,
      "viewCount": 1500000,
      "likeCount": 250000,
      "commentCount": 5000,
      "shareCount": 10000,
      "isLiked": false,
      "isFollowing": true
    }
  ],
  "nextCursor": "page_1"
}
```

---

### 1.3 Get Following Feed
Get videos from users you follow.

**Endpoint**: `GET /api/v1/videos/feed/following`

**Request**:
```http
GET /api/v1/videos/feed/following?userId=123&page=0&size=20
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**: Same as For You Feed

---

### 1.4 Like Video
Like a video.

**Endpoint**: `POST /api/v1/videos/{videoId}/like`

**Request**:
```http
POST /api/v1/videos/456789/like?userId=123
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**: `200 OK`

**Error Responses**:
- `400 Bad Request`: Already liked
- `404 Not Found`: Video not found

---

### 1.5 Unlike Video
Remove like from a video.

**Endpoint**: `DELETE /api/v1/videos/{videoId}/like`

**Request**:
```http
DELETE /api/v1/videos/456789/like?userId=123
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**: `200 OK`

---

### 1.6 Increment View Count
Track video view.

**Endpoint**: `POST /api/v1/videos/{videoId}/view`

**Request**:
```http
POST /api/v1/videos/456789/view
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**: `200 OK`

---

## 2. Live Streaming APIs

### 2.1 Create Live Stream
Create a new live stream session.

**Endpoint**: `POST /api/v1/live/create`

**Request**:
```http
POST /api/v1/live/create?userId=123
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "title": "Live Q&A Session",
  "description": "Ask me anything about coding!"
}
```

**Response**: `200 OK`
```json
{
  "streamId": 789,
  "streamKey": "abc123-def456-ghi789",
  "rtmpUrl": "rtmp://live.tiktok.com/live/abc123-def456-ghi789",
  "hlsUrl": "https://cdn.tiktok.com/live/abc123-def456-ghi789/index.m3u8",
  "status": "SCHEDULED",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Usage**:
```bash
# Start streaming with OBS Studio
Server: rtmp://live.tiktok.com/live
Stream Key: abc123-def456-ghi789
```

---

### 2.2 Start Live Stream
Mark stream as live (called automatically when RTMP connection established).

**Endpoint**: `POST /api/v1/live/start`

**Request**:
```http
POST /api/v1/live/start?streamKey=abc123-def456-ghi789
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**: `200 OK`

---

### 2.3 End Live Stream
End the live stream.

**Endpoint**: `POST /api/v1/live/end`

**Request**:
```http
POST /api/v1/live/end?streamKey=abc123-def456-ghi789
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**: `200 OK`

---

### 2.4 Get Active Live Streams
Get list of currently live streams.

**Endpoint**: `GET /api/v1/live/active`

**Request**:
```http
GET /api/v1/live/active
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**: `200 OK`
```json
{
  "streams": [
    {
      "streamId": 789,
      "userId": 123,
      "username": "john_doe",
      "profilePictureUrl": "https://cdn.tiktok.com/avatars/john.jpg",
      "title": "Live Q&A Session",
      "viewerCount": 5000,
      "likeCount": 1200,
      "hlsUrl": "https://cdn.tiktok.com/live/abc123/index.m3u8",
      "startedAt": "2024-01-15T10:30:00Z"
    }
  ]
}
```

---

### 2.5 Join Live Stream
Join a live stream as a viewer.

**Endpoint**: `POST /api/v1/live/{streamId}/join`

**Request**:
```http
POST /api/v1/live/789/join?userId=123
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**: `200 OK`

---

### 2.6 Leave Live Stream
Leave a live stream.

**Endpoint**: `POST /api/v1/live/{streamId}/leave`

**Request**:
```http
POST /api/v1/live/789/leave?userId=123
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**: `200 OK`

---

## 3. WebSocket API (Live Stream Interactions)

### 3.1 Connect to Live Stream
Establish WebSocket connection for real-time interactions.

**Endpoint**: `ws://api.tiktok.com/ws/live?streamId=789`

**Client Example**:
```javascript
const ws = new WebSocket('ws://api.tiktok.com/ws/live?streamId=789');

ws.onopen = () => {
  console.log('Connected to live stream');
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  
  switch(data.type) {
    case 'COMMENT':
      displayComment(data);
      break;
    case 'LIKE':
      updateLikeCount(data);
      break;
    case 'GIFT':
      displayGift(data);
      break;
    case 'VIEWER_COUNT':
      updateViewerCount(data.count);
      break;
  }
};

ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};

ws.onclose = () => {
  console.log('Disconnected from live stream');
};
```

---

### 3.2 Send Comment
Send a comment during live stream.

**Message Format**:
```json
{
  "type": "COMMENT",
  "userId": 123,
  "username": "john_doe",
  "message": "Great stream!",
  "timestamp": "2024-01-15T10:35:00Z"
}
```

**Client Code**:
```javascript
ws.send(JSON.stringify({
  type: 'COMMENT',
  userId: 123,
  username: 'john_doe',
  message: 'Great stream!'
}));
```

---

### 3.3 Send Like
Send a like during live stream.

**Message Format**:
```json
{
  "type": "LIKE",
  "userId": 123,
  "username": "john_doe",
  "timestamp": "2024-01-15T10:35:00Z"
}
```

**Client Code**:
```javascript
ws.send(JSON.stringify({
  type: 'LIKE',
  userId: 123
}));
```

---

### 3.4 Send Gift
Send a virtual gift during live stream.

**Message Format**:
```json
{
  "type": "GIFT",
  "userId": 123,
  "username": "john_doe",
  "giftId": 5,
  "giftName": "Rose",
  "giftValue": 100,
  "timestamp": "2024-01-15T10:35:00Z"
}
```

**Client Code**:
```javascript
ws.send(JSON.stringify({
  type: 'GIFT',
  userId: 123,
  giftId: 5,
  giftName: 'Rose',
  giftValue: 100
}));
```

---

## 4. Rate Limits

| Endpoint | Rate Limit |
|----------|-----------|
| Video Upload | 10 uploads/hour per user |
| Feed APIs | 100 requests/minute per user |
| Like/Unlike | 1000 requests/hour per user |
| Live Stream Create | 5 streams/day per user |
| WebSocket Messages | 100 messages/minute per user |

**Rate Limit Headers**:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642248000
```

---

## 5. Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 413 | Payload Too Large | File size exceeds limit |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Service temporarily unavailable |

**Error Response Format**:
```json
{
  "error": {
    "code": "INVALID_VIDEO_FORMAT",
    "message": "Video format must be MP4, MOV, or AVI",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

---

## 6. SDKs

### 6.1 JavaScript SDK
```javascript
import TikTokSDK from '@tiktok/sdk';

const client = new TikTokSDK({
  apiKey: 'your_api_key',
  apiSecret: 'your_api_secret'
});

// Upload video
const video = await client.videos.upload({
  file: videoFile,
  caption: 'Amazing dance moves!',
  isPublic: true
});

// Get feed
const feed = await client.videos.getForYouFeed({
  userId: 123,
  page: 0,
  size: 20
});

// Like video
await client.videos.like(videoId, userId);
```

### 6.2 Python SDK
```python
from tiktok_sdk import TikTokClient

client = TikTokClient(
    api_key='your_api_key',
    api_secret='your_api_secret'
)

# Upload video
video = client.videos.upload(
    file=open('video.mp4', 'rb'),
    caption='Amazing dance moves!',
    is_public=True
)

# Get feed
feed = client.videos.get_for_you_feed(
    user_id=123,
    page=0,
    size=20
)

# Like video
client.videos.like(video_id, user_id)
```

---

## 7. Webhooks

Subscribe to events via webhooks.

### 7.1 Video Processing Complete
Triggered when video transcoding is complete.

**Payload**:
```json
{
  "event": "video.processing.complete",
  "videoId": 123456,
  "userId": 789,
  "videoUrl": "https://cdn.tiktok.com/videos/abc123.mp4",
  "thumbnailUrl": "https://cdn.tiktok.com/thumbnails/abc123.jpg",
  "status": "READY",
  "timestamp": "2024-01-15T10:35:00Z"
}
```

### 7.2 Live Stream Started
Triggered when a live stream goes live.

**Payload**:
```json
{
  "event": "live.stream.started",
  "streamId": 789,
  "userId": 123,
  "hlsUrl": "https://cdn.tiktok.com/live/abc123/index.m3u8",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 8. Testing

### 8.1 Postman Collection
Import the Postman collection for easy API testing:
```
https://api.tiktok.com/postman/collection.json
```

### 8.2 Sandbox Environment
Test APIs in sandbox without affecting production data:
```
Base URL: https://api-sandbox.tiktok.com
Test User: test_user_123
Test Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test
```
