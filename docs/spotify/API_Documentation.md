# Spotify API Documentation

## Base URL
```
http://localhost:8098/api/v1
```

## Authentication
All endpoints require JWT token in Authorization header:
```
Authorization: Bearer <jwt_token>
```

## Track Management APIs

### 1. Upload Track
Upload a new track with audio file.

**Endpoint:** `POST /tracks/upload`

**Request:**
```bash
curl -X POST http://localhost:8098/api/v1/tracks/upload \
  -H "Authorization: Bearer <token>" \
  -F "metadata={
    \"title\": \"Bohemian Rhapsody\",
    \"artistId\": \"artist-123\",
    \"albumId\": \"album-456\",
    \"isrc\": \"GBUM71029604\",
    \"durationMs\": 354000,
    \"trackType\": \"SONG\",
    \"isExplicit\": false,
    \"genre\": \"Rock\",
    \"language\": \"English\"
  }" \
  -F "audioFile=@song.mp3"
```

**Response:** `201 Created`
```json
{
  "id": "track-789",
  "title": "Bohemian Rhapsody",
  "artistId": "artist-123",
  "albumId": "album-456",
  "durationMs": 354000,
  "genre": "Rock",
  "playCount": 0,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### 2. Get Track
Retrieve track metadata.

**Endpoint:** `GET /tracks/{trackId}`

**Response:** `200 OK`
```json
{
  "id": "track-789",
  "title": "Bohemian Rhapsody",
  "artistId": "artist-123",
  "artistName": "Queen",
  "albumId": "album-456",
  "albumName": "A Night at the Opera",
  "durationMs": 354000,
  "genre": "Rock",
  "isExplicit": false,
  "playCount": 1500000000,
  "coverImageUrl": "https://cdn.spotify.com/covers/album-456.jpg",
  "releaseDate": "1975-10-31T00:00:00Z"
}
```

### 3. Search Tracks
Search for tracks by query.

**Endpoint:** `GET /tracks/search?query={query}`

**Example:**
```bash
curl "http://localhost:8098/api/v1/tracks/search?query=bohemian"
```

**Response:** `200 OK`
```json
[
  {
    "id": "track-789",
    "title": "Bohemian Rhapsody",
    "artistName": "Queen",
    "albumName": "A Night at the Opera",
    "durationMs": 354000,
    "genre": "Rock",
    "playCount": 1500000000
  }
]
```

### 4. Get Top Tracks
Get most played tracks.

**Endpoint:** `GET /tracks/top?page=0&size=20`

**Response:** `200 OK`
```json
{
  "content": [...],
  "totalElements": 1000,
  "totalPages": 50,
  "size": 20,
  "number": 0
}
```

### 5. Get Artist Tracks
Get all tracks by an artist.

**Endpoint:** `GET /tracks/artist/{artistId}?page=0&size=20`

### 6. Update Track
Update track metadata.

**Endpoint:** `PUT /tracks/{trackId}`

**Request:**
```json
{
  "title": "Bohemian Rhapsody (Remastered)",
  "genre": "Classic Rock",
  "lyrics": "Is this the real life..."
}
```

### 7. Delete Track
Soft delete a track.

**Endpoint:** `DELETE /tracks/{trackId}`

**Response:** `204 No Content`

## Streaming APIs

### 1. Stream Track
Stream audio file.

**Endpoint:** `POST /stream`

**Request:**
```json
{
  "trackId": "track-789",
  "userId": "user-123",
  "deviceId": "device-456",
  "audioQuality": "HIGH",
  "isOffline": false
}
```

**Response:** `200 OK`
```
Content-Type: audio/mpeg
Content-Length: 8388608
Accept-Ranges: bytes

<binary audio data>
```

**Audio Quality Options:**
- `LOW`: 96 kbps (Free tier)
- `MEDIUM`: 160 kbps (Premium)
- `HIGH`: 320 kbps (Premium)
- `LOSSLESS`: FLAC (HiFi tier)

## Playlist APIs

### 1. Create Playlist
Create a new playlist.

**Endpoint:** `POST /playlists?userId={userId}&name={name}&description={desc}&isPublic={true}`

**Example:**
```bash
curl -X POST "http://localhost:8098/api/v1/playlists?userId=user-123&name=My%20Favorites&isPublic=true" \
  -H "Authorization: Bearer <token>"
```

**Response:** `201 Created`
```json
{
  "id": "playlist-789",
  "name": "My Favorites",
  "userId": "user-123",
  "isPublic": true,
  "trackCount": 0,
  "followerCount": 0,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### 2. Add Track to Playlist
Add a track to playlist.

**Endpoint:** `POST /playlists/{playlistId}/tracks?trackId={trackId}&userId={userId}`

**Response:** `200 OK`

### 3. Remove Track from Playlist
Remove a track from playlist.

**Endpoint:** `DELETE /playlists/{playlistId}/tracks/{trackId}`

**Response:** `204 No Content`

### 4. Get User Playlists
Get all playlists created by user.

**Endpoint:** `GET /playlists/user/{userId}`

**Response:** `200 OK`
```json
[
  {
    "id": "playlist-789",
    "name": "My Favorites",
    "trackCount": 50,
    "followerCount": 10,
    "isPublic": true
  }
]
```

### 5. Get Playlist Tracks
Get all tracks in a playlist.

**Endpoint:** `GET /playlists/{playlistId}/tracks`

**Response:** `200 OK`
```json
[
  {
    "id": "pt-1",
    "playlistId": "playlist-789",
    "trackId": "track-123",
    "position": 0,
    "addedBy": "user-123",
    "addedAt": "2024-01-15T10:30:00Z"
  }
]
```

## Library APIs

### 1. Add to Library
Add track/album/artist to user's library.

**Endpoint:** `POST /library?userId={userId}&entityId={entityId}&entityType={type}`

**Entity Types:** `TRACK`, `ALBUM`, `PLAYLIST`, `ARTIST`, `PODCAST`

**Example:**
```bash
curl -X POST "http://localhost:8098/api/v1/library?userId=user-123&entityId=track-789&entityType=TRACK" \
  -H "Authorization: Bearer <token>"
```

**Response:** `200 OK`

### 2. Remove from Library
Remove from user's library.

**Endpoint:** `DELETE /library?userId={userId}&entityId={entityId}&entityType={type}`

**Response:** `204 No Content`

### 3. Get User Library
Get user's saved items.

**Endpoint:** `GET /library?userId={userId}&entityType={type}`

**Response:** `200 OK`
```json
[
  {
    "id": "lib-1",
    "userId": "user-123",
    "entityType": "TRACK",
    "entityId": "track-789",
    "addedAt": "2024-01-15T10:30:00Z"
  }
]
```

## Download APIs

### 1. Download Track
Download track for offline listening.

**Endpoint:** `POST /downloads?userId={userId}&trackId={trackId}&deviceId={deviceId}&quality={quality}`

**Quality Options:** `LOW`, `MEDIUM`, `HIGH`, `LOSSLESS`

**Example:**
```bash
curl -X POST "http://localhost:8098/api/v1/downloads?userId=user-123&trackId=track-789&deviceId=device-456&quality=HIGH" \
  -H "Authorization: Bearer <token>"
```

**Response:** `200 OK`
```json
{
  "id": "download-1",
  "userId": "user-123",
  "trackId": "track-789",
  "deviceId": "device-456",
  "audioQuality": "HIGH",
  "downloadedAt": "2024-01-15T10:30:00Z",
  "fileSizeBytes": 8388608
}
```

### 2. Get User Downloads
Get all downloaded tracks for a device.

**Endpoint:** `GET /downloads?userId={userId}&deviceId={deviceId}`

**Response:** `200 OK`
```json
[
  {
    "id": "download-1",
    "trackId": "track-789",
    "audioQuality": "HIGH",
    "downloadedAt": "2024-01-15T10:30:00Z",
    "fileSizeBytes": 8388608
  }
]
```

### 3. Delete Download
Remove downloaded track.

**Endpoint:** `DELETE /downloads?userId={userId}&trackId={trackId}&deviceId={deviceId}`

**Response:** `204 No Content`

## Error Responses

### 400 Bad Request
```json
{
  "error": "BAD_REQUEST",
  "message": "Invalid audio quality specified",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 401 Unauthorized
```json
{
  "error": "UNAUTHORIZED",
  "message": "Invalid or expired token",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 403 Forbidden
```json
{
  "error": "FORBIDDEN",
  "message": "Premium subscription required for high quality streaming",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 404 Not Found
```json
{
  "error": "NOT_FOUND",
  "message": "Track not found",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 429 Too Many Requests
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again later",
  "retryAfter": 60,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 500 Internal Server Error
```json
{
  "error": "INTERNAL_SERVER_ERROR",
  "message": "An unexpected error occurred",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Rate Limits

| Endpoint | Rate Limit |
|----------|------------|
| Search | 100 requests/minute |
| Stream | 1000 requests/hour |
| Upload | 10 requests/hour |
| Playlist Operations | 200 requests/minute |
| Library Operations | 200 requests/minute |

## Pagination

All list endpoints support pagination:
```
GET /tracks/top?page=0&size=20&sort=playCount,desc
```

**Parameters:**
- `page`: Page number (0-indexed)
- `size`: Items per page (max 100)
- `sort`: Sort field and direction

**Response:**
```json
{
  "content": [...],
  "totalElements": 1000,
  "totalPages": 50,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```
