# Netflix Clone - API Documentation

## 📋 Overview

The Netflix Clone API provides comprehensive endpoints for video streaming, user management, content discovery, and personalized recommendations. All APIs follow RESTful principles with JSON request/response format.

**Base URL**: `http://localhost:8098/api/v1/netflix`

## 🔐 Authentication

Most endpoints require authentication via JWT tokens obtained through the login process.

### Headers
```http
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

## 👤 User Management APIs

### Register User
Create a new user account with subscription preferences.

```http
POST /auth/register
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "SecurePass123!",
  "name": "John Doe",
  "region": "US-EAST",
  "preferredGenres": ["Action", "Drama", "Sci-Fi"]
}
```

**Response: 200 OK**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john.doe@example.com",
  "name": "John Doe",
  "plan": "BASIC",
  "region": "US-EAST"
}
```

**Error Response: 400 Bad Request**
```json
{
  "error": "User already exists with email: john.doe@example.com"
}
```

### Login User
Authenticate user and receive JWT token for subsequent requests.

```http
POST /auth/login
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

**Response: 200 OK**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john.doe@example.com",
  "name": "John Doe",
  "plan": "BASIC",
  "region": "US-EAST",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response: 401 Unauthorized**
```json
{
  "error": "Invalid credentials"
}
```

### Update Subscription Plan
Upgrade or downgrade user subscription plan.

```http
PUT /users/{userId}/subscription
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "plan": "PREMIUM"
}
```

**Response: 200 OK**
```json
{
  "plan": "PREMIUM"
}
```

**Subscription Plans:**
- **BASIC**: 1 concurrent stream, 720p max quality
- **STANDARD**: 2 concurrent streams, 1080p max quality
- **PREMIUM**: 4 concurrent streams, 4K quality

## 🎬 Content Discovery APIs

### Get Personalized Recommendations
Retrieve AI-powered personalized content recommendations for a user.

```http
GET /content/recommendations/{userId}
Authorization: Bearer <jwt-token>
```

**Response: 200 OK**
```json
{
  "recommendations": [
    {
      "id": "content-123",
      "title": "The Matrix",
      "description": "A computer programmer discovers that reality as he knows it is a simulation controlled by cyber-criminals.",
      "type": "MOVIE",
      "genres": ["Action", "Sci-Fi"],
      "releaseYear": 1999,
      "durationMinutes": 136,
      "rating": "R",
      "imdbScore": 8.7,
      "thumbnailUrl": "https://cdn-us-east-1.netflix.com/images/matrix-thumb.jpg",
      "trailerUrl": "https://cdn-us-east-1.netflix.com/trailers/matrix-trailer.mp4",
      "cast": ["Keanu Reeves", "Laurence Fishburne", "Carrie-Anne Moss"],
      "directors": ["Lana Wachowski", "Lilly Wachowski"],
      "viewCount": 15420000
    },
    {
      "id": "content-456",
      "title": "Stranger Things",
      "description": "When a young boy vanishes, a small town uncovers a mystery involving secret experiments.",
      "type": "TV_SHOW",
      "genres": ["Drama", "Fantasy", "Horror"],
      "releaseYear": 2016,
      "seasons": 4,
      "episodes": 34,
      "rating": "TV-14",
      "imdbScore": 8.7,
      "thumbnailUrl": "https://cdn-us-east-1.netflix.com/images/stranger-things-thumb.jpg",
      "viewCount": 28750000
    }
  ],
  "totalCount": 50,
  "generatedAt": "2024-01-15T10:30:00Z",
  "algorithm": "hybrid",
  "weights": {
    "collaborative": 0.3,
    "contentBased": 0.4,
    "trending": 0.2,
    "newReleases": 0.1
  }
}
```

### Search Content
Search for movies and TV shows with filters.

```http
GET /content/search?query=avengers&genre=Action&year=2019
```

**Query Parameters:**
- `query` (optional): Search term for title matching
- `genre` (optional): Filter by genre
- `year` (optional): Filter by release year

**Response: 200 OK**
```json
{
  "results": [
    {
      "id": "content-789",
      "title": "Avengers: Endgame",
      "description": "The Avengers assemble once more to reverse Thanos' actions and restore balance to the universe.",
      "type": "MOVIE",
      "genres": ["Action", "Adventure", "Drama"],
      "releaseYear": 2019,
      "durationMinutes": 181,
      "rating": "PG-13",
      "imdbScore": 8.4,
      "thumbnailUrl": "https://cdn-us-east-1.netflix.com/images/endgame-thumb.jpg"
    }
  ],
  "totalResults": 1,
  "searchQuery": "avengers",
  "filters": {
    "genre": "Action",
    "year": 2019
  }
}
```

## 📺 Streaming APIs

### Start Streaming
Initialize video streaming session and get playback URLs.

```http
POST /stream/start?userId=550e8400-e29b-41d4-a716-446655440000&contentId=content-123&deviceType=Web
Authorization: Bearer <jwt-token>
```

**Query Parameters:**
- `userId`: User identifier
- `contentId`: Content identifier
- `deviceType`: Device type (Web, Mobile, TV, Tablet)
- `bandwidth` (optional): User's bandwidth in kbps for quality selection

**Response: 200 OK**
```json
{
  "contentId": "content-123",
  "title": "The Matrix",
  "duration": 8160,
  "resumePosition": 1800,
  "streamingUrls": {
    "360p": "https://cdn-us-east-1.netflix.com/content/123/360p/playlist.m3u8",
    "720p": "https://cdn-us-east-1.netflix.com/content/123/720p/playlist.m3u8",
    "1080p": "https://cdn-us-east-1.netflix.com/content/123/1080p/playlist.m3u8",
    "4K": "https://cdn-us-east-1.netflix.com/content/123/4k/playlist.m3u8"
  },
  "thumbnailUrl": "https://cdn-us-east-1.netflix.com/images/matrix-thumb.jpg",
  "subtitles": {
    "en": "https://cdn-us-east-1.netflix.com/content/123/subtitles/en.vtt",
    "es": "https://cdn-us-east-1.netflix.com/content/123/subtitles/es.vtt",
    "fr": "https://cdn-us-east-1.netflix.com/content/123/subtitles/fr.vtt"
  },
  "cdnServer": "cdn-us-east-1.netflix.com",
  "sessionId": "stream-session-789"
}
```

**Error Response: 404 Not Found**
```json
{
  "error": "Content not found"
}
```

### Update Watch Progress
Track user's viewing progress for resume functionality.

```http
POST /stream/progress?userId=550e8400-e29b-41d4-a716-446655440000&contentId=content-123&currentPosition=3600&quality=1080p
Authorization: Bearer <jwt-token>
```

**Query Parameters:**
- `userId`: User identifier
- `contentId`: Content identifier  
- `currentPosition`: Current playback position in seconds
- `quality`: Current streaming quality (360p, 720p, 1080p, 4K)

**Response: 200 OK**
```json
{
  "status": "progress updated",
  "currentPosition": 3600,
  "completionPercentage": 44.1,
  "isCompleted": false
}
```

### Get Resume Position
Retrieve the last watched position for content.

```http
GET /stream/resume/{userId}/{contentId}
Authorization: Bearer <jwt-token>
```

**Response: 200 OK**
```json
{
  "resumePosition": 1800,
  "completionPercentage": 22.1,
  "lastWatched": "2024-01-15T09:45:00Z",
  "deviceType": "Web",
  "qualityWatched": "1080p"
}
```

### Get Adaptive Streaming URL
Get optimal streaming URL based on user's bandwidth.

```http
GET /stream/adaptive?contentId=content-123&userId=550e8400-e29b-41d4-a716-446655440000&bandwidth=5000
Authorization: Bearer <jwt-token>
```

**Query Parameters:**
- `contentId`: Content identifier
- `userId`: User identifier
- `bandwidth`: Available bandwidth in kbps

**Response: 200 OK**
```json
{
  "streamUrl": "https://cdn-us-east-1.netflix.com/content/123/1080p/playlist.m3u8",
  "selectedQuality": "1080p",
  "bandwidthRequirement": 5000,
  "cdnServer": "cdn-us-east-1.netflix.com"
}
```

**Quality Selection Logic:**
- `bandwidth >= 25000 kbps` → 4K
- `bandwidth >= 5000 kbps` → 1080p  
- `bandwidth >= 3000 kbps` → 720p
- `bandwidth < 3000 kbps` → 360p

## 🔧 System APIs

### Health Check
Check service health and status.

```http
GET /health
```

**Response: 200 OK**
```json
{
  "status": "healthy",
  "service": "Netflix Clone",
  "timestamp": 1705312200000,
  "version": "1.0.0",
  "components": {
    "database": "healthy",
    "redis": "healthy",
    "cdn": "healthy"
  },
  "metrics": {
    "activeStreams": 15420,
    "totalUsers": 2500000,
    "cdnServers": 125,
    "averageLatency": "85ms"
  }
}
```

## 📊 Error Handling

### Standard Error Response Format
```json
{
  "error": "Error message description",
  "code": "ERROR_CODE",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/netflix/stream/start",
  "details": {
    "field": "validation error details"
  }
}
```

### HTTP Status Codes
- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Access denied
- `404 Not Found` - Resource not found
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - Server error

### Common Error Codes
- `USER_NOT_FOUND` - User does not exist
- `CONTENT_NOT_FOUND` - Content does not exist
- `INVALID_SUBSCRIPTION` - Subscription plan doesn't allow this action
- `STREAMING_LIMIT_EXCEEDED` - Too many concurrent streams
- `CDN_UNAVAILABLE` - No healthy CDN servers available
- `INVALID_QUALITY` - Requested quality not available for subscription

## 🚀 Rate Limiting

API endpoints are rate limited to prevent abuse:

- **Authentication**: 5 requests per minute per IP
- **Streaming**: 10 requests per minute per user
- **Recommendations**: 20 requests per minute per user
- **Search**: 30 requests per minute per user

**Rate Limit Headers:**
```http
X-RateLimit-Limit: 20
X-RateLimit-Remaining: 15
X-RateLimit-Reset: 1705312260
```

## 📱 SDK Examples

### JavaScript/Node.js
```javascript
const NetflixAPI = require('netflix-clone-sdk');

const client = new NetflixAPI({
  baseURL: 'http://localhost:8098/api/v1/netflix',
  apiKey: 'your-api-key'
});

// Login user
const loginResponse = await client.auth.login({
  email: 'user@example.com',
  password: 'password123'
});

// Get recommendations
const recommendations = await client.content.getRecommendations(
  loginResponse.userId,
  { token: loginResponse.token }
);

// Start streaming
const streamInfo = await client.streaming.start({
  userId: loginResponse.userId,
  contentId: 'content-123',
  deviceType: 'Web'
}, { token: loginResponse.token });
```

### Python
```python
import netflix_clone_sdk

client = netflix_clone_sdk.Client(
    base_url='http://localhost:8098/api/v1/netflix',
    api_key='your-api-key'
)

# Login user
login_response = client.auth.login(
    email='user@example.com',
    password='password123'
)

# Get recommendations
recommendations = client.content.get_recommendations(
    user_id=login_response['userId'],
    token=login_response['token']
)

# Start streaming
stream_info = client.streaming.start(
    user_id=login_response['userId'],
    content_id='content-123',
    device_type='Web',
    token=login_response['token']
)
```

### cURL Examples
```bash
# Register user
curl -X POST http://localhost:8098/api/v1/netflix/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "name": "John Doe",
    "region": "US-EAST"
  }'

# Login user
curl -X POST http://localhost:8098/api/v1/netflix/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'

# Get recommendations
curl -H "Authorization: Bearer <jwt-token>" \
  http://localhost:8098/api/v1/netflix/content/recommendations/user-123

# Start streaming
curl -X POST "http://localhost:8098/api/v1/netflix/stream/start?userId=user-123&contentId=content-456&deviceType=Web" \
  -H "Authorization: Bearer <jwt-token>"

# Search content
curl "http://localhost:8098/api/v1/netflix/content/search?query=matrix&genre=Action"
```

## 🔄 Webhooks (Future Enhancement)

### Streaming Events
```http
POST https://your-app.com/webhooks/netflix
Content-Type: application/json
X-Netflix-Signature: sha256=...

{
  "event": "streaming.started",
  "userId": "user-123",
  "contentId": "content-456",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": {
    "deviceType": "Web",
    "quality": "1080p",
    "cdnServer": "cdn-us-east-1.netflix.com"
  }
}
```

### Watch Progress Events
```http
POST https://your-app.com/webhooks/netflix
Content-Type: application/json

{
  "event": "watch.completed",
  "userId": "user-123", 
  "contentId": "content-456",
  "timestamp": "2024-01-15T12:15:00Z",
  "data": {
    "watchDuration": 8160,
    "completionPercentage": 100,
    "deviceType": "Web"
  }
}
```

---

This API documentation provides comprehensive coverage of all Netflix Clone endpoints with detailed examples and integration patterns for building client applications.