# Instagram Clone - API Documentation

## Base URL
```
Production: https://api.instagram-clone.com/v1
Staging: https://staging-api.instagram-clone.com/v1
Development: http://localhost:8087/api/v1
```

## Authentication
All authenticated endpoints require a Bearer token in the Authorization header:
```
Authorization: Bearer <access_token>
```

## Response Format
All API responses follow this standard format:
```json
{
  "success": true,
  "data": {},
  "message": "Success",
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req_123456789"
}
```

Error responses:
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input parameters",
    "details": ["Username is required", "Email format is invalid"]
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req_123456789"
}
```

## 1. User Management APIs

### 1.1 User Registration
```http
POST /users/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe",
  "dateOfBirth": "1990-01-15"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": 12345,
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "refresh_token_here",
    "expiresIn": 3600
  }
}
```

### 1.2 User Login
```http
POST /users/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": 12345,
    "username": "johndoe",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "refresh_token_here",
    "expiresIn": 3600,
    "profile": {
      "fullName": "John Doe",
      "profilePictureUrl": "https://cdn.instagram-clone.com/profiles/12345.jpg",
      "isVerified": false,
      "followerCount": 150,
      "followingCount": 200,
      "postCount": 45
    }
  }
}
```

### 1.3 Get User Profile
```http
GET /users/{userId}
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": 12345,
    "username": "johndoe",
    "fullName": "John Doe",
    "bio": "Photography enthusiast 📸 | Travel lover ✈️",
    "profilePictureUrl": "https://cdn.instagram-clone.com/profiles/12345.jpg",
    "isVerified": false,
    "isPrivate": false,
    "followerCount": 1250,
    "followingCount": 890,
    "postCount": 156,
    "isFollowing": false,
    "isFollowedBy": false,
    "createdAt": "2023-01-15T10:30:00Z"
  }
}
```

### 1.4 Update User Profile
```http
PUT /users/profile
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "fullName": "John Doe Updated",
  "bio": "Updated bio text",
  "isPrivate": true
}
```

### 1.5 Follow User
```http
POST /users/{userId}/follow
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "isFollowing": true,
    "followerCount": 1251
  }
}
```

### 1.6 Unfollow User
```http
DELETE /users/{userId}/follow
Authorization: Bearer <access_token>
```

### 1.7 Get Followers
```http
GET /users/{userId}/followers?page=0&size=20
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "users": [
      {
        "userId": 67890,
        "username": "janedoe",
        "fullName": "Jane Doe",
        "profilePictureUrl": "https://cdn.instagram-clone.com/profiles/67890.jpg",
        "isVerified": true,
        "isFollowing": false
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 1250,
      "totalPages": 63,
      "hasNext": true
    }
  }
}
```

### 1.8 Get Following
```http
GET /users/{userId}/following?page=0&size=20
Authorization: Bearer <access_token>
```

## 2. Post Management APIs

### 2.1 Create Post
```http
POST /posts
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "content": "Beautiful sunset at the beach! 🌅 #sunset #beach #photography",
  "mediaUrls": [
    "https://cdn.instagram-clone.com/media/post_123_1.jpg",
    "https://cdn.instagram-clone.com/media/post_123_2.jpg"
  ],
  "location": "Malibu Beach, CA",
  "hashtags": ["sunset", "beach", "photography"]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "postId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": 12345,
    "content": "Beautiful sunset at the beach! 🌅 #sunset #beach #photography",
    "mediaUrls": [
      "https://cdn.instagram-clone.com/media/post_123_1.jpg",
      "https://cdn.instagram-clone.com/media/post_123_2.jpg"
    ],
    "location": "Malibu Beach, CA",
    "hashtags": ["sunset", "beach", "photography"],
    "likeCount": 0,
    "commentCount": 0,
    "shareCount": 0,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

### 2.2 Get Post
```http
GET /posts/{postId}
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "postId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": 12345,
    "username": "johndoe",
    "userProfilePicture": "https://cdn.instagram-clone.com/profiles/12345.jpg",
    "isVerified": false,
    "content": "Beautiful sunset at the beach! 🌅 #sunset #beach #photography",
    "mediaUrls": [
      "https://cdn.instagram-clone.com/media/post_123_1.jpg",
      "https://cdn.instagram-clone.com/media/post_123_2.jpg"
    ],
    "location": "Malibu Beach, CA",
    "hashtags": ["sunset", "beach", "photography"],
    "likeCount": 245,
    "commentCount": 18,
    "shareCount": 12,
    "isLiked": false,
    "isSaved": false,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

### 2.3 Update Post
```http
PUT /posts/{postId}
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "content": "Updated caption for the sunset post! 🌅 #sunset #beach",
  "location": "Santa Monica Beach, CA"
}
```

### 2.4 Delete Post
```http
DELETE /posts/{postId}
Authorization: Bearer <access_token>
```

### 2.5 Like Post
```http
POST /posts/{postId}/like
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "isLiked": true,
    "likeCount": 246
  }
}
```

### 2.6 Unlike Post
```http
DELETE /posts/{postId}/like
Authorization: Bearer <access_token>
```

### 2.7 Get Post Likes
```http
GET /posts/{postId}/likes?page=0&size=20
Authorization: Bearer <access_token>
```

### 2.8 Save Post
```http
POST /posts/{postId}/save
Authorization: Bearer <access_token>
```

### 2.9 Get User Posts
```http
GET /users/{userId}/posts?page=0&size=20
Authorization: Bearer <access_token>
```

## 3. Comment APIs

### 3.1 Add Comment
```http
POST /posts/{postId}/comments
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "content": "Amazing shot! 📸",
  "parentCommentId": null
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "commentId": "comment_123456",
    "postId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": 67890,
    "username": "janedoe",
    "userProfilePicture": "https://cdn.instagram-clone.com/profiles/67890.jpg",
    "content": "Amazing shot! 📸",
    "likeCount": 0,
    "replyCount": 0,
    "parentCommentId": null,
    "createdAt": "2024-01-15T11:00:00Z"
  }
}
```

### 3.2 Get Comments
```http
GET /posts/{postId}/comments?page=0&size=20&sort=createdAt,desc
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "comments": [
      {
        "commentId": "comment_123456",
        "userId": 67890,
        "username": "janedoe",
        "userProfilePicture": "https://cdn.instagram-clone.com/profiles/67890.jpg",
        "isVerified": true,
        "content": "Amazing shot! 📸",
        "likeCount": 5,
        "replyCount": 2,
        "isLiked": false,
        "parentCommentId": null,
        "createdAt": "2024-01-15T11:00:00Z",
        "replies": [
          {
            "commentId": "comment_123457",
            "userId": 12345,
            "username": "johndoe",
            "content": "Thank you! 😊",
            "likeCount": 1,
            "createdAt": "2024-01-15T11:05:00Z"
          }
        ]
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 18,
      "hasNext": false
    }
  }
}
```

### 3.3 Like Comment
```http
POST /comments/{commentId}/like
Authorization: Bearer <access_token>
```

### 3.4 Delete Comment
```http
DELETE /comments/{commentId}
Authorization: Bearer <access_token>
```

## 4. Feed APIs

### 4.1 Get News Feed
```http
GET /feed?page=0&size=20&cursor=eyJjcmVhdGVkQXQiOiIyMDI0LTAxLTE1VDEwOjMwOjAwWiJ9
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "posts": [
      {
        "postId": "550e8400-e29b-41d4-a716-446655440000",
        "userId": 12345,
        "username": "johndoe",
        "userProfilePicture": "https://cdn.instagram-clone.com/profiles/12345.jpg",
        "isVerified": false,
        "content": "Beautiful sunset at the beach! 🌅",
        "mediaUrls": ["https://cdn.instagram-clone.com/media/post_123_1.jpg"],
        "location": "Malibu Beach, CA",
        "likeCount": 245,
        "commentCount": 18,
        "shareCount": 12,
        "isLiked": false,
        "isSaved": false,
        "createdAt": "2024-01-15T10:30:00Z"
      }
    ],
    "pagination": {
      "hasNext": true,
      "nextCursor": "eyJjcmVhdGVkQXQiOiIyMDI0LTAxLTE1VDA5OjMwOjAwWiJ9"
    }
  }
}
```

### 4.2 Get Explore Feed
```http
GET /feed/explore?page=0&size=20
Authorization: Bearer <access_token>
```

## 5. Media Upload APIs

### 5.1 Upload Media
```http
POST /media/upload
Authorization: Bearer <access_token>
Content-Type: multipart/form-data

file: <binary_file_data>
mediaType: image
```

**Response:**
```json
{
  "success": true,
  "data": {
    "mediaId": "media_123456789",
    "originalUrl": "https://cdn.instagram-clone.com/media/original/media_123456789.jpg",
    "thumbnailUrl": "https://cdn.instagram-clone.com/media/thumbnails/media_123456789_150x150.jpg",
    "mediaType": "image",
    "size": 2048576,
    "dimensions": {
      "width": 1080,
      "height": 1080
    },
    "uploadedAt": "2024-01-15T10:25:00Z"
  }
}
```

### 5.2 Get Media Processing Status
```http
GET /media/{mediaId}/status
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "mediaId": "media_123456789",
    "status": "completed",
    "processingProgress": 100,
    "availableFormats": [
      {
        "format": "original",
        "url": "https://cdn.instagram-clone.com/media/original/media_123456789.jpg",
        "dimensions": {"width": 1080, "height": 1080}
      },
      {
        "format": "thumbnail_150",
        "url": "https://cdn.instagram-clone.com/media/thumbnails/media_123456789_150x150.jpg",
        "dimensions": {"width": 150, "height": 150}
      }
    ]
  }
}
```

## 6. Search APIs

### 6.1 Search Users
```http
GET /search/users?q=john&page=0&size=20
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "users": [
      {
        "userId": 12345,
        "username": "johndoe",
        "fullName": "John Doe",
        "profilePictureUrl": "https://cdn.instagram-clone.com/profiles/12345.jpg",
        "isVerified": false,
        "followerCount": 1250,
        "isFollowing": false
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 45,
      "hasNext": true
    }
  }
}
```

### 6.2 Search Posts
```http
GET /search/posts?q=sunset&hashtags=beach,photography&page=0&size=20
Authorization: Bearer <access_token>
```

### 6.3 Search Hashtags
```http
GET /search/hashtags?q=sun&page=0&size=20
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "hashtags": [
      {
        "name": "sunset",
        "postCount": 125000,
        "isFollowing": false
      },
      {
        "name": "sunshine",
        "postCount": 89000,
        "isFollowing": true
      }
    ]
  }
}
```

### 6.4 Get Search Suggestions
```http
GET /search/suggestions?q=jo
Authorization: Bearer <access_token>
```

## 7. Story APIs

### 7.1 Create Story
```http
POST /stories
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "mediaUrl": "https://cdn.instagram-clone.com/media/story_123.jpg",
  "mediaType": "image",
  "duration": 15,
  "backgroundColor": "#FF5733"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "storyId": "story_123456789",
    "userId": 12345,
    "mediaUrl": "https://cdn.instagram-clone.com/media/story_123.jpg",
    "mediaType": "image",
    "duration": 15,
    "viewCount": 0,
    "createdAt": "2024-01-15T10:30:00Z",
    "expiresAt": "2024-01-16T10:30:00Z"
  }
}
```

### 7.2 Get User Stories
```http
GET /users/{userId}/stories
Authorization: Bearer <access_token>
```

### 7.3 Get Story Feed
```http
GET /stories/feed
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "storyGroups": [
      {
        "userId": 12345,
        "username": "johndoe",
        "profilePictureUrl": "https://cdn.instagram-clone.com/profiles/12345.jpg",
        "hasUnseenStories": true,
        "stories": [
          {
            "storyId": "story_123456789",
            "mediaUrl": "https://cdn.instagram-clone.com/media/story_123.jpg",
            "mediaType": "image",
            "duration": 15,
            "viewCount": 45,
            "isViewed": false,
            "createdAt": "2024-01-15T10:30:00Z"
          }
        ]
      }
    ]
  }
}
```

### 7.4 View Story
```http
POST /stories/{storyId}/view
Authorization: Bearer <access_token>
```

### 7.5 Get Story Views
```http
GET /stories/{storyId}/views?page=0&size=20
Authorization: Bearer <access_token>
```

## 8. Messaging APIs

### 8.1 Get Conversations
```http
GET /messages/conversations?page=0&size=20
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "conversations": [
      {
        "conversationId": "conv_123456789",
        "participants": [
          {
            "userId": 12345,
            "username": "johndoe",
            "profilePictureUrl": "https://cdn.instagram-clone.com/profiles/12345.jpg"
          },
          {
            "userId": 67890,
            "username": "janedoe",
            "profilePictureUrl": "https://cdn.instagram-clone.com/profiles/67890.jpg"
          }
        ],
        "lastMessage": {
          "messageId": "msg_987654321",
          "senderId": 67890,
          "content": "Hey, how are you?",
          "messageType": "text",
          "timestamp": "2024-01-15T10:30:00Z",
          "isRead": false
        },
        "unreadCount": 2,
        "updatedAt": "2024-01-15T10:30:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "hasNext": false
    }
  }
}
```

### 8.2 Get Messages
```http
GET /messages/conversations/{conversationId}?page=0&size=50
Authorization: Bearer <access_token>
```

### 8.3 Send Message
```http
POST /messages/conversations/{conversationId}
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "content": "Hello! How are you doing?",
  "messageType": "text"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "messageId": "msg_123456789",
    "conversationId": "conv_123456789",
    "senderId": 12345,
    "content": "Hello! How are you doing?",
    "messageType": "text",
    "deliveryStatus": "sent",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

### 8.4 Mark Messages as Read
```http
PUT /messages/conversations/{conversationId}/read
Authorization: Bearer <access_token>
```

## 9. Notification APIs

### 9.1 Get Notifications
```http
GET /notifications?page=0&size=20&type=all
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "notifications": [
      {
        "notificationId": "notif_123456789",
        "type": "POST_LIKED",
        "title": "New Like",
        "message": "janedoe liked your post",
        "data": {
          "postId": "550e8400-e29b-41d4-a716-446655440000",
          "userId": 67890,
          "username": "janedoe",
          "profilePictureUrl": "https://cdn.instagram-clone.com/profiles/67890.jpg"
        },
        "isRead": false,
        "createdAt": "2024-01-15T10:30:00Z"
      }
    ],
    "unreadCount": 5,
    "pagination": {
      "page": 0,
      "size": 20,
      "hasNext": true
    }
  }
}
```

### 9.2 Mark Notification as Read
```http
PUT /notifications/{notificationId}/read
Authorization: Bearer <access_token>
```

### 9.3 Mark All Notifications as Read
```http
PUT /notifications/read-all
Authorization: Bearer <access_token>
```

## 10. Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Invalid input parameters |
| `UNAUTHORIZED` | 401 | Invalid or missing authentication token |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `CONFLICT` | 409 | Resource already exists |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Internal server error |
| `SERVICE_UNAVAILABLE` | 503 | Service temporarily unavailable |

## 11. Rate Limits

| Endpoint Category | Limit | Window |
|-------------------|-------|--------|
| Authentication | 10 requests | 1 minute |
| User Operations | 100 requests | 1 hour |
| Post Operations | 200 requests | 1 hour |
| Media Upload | 50 uploads | 1 hour |
| Search | 300 requests | 1 hour |
| Messaging | 1000 requests | 1 hour |

## 12. WebSocket Events

### Connection
```javascript
// Connect to WebSocket
const ws = new WebSocket('wss://api.instagram-clone.com/ws');

// Authentication
ws.send(JSON.stringify({
  type: 'auth',
  token: 'your_access_token'
}));
```

### Real-time Events
```javascript
// New message received
{
  "type": "NEW_MESSAGE",
  "data": {
    "messageId": "msg_123456789",
    "conversationId": "conv_123456789",
    "senderId": 67890,
    "content": "Hello!",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}

// New notification
{
  "type": "NEW_NOTIFICATION",
  "data": {
    "notificationId": "notif_123456789",
    "type": "POST_LIKED",
    "message": "janedoe liked your post",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}

// User online status
{
  "type": "USER_STATUS",
  "data": {
    "userId": 67890,
    "status": "online",
    "lastSeen": "2024-01-15T10:30:00Z"
  }
}
```

This API documentation provides comprehensive coverage of all Instagram clone features with detailed request/response examples and proper error handling.